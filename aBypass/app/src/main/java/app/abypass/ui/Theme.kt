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
    primary = Color(0xFF006B5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9BF2DA),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4A635A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8DB),
    onSecondaryContainer = Color(0xFF072018),
    tertiary = Color(0xFF645484),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF21103E),
    background = Color(0xFFF5FAF6),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF5FAF6),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DE),
    onSurfaceVariant = Color(0xFF3F4943),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F0),
    surfaceContainer = Color(0xFFE9EFEB),
    surfaceContainerHigh = Color(0xFFE3EAE5),
    surfaceContainerHighest = Color(0xFFDEE4DF),
    outline = Color(0xFF6F7972),
    outlineVariant = Color(0xFFBFC9C1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD6BF),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005143),
    onPrimaryContainer = Color(0xFF9BF2DA),
    secondary = Color(0xFFB1CCBF),
    onSecondary = Color(0xFF1D352B),
    secondaryContainer = Color(0xFF334C41),
    onSecondaryContainer = Color(0xFFCDE8DB),
    tertiary = Color(0xFFCFC0EC),
    onTertiary = Color(0xFF36264F),
    tertiaryContainer = Color(0xFF4C3C69),
    onTertiaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = Color(0xFFBFC9C1),
    surfaceContainerLowest = Color(0xFF0A100D),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303632),
    outline = Color(0xFF89938B),
    outlineVariant = Color(0xFF3F4943),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
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
