package com.autopilot.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autopilot.app.automation.CountryRegionCatalog
import com.autopilot.app.automation.ProxyHealthChecker
import com.autopilot.app.domain.model.DeviceFingerprint
import com.autopilot.app.domain.model.ExitIpResult
import com.autopilot.app.domain.model.ProxyFetchSummary
import com.autopilot.app.domain.model.ProxyInfo
import com.autopilot.app.domain.model.ProxyProtocol
import com.autopilot.app.domain.model.WebRtcPolicy
import com.autopilot.app.ui.theme.JetBrainsMonoFontFamily
import com.autopilot.app.ui.theme.StatusError
import com.autopilot.app.ui.theme.StatusPaused
import com.autopilot.app.ui.theme.StatusRunning
import com.autopilot.app.ui.viewmodel.AutoPilotViewModel

@Composable
fun StealthScreen(
    viewModel: AutoPilotViewModel,
    modifier: Modifier = Modifier
) {
    val fingerprints by viewModel.fingerprints.collectAsStateWithLifecycle()
    val activeFpId by viewModel.activeFingerprintId.collectAsStateWithLifecycle()
    val proxies by viewModel.proxies.collectAsStateWithLifecycle()
    val countryStats by viewModel.countryStats.collectAsStateWithLifecycle()
    val activeProxyKey by viewModel.activeProxyKey.collectAsStateWithLifecycle()
    val isProbing by viewModel.isProbingProxies.collectAsStateWithLifecycle()
    val isFetchingProxies by viewModel.isFetchingProxies.collectAsStateWithLifecycle()
    val fetchSummary by viewModel.proxyFetchSummary.collectAsStateWithLifecycle()
    val isCheckingIp by viewModel.isCheckingIp.collectAsStateWithLifecycle()
    val exitIpResult by viewModel.exitIpResult.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddFpDialog by remember { mutableStateOf(false) }
    var showAddProxyDialog by remember { mutableStateOf(false) }
    var selectedCountryFilter by remember { mutableStateOf("All") }

    val webViewProxySupported = remember { ProxyHealthChecker.isWebViewProxySupported() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf("Fingerprints", "Proxy Pool")
                tabs.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        modifier = Modifier.testTag("stealth_tab_$index")
                    ) {
                        Text(title)
                    }
                }
            }

            if (selectedTab == 0) {
                val activeFp = fingerprints.firstOrNull { it.id == activeFpId } ?: fingerprints.firstOrNull()
                if (activeFp != null) {
                    ActiveFingerprintHero(
                        fp = activeFp,
                        onRotate = { viewModel.rotateFingerprint() }
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(fingerprints, key = { it.id }) { fp ->
                        FingerprintProfileCard(
                            fp = fp,
                            isSelected = fp.id == activeFpId,
                            onSelect = { viewModel.selectFingerprint(fp.id) },
                            onDelete = { viewModel.deleteFingerprint(fp.id) },
                            canDelete = fingerprints.size > 1
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ProxyRoutingScopeCard(
                            webViewSupported = webViewProxySupported,
                            activeProxyKey = activeProxyKey,
                            isCheckingIp = isCheckingIp,
                            exitIpResult = exitIpResult,
                            onCheckExitIp = { viewModel.checkCurrentExitIp() }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${proxies.size} proxies (${proxies.count { it.isAlive }} alive)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.fetchPublicProxies() },
                                    enabled = !isFetchingProxies,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("fetch_public_proxies_button")
                                ) {
                                    if (isFetchingProxies) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Fetching...")
                                    } else {
                                        Icon(
                                            Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Fetch")
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { viewModel.probeAllProxies() },
                                    enabled = proxies.isNotEmpty() && !isProbing,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("probe_all_proxies_button")
                                ) {
                                    if (isProbing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Testing...")
                                    } else {
                                        Icon(
                                            Icons.Default.NetworkCheck,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Test All")
                                    }
                                }

                                if (proxies.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.clearAllProxies() },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("clear_proxies_button")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteSweep,
                                            contentDescription = "Clear proxy pool"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (fetchSummary != null) {
                        item {
                            ProxySourceSummaryCard(summary = fetchSummary!!)
                        }
                    }

                    if (countryStats.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCountryFilter == "All",
                                    onClick = { selectedCountryFilter = "All" },
                                    label = { Text("All (${proxies.size})") }
                                )
                                countryStats.forEach { stat ->
                                    FilterChip(
                                        selected = selectedCountryFilter == stat.countryCode,
                                        onClick = { selectedCountryFilter = stat.countryCode },
                                        label = { Text("${stat.countryName} (${stat.proxyCount})") }
                                    )
                                }
                            }
                        }
                    }

                    val visibleProxies = if (selectedCountryFilter == "All") {
                        proxies
                    } else {
                        proxies.filter { proxy ->
                            val code = CountryRegionCatalog.resolveCountryCode(proxy.country)
                                .ifBlank { "UNKNOWN" }
                            code == selectedCountryFilter
                        }
                    }

                    if (visibleProxies.isEmpty()) {
                        item {
                            EmptyPlaceholder(
                                icon = Icons.Default.Router,
                                title = "Proxy Pool Empty",
                                subtitle = "Tap Fetch to query public proxy feeds (GeoNode, Monosans, ProxyScrape, TheSpeedX) or + to add a custom endpoint."
                            )
                        }
                    } else {
                        items(visibleProxies, key = { "${it.host}:${it.port}" }) { proxy ->
                            val key = "${proxy.host}:${proxy.port}"
                            ProxyCard(
                                proxy = proxy,
                                isSelected = activeProxyKey == key,
                                onSelect = { viewModel.selectActiveProxy(proxy.host, proxy.port) },
                                onProbe = { viewModel.probeSingleProxy(proxy) },
                                onDelete = { viewModel.deleteProxy(proxy.host, proxy.port) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (selectedTab == 0) showAddFpDialog = true else showAddProxyDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag(if (selectedTab == 0) "add_fingerprint_fab" else "add_proxy_fab")
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = if (selectedTab == 0) "Add fingerprint profile" else "Add proxy"
            )
        }
    }

    if (showAddFpDialog) {
        AddFingerprintDialog(
            onDismiss = { showAddFpDialog = false },
            onSave = { mfg, model, androidVer, width, height, cores, ram, webrtc, tz, ua ->
                viewModel.addCustomFingerprint(
                    manufacturer = mfg,
                    model = model,
                    androidVersion = androidVer,
                    screenWidth = width,
                    screenHeight = height,
                    cpuCores = cores,
                    deviceMemory = ram,
                    webrtcPolicy = webrtc,
                    timezone = tz,
                    userAgent = ua
                )
                showAddFpDialog = false
            }
        )
    }

    if (showAddProxyDialog) {
        AddProxyDialog(
            onDismiss = { showAddProxyDialog = false },
            onSave = { host, port, protocol, country ->
                viewModel.addProxy(host, port, protocol, country)
                showAddProxyDialog = false
            }
        )
    }
}

@Composable
private fun ProxyRoutingScopeCard(
    webViewSupported: Boolean,
    activeProxyKey: String,
    isCheckingIp: Boolean,
    exitIpResult: ExitIpResult?,
    onCheckExitIp: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Proxy Routing & Exit Check",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                FilledTonalButton(
                    onClick = onCheckExitIp,
                    enabled = !isCheckingIp,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("verify_exit_ip_button")
                ) {
                    if (isCheckingIp) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Checking...")
                    } else {
                        Text("Verify Exit IP")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WebView Proxy Override", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = when {
                        !webViewSupported -> "Unsupported by WebView"
                        activeProxyKey.isNotBlank() -> "Selected: $activeProxyKey"
                        else -> "Supported (Direct)"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = if (webViewSupported) StatusRunning else StatusPaused
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnLock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("Device-Wide VPN Tunnel", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "Disabled (Not provisioned)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Note: Android VpnService is not declared and no VPN server is configured. Proxy routing applies strictly to in-app WebView traffic.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (exitIpResult != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                if (exitIpResult.errorMessage == null) {
                    Text(
                        text = "Exit IP: ${exitIpResult.ip} (${exitIpResult.countryName}) via ${exitIpResult.viaProxy} [${exitIpResult.provider}]",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = StatusRunning
                    )
                } else {
                    Text(
                        text = "IP check failed (${exitIpResult.viaProxy}): ${exitIpResult.errorMessage}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = StatusError
                    )
                }
            }
        }
    }
}

