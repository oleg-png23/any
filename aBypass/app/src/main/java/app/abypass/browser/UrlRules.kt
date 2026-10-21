package app.abypass.browser

import java.net.URI

object UrlRules {
    fun adapterService(url: String): String? = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.userInfo != null || (uri.port != -1 && uri.port != 443)) return@runCatching null
        when (uri.host?.lowercase()) {
            "flow.google.com" -> "flow"
            "labs.google" -> if (isFlowPath(uri.path)) "flow" else "labs"
            else -> null
        }
    }.getOrNull()

    private fun isFlowPath(path: String) = path == "/flow" || path.startsWith("/flow/") || path == "/fx/tools/flow" || path.startsWith("/fx/tools/flow/")
    fun isServicePage(service: String, url: String): Boolean = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.userInfo != null || (uri.port != -1 && uri.port != 443)) return@runCatching false
        val host = uri.host?.lowercase() ?: return@runCatching false
        when (service) {
            "flow" -> host == "flow.google.com" || (host == "labs.google" && isFlowPath(uri.path))
            "studio" -> host == "aistudio.google.com"
            "labs" -> host == "labs.google"
            "gemini" -> host == "gemini.google.com"
            else -> false
        }
    }.getOrDefault(false)
}
