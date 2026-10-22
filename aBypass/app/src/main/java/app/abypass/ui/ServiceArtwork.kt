package app.abypass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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

internal fun serviceIcon(id: String): ImageVector = when (id) {
    "flow" -> Icons.Outlined.Videocam
    "studio" -> Icons.Outlined.Terminal
    "labs" -> Icons.Outlined.Science
    else -> Icons.Outlined.AutoAwesome
}

@Composable
internal fun serviceStyle(id: String): ServiceStyle {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.35f
    return when (id) {
        "flow" -> if (dark) ServiceStyle(Color(0xFF142F43), Color(0xFF1B4059), Color(0xFFE4F3FF), Color(0xFF9CCFFF))
            else ServiceStyle(Color(0xFFDDF5FF), Color(0xFFC9E3F9), Color(0xFF16334C), Color(0xFF155D96))
        "studio" -> if (dark) ServiceStyle(Color(0xFF2E2441), Color(0xFF283450), Color(0xFFF1E9FF), Color(0xFFD4BAFF))
            else ServiceStyle(Color(0xFFF0E5FF), Color(0xFFDFE6FF), Color(0xFF35264F), Color(0xFF7146A4))
        "labs" -> if (dark) ServiceStyle(Color(0xFF3D2D1B), Color(0xFF313A26), Color(0xFFFFEDCC), Color(0xFFFFD28C))
            else ServiceStyle(Color(0xFFFFF0CF), Color(0xFFF0E9D5), Color(0xFF493114), Color(0xFF8B530A))
        else -> if (dark) ServiceStyle(Color(0xFF292E4B), Color(0xFF1A3B3D), Color(0xFFE9ECFF), Color(0xFFC4C9FF))
            else ServiceStyle(Color(0xFFE8E5FF), Color(0xFFD8F2EF), Color(0xFF2C3157), Color(0xFF5556A3))
    }
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
    Surface(modifier.size(52.dp), shape = MaterialTheme.shapes.medium, color = style.accent.copy(alpha = 0.12f), contentColor = style.accent) {
        Box(contentAlignment = Alignment.Center) { Icon(serviceIcon(id), contentDescription = null, modifier = Modifier.size(28.dp)) }
    }
}
