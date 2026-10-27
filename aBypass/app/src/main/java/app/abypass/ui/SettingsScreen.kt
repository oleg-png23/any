@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.abypass.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.abypass.BuildConfig
import app.abypass.R
import app.abypass.data.*

/** Pixel-style connected settings: outer containment and small gaps replace divider lines. */
@Composable
internal fun SettingsScreen(settings: AppSettings, onSettingsChange: (AppSettings) -> Unit, onClearData: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader(stringResource(R.string.settings_title), stringResource(R.string.settings_subtitle), AppSymbols.Settings) }
        item { SettingsLabel(stringResource(R.string.appearance)) }
        item {
            val options = listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
            if (LocalDensity.current.fontScale > 1.2f) {
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (theme, label) -> ThemeTile(theme, stringResource(label), theme == settings.theme, { onSettingsChange(settings.copy(theme = theme)) }, Modifier.fillMaxWidth()) }
                }
            } else {
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEach { (theme, label) -> ThemeTile(theme, stringResource(label), theme == settings.theme, { onSettingsChange(settings.copy(theme = theme)) }, Modifier.weight(1f)) }
                }
            }
        }
        item {
            SettingsGroup {
                SwitchSetting(stringResource(R.string.dynamic_colors), stringResource(if (Build.VERSION.SDK_INT >= 31) R.string.dynamic_colors_description else R.string.dynamic_colors_unavailable), AppSymbols.Palette,
                    settings.dynamicColor, Build.VERSION.SDK_INT >= 31, { onSettingsChange(settings.copy(dynamicColor = it)) })
            }
        }
        item { SettingsLabel(stringResource(R.string.browsing)) }
        item {
            SettingsGroup {
                SwitchSetting(stringResource(R.string.desktop_mode), stringResource(R.string.desktop_mode_description), AppSymbols.DesktopWindows,
                    settings.desktopMode, onCheckedChange = { onSettingsChange(settings.copy(desktopMode = it)) }, accent = MaterialTheme.colorScheme.tertiaryContainer)
            }
        }
        item { SettingsLabel(stringResource(R.string.compatibility)) }
        item {
            SettingsGroup {
                Services.all.filter { it.hasAdapter }.forEachIndexed { index, service ->
                    SwitchSetting(service.name, stringResource(R.string.compatibility_setting_description), serviceIcon(service.id), service.id in settings.adapters,
                        onCheckedChange = { enabled -> onSettingsChange(settings.copy(adapters = if (enabled) settings.adapters + service.id else settings.adapters - service.id)) },
                        accent = MaterialTheme.colorScheme.secondaryContainer, index = index, count = 1)
                }
            }
        }
        item { Text(stringResource(R.string.compatibility_setting_footer), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SettingsLabel(stringResource(R.string.data_and_privacy)) }
        item {
            SettingsGroup {
                SegmentedListItem(
                    onClick = onClearData, shapes = ListItemDefaults.segmentedShapes(0, 1),
                    content = { Text(stringResource(R.string.clear_data_action)) },
                    supportingContent = { Text(stringResource(R.string.clear_data_subtitle)) },
                    leadingContent = { SettingIcon(Icons.Outlined.DeleteOutline, MaterialTheme.colorScheme.tertiaryContainer) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                )
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Shield, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Text("${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_independent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, Modifier.padding(start = 12.dp, top = 8.dp).semantics { heading() }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
}

@Composable
private fun SettingIcon(icon: ImageVector, accent: Color) {
    Surface(shape = CircleShape, color = accent, contentColor = contentColorFor(accent)) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(24.dp)) }
    }
}

@Composable
private fun ThemeTile(theme: ThemeMode, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val press = rememberPressFeedback(0.96f)
    val corner by animateDpAsState(if (press.pressed) 32.dp else if (selected) 24.dp else 20.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "theme-shape")
    val background by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerLowest, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "theme-choice")
    Surface(
        modifier = modifier.then(press.modifier).selectable(selected, interactionSource = press.source, indication = LocalIndication.current, role = Role.RadioButton, onClick = onClick),
        color = background, contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        shape = RoundedCornerShape(corner), border = BorderStroke(2.dp, if (selected) colors.primary else Color.Transparent),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(64.dp, 76.dp), contentAlignment = Alignment.Center) {
                ThemePreview(theme)
                if (selected) Icon(Icons.Outlined.CheckCircle, null, Modifier.align(Alignment.BottomEnd).size(22.dp), tint = colors.primary)
            }
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, minLines = 2)
        }
    }
}

@Composable
private fun ThemePreview(theme: ThemeMode) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(44.dp, 72.dp)) {
        val dark = Color(0xFF25212E); val light = Color(0xFFFFFBFF)
        drawRoundRect(if (theme == ThemeMode.DARK) dark else light, cornerRadius = CornerRadius(9.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Fill)
        if (theme == ThemeMode.SYSTEM) {
            drawRoundRect(dark, topLeft = Offset(size.width / 2, 0f), size = Size(size.width / 2, size.height), cornerRadius = CornerRadius(9.dp.toPx()))
        }
        drawRoundRect(accent.copy(alpha = 0.6f), topLeft = Offset(6.dp.toPx(), 14.dp.toPx()), size = Size(size.width - 12.dp.toPx(), 18.dp.toPx()), cornerRadius = CornerRadius(6.dp.toPx()))
        repeat(2) { i -> drawRoundRect(accent.copy(alpha = 0.3f), topLeft = Offset(6.dp.toPx(), (38 + i * 12).dp.toPx()), size = Size(size.width - 12.dp.toPx(), 8.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx())) }
    }
}

@Composable
private fun SwitchSetting(title: String, description: String, icon: ImageVector, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit, accent: Color = MaterialTheme.colorScheme.primaryContainer, index: Int = 0, count: Int = 1) {
    val background by animateColorAsState(if (checked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "setting-background")
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) }, enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        modifier = Modifier.semantics { role = Role.Switch; toggleableState = androidx.compose.ui.state.ToggleableState(checked) },
        content = { Text(title) }, supportingContent = { Text(description) },
        leadingContent = { SettingIcon(icon, accent) },
        trailingContent = { Switch(checked, onCheckedChange = null, enabled = enabled, thumbContent = { Icon(if (checked) Icons.Outlined.Check else Icons.Outlined.Close, null, Modifier.size(16.dp)) }) },
        colors = ListItemDefaults.colors(containerColor = background),
    )
}
