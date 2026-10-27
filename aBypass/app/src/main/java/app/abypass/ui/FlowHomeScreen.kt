@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.abypass.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.abypass.R
import app.abypass.data.*

@Composable
internal fun FlowHomeScreen(settings: AppSettings, record: ServiceRecord, onOpen: () -> Unit, onChecks: () -> Unit, modifier: Modifier = Modifier) {
    val motion = MaterialTheme.motionScheme
    val largeType = LocalDensity.current.fontScale > 1.2f
    val press = rememberPressFeedback(0.97f)
    val radius by animateDpAsState(if (press.pressed) 52.dp else 36.dp, motion.defaultSpatialSpec(), label = "flow-card-shape")
    val buttonPress = rememberPressFeedback(0.96f)
    val style = serviceStyle("flow")
    LazyColumn(modifier.testTag("flow-home"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item(key = "brand") {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f).semantics { heading() })
                    if (!largeType) BrandMark()
                }
                Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "flow") {
            Card(onClick = onOpen, interactionSource = press.source,
                modifier = Modifier.fillMaxWidth().testTag("flow-hero").then(press.modifier),
                shape = RoundedCornerShape(radius), colors = CardDefaults.cardColors(containerColor = style.start, contentColor = style.ink)) {
                Box(Modifier.fillMaxWidth()) {
                    ServiceBackdrop("flow", style, Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServiceAvatar("flow")
                            Text("Google Flow", style = MaterialTheme.typography.titleMedium)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Flow", fontSize = 52.sp, lineHeight = 60.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.flow_description), style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bolt, null, Modifier.size(18.dp))
                            AnimatedContent("flow" in settings.adapters, transitionSpec = {
                                fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
                            }, label = "compatibility-state") { enabled ->
                                Text(stringResource(if (enabled) R.string.compatibility_enabled else R.string.compatibility_disabled), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Button(onClick = onOpen, interactionSource = buttonPress.source,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("flow-open").then(buttonPress.modifier),
                            colors = ButtonDefaults.buttonColors(containerColor = style.ink, contentColor = style.start),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp)) {
                            Text(stringResource(R.string.flow_open), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    }
                }
            }
        }
        item(key = "last-result") {
            val resultPress = rememberPressFeedback(0.98f)
            Surface(onClick = onChecks, interactionSource = resultPress.source,
                modifier = Modifier.fillMaxWidth().then(resultPress.modifier), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.last_open), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedContent(record.status, transitionSpec = {
                            (fadeIn(motion.defaultEffectsSpec()) + slideInVertically(motion.defaultSpatialSpec()) { it / 2 }) togetherWith
                                (fadeOut(motion.fastEffectsSpec()) + slideOutVertically(motion.defaultSpatialSpec()) { -it / 2 })
                        }, label = "flow-result") { StatusLabel(it, Modifier.fillMaxWidth().testTag("service-status-flow")) }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.nav_checks), Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun BrandMark() {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { Text("Cf", fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium) }
    }
}
