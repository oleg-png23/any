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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal data class ServiceStyle(val start: Color, val end: Color, val ink: Color, val accent: Color)

@Composable
internal fun serviceIcon(id: String): ImageVector = AppSymbols.Videocam

@Composable
internal fun serviceStyle(id: String): ServiceStyle {
    val colors = MaterialTheme.colorScheme
    return ServiceStyle(colors.primary, colors.primary, colors.onPrimary, colors.onPrimary)
}

/** Local vector waves: no downloads or permanently running decorative animation. */
@Composable
internal fun ServiceBackdrop(id: String, style: ServiceStyle, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        repeat(5) { index ->
            val y = h * (0.08f + index * 0.22f)
            val wave = Path().apply {
                moveTo(w * 0.58f, y + h * 0.20f)
                cubicTo(w * 0.75f, y - h * 0.50f, w * 0.88f, y + h * 0.45f, w * 1.12f, y - h * 0.16f)
            }
            drawPath(wave, style.ink.copy(alpha = 0.10f), style = Stroke(2.dp.toPx()))
        }
        drawCircle(style.ink.copy(alpha = 0.06f), w * 0.22f, Offset(w * 0.96f, h * 0.12f))
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
