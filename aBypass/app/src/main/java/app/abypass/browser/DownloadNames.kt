package app.abypass.browser

object DownloadNames {
    fun clean(name: String?): String = name.orEmpty().substringAfterLast('/').substringAfterLast('\\')
        .replace(Regex("[\\p{Cntrl}<>:\"|?*]"), "_").trim().trim('.').take(120).ifEmpty { "californium-download" }
}
