package app.abypass.data

data class ServiceDefinition(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val hasAdapter: Boolean,
    val shortName: String,
)

object Services {
    val all = listOf(
        ServiceDefinition("flow", "Flow", "Видео и изображения", "https://flow.google.com/", true, "F"),
        ServiceDefinition("studio", "AI Studio", "Модели и инструменты Gemini", "https://aistudio.google.com/", false, "AI"),
        ServiceDefinition("labs", "Google Labs", "Эксперименты Google", "https://labs.google/", true, "L"),
        ServiceDefinition("gemini", "Gemini", "AI-ассистент", "https://gemini.google.com/", false, "G"),
    )
    fun find(id: String?) = all.firstOrNull { it.id == id }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class CheckStatus { NOT_CHECKED, PAGE_OPENED, SIGN_IN_REQUIRED, NETWORK_ERROR }

data class ServiceRecord(
    val status: CheckStatus = CheckStatus.NOT_CHECKED,
    val checkedAt: Long = 0,
    val adapterChanges: Int = 0,
)

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val desktopMode: Boolean = false,
    val favorites: Set<String> = setOf("flow", "studio"),
    val adapters: Set<String> = setOf("flow", "labs"),
)

data class DiagnosticEvent(val time: Long, val serviceId: String, val message: String)

data class BrowserUiState(
    val serviceId: String? = null,
    val url: String = "",
    val title: String = "",
    val loading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: String? = null,
    val extensionReady: Boolean = false,
)

