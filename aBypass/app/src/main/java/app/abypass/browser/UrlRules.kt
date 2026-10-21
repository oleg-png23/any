package app.abypass.browser

import java.net.URI

object UrlRules {
    fun isServicePage(service: String, url: String): Boolean = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.userInfo != null || (uri.port != -1 && uri.port != 443)) return false
        val host = uri.host?.lowercase() ?: return false
        when (service) {
            "flow" -> host == "flow.google.com" || (host == "labs.google" && (uri.path.startsWith("/fx/tools/flow") || uri.path.startsWith("/flow/")))
            "studio" -> host == "aistudio.google.com"
            "labs" -> host == "labs.google"
            "gemini" -> host == "gemini.google.com"
            else -> false
        }
    }.getOrDefault(false)
}
