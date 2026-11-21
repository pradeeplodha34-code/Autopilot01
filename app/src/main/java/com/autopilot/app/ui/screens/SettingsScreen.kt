package com.autopilot.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autopilot.app.BuildConfig
import com.autopilot.app.automation.CountryRegionCatalog
import com.autopilot.app.ui.theme.JetBrainsMonoFontFamily
import com.autopilot.app.ui.theme.StatusPaused
import com.autopilot.app.ui.theme.StatusRunning
import com.autopilot.app.ui.viewmodel.AutoPilotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: AutoPilotViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val rootEnv by viewModel.rootEnvironment.collectAsStateWithLifecycle()
    val rootVerification by viewModel.rootVerification.collectAsStateWithLifecycle()
    val lastSyncAt by viewModel.lastSyncAt.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.US) }

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
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = user.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showProfileDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit operator profile")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lastSyncAt == 0L) {
                                    "Background sync ready"
                                } else {
                                    "Last sync: ${dateFormat.format(Date(lastSyncAt))}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilledTonalButton(
                                onClick = { viewModel.triggerManualSync() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("sync_now_button")
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sync Now")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Default Automation Preferences", style = MaterialTheme.typography.titleMedium)
                        }

                        Text("App Theme", style = MaterialTheme.typography.titleSmall)
                        val themes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themes.forEachIndexed { index, (key, label) ->
                                SegmentedButton(
                                    selected = settings.theme.equals(key, ignoreCase = true),
                                    onClick = { viewModel.updateUserSettings(settings.copy(theme = key)) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size),
                                    modifier = Modifier.testTag("theme_option_$key")
                                ) {
                                    Text(label)
                                }
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Default Tabs", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${settings.defaultTabs}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.defaultTabs.toFloat(),
                                onValueChange = {
                                    viewModel.updateUserSettings(settings.copy(defaultTabs = it.roundToInt()))
                                },
                                valueRange = 1f..12f,
                                steps = 10
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Default Refresh (sec)", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${settings.defaultRefreshSec}s",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.defaultRefreshSec.toFloat(),
                                onValueChange = {
                                    viewModel.updateUserSettings(settings.copy(defaultRefreshSec = it.roundToInt()))
                                },
                                valueRange = 10f..180f,
                                steps = 16
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Default Scroll (sec)", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${settings.defaultScrollSec}s",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.defaultScrollSec.toFloat(),
                                onValueChange = {
                                    viewModel.updateUserSettings(settings.copy(defaultScrollSec = it.roundToInt()))
                                },
                                valueRange = 2f..60f,
                                steps = 28
                            )
                        }

                        OutlinedTextField(
                            value = settings.preferredProxyCountry,
                            onValueChange = {
                                viewModel.updateUserSettings(settings.copy(preferredProxyCountry = it))
                            },
                            label = { Text("Preferred Proxy Country (code or name)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CountryRegionCatalog.quickSelectCountries.forEach { (code, _) ->
                                FilterChip(
                                    selected = settings.preferredProxyCountry.equals(code, ignoreCase = true),
                                    onClick = {
                                        viewModel.updateUserSettings(settings.copy(preferredProxyCountry = code))
                                    },
                                    label = { Text(code) },
                                    modifier = Modifier.testTag("country_chip_$code")
                                )
                            }
                        }

                        val matchedTz = CountryRegionCatalog.timezoneForCountry(settings.preferredProxyCountry)
                        Text(
                            text = if (matchedTz != null) {
                                "Region: ${CountryRegionCatalog.resolveCountryName(settings.preferredProxyCountry)} • Mapped timezone: $matchedTz"
                            } else {
                                "Supports Any or 60+ ISO country codes (US, GB, DE, FR, IN, JP, SG, etc.)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Root & Environment", style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(
                                onClick = { viewModel.rescanRootEnvironment() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("rescan_root_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Re-scan environment")
                            }
                        }

                        DiagnosticLine("Root Access", if (rootEnv.isRooted) "Detected" else "Standard User", rootEnv.isRooted)
                        DiagnosticLine("Magisk", if (rootEnv.hasMagisk) rootEnv.magiskVersion.ifBlank { "Detected" } else "Not present", rootEnv.hasMagisk)
                        DiagnosticLine("Zygisk", if (rootEnv.hasZygisk) "Active" else "Not present", rootEnv.hasZygisk)
                        DiagnosticLine("LSPosed", if (rootEnv.hasLSPosed) rootEnv.lsposedVersion.ifBlank { "Detected" } else "Not present", rootEnv.hasLSPosed)
                        DiagnosticLine(
                            "System Spoofing",
                            if (rootVerification.systemSpoofApplied) "Root Applied" else "WebView JS Only",
                            rootVerification.systemSpoofApplied
                        )
                        if (rootEnv.suPath.isNotBlank()) {
                            Text(
                                text = "su binary: ${rootEnv.suPath}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (rootVerification.observedModel.isNotBlank()) {
                            Text(
                                text = "Observed build: ${rootVerification.observedManufacturer} ${rootVerification.observedModel}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = rootVerification.statusMessage.ifBlank {
                                "System-wide build property spoofing requires root (Magisk/su)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { viewModel.applyActiveFingerprintToRootSystem() },
                            enabled = rootEnv.isRooted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("apply_root_spoof_button")
                        ) {
                            Text(
                                if (rootEnv.isRooted) {
                                    "Apply Active Profile via Root (resetprop)"
                                } else {
                                    "Root System Spoof Unavailable (No Root)"
                                }
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Cloud Backend Status", style = MaterialTheme.typography.titleMedium)
                        }
                        DiagnosticLine("Firebase Auth / RTDB", "Not Configured", false)
                        DiagnosticLine("Active Storage", "Local Room + DataStore", true)
                        Text(
                            text = "No google-services.json or backend credentials are bundled in this build. Sessions, proxies, fingerprints, and schedules persist locally on device. See BACKEND_SETUP.md to connect your own Firebase project.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Service & Permissions", style = MaterialTheme.typography.titleMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Foreground Notifications", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = if (notifGranted) "Permission granted" else "Required for foreground service status",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (notifGranted) StatusRunning else StatusPaused
                                )
                            }
                            if (notifGranted) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = StatusRunning)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Button(
                                    onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    modifier = Modifier.testTag("grant_notifications_button")
                                ) {
                                    Text("Allow")
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Text(
                            text = "AutoPilot v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        EditProfileDialog(
            initialName = user.name,
            initialEmail = user.email,
            onDismiss = { showProfileDialog = false },
            onSave = { name, email ->
                viewModel.updateUserProfile(name, email)
                showProfileDialog = false
            }
        )
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String, highlighted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Surface(
            color = if (highlighted) StatusRunning.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = if (highlighted) StatusRunning else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Operator Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, email) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
