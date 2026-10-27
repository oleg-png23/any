package app.abypass.ui

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.abypass.data.SettingsStore
import app.abypass.data.ThemeMode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsMigrationTest {
    @Test fun upgradeRemovesOtherServicesAndRetainsFlowPreferences() {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("abypass", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("theme", "DARK").putBoolean("desktop", true)
            .putStringSet("favorites", setOf("studio")).putStringSet("adapters", setOf("flow", "labs"))
            .putString("status_studio", "PAGE_OPENED").putString("status_flow", "SIGN_IN_REQUIRED")
            .putString("events", "[{\"time\":1,\"service\":\"studio\",\"message\":\"old\"},{\"time\":2,\"service\":\"flow\",\"message\":\"kept\"}]").commit()
        try {
            val store = SettingsStore(InstrumentationRegistry.getInstrumentation().targetContext)
            assertEquals(ThemeMode.DARK, store.settings().theme)
            assertTrue(store.settings().desktopMode)
            assertEquals(setOf("flow"), store.settings().adapters)
            assertEquals(setOf("flow"), store.records().keys)
            assertEquals(listOf("flow"), store.events().map { it.serviceId })
            assertFalse(prefs.contains("favorites"))
            assertFalse(prefs.contains("status_studio"))
        } finally { prefs.edit().clear().commit() }
    }
}
