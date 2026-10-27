package app.abypass.data

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.abypass.browser.BrowserEngine
import app.abypass.browser.BrowserPrompts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.WebResponse
import app.abypass.browser.DownloadNames
import android.webkit.URLUtil
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

data class SaveDownload(val file: File, val name: String, val mimeType: String)

class AppModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)
    var settings by mutableStateOf(store.settings())
        private set
    var records by mutableStateOf(store.records())
        private set
    var events by mutableStateOf(store.events())
        private set
    val prompts = BrowserPrompts(application, viewModelScope)
    val engine = BrowserEngine(application, { settings }, ::record, ::event).also {
        it.promptDelegate = prompts
        it.onDownload = ::startDownload
    }
    var saveDownload by mutableStateOf<SaveDownload?>(null)
        private set
    var downloadPickerActive = false
    private var downloadJob: Job? = null
    private var downloadBody: InputStream? = null
    var notice by mutableStateOf<String?>(null)
        private set
    fun consumeNotice() { notice = null }
    private fun startDownload(response: WebResponse) {
        val body = response.body
        if (body == null || downloadJob?.isActive == true || saveDownload != null || downloadPickerActive) {
            body?.close()
            notice = "Дождитесь завершения текущего скачивания"
            return
        }
        fun header(name: String) = response.headers.entries.firstOrNull { it.key.equals(name, true) }?.value
        val mime = header("content-type")?.substringBefore(';')?.trim()?.takeIf { it.contains('/') } ?: "application/octet-stream"
        val name = DownloadNames.clean(URLUtil.guessFileName(response.uri, header("content-disposition"), mime))
        val temp = File(getApplication<Application>().cacheDir, "download-${java.util.UUID.randomUUID()}")
        downloadBody = body
        notice = "Скачивание началось"
        downloadJob = viewModelScope.launch {
            var ready = false
            try {
                withContext(Dispatchers.IO) {
                    body.use { input -> temp.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                    } }
                }
                saveDownload = SaveDownload(temp, name, mime)
                ready = true
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { notice = "Не удалось скачать файл. Повторите на странице сервиса." }
            finally {
                body.close()
                downloadBody = null
                if (!ready) temp.delete()
            }
        }
    }
    fun completeDownload(target: Uri?) {
        val download = saveDownload ?: return
        saveDownload = null
        downloadPickerActive = false
        if (target == null) { download.file.delete(); return }
        downloadJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    download.file.inputStream().use { input ->
                        getApplication<Application>().contentResolver.openOutputStream(target, "wt")!!.use { output -> input.copyTo(output) }
                    }
                }
                notice = "Файл сохранён"
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { notice = "Не удалось сохранить файл" }
            finally { download.file.delete() }
        }
    }
    private fun cancelDownload() {
        downloadJob?.cancel()
        runCatching { downloadBody?.close() }
        downloadBody = null
        saveDownload?.file?.delete()
        saveDownload = null
    }

    fun updateSettings(value: AppSettings) {
        settings = value
        store.save(value)
        engine.updateSettings()
    }
    private fun record(id: String, status: CheckStatus) {
        val previous = records[id] ?: ServiceRecord()
        val next = previous.copy(status = status, checkedAt = System.currentTimeMillis())
        records = records + (id to next)
        store.saveRecord(id, next)
    }
    fun event(id: String, message: String, patched: Boolean = false) {
        if (patched) {
            val next = (records[id] ?: ServiceRecord()).let { it.copy(adapterChanges = it.adapterChanges + 1) }
            records = records + (id to next)
            store.saveRecord(id, next)
        }
        if (events.lastOrNull()?.let { it.serviceId == id && it.message == message && System.currentTimeMillis() - it.time < 1500 } == true) return
        events = (events + DiagnosticEvent(System.currentTimeMillis(), id, message)).takeLast(150)
        store.saveEvents(events)
    }
    fun clearDiagnostics() { events = emptyList(); store.saveEvents(events) }
    fun clearData(onComplete: (Boolean) -> Unit) {
        cancelDownload()
        prompts.dismissAll()
        engine.clearData { success ->
            if (success) {
                store.resetRecords()
                records = store.records()
                clearDiagnostics()
                viewModelScope.launch(Dispatchers.IO) { java.io.File(getApplication<Application>().cacheDir, "uploads").deleteRecursively() }
            }
            onComplete(success)
        }
    }
    fun exportText(): String = buildString {
        appendLine("californium ${app.abypass.BuildConfig.VERSION_NAME}")
        appendLine("Android API ${android.os.Build.VERSION.SDK_INT}")
        appendLine("Настольный режим: ${settings.desktopMode}")
        appendLine("Адаптеры: ${settings.adapters.filter { it in setOf("flow") }.sorted().joinToString()}")
        appendLine("Открытие страницы не подтверждает работу функций сервиса.")
        events.forEach { appendLine("${java.time.Instant.ofEpochMilli(it.time)} | ${it.serviceId} | ${it.message}") }
    }
    fun writeDiagnostics(uri: Uri, done: (Boolean) -> Unit) {
        val text = exportText()
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
            }.isSuccess }
            done(ok)
        }
    }
    override fun onCleared() { cancelDownload(); prompts.dismissAll(); engine.closeAll(); super.onCleared() }
}

