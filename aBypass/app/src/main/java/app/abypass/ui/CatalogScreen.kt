@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package app.abypass.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.abypass.R
import app.abypass.data.*

/** Quick-access tiles; large text switches to a single, unconstrained column. */
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
    val motion = MaterialTheme.motionScheme
    BoxWithConstraints(modifier) {
        val largeType = LocalDensity.current.fontScale > 1.2f || maxWidth < 340.dp
        LazyVerticalGrid(
            columns = if (largeType) GridCells.Fixed(1) else GridCells.Adaptive(164.dp),
            modifier = Modifier.fillMaxSize().testTag("service-grid"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
                        Text(stringResource(R.string.services_hero_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(AppSymbols.AutoAwesome, null, Modifier.padding(14.dp).size(26.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                TextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_services)) },
                    leadingIcon = { Icon(AppSymbols.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, stringResource(R.string.clear_search)) }
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
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(false, true).forEach { favorites ->
                        val selected = favoritesOnly == favorites
                        val weight by animateFloatAsState(if (selected) 1.16f else 1f, motion.defaultSpatialSpec(), label = "filter-width")
                        val corner by animateDpAsState(if (selected) 28.dp else 12.dp, motion.fastSpatialSpec(), label = "filter-shape")
                        val color by animateColorAsState(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, motion.defaultEffectsSpec(), label = "filter-color")
                        Surface(
                            modifier = Modifier.weight(weight).heightIn(min = 52.dp).selectable(selected, role = Role.Tab, onClick = { favoritesOnly = favorites }),
                            shape = if (favorites) RoundedCornerShape(topStart = corner, bottomStart = corner, topEnd = 28.dp, bottomEnd = 28.dp) else RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = corner, bottomEnd = corner),
                            color = color,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (favorites) AppSymbols.Favorite else AppSymbols.Apps, null, Modifier.size(20.dp))
                                Text(stringResource(if (favorites) R.string.favorites else R.string.all_services), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            items(filtered, key = { it.id }) { service ->
                ServiceTile(
                    service = service, record = records[service.id] ?: ServiceRecord(),
                    favorite = service.id in settings.favorites, largeType = largeType,
                    onFavorite = {
                        val favorites = if (service.id in settings.favorites) settings.favorites - service.id else settings.favorites + service.id
                        onSettingsChange(settings.copy(favorites = favorites))
                    },
                    onOpen = { onOpenService(service) },
                    modifier = Modifier.animateItem(placementSpec = motion.defaultSpatialSpec()),
                )
            }
            if (filtered.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(AppSymbols.Search, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.no_services_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (favoritesOnly) R.string.no_favorites_description else R.string.no_services_description), style = MaterialTheme.typography.bodyMedium)
                }
            }

        }
    }
}

@Composable
private fun ServiceTile(
    service: ServiceDefinition,
    record: ServiceRecord,
    favorite: Boolean,
    largeType: Boolean,
    onFavorite: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val style = serviceStyle(service.id)
    val motion = MaterialTheme.motionScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, motion.fastSpatialSpec(), label = "tile-scale")
    val corner by animateDpAsState(if (pressed) 48.dp else 32.dp, motion.fastSpatialSpec(), label = "tile-shape")
    Card(
        onClick = onOpen, interactionSource = interaction,
        modifier = modifier.fillMaxWidth().testTag("service-tile-${service.id}").graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = style.start, contentColor = style.ink),
    ) {
        Box(Modifier.fillMaxWidth()) {
            ServiceBackdrop(service.id, style, Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().heightIn(min = if (largeType) 240.dp else 224.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ServiceAvatar(service.id)
                    Spacer(Modifier.weight(1f))
                    IconToggleButton(checked = favorite, onCheckedChange = { onFavorite() }, modifier = Modifier.size(48.dp)) {
                        AnimatedContent(targetState = favorite, transitionSpec = {
                            (scaleIn(motion.fastSpatialSpec(), initialScale = 0.4f) + fadeIn(motion.fastEffectsSpec())) togetherWith
                                (scaleOut(motion.fastSpatialSpec(), targetScale = 0.4f) + fadeOut(motion.fastEffectsSpec()))
                        }, label = "favorite") { checked ->
                            Icon(if (checked) AppSymbols.Favorite else AppSymbols.FavoriteBorder,
                                stringResource(if (checked) R.string.remove_favorite else R.string.add_favorite, service.name), tint = style.ink)
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(service.name, style = MaterialTheme.typography.titleLarge)
                    Text(service.description, style = MaterialTheme.typography.bodySmall)
                }
                if (record.status == CheckStatus.NOT_CHECKED) {
                    Row(Modifier.fillMaxWidth().testTag("service-status-${service.id}"), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.open_service), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.open_named_service, service.name), Modifier.size(20.dp))
                    }
                } else {
                    StatusLabel(record.status, Modifier.fillMaxWidth().testTag("service-status-${service.id}"), style.ink)
                }
            }
        }
    }
}
