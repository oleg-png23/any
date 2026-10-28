@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.abypass.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** Use Material's native feedback without scaling or animating the whole control. */
@Composable
internal fun MotionIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, filled: Boolean = false, content: @Composable () -> Unit) {
    if (filled) FilledIconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    else IconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

@Composable
internal fun MotionTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

@Composable
internal fun MotionConfirmation(title: String, message: String, confirmLabel: String, dismissLabel: String, icon: ImageVector, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, null) }, title = { Text(title) }, text = { Text(message) },
        confirmButton = { MotionTextButton(onConfirm) { Text(confirmLabel) } },
        dismissButton = { MotionTextButton(onDismiss) { Text(dismissLabel) } },
    )
}
