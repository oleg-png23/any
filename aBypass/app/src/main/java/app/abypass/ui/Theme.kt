@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
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
    primary = Color(0xFF6550A5), onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF), onPrimaryContainer = Color(0xFF251347),
    secondary = Color(0xFF56632E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDEAAB), onSecondaryContainer = Color(0xFF1B2605),
    tertiary = Color(0xFF7E5435), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBC2), onTertiaryContainer = Color(0xFF2E1705),
    error = Color(0xFFB3261E), errorContainer = Color(0xFFF9DEDC),
    background = Color(0xFFF7F2FA), onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFF7F2FA), onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EB), onSurfaceVariant = Color(0xFF49454E),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFFBFF),
    surfaceContainer = Color(0xFFF0EAF5), surfaceContainerHigh = Color(0xFFECE5F2),
    surfaceContainerHighest = Color(0xFFE4DCEB),
    outline = Color(0xFF7A747F), outlineVariant = Color(0xFFCBC3D1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF372371),
    primaryContainer = Color(0xFF4E3A8B), onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFBFCE91), onSecondary = Color(0xFF2B370F),
    secondaryContainer = Color(0xFF414E20), onSecondaryContainer = Color(0xFFDDEAAB),
    tertiary = Color(0xFFF2B991), onTertiary = Color(0xFF49290F),
    tertiaryContainer = Color(0xFF633E22), onTertiaryContainer = Color(0xFFFFDBC2),
    error = Color(0xFFF2B8B5), errorContainer = Color(0xFF8C1D18),
    background = Color(0xFF141218), onBackground = Color(0xFFE8E0EC),
    surface = Color(0xFF141218), onSurface = Color(0xFFE8E0EC),
    surfaceVariant = Color(0xFF49454E), onSurfaceVariant = Color(0xFFCBC3D1),
    surfaceContainerLowest = Color(0xFF0F0D13), surfaceContainerLow = Color(0xFF1D1A22),
    surfaceContainer = Color(0xFF221F28), surfaceContainerHigh = Color(0xFF2C2833),
    surfaceContainerHighest = Color(0xFF37313E),
    outline = Color(0xFF958D9C), outlineVariant = Color(0xFF49454E),
)

internal val LocalWallpaperColors = androidx.compose.runtime.staticCompositionLocalOf { false }

private val AppTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun CaliforniumTheme(settings: AppSettings, content: @Composable () -> Unit) {
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
        MaterialExpressiveTheme(
            motionScheme = MotionScheme.expressive(),
            colorScheme = animatedColors(colors),
            typography = AppTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(4.dp),
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(32.dp),
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