@Composable
private fun ProxySourceSummaryCard(summary: ProxyFetchSummary) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Public Sources: ${summary.totalUnique} unique proxies parsed",
                style = MaterialTheme.typography.labelLarge
            )
            summary.sources.forEach { source ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = source.sourceName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    val statusText = if (source.isSuccess && source.errorMessage == null) {
                        "${source.fetchedCount} fetched (${source.durationMs}ms)"
                    } else {
                        source.errorMessage ?: "Failed"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (source.isSuccess && source.fetchedCount > 0) StatusRunning else StatusError,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveFingerprintHero(
    fp: DeviceFingerprint,
    onRotate: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Active Spoof Profile",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                        Text(
                            text = "${fp.buildManufacturer} ${fp.buildModel}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Button(
                    onClick = onRotate,
                    modifier = Modifier.testTag("rotate_fingerprint_button")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rotate")
                }
            }

            Text(
                text = "Android ${fp.fingerprintAndroidVersion} \u2022 ${fp.screenWidth}x${fp.screenHeight} @ ${fp.screenDpi}dpi \u2022 ${fp.cpuCores}c/${fp.deviceMemory}GB \u2022 WebRTC: ${fp.webrtcPolicy.name}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "GL: ${fp.glVendor} ${fp.glRenderer} \u2022 Canvas #${fp.canvasNoiseSeed} \u2022 TZ: ${fp.timezone}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FingerprintProfileCard(
    fp: DeviceFingerprint,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Card(
        onClick = onSelect,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fingerprint_card_${fp.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${fp.buildManufacturer} ${fp.buildModel}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = fp.userAgent,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${fp.screenWidth}x${fp.screenHeight} \u2022 ${fp.locale} \u2022 WebRTC ${fp.webrtcPolicy.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_fingerprint_${fp.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete fingerprint")
                }
            }
        }
    }
}

@Composable
private fun ProxyCard(
    proxy: ProxyInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onProbe: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onSelect,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("proxy_card_${proxy.host}_${proxy.port}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${proxy.host}:${proxy.port}",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = proxy.protocol.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected proxy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val latencyLabel = when {
                    proxy.lastChecked == 0L -> "Untested"
                    proxy.isAlive -> "${proxy.latencyMs} ms \u2022 Alive"
                    else -> "Unreachable (${proxy.failureCount} fails)"
                }
                val countryDisplay = CountryRegionCatalog.resolveCountryName(proxy.country)
                Text(
                    text = "$countryDisplay \u2022 $latencyLabel",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = when {
                        proxy.lastChecked == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                        proxy.isAlive -> StatusRunning
                        else -> StatusError
                    }
                )
            }

            IconButton(
                onClick = onProbe,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Test proxy latency")
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete proxy")
            }
        }
    }
}

