@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.abypass.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Every native press shares the expressive spatial spring and Android's motion-duration scale. */
internal data class PressFeedback(val source: MutableInteractionSource, val pressed: Boolean, val modifier: Modifier)

@Composable
internal fun rememberPressFeedback(pressedScale: Float = 0.92f): PressFeedback {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "press-feedback")
    return PressFeedback(source, pressed, Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
}

@Composable
internal fun MotionIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, filled: Boolean = false, content: @Composable () -> Unit) {
    val press = rememberPressFeedback()
    if (filled) FilledIconButton(onClick, modifier.then(press.modifier), enabled, interactionSource = press.source, content = content)
    else IconButton(onClick, modifier.then(press.modifier), enabled, interactionSource = press.source, content = content)
}

@Composable
internal fun MotionIconToggleButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val press = rememberPressFeedback()
    IconToggleButton(checked, onCheckedChange, modifier.then(press.modifier), interactionSource = press.source, content = content)
}

@Composable
internal fun MotionTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val press = rememberPressFeedback(0.96f)
    TextButton(onClick, modifier.then(press.modifier), enabled, interactionSource = press.source, content = content)
}

@Composable
internal fun MotionConfirmation(title: String, message: String, confirmLabel: String, dismissLabel: String, icon: ImageVector, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val motion = MaterialTheme.motionScheme
    val scope = rememberCoroutineScope()
    var ending by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progress.animateTo(1f, motion.defaultSpatialSpec()) }
    fun finish(action: () -> Unit) {
        if (ending) return
        ending = true
        scope.launch { progress.animateTo(0f, motion.fastEffectsSpec()); action() }
    }
    AlertDialog(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            scaleX = 0.86f + 0.14f * progress.value
            scaleY = scaleX
            translationY = (1f - progress.value) * 16.dp.toPx()
        },
        onDismissRequest = { finish(onDismiss) },
        icon = { Icon(icon, null) }, title = { Text(title) }, text = { Text(message) },
        confirmButton = { MotionTextButton({ finish(onConfirm) }, enabled = !ending) { Text(confirmLabel) } },
        dismissButton = { MotionTextButton({ finish(onDismiss) }, enabled = !ending) { Text(dismissLabel) } },
    )
}
