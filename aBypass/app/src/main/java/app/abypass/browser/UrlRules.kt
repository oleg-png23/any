package app.abypass.browser

import java.net.URI

object UrlRules {
    /** Flow still uses these legacy Labs paths; other Labs tools have no adapter. */
    fun adapterService(url: String): String? = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.userInfo != null || (uri.port != -1 && uri.port != 443)) return@runCatching null
        when (uri.host?.lowercase()) {
            "flow.google.com" -> "flow"
            "labs.google" -> if (isFlowPath(uri.path)) "flow" else null
            else -> null
        }
    }.getOrNull()

    private fun isFlowPath(path: String) = path == "/flow" || path.startsWith("/flow/") || path == "/fx/tools/flow" || path.startsWith("/fx/tools/flow/")
    fun isServicePage(service: String, url: String): Boolean = service == "flow" && adapterService(url) == "flow"
}
