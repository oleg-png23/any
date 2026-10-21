package app.abypass.ui

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.abypass.R
import app.abypass.data.AppSettings
import app.abypass.data.BrowserUiState
import app.abypass.data.ServiceDefinition
import app.abypass.data.ThemeMode
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI contracts only: no browser runtime, account, network access or user data is needed. */
@RunWith(AndroidJUnit4::class)
class AppScreensTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun catalogOpensSelectedService() {
        val opened = mutableListOf<ServiceDefinition>()
        render(onOpenService = { opened += it })
        compose.saveScreenshot("catalog")

        compose.onNodeWithText("Flow").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals(listOf("flow"), opened.map { it.id })
            assertEquals("https://flow.google.com/", opened.single().url)
        }
    }

    @Test
    fun searchShowsOnlyMatchingServices() {
        render()

        compose.onNodeWithText(text(R.string.search_services)).performTextInput("AI Studio")

        compose.onNodeWithText("AI Studio").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Flow").assertDoesNotExist()
        compose.onNodeWithText("Google Labs").assertDoesNotExist()
        compose.onNodeWithText("Gemini", substring = false).assertDoesNotExist()

        compose.onNodeWithContentDescription(text(R.string.clear_search)).performClick()
        compose.onNodeWithText("Flow").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun favoritesUpdateSettingsAndFilterTheCatalog() {
        val settings = render()

        compose.onNodeWithContentDescription(text(R.string.remove_favorite, "Flow"))
            .performScrollTo()
            .performClick()

        compose.runOnIdle {
            assertFalse("Flow must be removed by the settings callback", "flow" in settings.value.favorites)
            assertTrue("The other favorite must be retained", "studio" in settings.value.favorites)
        }
        compose.onNode(hasText(text(R.string.favorites)) and hasClickAction())
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Flow").assertDoesNotExist()
        compose.onNodeWithText("AI Studio").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun selectedThemeUpdatesSettingsAndRendersDarkSettings() {
        val settings = render(initialSettings = AppSettings(theme = ThemeMode.LIGHT, dynamicColor = false))
        navigateTo(R.string.nav_settings)

        compose.onNodeWithText(text(R.string.theme_dark)).performScrollTo().performClick()

        compose.runOnIdle { assertEquals(ThemeMode.DARK, settings.value.theme) }
        compose.onNode(hasText(text(R.string.theme_dark)) and hasClickAction()).assertIsSelected()
        compose.saveScreenshot("settings-dark")
    }

    @Test
    fun clearingSharedSiteDataRequiresConfirmationAndCanBeCancelled() {
        var clearCalls = 0
        render(onClearData = { clearCalls++ })
        navigateTo(R.string.nav_settings)

        compose.onNodeWithText(text(R.string.clear_data_action)).performScrollTo().performClick()

        compose.onNodeWithText(text(R.string.clear_data_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.clear_data_description)).assertIsDisplayed()
        compose.runOnIdle { assertEquals("Opening the dialog must not clear site data", 0, clearCalls) }

        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.onNodeWithText(text(R.string.clear_data_title)).assertDoesNotExist()
        compose.runOnIdle { assertEquals("Cancelling must retain the shared browser profile", 0, clearCalls) }

        compose.onNodeWithText(text(R.string.clear_data_action)).performClick()
        compose.onNodeWithText(text(R.string.clear_data_confirm)).performClick()

        compose.runOnIdle { assertEquals("An explicit confirmation clears exactly once", 1, clearCalls) }
        compose.onNodeWithText(text(R.string.clear_data_title)).assertDoesNotExist()
    }

    @Test
    fun browserHostsProvidedContentAndCloseReturnsToTheCatalogCallback() {
        var closeCalls = 0
        render(
            browser = BrowserUiState(serviceId = "studio", url = "https://aistudio.google.com/"),
            onCloseBrowser = { closeCalls++ },
        )

        compose.onNodeWithText(FAKE_BROWSER_CONTENT).assertIsDisplayed()
        compose.onNodeWithText("aistudio.google.com").assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.close_service)).performClick()
        compose.runOnIdle { assertEquals(1, closeCalls) }
    }

    private fun render(
        initialSettings: AppSettings = AppSettings(theme = ThemeMode.LIGHT, dynamicColor = false),
        browser: BrowserUiState = BrowserUiState(),
        onOpenService: (ServiceDefinition) -> Unit = {},
        onClearData: () -> Unit = {},
        onCloseBrowser: () -> Unit = {},
    ): MutableState<AppSettings> {
        val settings = mutableStateOf(initialSettings)
        compose.setContent {
            ABypassTheme(settings.value) {
                ABypassApp(
                    settings = settings.value,
                    records = emptyMap(),
                    events = emptyList(),
                    browser = browser,
                    onSettingsChange = { settings.value = it },
                    onOpenService = onOpenService,
                    onCloseBrowser = onCloseBrowser,
                    onBack = {},
                    onForward = {},
                    onReload = {},
                    onDesktopToggle = {},
                    onExternalOpen = {},
                    onClearData = onClearData,
                    onExportDiagnostics = {},
                    onClearDiagnostics = {},
                    browserContent = { Text(FAKE_BROWSER_CONTENT) },
                )
            }
        }
        compose.waitForIdle()
        return settings
    }

    private fun navigateTo(@StringRes resource: Int) {
        compose.onNode(hasText(text(resource)) and hasClickAction()).performClick()
    }

    private fun text(@StringRes resource: Int, vararg arguments: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resource, *arguments)

    private companion object {
        const val FAKE_BROWSER_CONTENT = "Browser content supplied by the host"
    }
}

/** Saves the actual composed root for visual review after the UI has become idle. */
internal fun ComposeContentTestRule.saveScreenshot(name: String): File {
    require(name.matches(Regex("[a-z0-9-]+")))
    waitForIdle()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val directory = File(requireNotNull(context.getExternalFilesDir(null)), "screenshots")
    check(directory.isDirectory || directory.mkdirs())
    val file = File(directory, "$name.png")
    val bitmap = onRoot().captureToImage().asAndroidBitmap()
    FileOutputStream(file).use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
    }
    return file
}
