package app.abypass.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlRulesTest {
    @Test fun flowSettingAppliesToBothCanonicalOriginsAndSpaRoutes() {
        assertEquals("flow", UrlRules.adapterService("https://flow.google.com/"))
        assertEquals("flow", UrlRules.adapterService("https://labs.google/fx/tools/flow/project/1"))
        assertEquals("labs", UrlRules.adapterService("https://labs.google/"))
        assertEquals("labs", UrlRules.adapterService("https://labs.google/fx/tools/flow-other"))
        assertNull(UrlRules.adapterService("https://accounts.google.com/"))
        assertNull(UrlRules.adapterService("https://labs.google.example.com/"))
    }
    @Test fun pageStatusIsOnlyRecordedForTheSelectedService() {
        assertTrue(UrlRules.isServicePage("flow", "https://flow.google.com/project/1"))
        assertTrue(UrlRules.isServicePage("flow", "https://labs.google/fx/tools/flow/project/1"))
        assertFalse(UrlRules.isServicePage("flow", "https://accounts.google.com/"))
        assertFalse(UrlRules.isServicePage("flow", "https://labs.google/fx/tools/other"))
        assertFalse(UrlRules.isServicePage("studio", "https://flow.google.com/"))
    }
    @Test fun deceptiveAndInsecureHostsNeverBecomeSuccessfulChecks() {
        listOf("https://aistudio.google.com.example.com/", "https://aistudio.google.com@evil.example/", "http://aistudio.google.com/", "https://aistudio.google.com:8443/", "about:blank", "not a URL")
            .forEach { assertFalse(it, UrlRules.isServicePage("studio", it)) }
    }
}
