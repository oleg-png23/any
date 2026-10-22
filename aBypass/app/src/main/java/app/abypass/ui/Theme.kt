package app.abypass.ui

import android.os.Build
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.abypass.data.AppSettings
import app.abypass.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B57D0), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD), onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF146C2E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFC4EED0), onSecondaryContainer = Color(0xFF072711),
    tertiary = Color(0xFF805600), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7A5), onTertiaryContainer = Color(0xFF291800),
    error = Color(0xFFB3261E), errorContainer = Color(0xFFF9DEDC),
    background = Color(0xFFF8FAFD), onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFF8FAFD), onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFE1E7F0), onSurfaceVariant = Color(0xFF444746),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F4F9),
    surfaceContainer = Color(0xFFE9EEF6), surfaceContainerHigh = Color(0xFFE1E9F4),
    surfaceContainerHighest = Color(0xFFD9E2EF),
    outline = Color(0xFF747775), outlineVariant = Color(0xFFC4C7C5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0), onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF6DD58C), onSecondary = Color(0xFF003918),
    secondaryContainer = Color(0xFF0F5223), onSecondaryContainer = Color(0xFFC4EED0),
    tertiary = Color(0xFFFFD66D), onTertiary = Color(0xFF432C00),
    tertiaryContainer = Color(0xFF614100), onTertiaryContainer = Color(0xFFFFE7A5),
    error = Color(0xFFF2B8B5), errorContainer = Color(0xFF8C1D18),
    background = Color(0xFF101318), onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF101318), onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF444746), onSurfaceVariant = Color(0xFFC4C7C5),
    surfaceContainerLowest = Color(0xFF0B0E12), surfaceContainerLow = Color(0xFF191D24),
    surfaceContainer = Color(0xFF1E232C), surfaceContainerHigh = Color(0xFF292F3A),
    surfaceContainerHighest = Color(0xFF343C49),
    outline = Color(0xFF8E918F), outlineVariant = Color(0xFF444746),
)

internal val LocalWallpaperColors = androidx.compose.runtime.staticCompositionLocalOf { false }

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun ABypassTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val view = LocalView.current
    SideEffect {
        var current: Context = context
        while (current is ContextWrapper && current !is Activity) current = current.baseContext
        (current as? Activity)?.let {
            WindowCompat.getInsetsController(it.window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val colors = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalWallpaperColors provides (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
        MaterialTheme(
            colorScheme = animatedColors(colors),
            typography = AppTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(36.dp),
            ),
            content = content,
        )
    }
}

/** Animate only native theme tokens; browser views retain their own session and lifetime. */
@Composable
private fun animatedColors(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = motionColor(target.primary, "primary"), onPrimary = motionColor(target.onPrimary, "on-primary"),
        primaryContainer = motionColor(target.primaryContainer, "primary-container"), onPrimaryContainer = motionColor(target.onPrimaryContainer, "on-primary-container"),
        secondary = motionColor(target.secondary, "secondary"), onSecondary = motionColor(target.onSecondary, "on-secondary"),
        secondaryContainer = motionColor(target.secondaryContainer, "secondary-container"), onSecondaryContainer = motionColor(target.onSecondaryContainer, "on-secondary-container"),
        tertiaryContainer = motionColor(target.tertiaryContainer, "tertiary-container"), onTertiaryContainer = motionColor(target.onTertiaryContainer, "on-tertiary-container"),
        background = motionColor(target.background, "background"), onBackground = motionColor(target.onBackground, "on-background"),
        surface = motionColor(target.surface, "surface"), onSurface = motionColor(target.onSurface, "on-surface"),
        surfaceContainerLowest = motionColor(target.surfaceContainerLowest, "surface-lowest"),
        surfaceContainerLow = motionColor(target.surfaceContainerLow, "surface-low"),
        surfaceContainer = motionColor(target.surfaceContainer, "surface-container"),
        surfaceContainerHigh = motionColor(target.surfaceContainerHigh, "surface-high"),
        surfaceContainerHighest = motionColor(target.surfaceContainerHighest, "surface-highest"),
        surfaceVariant = motionColor(target.surfaceVariant, "surface-variant"),
        onSurfaceVariant = motionColor(target.onSurfaceVariant, "on-surface-variant"),
        outline = motionColor(target.outline, "outline"), outlineVariant = motionColor(target.outlineVariant, "outline-variant"),
    )
}

@Composable
private fun motionColor(color: Color, role: String): Color = animateColorAsState(color, tween(260), label = role).value

