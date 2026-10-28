@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package app.abypass.ui

import android.os.Build
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
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
    primary = Color(0xFF2E57C7), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF), onPrimaryContainer = Color(0xFF102A61),
    secondary = Color(0xFF50617A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6F6), onSecondaryContainer = Color(0xFF172D47),
    tertiary = Color(0xFF35666A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDECEF), onTertiaryContainer = Color(0xFF0A3436),
    error = Color(0xFFB3261E), errorContainer = Color(0xFFF9DEDC),
    background = Color(0xFFF6F8FC), onBackground = Color(0xFF171B23),
    surface = Color(0xFFF6F8FC), onSurface = Color(0xFF171B23),
    surfaceVariant = Color(0xFFE3E8F2), onSurfaceVariant = Color(0xFF454D5B),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF2F5FB),
    surfaceContainer = Color(0xFFEDF1F8), surfaceContainerHigh = Color(0xFFE8EDF7),
    surfaceContainerHighest = Color(0xFFE2E8F3),
    outline = Color(0xFF747D8D), outlineVariant = Color(0xFFC4CBD9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC6FF), onPrimary = Color(0xFF102A61),
    primaryContainer = Color(0xFF29477D), onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFB7C9E4), onSecondary = Color(0xFF203249),
    secondaryContainer = Color(0xFF344A62), onSecondaryContainer = Color(0xFFD6E4F7),
    tertiary = Color(0xFF94D2D3), onTertiary = Color(0xFF073739),
    tertiaryContainer = Color(0xFF214E50), onTertiaryContainer = Color(0xFFB0EDEE),
    error = Color(0xFFF2B8B5), errorContainer = Color(0xFF8C1D18),
    background = Color(0xFF10151E), onBackground = Color(0xFFDFE4ED),
    surface = Color(0xFF10151E), onSurface = Color(0xFFDFE4ED),
    surfaceVariant = Color(0xFF444B58), onSurfaceVariant = Color(0xFFC5CCD9),
    surfaceContainerLowest = Color(0xFF0B1017), surfaceContainerLow = Color(0xFF171C26),
    surfaceContainer = Color(0xFF1A202B), surfaceContainerHigh = Color(0xFF242B36),
    surfaceContainerHighest = Color(0xFF2F3643),
    outline = Color(0xFF8E98A8), outlineVariant = Color(0xFF444B58),
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
            colorScheme = colors,
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



