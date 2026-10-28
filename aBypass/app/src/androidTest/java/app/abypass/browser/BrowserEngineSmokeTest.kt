package app.abypass.browser

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.abypass.data.AppSettings
import app.abypass.data.ServiceDefinition
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.GeckoView

/** Executes the real native engine and built-in extension without a Google account or network. */
@RunWith(AndroidJUnit4::class)
class BrowserEngineSmokeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun disabledFlowAdapterDoesNotDelayOpening() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var engine: BrowserEngine
        compose.runOnUiThread {
            engine = BrowserEngine(context, { AppSettings(adapters = emptySet()) }, { _, _ -> }, { _, _, _ -> })
            try {
                engine.open(ServiceDefinition("flow", "Flow", "", "data:text/html,<html><body>Flow</body></html>", true, "F"))
                assertNotNull("A disabled adapter must not delay creation of the browser session", engine.activeSession)
                assertEquals("flow", engine.state.serviceId)
            } finally { engine.closeAll() }
        }
    }

    @Test fun backDuringInitialOpenReturnsHome() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var engine: BrowserEngine
        compose.runOnUiThread {
            engine = BrowserEngine(context, { AppSettings() }, { _, _ -> }, { _, _, _ -> })
            engine.open(ServiceDefinition("cancel-smoke", "Cancel smoke", "", "data:text/html,<html><body>Cancelled initial open</body></html>", false, "a"))
            engine.back()
            assertNull("Back must leave the browser even before a session exists", engine.state.serviceId)
        }
        try {
            compose.waitForIdle()
            compose.runOnIdle { assertNull("A late extension callback must not reopen a cancelled service", engine.state.serviceId) }
        } finally { compose.runOnUiThread { engine.closeAll() } }
    }

    @Test fun nativeEngineLoadsPageAndInstallsBundledAdapter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var engine: BrowserEngine
        compose.runOnUiThread {
            engine = BrowserEngine(context, { AppSettings() }, { _, _ -> }, { _, _, _ -> })
        }
        compose.setContent {
            AndroidView(factory = { GeckoView(it) }, modifier = Modifier.fillMaxSize(), onRelease = { it.releaseSession() }, update = { view ->
                val session = engine.activeSession
                if (view.session !== session) { view.releaseSession(); if (session != null) view.setSession(session) }
            })
        }
        compose.runOnUiThread {
            engine.open(ServiceDefinition("smoke", "Smoke", "", "data:text/html,<html><head><title>californium browser smoke</title></head><body><h1>californium</h1><p>Native browser is running</p></body></html>", false, "a"))
        }
        try {
            compose.waitUntil(30_000) { engine.state.title == "californium browser smoke" && !engine.state.loading }
            compose.runOnIdle {
                assertNotNull(engine.activeSession)
                assertTrue("Bundled extension must install successfully", engine.state.extensionReady)
                engine.stopLoading()
                assertFalse("Stopping an idle page must not mark it cancelled", engine.state.loadStopped)
                engine.reload()
                engine.stopLoading()
                assertTrue("An explicit cancellation must be represented immediately", engine.state.loadStopped)
                assertFalse(engine.state.loading)
                assertNull(engine.state.error)
                engine.hide()
                assertTrue(engine.state.serviceId == null)
            }
        } finally { compose.runOnUiThread { engine.closeAll() } }
    }
}

