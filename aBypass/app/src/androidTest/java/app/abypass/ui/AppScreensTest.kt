package app.abypass.ui

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.abypass.R
import app.abypass.data.AppSettings
import app.abypass.data.BrowserUiState
import app.abypass.data.ServiceDefinition
import app.abypass.data.ThemeMode
import app.abypass.data.Services
import app.abypass.data.ServiceRecord
import app.abypass.data.CheckStatus
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

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("AI Studio"))
        compose.saveScreenshot("catalog-studio")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Gemini"))
        compose.saveScreenshot("catalog-labs-gemini")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Flow"))

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

        compose.onNode(hasText("AI Studio") and !hasSetTextAction()).performScrollTo().assertIsDisplayed()
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
        navigateTo(R.string.nav_services)
        compose.saveScreenshot("catalog-dark")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Gemini"))
        compose.saveScreenshot("catalog-dark-labs-gemini")
    }

    @Test
    fun clearingSharedSiteDataRequiresConfirmationAndCanBeCancelled() {
        var clearCalls = 0
        render(onClearData = { clearCalls++ })
        navigateTo(R.string.nav_settings)

        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text(R.string.clear_data_action)))
        compose.onNodeWithText(text(R.string.clear_data_action)).performClick()

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

    @Test
    fun studioEligibilityRedirectShowsAnExplanationWithoutHidingTheGooglePage() {
        render(browser = BrowserUiState(serviceId = "studio",
            url = "https://ai.google.dev/gemini-api/docs/available-regions", accessRestricted = true))
        compose.onNodeWithText(FAKE_BROWSER_CONTENT).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.studio_access_title)).assertIsDisplayed()
        compose.saveScreenshot("studio-access")
        compose.onNodeWithContentDescription(text(R.string.studio_access_details)).performClick()
        compose.onNodeWithText(text(R.string.studio_access_help)).assertIsDisplayed()
    }

    @Test
    fun loadingCanBeStoppedRetriedAndLeftWithoutHistory() {
        val browser = mutableStateOf(BrowserUiState(serviceId = "flow", url = "https://flow.google.com/", loading = true, progress = 25))
        var stopped = 0
        var reloaded = 0
        var back = 0
        render(browserState = browser, onStopLoading = {
            stopped++
            browser.value = browser.value.copy(loading = false, loadStopped = true)
        }, onReload = {
            reloaded++
            browser.value = browser.value.copy(loading = true, loadStopped = false, progress = 0)
        }, onBack = { back++ })
        compose.onNodeWithContentDescription(text(R.string.browser_stop_loading)).assertIsDisplayed()
        compose.saveScreenshot("browser-loading")
        compose.onNodeWithContentDescription(text(R.string.browser_stop_loading)).performClick()
        compose.onNodeWithText(text(R.string.browser_loading_stopped)).assertIsDisplayed()
        compose.onNodeWithText(FAKE_BROWSER_CONTENT).assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, stopped) }
        compose.saveScreenshot("browser-stopped")
        compose.onNodeWithText(text(R.string.retry)).performClick()
        compose.onNodeWithContentDescription(text(R.string.browser_stop_loading)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.browser_loading_stopped)).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, reloaded) }
        compose.onNodeWithContentDescription(text(R.string.browser_back)).performClick()
        compose.runOnIdle { assertEquals(1, back) }
    }

    @Test
    fun pageSheetActionsReachTheirCallbacksAndDismissTheSheet() {
        var exported = 0
        var toggled = 0
        var external = 0
        render(browser = BrowserUiState(serviceId = "flow", url = "https://flow.google.com/"),
            onExportDiagnostics = { exported++ }, onDesktopToggle = { toggled++ }, onExternalOpen = { external++ })
        compose.onNodeWithContentDescription(text(R.string.page_information)).performClick()
        compose.onNodeWithText(text(R.string.export_diagnostics)).performScrollTo().assertIsDisplayed()
        compose.saveScreenshot("browser-sheet")
        compose.onNodeWithText(text(R.string.export_diagnostics)).performClick()
        compose.runOnIdle { assertEquals(1, exported) }
        compose.onNodeWithText(text(R.string.export_diagnostics)).assertDoesNotExist()
        compose.onNodeWithText(FAKE_BROWSER_CONTENT).assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.page_information)).performClick()
        compose.onNodeWithText(text(R.string.desktop_mode)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, toggled) }
        compose.onNodeWithContentDescription(text(R.string.page_information)).performClick()
        compose.onNodeWithText(text(R.string.browser_external)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, external) }
    }

    @Test
    fun recordedStatusesRenderOnTheServiceSurfacesInBothThemes() {
        render(records = mapOf("flow" to ServiceRecord(CheckStatus.PAGE_OPENED),
            "studio" to ServiceRecord(CheckStatus.NETWORK_ERROR),
            "labs" to ServiceRecord(CheckStatus.SIGN_IN_REQUIRED),
            "gemini" to ServiceRecord(CheckStatus.ACCESS_RESTRICTED)))
        compose.onNodeWithText("Flow").performScrollTo()
        compose.saveScreenshot("catalog-status-light")
        navigateTo(R.string.nav_settings)
        compose.onNodeWithText(text(R.string.theme_dark)).performScrollTo().performClick()
        navigateTo(R.string.nav_services)
        compose.onNodeWithText("Flow").performScrollTo()
        compose.saveScreenshot("catalog-status-dark")
    }

    @Test
    fun serviceAvatarsStaySquareAndCenteredOnNarrowScreensWithLargeText() {
        render(largeText = true)
        for (service in Services.all) {
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(service.name))
            val avatar = compose.onNodeWithTag("service-avatar-${service.id}", useUnmergedTree = true)
            avatar.assertWidthIsEqualTo(52.dp).assertHeightIsEqualTo(52.dp)
            compose.onNodeWithTag("service-status-${service.id}", useUnmergedTree = true).assertWidthIsAtLeast(160.dp)
            val bounds = avatar.fetchSemanticsNode().boundsInRoot
            val glyph = compose.onNodeWithTag("service-glyph-${service.id}", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            assertEquals("Icon must remain horizontally centered", bounds.center.x, glyph.center.x, 1f)
            assertEquals("Icon must remain vertically centered", bounds.center.y, glyph.center.y, 1f)
        }
        compose.saveScreenshot("catalog-large-type")
    }

    @Test
    fun wallpaperColorsRenderAndCanBeSwitchedBackToGooglePalette() {
        val settings = render(initialSettings = AppSettings(theme = ThemeMode.LIGHT, dynamicColor = true))
        compose.saveScreenshot("catalog-wallpaper")
        navigateTo(R.string.nav_settings)
        compose.onNodeWithText(text(R.string.dynamic_colors)).performScrollTo().performClick()
        compose.runOnIdle { assertFalse(settings.value.dynamicColor) }
        navigateTo(R.string.nav_services)
        compose.saveScreenshot("catalog-google-palette")
    }

    @Test
    fun wallpaperPaletteKeepsTextAndControlsReadableInBothThemes() {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        var styles = emptyList<ServiceStyle>()
        compose.setContent {
            ABypassTheme(AppSettings(theme = theme.value, dynamicColor = true)) {
                val current = Services.all.map { serviceStyle(it.id) }
                SideEffect { styles = current }
            }
        }
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            compose.runOnIdle { theme.value = mode }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(Services.all.size, styles.size)
                fun contrast(first: androidx.compose.ui.graphics.Color, second: androidx.compose.ui.graphics.Color): Float {
                    val a = first.luminance(); val b = second.luminance()
                    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
                }
                for (style in styles) {
                    assertTrue("Card text must retain readable contrast", contrast(style.ink, style.start) >= 4.5f)
                    assertTrue("Button and avatar must retain readable contrast", contrast(style.accent, style.start) >= 4.5f)
                }
            }
        }
    }

    @Test
    fun phoneShowsAllFourServicesWithoutScrollingAndKeepsSearchAcrossNavigation() {
        render()
        for (service in Services.all) compose.onNodeWithText(service.name).assertIsDisplayed()
        val flow = compose.onNodeWithTag("service-tile-flow").fetchSemanticsNode().boundsInRoot
        val studio = compose.onNodeWithTag("service-tile-studio").fetchSemanticsNode().boundsInRoot
        val labs = compose.onNodeWithTag("service-tile-labs").fetchSemanticsNode().boundsInRoot
        assertEquals("First two services share a row", flow.top, studio.top, 1f)
        assertTrue("The third service starts the next row", labs.top >= flow.bottom)
        compose.onNodeWithText(text(R.string.search_services)).performTextInput("Flow")
        navigateTo(R.string.nav_settings)
        navigateTo(R.string.nav_services)
        compose.onNodeWithText("Flow").assertIsDisplayed()
        compose.onNodeWithText("AI Studio").assertDoesNotExist()
    }

    @Test
    fun recordNativeMotionAndPreserveSelectionThroughTransitions() {
        render()
        compose.mainClock.autoAdvance = false
        try {
            val tile = compose.onNodeWithTag("service-tile-flow")
            val resting = tile.fetchSemanticsNode().boundsInRoot.width
            tile.performTouchInput { down(center) }
            for (frame in 0 until 12) {
                compose.mainClock.advanceTimeBy(32)
                compose.saveScreenshot("motion-press-${frame.toString().padStart(2, '0')}")
            }
            assertTrue("A held tile must visibly respond", tile.fetchSemanticsNode().boundsInRoot.width < resting - 1f)
            tile.performTouchInput { up() }
            for (frame in 0 until 12) {
                compose.mainClock.advanceTimeBy(32)
                compose.saveScreenshot("motion-release-${frame.toString().padStart(2, '0')}")
            }
            compose.onNode(hasText(text(R.string.favorites)) and hasClickAction()).performClick()
            for (frame in 0 until 20) {
                compose.mainClock.advanceTimeBy(32)
                compose.saveScreenshot("motion-filter-${frame.toString().padStart(2, '0')}")
            }
            compose.onNode(hasText(text(R.string.favorites)) and hasClickAction()).assertIsSelected()
            navigateTo(R.string.nav_settings)
            for (frame in 0 until 20) {
                compose.mainClock.advanceTimeBy(32)
                compose.saveScreenshot("motion-navigation-${frame.toString().padStart(2, '0')}")
            }
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.onNodeWithText(text(R.string.appearance)).assertIsDisplayed()
        navigateTo(R.string.nav_services)
        compose.onNode(hasText(text(R.string.favorites)) and hasClickAction()).assertIsSelected()
        compose.onNodeWithText("Google Labs").assertDoesNotExist()
    }

    private fun render(
        initialSettings: AppSettings = AppSettings(theme = ThemeMode.LIGHT, dynamicColor = false),
        browser: BrowserUiState = BrowserUiState(),
        onOpenService: (ServiceDefinition) -> Unit = {},
        onClearData: () -> Unit = {},
        onCloseBrowser: () -> Unit = {},
        largeText: Boolean = false,
        records: Map<String, ServiceRecord> = emptyMap(),
        onExportDiagnostics: () -> Unit = {},
        onStopLoading: () -> Unit = {},
        onReload: () -> Unit = {},
        onBack: () -> Unit = {},
        browserState: MutableState<BrowserUiState>? = null,
        onDesktopToggle: () -> Unit = {},
        onExternalOpen: () -> Unit = {},
    ): MutableState<AppSettings> {
        val settings = mutableStateOf(initialSettings)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else density.fontScale)) {
            Box(if (largeText) Modifier.width(320.dp) else Modifier) {
            ABypassTheme(settings.value) {
                ABypassApp(
                    settings = settings.value,
                    records = records,
                    events = emptyList(),
                    browser = browserState?.value ?: browser,
                    onSettingsChange = { settings.value = it },
                    onOpenService = onOpenService,
                    onCloseBrowser = onCloseBrowser,
                    onBack = onBack,
                    onForward = {},
                    onReload = onReload,
                    onStopLoading = onStopLoading,
                    onDesktopToggle = onDesktopToggle,
                    onExternalOpen = onExternalOpen,
                    onClearData = onClearData,
                    onExportDiagnostics = onExportDiagnostics,
                    onClearDiagnostics = {},
                    browserContent = { Text(FAKE_BROWSER_CONTENT) },
                )
            }
            }
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
    val roots = onAllNodes(isRoot())
    val bitmap = if (name == "browser-sheet") {
        requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
    } else roots[roots.fetchSemanticsNodes().lastIndex].captureToImage().asAndroidBitmap()
    FileOutputStream(file).use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
    }
    return file
}



