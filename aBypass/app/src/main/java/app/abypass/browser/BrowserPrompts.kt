package app.abypass.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import java.io.File
import java.util.UUID

private typealias Prompts = GeckoSession.PromptDelegate

data class WebPrompt(
    val title: String,
    val message: String,
    val defaultText: String? = null,
    val choices: List<Prompts.ChoicePrompt.Choice> = emptyList(),
    val multiple: Boolean = false,
    val accept: (String, List<String>) -> Unit,
    val cancel: () -> Unit,
)

class BrowserPrompts(private val context: Context, private val scope: CoroutineScope) : Prompts {
    var dialog by mutableStateOf<WebPrompt?>(null)
        private set
    var launchPicker: ((Intent) -> Unit)? = null
    private var pendingFile: Pair<Prompts.FilePrompt, GeckoResult<Prompts.PromptResponse>>? = null

    private fun request(title: String, message: String, prompt: Prompts.BasePrompt, defaultText: String? = null,
                        choices: List<Prompts.ChoicePrompt.Choice> = emptyList(), multiple: Boolean = false,
                        confirm: (String, List<String>) -> Prompts.PromptResponse): GeckoResult<Prompts.PromptResponse> {
        dialog?.cancel?.invoke()
        val result = GeckoResult<Prompts.PromptResponse>()
        fun complete(response: () -> Prompts.PromptResponse) {
            dialog = null
            if (!prompt.isComplete) result.complete(response())
        }
        dialog = WebPrompt(title, message, defaultText, choices, multiple,
            accept = { text, ids -> complete { confirm(text, ids) } },
            cancel = { complete { prompt.dismiss() } })
        prompt.setDelegate(object : Prompts.PromptInstanceDelegate {
            override fun onPromptDismiss(prompt: Prompts.BasePrompt) { dialog = null }
        })
        return result
    }

    override fun onAlertPrompt(session: GeckoSession, prompt: Prompts.AlertPrompt) =
        request("Сообщение сайта", prompt.message.orEmpty(), prompt) { _, _ -> prompt.dismiss() }
    override fun onButtonPrompt(session: GeckoSession, prompt: Prompts.ButtonPrompt) =
        request("Подтверждение", prompt.message.orEmpty(), prompt) { _, _ -> prompt.confirm(Prompts.ButtonPrompt.Type.POSITIVE) }
    override fun onTextPrompt(session: GeckoSession, prompt: Prompts.TextPrompt) =
        request("Ответ сайту", prompt.message.orEmpty(), prompt, defaultText = prompt.defaultValue.orEmpty()) { text, _ -> prompt.confirm(text) }
    override fun onBeforeUnloadPrompt(session: GeckoSession, prompt: Prompts.BeforeUnloadPrompt) =
        request("Покинуть страницу?", "Несохранённые изменения могут быть потеряны.", prompt) { _, _ -> prompt.confirm(AllowOrDeny.ALLOW) }
    override fun onRepostConfirmPrompt(session: GeckoSession, prompt: Prompts.RepostConfirmPrompt) =
        request("Повторить отправку?", "Страница просит повторно отправить форму.", prompt) { _, _ -> prompt.confirm(AllowOrDeny.ALLOW) }
    override fun onPopupPrompt(session: GeckoSession, prompt: Prompts.PopupPrompt) =
        request("Открыть новое окно?", "Сайт запрашивает дополнительное окно.", prompt) { _, _ -> prompt.confirm(AllowOrDeny.ALLOW) }
    override fun onChoicePrompt(session: GeckoSession, prompt: Prompts.ChoicePrompt): GeckoResult<Prompts.PromptResponse> {
        fun flatten(items: Array<Prompts.ChoicePrompt.Choice>): List<Prompts.ChoicePrompt.Choice> =
            items.flatMap { choice -> choice.items?.let { flatten(it) } ?: listOf(choice) }
        return request("Выбор на сайте", prompt.message.orEmpty(), prompt,
            choices = flatten(prompt.choices).filter { !it.separator },
            multiple = prompt.type == Prompts.ChoicePrompt.Type.MULTIPLE) { _, ids ->
            if (prompt.type == Prompts.ChoicePrompt.Type.MULTIPLE) prompt.confirm(ids.toTypedArray())
            else if (ids.isNotEmpty()) prompt.confirm(ids.first()) else prompt.dismiss()
        }
    }

    override fun onFilePrompt(session: GeckoSession, prompt: Prompts.FilePrompt): GeckoResult<Prompts.PromptResponse> {
        dismissFile()
        val result = GeckoResult<Prompts.PromptResponse>()
        if (launchPicker == null) return GeckoResult.fromValue(prompt.dismiss())
        pendingFile = prompt to result
        val intent = if (prompt.type == Prompts.FilePrompt.Type.FOLDER) Intent(Intent.ACTION_OPEN_DOCUMENT_TREE) else {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                prompt.mimeTypes?.filter { it.contains('/') }?.takeIf { it.isNotEmpty() }?.let { putExtra(Intent.EXTRA_MIME_TYPES, it.toTypedArray()) }
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, prompt.type == Prompts.FilePrompt.Type.MULTIPLE)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        launchPicker?.invoke(intent)
        return result
    }

    fun receiveFiles(uris: List<Uri>) {
        val (prompt, result) = pendingFile ?: return
        pendingFile = null
        if (prompt.isComplete) return
        if (uris.isEmpty()) { result.complete(prompt.dismiss()); return }
        if (prompt.type == Prompts.FilePrompt.Type.FOLDER) { result.complete(prompt.confirm(context, uris.first())); return }
        scope.launch {
            val files = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(context.cacheDir, "uploads").apply { mkdirs() }
                    uris.map { uri ->
                        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                            if (it.moveToFirst()) it.getString(0) else "upload"
                        } ?: "upload"
                        val file = File(dir, UUID.randomUUID().toString() + "-" + DownloadNames.clean(name))
                        context.contentResolver.openInputStream(uri)!!.use { source -> file.outputStream().use { source.copyTo(it) } }
                        Uri.fromFile(file)
                    }
                }.getOrNull()
            }
            if (!prompt.isComplete) result.complete(if (files == null) prompt.dismiss() else prompt.confirm(context, files.toTypedArray()))
        }
    }

    private fun dismissFile() {
        pendingFile?.let { (prompt, result) -> if (!prompt.isComplete) result.complete(prompt.dismiss()) }
        pendingFile = null
    }
    fun dismissAll() { dialog?.cancel?.invoke(); dismissFile() }
}

@Composable
fun WebPromptHost(controller: BrowserPrompts) {
    val prompt = controller.dialog ?: return
    var text by remember(prompt) { mutableStateOf(prompt.defaultText.orEmpty()) }
    var selected by remember(prompt) { mutableStateOf(prompt.choices.filter { it.selected }.map { it.id }) }
    AlertDialog(
        onDismissRequest = prompt.cancel,
        title = { Text(prompt.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (prompt.message.isNotEmpty()) Text(prompt.message)
                if (prompt.defaultText != null) OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Ответ") })
                prompt.choices.forEach { choice ->
                    Row {
                        Checkbox(checked = choice.id in selected, enabled = !choice.disabled, onCheckedChange = { checked ->
                            selected = if (prompt.multiple) { if (checked) selected + choice.id else selected - choice.id } else listOf(choice.id)
                        })
                        Text(choice.label, Modifier.padding(top = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { prompt.accept(text, selected) }) { Text("Продолжить") } },
        dismissButton = { TextButton(onClick = prompt.cancel) { Text("Отмена") } },
    )
}
