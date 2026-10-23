package app.abypass.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DownloadNamesTest {
    @Test fun pathsCannotEscapeDestination() {
        assertEquals("video.mp4", DownloadNames.clean("../../video.mp4"))
        assertEquals("image.png", DownloadNames.clean("C:\\images\\image.png"))
        assertFalse(DownloadNames.clean("\u0000bad\nname?.mp4").any { it.code < 32 || it == '?' })
    }
    @Test fun emptyNamesHaveUsableFallback() {
        listOf(null, "", ".", "../", "  ").forEach { assertEquals("aBypass-download", DownloadNames.clean(it)) }
        assertEquals(120, DownloadNames.clean("a".repeat(1000)).length)
    }
}
