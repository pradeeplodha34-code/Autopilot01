package com.autopilot.app.ui.screens

import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autopilot.app.domain.model.AutomationMode
import com.autopilot.app.domain.model.SessionStatus
import com.autopilot.app.ui.theme.CobaltDark
import com.autopilot.app.ui.theme.ElectricCyan
import com.autopilot.app.ui.theme.JetBrainsMonoFontFamily
import com.autopilot.app.ui.theme.MidnightNavy
import com.autopilot.app.ui.theme.StatusError
import com.autopilot.app.ui.theme.StatusPaused
import com.autopilot.app.ui.theme.StatusRunning
import com.autopilot.app.ui.theme.StatusStopped
import com.autopilot.app.ui.viewmodel.AutoPilotViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EngineScreen(
    viewModel: AutoPilotViewModel,
    modifier: Modifier = Modifier
) {
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val draftUrl by viewModel.draftUrl.collectAsStateWithLifecycle()
    val draftExtraUrlsText by viewModel.draftExtraUrlsText.collectAsStateWithLifecycle()
    val draftConfig by viewModel.draftConfig.collectAsStateWithLifecycle()
    val showLivePreview by viewModel.showLivePreview.collectAsStateWithLifecycle()
    val activeProxyKey by viewModel.activeProxyKey.collectAsStateWithLifecycle()
    val fingerprints by viewModel.fingerprints.collectAsStateWithLifecycle()
    val activeFpId by viewModel.activeFingerprintId.collectAsStateWithLifecycle()

    val activeFingerprint = fingerprints.firstOrNull { it.id == activeFpId }
    var showMultiUrls by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                EngineHeroCard(
                    status = engineState.status,
                    runtime = engineState.runtime,
                    activeTabs = engineState.activeTabs,
                    totalTabs = engineState.totalTabs,
                    totalRefreshes = engineState.totalRefreshes,
                    totalScrolls = engineState.totalScrolls,
                    memoryPercent = engineState.memoryPercent,
                    currentActivity = engineState.currentActivity,
                    spoofedModel = engineState.spoofedModel,
                    proxyHost = engineState.proxyHost
                )
            }

            item {
                EngineControlsRow(
                    status = engineState.status,
                    totalTabs = engineState.totalTabs,
                    onStart = { viewModel.startAutomation() },
                    onPause = { viewModel.pauseAutomation() },
                    onResume = { viewModel.resumeAutomation() },
                    onStop = { viewModel.stopAutomation() },
                    onAddTab = { viewModel.addLiveTab() },
                    onRemoveTab = { viewModel.removeLiveTab() }
                )
            }

            if (engineState.status != SessionStatus.STOPPED && engineState.tabStates.isNotEmpty()) {
                item {
                    LiveTabMonitorCard(
                        viewModel = viewModel,
                        tabStates = engineState.tabStates.values.sortedBy { it.index },
                        activeTabId = activeTabId,
                        showLivePreview = showLivePreview,
                        onSelectTab = { viewModel.selectActiveTab(it) },
                        onTogglePreview = { viewModel.toggleLivePreview() }
                    )
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Session Configuration",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        OutlinedTextField(
                            value = draftUrl,
                            onValueChange = { viewModel.updateDraftUrl(it) },
                            label = { Text("Primary Target URL") },
                            placeholder = { Text("https://example.com") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_url_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Multi-URL Rotation (${draftConfig.urls.size} extra)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilledTonalButton(
                                onClick = { showMultiUrls = !showMultiUrls },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("toggle_multi_urls_button")
                            ) {
                                Icon(
                                    imageVector = if (showMultiUrls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle multi URLs",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (showMultiUrls) "Hide" else "Edit URLs")
                            }
                        }

                        AnimatedVisibility(visible = showMultiUrls) {
                            OutlinedTextField(
                                value = draftExtraUrlsText,
                                onValueChange = { viewModel.updateDraftExtraUrlsText(it) },
                                label = { Text("Additional URLs (one per line)") },
                                placeholder = { Text("https://news.ycombinator.com\nhttps://wikipedia.org") },
                                minLines = 2,
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("multi_urls_input")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Text(
                            text = "Automation Mode",
                            style = MaterialTheme.typography.titleSmall
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AutomationMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = draftConfig.mode == mode,
                                    onClick = { viewModel.updateDraftConfig { it.copy(mode = mode) } },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("mode_chip_${mode.name.lowercase()}")
                                )
                            }
                        }

                        Text(
                            text = draftConfig.mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AnimatedVisibility(visible = draftConfig.mode == AutomationMode.CUSTOM_JS) {
                            OutlinedTextField(
                                value = draftConfig.customJs,
                                onValueChange = { js ->
                                    viewModel.updateDraftConfig { it.copy(customJs = js) }
                                },
                                label = { Text("Custom JavaScript Payload") },
                                placeholder = { Text("window.scrollBy(0, 400);") },
                                leadingIcon = {
                                    Icon(Icons.Default.Code, contentDescription = null)
                                },
                                minLines = 3,
                                maxLines = 6,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_js_input")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        SliderRow(
                            label = "Concurrent Tabs",
                            valueText = "${draftConfig.tabCount} tabs",
                            value = draftConfig.tabCount.toFloat(),
                            range = 1f..12f,
                            steps = 10,
                            tag = "tab_count_slider",
                            onValueChange = { v ->
                                viewModel.updateDraftConfig { it.copy(tabCount = v.roundToInt()) }
                            }
                        )

                        SliderRow(
                            label = "Refresh Interval",
                            valueText = "${draftConfig.refreshIntervalSec}s",
                            value = draftConfig.refreshIntervalSec.toFloat(),
                            range = 10f..180f,
                            steps = 16,
                            tag = "refresh_interval_slider",
                            onValueChange = { v ->
                                viewModel.updateDraftConfig { it.copy(refreshIntervalSec = v.roundToInt()) }
                            }
                        )

                        SliderRow(
                            label = "Scroll Interval",
                            valueText = "${draftConfig.scrollIntervalSec}s",
                            value = draftConfig.scrollIntervalSec.toFloat(),
                            range = 2f..60f,
                            steps = 28,
                            tag = "scroll_interval_slider",
                            onValueChange = { v ->
                                viewModel.updateDraftConfig { it.copy(scrollIntervalSec = v.roundToInt()) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        ConfigToggleRow(
                            title = "Randomize Intervals",
                            subtitle = "Jitter scroll timing (+/-25%) for natural behavior",
                            checked = draftConfig.randomizeIntervals,
                            tag = "switch_randomize",
                            onCheckedChange = { checked ->
                                viewModel.updateDraftConfig { it.copy(randomizeIntervals = checked) }
                            }
                        )

                        ConfigToggleRow(
                            title = "Device Fingerprint Spoofing",
                            subtitle = activeFingerprint?.let {
                                "Active: ${it.buildManufacturer} ${it.buildModel} (Android ${it.fingerprintAndroidVersion})"
                            } ?: "Inject navigator, WebGL & canvas stealth scripts",
                            checked = draftConfig.enableSpoofing,
                            tag = "switch_spoofing",
                            onCheckedChange = { checked ->
                                viewModel.updateDraftConfig { it.copy(enableSpoofing = checked) }
                            }
                        )

                        ConfigToggleRow(
                            title = "Route Through Active Proxy",
                            subtitle = if (activeProxyKey.isNotBlank()) {
                                "Selected: $activeProxyKey"
                            } else {
                                "Select a proxy in Stealth tab (uses WebView ProxyOverride)"
                            },
                            checked = draftConfig.enableProxy,
                            tag = "switch_proxy",
                            onCheckedChange = { checked ->
                                viewModel.updateDraftConfig { it.copy(enableProxy = checked) }
                            }
                        )

                        ConfigToggleRow(
                            title = "Auto-Pause on Low Memory",
                            subtitle = "Pause engine automatically if system RAM >= 88%",
                            checked = draftConfig.autoPauseOnLowMemory,
                            tag = "switch_low_memory",
                            onCheckedChange = { checked ->
                                viewModel.updateDraftConfig { it.copy(autoPauseOnLowMemory = checked) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineHeroCard(
    status: SessionStatus,
    runtime: String,
    activeTabs: Int,
    totalTabs: Int,
    totalRefreshes: Int,
    totalScrolls: Int,
    memoryPercent: Int,
    currentActivity: String,
    spoofedModel: String,
    proxyHost: String
) {
    val statusColor = when (status) {
        SessionStatus.RUNNING -> StatusRunning
        SessionStatus.PAUSED -> StatusPaused
        SessionStatus.STOPPED -> StatusStopped
        SessionStatus.ERROR -> StatusError
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(MidnightNavy, CobaltDark)
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = status.label.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = runtime,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ElectricCyan
                    )
                }

                Text(
                    text = currentActivity,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroStatPill(
                        label = "Tabs",
                        value = "$activeTabs/$totalTabs",
                        icon = Icons.Default.Tab,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = "Refreshes",
                        value = totalRefreshes.toString(),
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = "Scrolls",
                        value = totalScrolls.toString(),
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = "RAM",
                        value = "$memoryPercent%",
                        icon = Icons.Default.Memory,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (spoofedModel.isNotBlank() || proxyHost.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        if (spoofedModel.isNotBlank()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = spoofedModel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        if (proxyHost.isNotBlank()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VpnLock,
                                        contentDescription = null,
                                        tint = StatusRunning,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = proxyHost,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = Color.White
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

@Composable
private fun HeroStatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EngineControlsRow(
    status: SessionStatus,
    totalTabs: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onAddTab: () -> Unit,
    onRemoveTab: () -> Unit
) {
    if (status == SessionStatus.STOPPED || status == SessionStatus.ERROR) {
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_automation_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Automation Session", style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (status == SessionStatus.RUNNING) {
                    FilledTonalButton(
                        onClick = onPause,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pause_automation_button")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pause")
                    }
                } else {
                    Button(
                        onClick = onResume,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("resume_automation_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Resume")
                    }
                }

                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("stop_automation_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Stop")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAddTab,
                    enabled = totalTabs < 12,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_tab_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Tab ($totalTabs/12)")
                }

                OutlinedButton(
                    onClick = onRemoveTab,
                    enabled = totalTabs > 1,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("remove_tab_button")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove Tab")
                }
            }
        }
    }
}

@Composable
private fun LiveTabMonitorCard(
    viewModel: AutoPilotViewModel,
    tabStates: List<com.autopilot.app.domain.model.TabRealTimeState>,
    activeTabId: String?,
    showLivePreview: Boolean,
    onSelectTab: (String) -> Unit,
    onTogglePreview: () -> Unit
) {
    val context = LocalContext.current
    val previewContainer = remember(context) { FrameLayout(context) }

    DisposableEffect(viewModel, previewContainer) {
        viewModel.engine.attachPreview(previewContainer)
        onDispose {
            viewModel.engine.detachPreview(previewContainer)
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Tab Monitor",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = onTogglePreview,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_live_preview_button")
                ) {
                    Icon(
                        imageVector = if (showLivePreview) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showLivePreview) "Hide live WebView preview" else "Show live WebView preview"
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabStates.forEach { tab ->
                    val selected = tab.tabId == activeTabId
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectTab(tab.tabId) },
                        label = {
                            Text("Tab ${tab.index + 1} (${tab.scrollCount}s/${tab.refreshCount}r)")
                        },
                        modifier = Modifier.testTag("live_tab_chip_${tab.index}")
                    )
                }
            }

            val currentTab = tabStates.firstOrNull { it.tabId == activeTabId } ?: tabStates.firstOrNull()
            if (currentTab != null) {
                Text(
                    text = "Tab ${currentTab.index + 1}: ${currentTab.lastAction}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility(visible = showLivePreview) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AndroidView(
                        factory = { previewContainer },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    tag: String,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun ConfigToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(tag)
        )
    }
}
