package app.abypass

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import app.abypass.browser.DownloadNames
import app.abypass.browser.WebPromptHost
import app.abypass.data.AppModel
import app.abypass.ui.ABypassApp
import app.abypass.ui.ABypassTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()
    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = if (result.resultCode == RESULT_OK) {
            val data = result.data
            data?.clipData?.let { clips -> (0 until clips.itemCount).map { clips.getItemAt(it).uri } }
                ?: listOfNotNull(data?.data)
        } else emptyList()
        model.prompts.receiveFiles(uris)
    }
    private val exportPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) model.writeDiagnostics(uri) { toast(if (it) "Диагностика сохранена" else "Не удалось сохранить файл") }
    }
    private val downloadPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val response = model.pendingDownload
        model.pendingDownload = null
        if (uri != null && response != null) {
            toast("Скачивание началось")
            model.engine.download(response.uri, uri, lifecycleScope) { success ->
                toast(if (success) "Файл сохранён" else "Не удалось скачать файл. Повторите на странице сервиса.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        model.prompts.launchPicker = { intent ->
            try { filePicker.launch(intent) } catch (_: ActivityNotFoundException) { model.prompts.receiveFiles(emptyList()); toast("На устройстве нет приложения для выбора файлов") }
        }
        model.engine.onDownload = { response ->
            if (model.pendingDownload == null) {
                model.pendingDownload = response
                downloadPicker.launch(DownloadNames.clean(response.filename))
            }
        }
        setContent {
            ABypassTheme(model.settings) {
                BackHandler(enabled = model.engine.state.serviceId != null) { model.engine.back() }
                ABypassApp(
                    settings = model.settings,
                    records = model.records,
                    events = model.events,
                    browser = model.engine.state,
                    onSettingsChange = model::updateSettings,
                    onOpenService = model.engine::open,
                    onCloseBrowser = model.engine::hide,
                    onBack = model.engine::back,
                    onForward = model.engine::forward,
                    onReload = model.engine::reload,
                    onDesktopToggle = {
                        model.updateSettings(model.settings.copy(desktopMode = !model.settings.desktopMode))
                        model.engine.reload()
                    },
                    onExternalOpen = ::openExternal,
                    onClearData = { model.clearData { toast(if (it) "Данные сайтов очищены" else "Не удалось очистить данные. Повторите попытку.") } },
                    onExportDiagnostics = { exportPicker.launch("aBypass-diagnostics.txt") },
                    onClearDiagnostics = model::clearDiagnostics,
                    browserContent = {
                        AndroidView(
                            factory = { GeckoView(it) },
                            modifier = Modifier.fillMaxSize(),
                            onRelease = { it.releaseSession() },
                            update = { view ->
                                val current = model.engine.activeSession
                                if (view.session !== current) {
                                    view.releaseSession()
                                    if (current != null) view.setSession(current)
                                }
                            },
                        )
                    },
                )
                WebPromptHost(model.prompts)
            }
        }
    }

    private fun openExternal() {
        val uri = Uri.parse(model.engine.state.url)
        if (uri.scheme != "https") return
        try { startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)) }
        catch (_: ActivityNotFoundException) { toast("На устройстве нет другого браузера") }
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    override fun onStart() { super.onStart(); model.engine.setVisible(true) }
    override fun onStop() { model.engine.setVisible(false); super.onStop() }
    override fun onDestroy() { model.prompts.launchPicker = null; model.engine.onDownload = null; super.onDestroy() }
}
