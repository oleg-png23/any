@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.abypass.ui

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.abypass.R
import app.abypass.BuildConfig
import app.abypass.data.AppSettings
import app.abypass.data.BrowserUiState
import app.abypass.data.CheckStatus
import app.abypass.data.DiagnosticEvent
import app.abypass.data.ServiceDefinition
import app.abypass.data.ServiceRecord
import app.abypass.data.Services
import app.abypass.data.ThemeMode
import java.net.URI
import java.text.DateFormat
import java.util.Date

private enum class Destination(@StringRes val title: Int, val icon: ImageVector) {
    SERVICES(R.string.nav_services, Icons.Outlined.Apps),
    CHECKS(R.string.nav_checks, Icons.Outlined.VerifiedUser),
    SETTINGS(R.string.nav_settings, Icons.Outlined.Settings),
}

@Composable
fun ABypassApp(
    settings: AppSettings,
    records: Map<String, ServiceRecord>,
    events: List<DiagnosticEvent>,
    browser: BrowserUiState,
    onSettingsChange: (AppSettings) -> Unit,
    onOpenService: (ServiceDefinition) -> Unit,
    onCloseBrowser: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onDesktopToggle: () -> Unit,
    onExternalOpen: () -> Unit,
    onClearData: () -> Unit,
    onExportDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
    browserContent: @Composable () -> Unit,
) {
    var destinationName by rememberSaveable { mutableStateOf(Destination.SERVICES.name) }
    val destination = Destination.valueOf(destinationName)
    val screenState = rememberSaveableStateHolder()
    var showClearData by remember { mutableStateOf(false) }
    var showClearDiagnostics by remember { mutableStateOf(false) }

    if (showClearData) {
        AlertDialog(
            onDismissRequest = { showClearData = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.clear_data_title)) },
            text = { Text(stringResource(R.string.clear_data_description)) },
            confirmButton = {
                TextButton(onClick = { showClearData = false; onClearData() }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.clear_data_confirm))
                }
            },
            dismissButton = {
                    TextButton(onClick = { showClearData = false }) { Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (showClearDiagnostics) {
        AlertDialog(
            onDismissRequest = { showClearDiagnostics = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.clear_diagnostics_title)) },
            text = { Text(stringResource(R.string.clear_diagnostics_description)) },
            confirmButton = {
                TextButton(onClick = { showClearDiagnostics = false; onClearDiagnostics() }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDiagnostics = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (browser.serviceId != null) {
        BrowserScreen(
            browser = browser,
            desktopMode = settings.desktopMode,
            onClose = onCloseBrowser,
            onBack = onBack,
            onForward = onForward,
            onReload = onReload,
            onDesktopToggle = onDesktopToggle,
            onExternalOpen = onExternalOpen,
            browserContent = browserContent,
        )
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                if (!wide) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        Destination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = item == destination,
                                onClick = { destinationName = item.name },
                                icon = { NavigationIcon(item.icon, item == destination) },
                                label = { Text(stringResource(item.title)) },
                            )
                        }
                    }
                }
            },
        ) { insets ->
            Row(Modifier.fillMaxSize().padding(insets)) {
                if (wide) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        header = {
                            Surface(
                                modifier = Modifier.padding(vertical = 24.dp).size(56.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Shield, contentDescription = stringResource(R.string.app_name), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        },
                    ) {
                        Destination.entries.forEach { item ->
                            NavigationRailItem(
                                selected = item == destination,
                                onClick = { destinationName = item.name },
                                icon = { NavigationIcon(item.icon, item == destination) },
                                label = { Text(stringResource(item.title)) },
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    val pageModifier = Modifier.widthIn(max = 980.dp).fillMaxSize()
                    AnimatedContent(
                        targetState = destination,
                        modifier = pageModifier,
                        transitionSpec = {
                            (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 24 }) togetherWith fadeOut(tween(120))
                        },
                        label = "screen-transition",
                    ) { target ->
                        screenState.SaveableStateProvider(target.name) {
                            when (target) {
                                Destination.SERVICES -> ServicesScreen(settings, records, onSettingsChange, onOpenService, Modifier.fillMaxSize())
                                Destination.CHECKS -> ChecksScreen(
                                    records, events, onOpenService, onExportDiagnostics,
                                    { showClearDiagnostics = true }, Modifier.fillMaxSize(),
                                )
                                Destination.SETTINGS -> SettingsScreen(
                                    settings, onSettingsChange, { showClearData = true }, Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationIcon(icon: ImageVector, selected: Boolean) {
    val scale by animateFloatAsState(if (selected) 1.1f else 1f, spring(dampingRatio = 0.65f), label = "navigation-icon")
    Icon(icon, contentDescription = null, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
}

@Composable
private fun ChecksScreen(
    records: Map<String, ServiceRecord>,
    events: List<DiagnosticEvent>,
    onOpenService: (ServiceDefinition) -> Unit,
    onExportDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PageHeader(stringResource(R.string.checks_title), stringResource(R.string.checks_subtitle), Icons.Outlined.VerifiedUser) }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(stringResource(R.string.checks_explanation), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        items(Services.all, key = { "record-${it.id}" }) { service ->
            val record = records[service.id] ?: ServiceRecord()
            val style = serviceStyle(service.id)
            Card(
                onClick = { onOpenService(service) },
                colors = CardDefaults.cardColors(containerColor = style.start, contentColor = style.ink),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.animateItem(),
            ) {
                Box {
                    ServiceBackdrop(service.id, style, Modifier.matchParentSize())
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        ServiceAvatar(service.id)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(service.name, style = MaterialTheme.typography.titleMedium)
                            StatusLabel(record.status, fallbackColor = style.ink)
                            if (record.checkedAt > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(formatTimestamp(record.checkedAt), style = MaterialTheme.typography.bodySmall, color = style.ink)
                                }
                            }
                        }
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = stringResource(R.string.open_named_service, service.name), tint = style.accent)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth()) {
                    ListItem(
                        modifier = Modifier.clickable { detailsExpanded = !detailsExpanded },
                        headlineContent = { Text(stringResource(R.string.technical_details)) },
                        supportingContent = { Text(stringResource(R.string.technical_details_subtitle)) },
                        leadingContent = { Icon(Icons.Outlined.HelpOutline, contentDescription = null) },
                        trailingContent = { Icon(if (detailsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = stringResource(if (detailsExpanded) R.string.collapse else R.string.expand)) },
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    )
                    AnimatedVisibility(visible = detailsExpanded, enter = expandVertically(tween(220)) + fadeIn(tween(180)), exit = shrinkVertically(tween(180)) + fadeOut(tween(120))) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(stringResource(R.string.compatibility_limits), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(R.string.diag_privacy), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = onExportDiagnostics, enabled = events.isNotEmpty()) {
                                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.export_diagnostics))
                                    }
                                    IconButton(onClick = onClearDiagnostics, enabled = events.isNotEmpty()) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.clear_diagnostics_title))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (detailsExpanded) {
            item { SectionLabel(stringResource(R.string.recent_events), Icons.Outlined.Schedule) }
            if (events.isEmpty()) {
                item { EmptyState(Icons.Outlined.VerifiedUser, stringResource(R.string.no_events_title), stringResource(R.string.no_events_description)) }
            } else {
                items(events.takeLast(100).asReversed()) { event ->
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(serviceIcon(event.serviceId), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(Services.find(event.serviceId)?.name ?: stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                Text(formatTimestamp(event.time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(event.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PageHeader(stringResource(R.string.settings_title), stringResource(R.string.settings_subtitle), Icons.Outlined.Settings) }
        item { SectionLabel(stringResource(R.string.appearance), Icons.Outlined.Palette) }
        item {
            SettingsGroup {
                ThemeOption(ThemeMode.SYSTEM, settings.theme, Icons.Outlined.BrightnessAuto, R.string.theme_system) { onSettingsChange(settings.copy(theme = it)) }
                ThemeOption(ThemeMode.LIGHT, settings.theme, Icons.Outlined.LightMode, R.string.theme_light) { onSettingsChange(settings.copy(theme = it)) }
                ThemeOption(ThemeMode.DARK, settings.theme, Icons.Outlined.DarkMode, R.string.theme_dark) { onSettingsChange(settings.copy(theme = it)) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchSetting(
                    title = stringResource(R.string.dynamic_colors),
                    description = stringResource(if (Build.VERSION.SDK_INT >= 31) R.string.dynamic_colors_description else R.string.dynamic_colors_unavailable),
                    icon = Icons.Outlined.Palette,
                    checked = settings.dynamicColor,
                    enabled = Build.VERSION.SDK_INT >= 31,
                    onCheckedChange = { onSettingsChange(settings.copy(dynamicColor = it)) },
                )
            }
        }
        item { SectionLabel(stringResource(R.string.browsing), Icons.Outlined.DesktopWindows) }
        item {
            SettingsGroup {
                SwitchSetting(
                    title = stringResource(R.string.desktop_mode),
                    description = stringResource(R.string.desktop_mode_description),
                    icon = Icons.Outlined.DesktopWindows,
                    checked = settings.desktopMode,
                    onCheckedChange = { onSettingsChange(settings.copy(desktopMode = it)) },
                )
            }
        }
        item { SectionLabel(stringResource(R.string.compatibility), Icons.Outlined.VerifiedUser) }
        item {
            SettingsGroup {
                Services.all.filter { it.hasAdapter }.forEachIndexed { index, service ->
                    if (index != 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SwitchSetting(
                        title = service.name,
                        description = stringResource(R.string.compatibility_setting_description),
                        icon = serviceIcon(service.id),
                        checked = service.id in settings.adapters,
                        onCheckedChange = { enabled ->
                            val adapters = if (enabled) settings.adapters + service.id else settings.adapters - service.id
                            onSettingsChange(settings.copy(adapters = adapters))
                        },
                    )
                }
            }
        }
        item {
            Text(stringResource(R.string.compatibility_setting_footer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
        }
        item { SectionLabel(stringResource(R.string.data_and_privacy), Icons.Outlined.Lock) }
        item {
            SettingsGroup {
                ListItem(
                    modifier = Modifier.clickable(onClick = onClearData),
                    headlineContent = { Text(stringResource(R.string.clear_data_action)) },
                    supportingContent = { Text(stringResource(R.string.clear_data_subtitle)) },
                    leadingContent = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                )
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Shield, contentDescription = null)
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    }
                    Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.about_independent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), content = { content() })
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) }
    }
}

@Composable
private fun ThemeOption(
    value: ThemeMode,
    selected: ThemeMode,
    icon: ImageVector,
    @StringRes label: Int,
    onSelect: (ThemeMode) -> Unit,
) {
    ListItem(
        modifier = Modifier.selectable(selected = value == selected, onClick = { onSelect(value) }, role = Role.RadioButton),
        headlineContent = { Text(stringResource(label)) },
        leadingContent = { SettingIcon(icon) },
        trailingContent = { RadioButton(selected = value == selected, onClick = null) },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = { SettingIcon(icon) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Composable
private fun BrowserScreen(
    browser: BrowserUiState,
    desktopMode: Boolean,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onDesktopToggle: () -> Unit,
    onExternalOpen: () -> Unit,
    browserContent: @Composable () -> Unit,
) {
    val service = Services.find(browser.serviceId)
    val progress by animateFloatAsState(browser.progress.coerceIn(0, 100) / 100f, tween(220), label = "page-progress")
    var showPageInfo by remember(browser.serviceId) { mutableStateOf(false) }
    if (showPageInfo) {
        AlertDialog(
            onDismissRequest = { showPageInfo = false },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text(service?.name ?: stringResource(R.string.page)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(browser.url, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.browser_page_info), style = MaterialTheme.typography.bodyMedium)
                    if (service?.hasAdapter == true) {
                        Text(stringResource(if (browser.extensionReady) R.string.browser_extension_ready else R.string.browser_extension_waiting), style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(stringResource(R.string.browser_no_adapter), style = MaterialTheme.typography.bodySmall)
                    }
                    if (service?.id == "studio") {
                        Text(stringResource(R.string.studio_access_help), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPageInfo = false }) { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.got_it)) } },
        )
    }
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(serviceIcon(service?.id.orEmpty()), contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(service?.name ?: stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(domainOf(browser.url), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close_service)) } },
                    actions = { IconButton(onClick = { showPageInfo = true }) { Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.page_information)) } },
                )
                if (browser.loading) {
                    if (browser.progress <= 0) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            Surface(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, enabled = browser.canGoBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.browser_back)) }
                    IconButton(onClick = onForward, enabled = browser.canGoForward) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.browser_forward)) }
                    IconButton(onClick = onReload) { Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.browser_reload)) }
                    IconToggleButton(checked = desktopMode, onCheckedChange = { onDesktopToggle() }) {
                        Icon(Icons.Outlined.DesktopWindows, contentDescription = stringResource(R.string.desktop_mode), tint = if (desktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onExternalOpen) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = stringResource(R.string.browser_external)) }
                }
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            AnimatedVisibility(browser.accessRestricted) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Info, null)
                        Text(stringResource(R.string.studio_access_title), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { showPageInfo = true }) {
                            Icon(Icons.Outlined.HelpOutline, stringResource(R.string.studio_access_details))
                        }
                    }
                }
            }
            if (browser.error != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(stringResource(R.string.browser_error), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onReload) { Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.retry)) }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { browserContent() }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String, icon: ImageVector) {
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp).size(28.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f).semantics { heading() })
        }
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionLabel(label: String, icon: ImageVector) {
    Row(Modifier.padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun StatusLabel(status: CheckStatus, modifier: Modifier = Modifier, fallbackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val (icon, label) = when (status) {
        CheckStatus.NOT_CHECKED -> Icons.Outlined.HelpOutline to R.string.status_not_checked
        CheckStatus.PAGE_OPENED -> Icons.Outlined.CheckCircleOutline to R.string.status_page_opened
        CheckStatus.SIGN_IN_REQUIRED -> Icons.Outlined.Login to R.string.status_sign_in
        CheckStatus.NETWORK_ERROR -> Icons.Outlined.ErrorOutline to R.string.status_network_error
        CheckStatus.ACCESS_RESTRICTED -> Icons.Outlined.Lock to R.string.status_access_restricted
    }
    val color = when (status) {
        CheckStatus.PAGE_OPENED -> MaterialTheme.colorScheme.primary
        CheckStatus.NETWORK_ERROR -> MaterialTheme.colorScheme.error
        else -> fallbackColor
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

private fun formatTimestamp(time: Long): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time))

private fun domainOf(url: String): String = runCatching { URI(url).host }.getOrNull() ?: ""


