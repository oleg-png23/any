@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.abypass.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import app.abypass.data.AppSettings
import app.abypass.data.CheckStatus
import app.abypass.data.ServiceRecord

@Composable
internal fun FlowHomeScreen(settings: AppSettings, record: ServiceRecord, onOpen: () -> Unit, onChecks: () -> Unit, modifier: Modifier = Modifier) {
    val motion = MaterialTheme.motionScheme
    val largeType = LocalDensity.current.fontScale > 1.2f
    val buttonPress = rememberPressFeedback(0.96f)
    val style = serviceStyle("flow")
    LazyColumn(modifier.testTag("flow-home"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "brand") {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
                    if (!largeType) BrandMark()
                }
                Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.app_sign_in_info), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "flow") {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("flow-hero"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = style.start, contentColor = style.ink),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    ServiceBackdrop("flow", style, Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ServiceAvatar("flow")
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Google Flow", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                                Text(stringResource(R.string.flow_description), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Button(onClick = onOpen, interactionSource = buttonPress.source,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("flow-open").then(buttonPress.modifier),
                            colors = ButtonDefaults.buttonColors(containerColor = style.ink, contentColor = style.start),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                            Text(stringResource(R.string.flow_open), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bolt, null, Modifier.size(18.dp))
                            AnimatedContent("flow" in settings.adapters, transitionSpec = {
                                fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
                            }, label = "compatibility-setting") { enabled ->
                                Text(stringResource(if (enabled) R.string.compatibility_enabled else R.string.compatibility_disabled), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
        item(key = "last-result") {
            val resultPress = rememberPressFeedback(0.98f)
            Surface(onClick = onChecks, interactionSource = resultPress.source,
                modifier = Modifier.fillMaxWidth().then(resultPress.modifier), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.last_open), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedContent(record.status, transitionSpec = {
                            (fadeIn(motion.defaultEffectsSpec()) + slideInVertically(motion.defaultSpatialSpec()) { it / 2 }) togetherWith
                                (fadeOut(motion.fastEffectsSpec()) + slideOutVertically(motion.defaultSpatialSpec()) { -it / 2 })
                        }, label = "flow-result") { status ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusLabel(status, Modifier.fillMaxWidth().testTag("service-status-flow"))
                                if (status == CheckStatus.PAGE_OPENED) {
                                    Text(stringResource(R.string.status_generation_unchecked), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.nav_checks), Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun BrandMark() {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Text("Cf", fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium) }
    }
}
