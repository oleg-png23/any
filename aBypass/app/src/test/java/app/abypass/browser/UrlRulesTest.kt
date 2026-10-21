package app.abypass.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlRulesTest {
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
