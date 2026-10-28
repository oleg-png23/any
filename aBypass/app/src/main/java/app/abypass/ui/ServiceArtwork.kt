package app.abypass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal data class ServiceStyle(val start: Color, val end: Color, val ink: Color, val accent: Color)

@Composable
internal fun serviceIcon(id: String): ImageVector = AppSymbols.Videocam

@Composable
internal fun serviceStyle(id: String): ServiceStyle {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val background = if (dark) colors.primaryContainer else colors.primary
    val content = if (dark) colors.onPrimaryContainer else colors.onPrimary
    return ServiceStyle(background, background, content, content)
}

/** A single static accent avoids redrawing multiple decorative paths during interaction. */
@Composable
internal fun ServiceBackdrop(id: String, style: ServiceStyle, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(style.ink.copy(alpha = 0.07f), size.minDimension * 0.45f, Offset(size.width, 0f))
    }
}

@Composable
internal fun ServiceAvatar(id: String, modifier: Modifier = Modifier) {
    val style = serviceStyle(id)
    Surface(modifier.requiredSize(52.dp).testTag("service-avatar-$id"), shape = CircleShape, color = style.ink, contentColor = style.start) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(AppSymbols.Videocam, null, Modifier.requiredSize(28.dp).testTag("service-glyph-$id"))
        }
    }
}
