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
    val Flow = ServiceDefinition("flow", "Flow", "Видео и изображения", "https://flow.google.com/", true, "F")
    val all = listOf(Flow)
    fun find(id: String?) = all.firstOrNull { it.id == id }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class CheckStatus { NOT_CHECKED, PAGE_OPENED, SIGN_IN_REQUIRED, NETWORK_ERROR, ACCESS_RESTRICTED }

data class ServiceRecord(
    val status: CheckStatus = CheckStatus.NOT_CHECKED,
    val checkedAt: Long = 0,
    val adapterChanges: Int = 0,
)

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val desktopMode: Boolean = false,
    val adapters: Set<String> = setOf("flow"),
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
    val loadStopped: Boolean = false,
)


