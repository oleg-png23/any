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
import org.mozilla.geckoview.GeckoSession

class AppModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)
    var settings by mutableStateOf(store.settings())
        private set
    var records by mutableStateOf(store.records())
        private set
    var events by mutableStateOf(store.events())
        private set
    val prompts = BrowserPrompts(application, viewModelScope)
    val engine = BrowserEngine(application, { settings }, ::record, ::event).also { it.promptDelegate = prompts }
    var pendingDownload: GeckoSession.WebResponseInfo? = null

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
        appendLine("aBypass ${app.abypass.BuildConfig.VERSION_NAME}")
        appendLine("Android API ${android.os.Build.VERSION.SDK_INT}")
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
    override fun onCleared() { prompts.dismissAll(); engine.closeAll(); super.onCleared() }
}
