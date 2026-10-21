package app.abypass.browser

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.abypass.data.AppSettings
import app.abypass.data.ServiceDefinition
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.GeckoView

/** Executes the real native engine and built-in extension without a Google account or network. */
@RunWith(AndroidJUnit4::class)
class BrowserEngineSmokeTest {
    @get:Rule val compose = createComposeRule()

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
            engine.open(ServiceDefinition("smoke", "Smoke", "", "data:text/html,<html><head><title>aBypass browser smoke</title></head><body><h1>aBypass</h1><p>Native browser is running</p></body></html>", false, "a"))
        }
        try {
            compose.waitUntil(30_000) { engine.state.title == "aBypass browser smoke" && !engine.state.loading }
            compose.runOnIdle {
                assertNotNull(engine.activeSession)
                assertTrue("Bundled extension must install successfully", engine.state.extensionReady)
                engine.hide()
                assertTrue(engine.state.serviceId == null)
            }
        } finally { compose.runOnUiThread { engine.closeAll() } }
    }
}
