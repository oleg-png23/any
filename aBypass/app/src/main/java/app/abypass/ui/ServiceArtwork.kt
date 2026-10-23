package app.abypass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Local vector artwork: no image requests, bitmap allocations or continuously running animation. */
internal data class ServiceStyle(val start: Color, val end: Color, val ink: Color, val accent: Color)

@Composable
internal fun serviceIcon(id: String): ImageVector = when (id) {
    "flow" -> AppSymbols.Videocam
    "studio" -> AppSymbols.Terminal
    "labs" -> AppSymbols.Science
    else -> AppSymbols.AutoAwesome
}

@Composable
internal fun serviceStyle(id: String): ServiceStyle {
    val colors = MaterialTheme.colorScheme
    val (container, ink) = when (id) {
        "studio" -> colors.tertiaryContainer to colors.onTertiaryContainer
        "labs" -> colors.secondaryContainer to colors.onSecondaryContainer
        "gemini" -> colors.surfaceContainerHighest to colors.onSurface
        else -> colors.primary to colors.onPrimary
    }
    return ServiceStyle(container, container, ink, ink)
}

@Composable
internal fun ServiceBackdrop(id: String, style: ServiceStyle, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Brush.linearGradient(listOf(style.start, style.end)))) {
        // Decorations stay on the right and below opaque text contrast; the whole card clips them.
        val w = size.width
        val h = size.height
        val line = style.accent.copy(alpha = 0.12f)
        val fill = style.accent.copy(alpha = 0.07f)
        when (id) {
            "flow" -> {
                repeat(5) { index ->
                    val y = h * (0.08f + index * 0.22f)
                    val path = Path().apply {
                        moveTo(w * 0.58f, y + h * 0.20f)
                        cubicTo(w * 0.75f, y - h * 0.50f, w * 0.88f, y + h * 0.45f, w * 1.12f, y - h * 0.16f)
                    }
                    drawPath(path, line, style = Stroke(width = 2.dp.toPx()))
                }
                drawCircle(fill, w * 0.22f, Offset(w * 0.96f, h * 0.12f))
            }
            "studio" -> {
                val step = 24.dp.toPx()
                var x = w * 0.62f
                while (x < w) {
                    var y = step / 2
                    while (y < h) { drawCircle(line, 1.5.dp.toPx(), Offset(x, y)); y += step }
                    x += step
                }
                val path = Path().apply {
                    moveTo(w * 0.83f, h * 0.10f); lineTo(w * 0.73f, h * 0.27f); lineTo(w * 0.83f, h * 0.44f)
                    moveTo(w * 0.94f, h * 0.10f); lineTo(w * 1.04f, h * 0.27f); lineTo(w * 0.94f, h * 0.44f)
                }
                drawPath(path, line, style = Stroke(width = 7.dp.toPx()))
            }
            "labs" -> {
                repeat(3) { index ->
                    val radius = w * (0.14f + index * 0.09f)
                    drawCircle(line, radius, Offset(w * 0.94f, h * 0.23f), style = Stroke(1.5.dp.toPx()))
                }
                drawCircle(style.accent.copy(alpha = 0.15f), 11.dp.toPx(), Offset(w * 0.77f, h * 0.17f))
                drawCircle(fill, w * 0.14f, Offset(w * 0.98f, h * 0.62f))
            }
            else -> {
                repeat(5) { index ->
                    val center = Offset(w * (0.68f + (index % 3) * 0.15f), h * (0.13f + (index / 3) * 0.45f))
                    val radius = (if (index == 1) 32 else 14).dp.toPx()
                    val star = Path().apply {
                        repeat(8) { point ->
                            val angle = point * Math.PI / 4 - Math.PI / 2
                            val r = if (point % 2 == 0) radius else radius * 0.30f
                            val x = center.x + cos(angle).toFloat() * r
                            val y = center.y + sin(angle).toFloat() * r
                            if (point == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(star, if (index == 1) line else fill)
                }
            }
        }
    }
}

@Composable
internal fun ServiceAvatar(id: String, modifier: Modifier = Modifier) {
    val style = serviceStyle(id)
    Surface(modifier.requiredSize(52.dp).testTag("service-avatar-$id"), shape = when (id) { "studio" -> androidx.compose.foundation.shape.RoundedCornerShape(16.dp); "labs" -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 6.dp); else -> CircleShape }, color = style.accent, contentColor = style.start) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(serviceIcon(id), contentDescription = null, modifier = Modifier.requiredSize(28.dp).testTag("service-glyph-$id")) }
    }
}



