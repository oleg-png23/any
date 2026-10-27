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

    private fun isFlowPath(path: String) = path == "/flow" || path.startsWith("/flow/") || Regex("^/fx/(?:[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*/)?tools/flow(?:/|$)").containsMatchIn(path)
    fun isServicePage(service: String, url: String): Boolean = service == "flow" && adapterService(url) == "flow"
}
