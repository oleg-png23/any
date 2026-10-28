@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package app.abypass.ui

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.StringRes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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

private enum class Destination(@StringRes val title: Int, val icon: Int) {
    FLOW(R.string.nav_flow, R.drawable.symbol_videocam),
    CHECKS(R.string.nav_checks, R.drawable.symbol_verified_user),
    SETTINGS(R.string.nav_settings, R.drawable.symbol_settings),
}

@Composable
fun CaliforniumApp(
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
    onStopLoading: () -> Unit,
    onDesktopToggle: () -> Unit,
    onExternalOpen: () -> Unit,
    onClearData: () -> Unit,
    onExportDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
    browserContent: @Composable () -> Unit,
) {
    var destinationName by rememberSaveable { mutableStateOf(Destination.FLOW.name) }
    val destination = runCatching { Destination.valueOf(destinationName) }.getOrDefault(Destination.FLOW)
    val screenState = rememberSaveableStateHolder()
    var showClearData by remember { mutableStateOf(false) }
    var showClearDiagnostics by remember { mutableStateOf(false) }

    if (showClearData) MotionConfirmation(
        stringResource(R.string.clear_data_title), stringResource(R.string.clear_data_description),
        stringResource(R.string.clear_data_confirm), stringResource(R.string.cancel), Icons.Outlined.DeleteOutline,
        onConfirm = { showClearData = false; onClearData() }, onDismiss = { showClearData = false },
    )
    if (showClearDiagnostics) MotionConfirmation(
        stringResource(R.string.clear_diagnostics_title), stringResource(R.string.clear_diagnostics_description),
        stringResource(R.string.clear), stringResource(R.string.cancel), Icons.Outlined.DeleteOutline,
        onConfirm = { showClearDiagnostics = false; onClearDiagnostics() }, onDismiss = { showClearDiagnostics = false },
    )

    // Keep one browser host during progress updates, without animating the native view.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        if (browser.serviceId != null) {
            BrowserScreen(
                browser = browser,
                desktopMode = settings.desktopMode,
                adapterEnabled = "flow" in settings.adapters,
                onClose = onCloseBrowser,
                onBack = onBack,
                onForward = onForward,
                onReload = onReload,
                onStopLoading = onStopLoading,
                onDesktopToggle = onDesktopToggle,
                onExternalOpen = onExternalOpen,
                onExportDiagnostics = onExportDiagnostics,
                browserContent = browserContent,
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 600.dp
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surface,
                    bottomBar = {
                        if (!wide) {
                            ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Destination.entries.forEach { item ->
                                    ShortNavigationBarItem(
                                        selected = item == destination,
                                        onClick = { destinationName = item.name },
                                        icon = { NavigationIcon(item) },
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
                                        icon = { NavigationIcon(item) },
                                        label = { Text(stringResource(item.title)) },
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                }
                            }
                        }
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.widthIn(max = 980.dp).fillMaxSize()) {
                                screenState.SaveableStateProvider(destination.name) {
                                    when (destination) {
                                        Destination.FLOW -> FlowHomeScreen(settings, records["flow"] ?: ServiceRecord(), { onOpenService(Services.Flow) }, { destinationName = Destination.CHECKS.name }, Modifier.fillMaxSize())
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
    }
}

@Composable
private fun NavigationIcon(destination: Destination) {
    val icon = ImageVector.vectorResource(destination.icon)
    Icon(icon, contentDescription = null)
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PageHeader(stringResource(R.string.checks_title), stringResource(R.string.checks_subtitle), AppSymbols.VerifiedUser) }
        items(Services.all, key = { "record-${it.id}" }) { service ->
            val record = records[service.id] ?: ServiceRecord()
            val style = serviceStyle(service.id)
            Card(
                onClick = { onOpenService(service) },
                colors = CardDefaults.cardColors(containerColor = style.start, contentColor = style.ink),
                shape = MaterialTheme.shapes.extraLarge,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.open_named_service, service.name), tint = style.accent)
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.checks_explanation), Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth()) {
                    ListItem(
                        onClick = { detailsExpanded = !detailsExpanded },
                        content = { Text(stringResource(R.string.technical_details)) },
                        supportingContent = { Text(stringResource(R.string.technical_details_subtitle)) },
                        leadingContent = { Icon(Icons.Outlined.HelpOutline, contentDescription = null) },
                        trailingContent = { Icon(if (detailsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = stringResource(if (detailsExpanded) R.string.collapse else R.string.expand)) },
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    )
                    if (detailsExpanded) {
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
                                    MotionIconButton(onClick = onClearDiagnostics, enabled = events.isNotEmpty()) {
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
                item { EmptyState(AppSymbols.VerifiedUser, stringResource(R.string.no_events_title), stringResource(R.string.no_events_description)) }
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
private fun BrowserScreen(
    browser: BrowserUiState,
    desktopMode: Boolean,
    adapterEnabled: Boolean,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStopLoading: () -> Unit,
    onDesktopToggle: () -> Unit,
    onExternalOpen: () -> Unit,
    onExportDiagnostics: () -> Unit,
    browserContent: @Composable () -> Unit,
) {
    val service = Services.find(browser.serviceId)
    var showPageInfo by remember(browser.serviceId) { mutableStateOf(false) }
    val pageSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun closeInfoThen(action: () -> Unit) {
        showPageInfo = false
        action()
    }
    if (showPageInfo) {
        ModalBottomSheet(onDismissRequest = { showPageInfo = false }, sheetState = pageSheet) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceAvatar(service?.id.orEmpty())
                    Column(Modifier.weight(1f)) {
                        Text(service?.name ?: stringResource(R.string.page), style = MaterialTheme.typography.headlineSmall)
                        Text(domainOf(browser.url), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MotionIconButton(onClick = { closeInfoThen {} }) { Icon(Icons.Outlined.Close, stringResource(R.string.got_it)) }
                }
                Text(stringResource(R.string.browser_page_info), style = MaterialTheme.typography.bodyMedium)
                if (service?.hasAdapter == true && !adapterEnabled) {
                    Text(stringResource(R.string.compatibility_disabled), style = MaterialTheme.typography.bodyMedium)
                } else if (service?.hasAdapter == true) {
                    Text(stringResource(if (browser.extensionReady) R.string.browser_extension_ready else R.string.browser_extension_waiting), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(stringResource(R.string.browser_no_adapter), style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                ListItem(
                    onClick = { closeInfoThen(onExportDiagnostics) },
                    content = { Text(stringResource(R.string.export_diagnostics)) },
                    leadingContent = { Icon(Icons.Outlined.Download, null) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    onClick = { closeInfoThen(onDesktopToggle) },
                    content = { Text(stringResource(R.string.desktop_mode)) },
                    leadingContent = { Icon(AppSymbols.DesktopWindows, null) },
                    trailingContent = { Switch(checked = desktopMode, onCheckedChange = null) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    onClick = { closeInfoThen(onExternalOpen) },
                    content = { Text(stringResource(R.string.browser_external)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        val fallback = service?.name ?: stringResource(R.string.page)
                        Text(domainOf(browser.url).ifBlank { fallback }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = { MotionIconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close_service)) } },
                    actions = { MotionIconButton(onClick = { showPageInfo = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.page_information)) } },
                )
                Box(Modifier.fillMaxWidth().height(4.dp)) {
                    if (browser.loading) {
                        if (browser.progress <= 0) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                        else LinearProgressIndicator(progress = { browser.progress.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp))
                    }
                }
            }
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                HorizontalFloatingToolbar(expanded = true) {
                    MotionIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.browser_back)) }
                    MotionIconButton(onClick = onForward, enabled = browser.canGoForward) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.browser_forward)) }
                    MotionIconButton(onClick = if (browser.loading) onStopLoading else onReload, filled = true) {
                        Icon(if (browser.loading) Icons.Outlined.Stop else Icons.Outlined.Refresh,
                            contentDescription = stringResource(if (browser.loading) R.string.browser_stop_loading else R.string.browser_reload))
                    }
                }
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            if (browser.loadStopped) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Info, null)
                        Text(stringResource(R.string.browser_loading_stopped), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        MotionTextButton(onClick = onReload) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
            if (browser.error != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(stringResource(R.string.browser_error), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        MotionTextButton(onClick = onReload) { Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.retry)) }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { browserContent() }
        }
    }
}

@Composable
internal fun PageHeader(title: String, subtitle: String, icon: ImageVector) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f).semantics { heading() })
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    // Status text shares its surface's on-container role; unrelated theme accents may be unreadable here.
    val color = fallbackColor
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

private fun formatTimestamp(time: Long): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time))

private fun domainOf(url: String): String = runCatching { URI(url).host }.getOrNull() ?: ""



