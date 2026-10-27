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

    @Test fun flowIsTheOnlyServiceAndOpeningFiresOnce() {
        val opened = mutableListOf<ServiceDefinition>()
        render(onOpenService = { opened += it })
        compose.onNodeWithText("californium").assertIsDisplayed()
        for (removed in listOf("aBypass", "AI Studio", "Google Labs", "Gemini", "Избранное", "Найти сервис")) compose.onNodeWithText(removed).assertDoesNotExist()
        compose.onNodeWithTag("flow-open").assertIsDisplayed()
        compose.saveScreenshot("flow-home")
        compose.onNodeWithTag("flow-open").performClick()
        compose.runOnIdle { assertEquals(listOf("flow"), opened.map { it.id }); assertEquals("https://flow.google.com/", opened.single().url) }
    }

    @Test fun selectedThemeUpdatesSettingsAndRendersBothThemes() {
        val settings = render()
        navigateTo(R.string.nav_settings)
        compose.saveScreenshot("settings-light")
        compose.onNodeWithText(text(R.string.theme_dark)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(ThemeMode.DARK, settings.value.theme) }
        compose.onNode(hasText(text(R.string.theme_dark)) and hasClickAction()).assertIsSelected()
        compose.saveScreenshot("settings-dark")
        navigateTo(R.string.nav_flow)
        compose.saveScreenshot("flow-dark")
    }

    @Test fun compatibilitySettingOnlyControlsFlowAndIsReflectedOnHome() {
        val settings = render()
        navigateTo(R.string.nav_settings)
        compose.onNodeWithText(text(R.string.compatibility_setting_description)).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(settings.value.adapters.isEmpty()) }
        navigateTo(R.string.nav_flow)
        compose.onNodeWithText(text(R.string.compatibility_disabled)).assertIsDisplayed()
        navigateTo(R.string.nav_settings)
        compose.onNodeWithText(text(R.string.compatibility_setting_description)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(setOf("flow"), settings.value.adapters) }
    }

    @Test fun clearingDataRequiresConfirmationAndCanBeCancelled() {
        var cleared = 0
        render(onClearData = { cleared++ })
        navigateTo(R.string.nav_settings)
        compose.onNodeWithText(text(R.string.clear_data_action)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.clear_data_description)).assertIsDisplayed()
        compose.saveScreenshot("clear-dialog")
        compose.runOnIdle { assertEquals(0, cleared) }
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.onNodeWithText(text(R.string.clear_data_title)).assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, cleared) }
        compose.onNodeWithText(text(R.string.clear_data_action)).performClick()
        compose.onNodeWithText(text(R.string.clear_data_confirm)).performClick()
        compose.runOnIdle { assertEquals(1, cleared) }
        compose.onNodeWithText(text(R.string.clear_data_title)).assertDoesNotExist()
    }

    @Test fun browserHostSurvivesProgressAndIsReleasedOnceAfterAnimatedClose() {
        val browser = mutableStateOf(BrowserUiState())
        var mounted = 0; var released = 0; var opened = 0
        render(browserState = browser, onOpenService = {
            opened++; browser.value = BrowserUiState(serviceId = "flow", url = it.url, loading = true)
        }, onCloseBrowser = { browser.value = BrowserUiState() }, onBrowserMount = { if (it) mounted++ else released++ })
        compose.onNodeWithTag("flow-open").performClick()
        compose.onNodeWithText(FAKE_BROWSER_CONTENT).assertIsDisplayed()
        for (progress in listOf(25, 50, 100)) {
            compose.runOnIdle { browser.value = browser.value.copy(progress = progress, loading = progress < 100, title = "Flow $progress") }
            compose.waitForIdle()
        }
        compose.runOnIdle { assertEquals(1, opened); assertEquals(1, mounted); assertEquals(0, released) }
        compose.onNodeWithContentDescription(text(R.string.close_service)).performClick()
        compose.onNodeWithText("californium").assertIsDisplayed()
        compose.onNodeWithTag("flow-open").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, released) }
    }

    @Test fun loadingCanBeStoppedRetriedAndLeftWithoutHistory() {
        val browser = mutableStateOf(BrowserUiState(serviceId = "flow", url = "https://flow.google.com/", loading = true, progress = 25))
        var stopped = 0; var reloaded = 0; var back = 0
        render(browserState = browser, onStopLoading = { stopped++; browser.value = browser.value.copy(loading = false, loadStopped = true) },
            onReload = { reloaded++; browser.value = browser.value.copy(loading = true, loadStopped = false, progress = 0) }, onBack = { back++ })
        compose.onNodeWithContentDescription(text(R.string.browser_stop_loading)).assertIsDisplayed()
        compose.saveScreenshot("browser-loading")
        compose.onNodeWithContentDescription(text(R.string.browser_stop_loading)).performClick()
        compose.onNodeWithText(text(R.string.browser_loading_stopped)).assertIsDisplayed()
        compose.onNodeWithText(FAKE_BROWSER_CONTENT).assertIsDisplayed()
        compose.saveScreenshot("browser-stopped")
        compose.onNodeWithText(text(R.string.retry)).performClick()
        compose.onNodeWithContentDescription(text(R.string.browser_stop_loading)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.browser_loading_stopped)).assertDoesNotExist()
        compose.onNodeWithContentDescription(text(R.string.browser_back)).performClick()
        compose.runOnIdle { assertEquals(1, stopped); assertEquals(1, reloaded); assertEquals(1, back) }
    }

    @Test fun pageSheetActionsReachCallbacksAndDismiss() {
        var exported = 0; var toggled = 0; var external = 0
        render(browser = BrowserUiState(serviceId = "flow", url = "https://flow.google.com/"), onExportDiagnostics = { exported++ }, onDesktopToggle = { toggled++ }, onExternalOpen = { external++ })
        compose.onNodeWithContentDescription(text(R.string.page_information)).performClick()
        compose.onNodeWithText(text(R.string.export_diagnostics)).performScrollTo().assertIsDisplayed()
        compose.saveScreenshot("browser-sheet")
        compose.onNodeWithText(text(R.string.export_diagnostics)).performClick()
        compose.onNodeWithText(text(R.string.export_diagnostics)).assertDoesNotExist()
        compose.onNodeWithText(FAKE_BROWSER_CONTENT).assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.page_information)).performClick()
        compose.onNodeWithText(text(R.string.desktop_mode)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(text(R.string.page_information)).performClick()
        compose.onNodeWithText(text(R.string.browser_external)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, exported); assertEquals(1, toggled); assertEquals(1, external) }
    }

    @Test fun statusAndChecksShowOnlyFlowInBothThemes() {
        render(records = mapOf("flow" to ServiceRecord(CheckStatus.PAGE_OPENED)))
        compose.onNodeWithText(text(R.string.status_page_opened)).performScrollTo().assertIsDisplayed()
        compose.saveScreenshot("flow-status-light")
        navigateTo(R.string.nav_checks)
        compose.saveScreenshot("flow-checks")
        for (removed in listOf("AI Studio", "Google Labs", "Gemini")) compose.onNodeWithText(removed).assertDoesNotExist()
        navigateTo(R.string.nav_settings)
        compose.onNodeWithText(text(R.string.theme_dark)).performScrollTo().performClick()
        navigateTo(R.string.nav_flow)
        compose.saveScreenshot("flow-status-dark")
    }

    @Test fun flowAvatarStaysSquareAndCenteredWithLargeText() {
        render(largeText = true)
        val avatar = compose.onNodeWithTag("service-avatar-flow", useUnmergedTree = true)
        avatar.performScrollTo().assertWidthIsEqualTo(52.dp).assertHeightIsEqualTo(52.dp)
        val outer = avatar.fetchSemanticsNode().boundsInRoot
        val glyph = compose.onNodeWithTag("service-glyph-flow", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(outer.center.x, glyph.center.x, 1f); assertEquals(outer.center.y, glyph.center.y, 1f)
        compose.onNodeWithTag("flow-open").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("service-status-flow", useUnmergedTree = true).performScrollTo().assertWidthIsAtLeast(160.dp)
        compose.saveScreenshot("flow-large-type")
    }

    @Test fun wallpaperColorsCanBeSwitchedBackToTheBrandPalette() {
        val settings = render(initialSettings = AppSettings(theme = ThemeMode.LIGHT, dynamicColor = true))
        compose.saveScreenshot("flow-wallpaper")
        navigateTo(R.string.nav_settings)
        compose.onNodeWithText(text(R.string.dynamic_colors)).performScrollTo().performClick()
        compose.runOnIdle { assertFalse(settings.value.dynamicColor) }
        navigateTo(R.string.nav_flow)
        compose.saveScreenshot("flow-brand-palette")
    }

    @Test fun wallpaperPaletteKeepsFlowTextReadableInBothThemes() {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        var style: ServiceStyle? = null
        compose.setContent { CaliforniumTheme(AppSettings(theme = theme.value, dynamicColor = true)) {
            val current = serviceStyle("flow"); SideEffect { style = current }
        } }
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            compose.runOnIdle { theme.value = mode }; compose.waitForIdle()
            compose.runOnIdle { val s = requireNotNull(style); val a = s.ink.luminance(); val b = s.start.luminance(); assertTrue((maxOf(a,b)+0.05f)/(minOf(a,b)+0.05f) >= 4.5f) }
        }
    }

    @Test fun recordNativePressNavigationAndSwitchMotion() {
        render()
        compose.mainClock.autoAdvance = false
        fun frames(stage: String, count: Int) { repeat(count) { frame -> compose.mainClock.advanceTimeBy(32); compose.saveScreenshot("motion-$stage-${frame.toString().padStart(2, '0')}") } }
        try {
            val hero = compose.onNodeWithTag("flow-hero")
            val resting = hero.fetchSemanticsNode().boundsInRoot.width
            hero.performTouchInput { down(center) }; frames("press",12)
            assertTrue(hero.fetchSemanticsNode().boundsInRoot.width < resting - 1f)
            hero.performTouchInput { up() }; frames("release",12)
            navigateTo(R.string.nav_settings); frames("navigation",16)
            compose.onNodeWithText(text(R.string.dynamic_colors)).performClick(); frames("switch",16)
        } finally { compose.mainClock.autoAdvance = true }
        compose.onNodeWithText(text(R.string.clear_data_title)).assertDoesNotExist()
        navigateTo(R.string.nav_flow)
        compose.onNodeWithTag("flow-open").assertIsDisplayed()
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
        onBrowserMount: (Boolean) -> Unit = {},
    ): MutableState<AppSettings> {
        val settings = mutableStateOf(initialSettings)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else density.fontScale)) {
            Box(if (largeText) Modifier.width(320.dp) else Modifier) {
            CaliforniumTheme(settings.value) {
                CaliforniumApp(
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
                    browserContent = {
                        androidx.compose.runtime.DisposableEffect(Unit) { onBrowserMount(true); onDispose { onBrowserMount(false) } }
                        Text(FAKE_BROWSER_CONTENT)
                    },
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
    val bitmap = if (name == "browser-sheet" || name.contains("dialog")) {
        requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
    } else roots[roots.fetchSemanticsNodes().lastIndex].captureToImage().asAndroidBitmap()
    FileOutputStream(file).use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
    }
    return file
}