@Composable
private fun AddFingerprintDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int, Int, Int, Int, WebRtcPolicy, String, String) -> Unit
) {
    var mfg by remember { mutableStateOf("Google") }
    var model by remember { mutableStateOf("Pixel 9a") }
    var androidVer by remember { mutableStateOf("15") }
    var width by remember { mutableStateOf("1080") }
    var height by remember { mutableStateOf("2424") }
    var cores by remember { mutableStateOf("8") }
    var ram by remember { mutableStateOf("8") }
    var timezone by remember { mutableStateOf("America/New_York") }
    var ua by remember { mutableStateOf("") }
    var webrtc by remember { mutableStateOf(WebRtcPolicy.BLOCK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Fingerprint Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mfg,
                        onValueChange = { mfg = it },
                        label = { Text("Brand") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = androidVer,
                        onValueChange = { androidVer = it.filter { c -> c.isDigit() } },
                        label = { Text("Android") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cores,
                        onValueChange = { cores = it.filter { c -> c.isDigit() } },
                        label = { Text("Cores") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ram,
                        onValueChange = { ram = it.filter { c -> c.isDigit() } },
                        label = { Text("RAM GB") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it.filter { c -> c.isDigit() } },
                        label = { Text("Width") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it.filter { c -> c.isDigit() } },
                        label = { Text("Height") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = { Text("Timezone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ua,
                    onValueChange = { ua = it },
                    label = { Text("Custom User-Agent (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WebRtcPolicy.entries.forEach { policy ->
                        FilterChip(
                            selected = webrtc == policy,
                            onClick = { webrtc = policy },
                            label = { Text("WebRTC ${policy.name}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        mfg,
                        model,
                        androidVer.toIntOrNull() ?: 15,
                        width.toIntOrNull() ?: 1080,
                        height.toIntOrNull() ?: 2400,
                        cores.toIntOrNull() ?: 8,
                        ram.toIntOrNull() ?: 8,
                        webrtc,
                        timezone,
                        ua
                    )
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddProxyDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int, ProxyProtocol, String) -> Unit
) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }
    var country by remember { mutableStateOf("US") }
    var protocol by remember { mutableStateOf(ProxyProtocol.HTTP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Proxy Endpoint") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host / IP") },
                    placeholder = { Text("192.168.1.50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { c -> c.isDigit() } },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProxyProtocol.entries.forEach { p ->
                        FilterChip(
                            selected = protocol == p,
                            onClick = { protocol = p },
                            label = { Text(p.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPort = port.toIntOrNull() ?: 8080
                    if (host.isNotBlank() && parsedPort in 1..65535) {
                        onSave(host, parsedPort, protocol, country)
                    }
                }
            ) {
                Text("Add & Probe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
