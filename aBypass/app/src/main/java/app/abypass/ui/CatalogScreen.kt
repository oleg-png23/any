@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.abypass.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.abypass.BuildConfig
import app.abypass.R
import app.abypass.data.*

/** Native M3 components, tonal surfaces and a single, accessible action per service. */
@Composable
internal fun ServicesScreen(
    settings: AppSettings,
    records: Map<String, ServiceRecord>,
    onSettingsChange: (AppSettings) -> Unit,
    onOpenService: (ServiceDefinition) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = Services.all.filter {
        (!favoritesOnly || it.id in settings.favorites) &&
            (it.name.contains(query, true) || it.description.contains(query, true))
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (LocalDensity.current.fontScale > 1.2f) 480.dp else 320.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Text("a", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.services_hero_title), style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.services_hero_description), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            TextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_services)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                },
                singleLine = true, shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !favoritesOnly, onClick = { favoritesOnly = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { SegmentedButtonDefaults.Icon(!favoritesOnly) { Icon(Icons.Outlined.Apps, null, Modifier.size(18.dp)) } },
                ) { Text(stringResource(R.string.all_services)) }
                SegmentedButton(
                    selected = favoritesOnly, onClick = { favoritesOnly = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { SegmentedButtonDefaults.Icon(favoritesOnly) { Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(18.dp)) } },
                ) { Text(stringResource(R.string.favorites)) }
            }
        }
        items(filtered, key = { it.id }) { service ->
            CatalogCard(
                service, records[service.id] ?: ServiceRecord(), service.id in settings.favorites,
                service.id in settings.adapters,
                onFavorite = {
                    val favorites = if (service.id in settings.favorites) settings.favorites - service.id else settings.favorites + service.id
                    onSettingsChange(settings.copy(favorites = favorites))
                },
                onOpen = { onOpenService(service) }, modifier = Modifier.animateItem(),
            )
        }
        if (filtered.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.no_services_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(if (favoritesOnly) R.string.no_favorites_description else R.string.no_services_description), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.services_footer), Modifier.padding(horizontal = 8.dp, vertical = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun CatalogCard(
    service: ServiceDefinition,
    record: ServiceRecord,
    favorite: Boolean,
    adapterEnabled: Boolean,
    onFavorite: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val style = serviceStyle(service.id)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.75f, stiffness = 450f), label = "service-press")
    val corner by animateDpAsState(if (pressed) 20.dp else 28.dp, spring(dampingRatio = 0.8f), label = "service-shape")
    val favoriteScale by animateFloatAsState(if (favorite) 1.12f else 1f, spring(dampingRatio = 0.55f), label = "service-favorite")
    Card(
        onClick = onOpen, modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = style.start, contentColor = style.ink),
        interactionSource = interaction,
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().clipToBounds()) {
                ServiceBackdrop(service.id, style, Modifier.matchParentSize())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ServiceAvatar(service.id)
                Spacer(Modifier.weight(1f))
                IconToggleButton(checked = favorite, onCheckedChange = { onFavorite() }) {
                    Icon(if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        stringResource(if (favorite) R.string.remove_favorite else R.string.add_favorite, service.name),
                        Modifier.graphicsLayer { scaleX = favoriteScale; scaleY = favoriteScale }, tint = style.ink)
                }
            }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(service.name, style = MaterialTheme.typography.headlineLarge)
                Text(service.description, style = MaterialTheme.typography.bodyLarge)
            }
            // Text scales freely; the action moves to its own row on narrow/large-type layouts.
            BoxWithConstraints {
                val stacked = maxWidth < 280.dp || LocalDensity.current.fontScale > 1.2f
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatusLabel(record.status, Modifier.fillMaxWidth().testTag("service-status-${service.id}"), style.ink)
                        CardAction(service, style, onOpen)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusLabel(record.status, Modifier.weight(1f).testTag("service-status-${service.id}"), style.ink)
                        FilledIconButton(onClick = onOpen, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = style.accent, contentColor = style.start)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.open_named_service, service.name))
                        }
                    }
                }
            }
            if (service.hasAdapter && adapterEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Bolt, null, Modifier.size(16.dp))
                    Text(stringResource(R.string.compatibility_enabled), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun CardAction(service: ServiceDefinition, style: ServiceStyle, onOpen: () -> Unit) {
    Button(onClick = onOpen, colors = ButtonDefaults.buttonColors(containerColor = style.accent, contentColor = style.start)) {
        Text(stringResource(R.string.open_service))
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.open_named_service, service.name), Modifier.size(20.dp))
    }
}
