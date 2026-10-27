package app.abypass.browser

import org.junit.Assert.*
import org.junit.Test

class UrlRulesTest {
    @Test fun flowIncludesCanonicalAndLegacyProjectPaths() {
        listOf("https://flow.google.com/", "https://flow.google.com/project/1", "https://labs.google/flow", "https://labs.google/flow/project/1", "https://labs.google/fx/tools/flow", "https://labs.google/fx/tools/flow/project/1").forEach {
            assertEquals("flow", UrlRules.adapterService(it))
            assertTrue(UrlRules.isServicePage("flow", it))
        }
    }
    @Test fun removedServicesAndOtherLabsToolsHaveNoAdapter() {
        listOf("https://labs.google/", "https://labs.google/fx/tools/flow-other", "https://labs.google/fx/tools/other", "https://aistudio.google.com/", "https://gemini.google.com/", "https://accounts.google.com/").forEach {
            assertNull(it, UrlRules.adapterService(it)); assertFalse(UrlRules.isServicePage("flow", it))
        }
        assertFalse(UrlRules.isServicePage("other", "https://flow.google.com/"))
    }
    @Test fun deceptiveAndInsecureUrlsAreRejected() {
        listOf("https://flow.google.com.example.com/", "https://flow.google.com@evil.example/", "https://user:pass@flow.google.com/", "http://flow.google.com/", "https://flow.google.com:8443/", "about:blank", "not a URL").forEach {
            assertNull(it, UrlRules.adapterService(it)); assertFalse(UrlRules.isServicePage("flow", it))
        }
    }
}
