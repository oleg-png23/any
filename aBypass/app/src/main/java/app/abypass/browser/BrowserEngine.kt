package app.abypass.browser

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.abypass.data.AppSettings
import app.abypass.data.BrowserUiState
import app.abypass.data.CheckStatus
import app.abypass.data.ServiceDefinition
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse

private object RuntimeHolder {
    var runtime: GeckoRuntime? = null
    fun get(context: Context): GeckoRuntime = runtime ?: GeckoRuntime.create(
        context.applicationContext,
        GeckoRuntimeSettings.Builder().consoleOutput(false).remoteDebuggingEnabled(false).build(),
    ).also { runtime = it }
}

/** The UI, browser profile and compatibility adapter have separate lifetimes. */
class BrowserEngine(
    private val context: Context,
    private val settings: () -> AppSettings,
    private val onRecord: (String, CheckStatus) -> Unit,
    private val onEvent: (String, String, Boolean) -> Unit,
) {
    var state by mutableStateOf(BrowserUiState())
        private set
    var activeSession by mutableStateOf<GeckoSession?>(null)
        private set

    private val sessions = mutableMapOf<String, GeckoSession>()
    private val owners = mutableMapOf<GeckoSession, String>()
    private val openers = mutableMapOf<GeckoSession, GeckoSession>()
    private val snapshots = mutableMapOf<GeckoSession, BrowserUiState>()
    private val ports = mutableMapOf<WebExtension.Port, String>()
    private val sentConfigs = mutableMapOf<WebExtension.Port, Boolean>()
    private var extension: WebExtension? = null
    private var loadingExtension = false
    private var pending: ServiceDefinition? = null
    private var clearingData = false
    private var generation = 0L
    private var pendingGeneration = 0L
    private val needsReload = mutableSetOf<GeckoSession>()
    var promptDelegate: GeckoSession.PromptDelegate? = null
        set(value) { field = value; owners.keys.forEach { it.promptDelegate = value } }
    var onDownload: ((WebResponse) -> Unit)? = null

    fun open(service: ServiceDefinition) {
        if (clearingData) return
        pendingGeneration = generation
        if (loadingExtension) { pending = service; state = BrowserUiState(serviceId = service.id, loading = true); return }
        if (extension == null) {
            pending = service
            state = BrowserUiState(serviceId = service.id, loading = true)
            loadingExtension = true
            RuntimeHolder.get(context).webExtensionController.ensureBuiltIn(
                "resource://android/assets/compat/", "abypass-compat@app.abypass",
            ).accept({ value ->
                loadingExtension = false
                extension = value
                owners.forEach { (session, id) ->
                    attachAdapter(session, id)
                    change(session) { it.copy(extensionReady = true) }
                    needsReload += session
                }
                val next = pending
                pending = null
                if (next != null && !clearingData && pendingGeneration == generation) openSession(next)
            }, {
                loadingExtension = false
                val next = pending
                pending = null
                if (next != null && !clearingData && pendingGeneration == generation) {
                    onEvent(next.id, "Не удалось включить адаптер. Доступен обычный режим просмотра.", false)
                    openSession(next)
                }
            })
        } else openSession(service)
    }

    private fun openSession(service: ServiceDefinition) {
        val session = sessions[service.id] ?: createSession(service.id).also {
            sessions[service.id] = it
            it.open(RuntimeHolder.get(context))
            activate(it)
            it.loadUri(service.url)
        }
        activate(session)
        if (needsReload.remove(session)) session.reload()
    }

    private fun createSession(id: String): GeckoSession {
        val session = GeckoSession(GeckoSessionSettings.Builder()
            .userAgentMode(if (settings().desktopMode) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(if (settings().desktopMode) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .build())
        owners[session] = id
        snapshots[session] = BrowserUiState(serviceId = id, extensionReady = extension != null)
        session.promptDelegate = promptDelegate
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?, perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) {
                change(session) { it.copy(url = url.orEmpty()) }
                ports.keys.filter { it.sender.session === session }.forEach { port ->
                    val adapterId = UrlRules.adapterService(url.orEmpty()).orEmpty()
                    ports[port] = adapterId
                    sendConfig(port, adapterId)
                }
            }
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) { change(session) { it.copy(canGoBack = canGoBack) } }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) { change(session) { it.copy(canGoForward = canGoForward) } }
            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                val scheme = Uri.parse(request.uri).scheme
                return if (scheme in setOf("https", "about", "blob", "data")) null else GeckoResult.fromValue(AllowOrDeny.DENY)
            }
            override fun onLoadError(session: GeckoSession, uri: String?, error: WebRequestError): GeckoResult<String>? {
                if (session !in owners) return null
                change(session) { it.copy(loading = false, error = "Не удалось загрузить страницу. Проверьте подключение и повторите.") }
                onRecord(id, CheckStatus.NETWORK_ERROR)
                onEvent(id, "Ошибка загрузки страницы (код ${error.code}).", false)
                return null
            }
            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                if (session !in owners || clearingData) return null
                val child = createSession(id)
                openers[child] = session
                activate(child)
                // Gecko opens this new session itself and preserves window.opener.
                return GeckoResult.fromValue(child)
            }
        }
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                if (session === activeSession && session.isOpen) session.setActive(true)
                change(session) { it.copy(url = url, loading = true, error = null, progress = 0) }
            }
            override fun onProgressChange(session: GeckoSession, progress: Int) { change(session) { it.copy(progress = progress.coerceIn(0, 100)) } }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                val snapshot = snapshots[session] ?: return
                change(session) { it.copy(loading = false, progress = 100) }
                if (success) {
                    val host = Uri.parse(snapshot.url).host.orEmpty()
                    if (host == "accounts.google.com") {
                        onRecord(id, CheckStatus.SIGN_IN_REQUIRED)
                        onEvent(id, "Открыта страница входа Google.", false)
                    } else if (UrlRules.isServicePage(id, snapshot.url)) {
                        onRecord(id, CheckStatus.PAGE_OPENED)
                        onEvent(id, "Страница открылась. Работа функций ещё не проверена.", false)
                    }
                }
            }
        }
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) { change(session) { it.copy(title = title.orEmpty()) } }
            override fun onCloseRequest(session: GeckoSession) { closePopup(session) }
            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                val handler = onDownload
                if (session in owners && session === activeSession && handler != null) handler(response)
                else response.body?.close()
            }
        }
        attachAdapter(session, id)
        return session
    }

    private fun attachAdapter(session: GeckoSession, id: String) {
        extension?.let { adapter ->
            session.webExtensionController.setMessageDelegate(adapter, object : WebExtension.MessageDelegate {
                override fun onConnect(port: WebExtension.Port) {
                    val sender = port.sender
                    val uri = Uri.parse(sender.url)
                    if (sender.session !== session || !sender.isTopLevel || uri.scheme != "https" || uri.host !in setOf("flow.google.com", "labs.google")) {
                        port.disconnect()
                        return
                    }
                    val adapterId = UrlRules.adapterService(sender.url).orEmpty()
                    ports[port] = adapterId
                    port.setDelegate(object : WebExtension.PortDelegate {
                        override fun onDisconnect(port: WebExtension.Port) { ports.remove(port); sentConfigs.remove(port) }
                        override fun onPortMessage(message: Any, port: WebExtension.Port) {
                            if (session !in owners) return
                            val currentAdapterId = ports[port]?.takeIf { it.isNotEmpty() } ?: return
                            val data = message as? JSONObject ?: return
                            if (data.optString("type") != "diagnostic") return
                            when (data.optString("event")) {
                                "ready" -> onEvent(currentAdapterId, "Адаптер готов к проверке региональной совместимости.", false)
                                "config-enabled" -> onEvent(currentAdapterId, "Режим совместимости включён в текущей странице.", false)
                                "config-disabled" -> onEvent(currentAdapterId, "Режим совместимости выключен в текущей странице.", false)
                                "patched" -> onEvent(currentAdapterId, "Применена региональная коррекция. Это не подтверждает работу сервиса.", true)
                                "schema-mismatch" -> {
                                    val reason = when (data.optString("detail")) {
                                        "country-field-type" -> "тип географического поля"
                                        "json-format" -> "JSON или предел вложенности"
                                        "batch-fields" -> "поля RPC"
                                        "batch-framing" -> "длина или формат кадров RPC"
                                        "size-limit" -> "предел размера ответа"
                                        "response-read" -> "чтение копии ответа"
                                        else -> "неизвестная схема"
                                    }
                                    onEvent(currentAdapterId, "Ответ оставлен без изменений: $reason.", false)
                                }
                                "region-page" -> onEvent(currentAdapterId, "Сервис показал региональное ограничение.", false)
                            }
                        }
                    })
                    sendConfig(port, adapterId)
                }
            }, "abypass")
        }
    }

    private fun change(session: GeckoSession, update: (BrowserUiState) -> BrowserUiState) {
        val next = update(snapshots[session] ?: return)
        snapshots[session] = next
        if (activeSession === session) state = next
    }

    private fun activate(session: GeckoSession) {
        activeSession?.setActive(false)
        activeSession = session
        state = snapshots[session] ?: BrowserUiState()
        if (session.isOpen) session.setActive(true)
    }

    private fun closePopup(session: GeckoSession) {
        val opener = openers.remove(session) ?: return
        ports.keys.filter { it.sender.session === session }.toList().forEach { ports.remove(it); sentConfigs.remove(it); it.disconnect() }
        session.close()
        owners.remove(session)
        snapshots.remove(session)
        if (activeSession === session) activate(opener)
    }

    fun back() {
        val session = activeSession ?: return
        when {
            state.canGoBack -> session.goBack()
            openers.containsKey(session) -> closePopup(session)
            else -> hide()
        }
    }
    fun forward() { activeSession?.goForward() }
    fun reload() { activeSession?.reload() }
    fun hide() { pending = null; activeSession?.setActive(false); activeSession = null; state = BrowserUiState() }
    fun setVisible(visible: Boolean) { activeSession?.setActive(visible) }
    fun updateSettings() {
        owners.keys.forEach {
            it.settings.userAgentMode = if (settings().desktopMode) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            it.settings.viewportMode = if (settings().desktopMode) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        }
        ports.forEach { (port, id) -> sendConfig(port, id) }
    }
    private fun sendConfig(port: WebExtension.Port, id: String) {
        val enabled = id in settings().adapters
        if (sentConfigs[port] == enabled) return
        port.postMessage(JSONObject().put("type", "config").put("enabled", enabled))
        sentConfigs[port] = enabled
    }

    fun closeAll() {
        generation++
        pending = null
        ports.keys.toList().forEach { it.disconnect() }
        ports.clear()
        sentConfigs.clear()
        owners.keys.toList().forEach { if (it.isOpen) it.close() }
        sessions.clear(); owners.clear(); openers.clear(); snapshots.clear(); needsReload.clear()
        activeSession = null
        state = BrowserUiState()
    }

    fun clearData(onComplete: (Boolean) -> Unit) {
        if (clearingData) { onComplete(false); return }
        clearingData = true
        closeAll()
        val runtime = RuntimeHolder.runtime
        if (runtime == null) { clearingData = false; onComplete(true); return }
        runtime.storageController.clearData(StorageController.ClearFlags.ALL).accept({ clearingData = false; onComplete(true) }, { clearingData = false; onComplete(false) })
    }

}

