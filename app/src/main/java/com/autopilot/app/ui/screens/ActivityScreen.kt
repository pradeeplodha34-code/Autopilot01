package com.autopilot.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.autopilot.app.data.local.SessionHistoryEntity
import com.autopilot.app.domain.model.AutomationEvent
import com.autopilot.app.domain.model.AutomationMode
import com.autopilot.app.domain.model.ScheduledTask
import com.autopilot.app.ui.theme.ElectricCyan
import com.autopilot.app.ui.theme.JetBrainsMonoFontFamily
import com.autopilot.app.ui.theme.StatusError
import com.autopilot.app.ui.theme.StatusPaused
import com.autopilot.app.ui.theme.StatusRunning
import com.autopilot.app.ui.viewmodel.AutoPilotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    viewModel: AutoPilotViewModel,
    onSessionLoaded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val history by viewModel.sessionHistory.collectAsStateWithLifecycle()
    val schedules by viewModel.scheduledTasks.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var eventFilter by remember { mutableStateOf("All") }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }

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
                val tabs = listOf("Live Feed", "History", "Schedules")
                tabs.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        modifier = Modifier.testTag("activity_tab_$index")
                    ) {
                        Text(title)
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    val filters = listOf("All", "Scroll", "Refresh", "Stealth", "Proxy", "Spoof", "JS", "Error")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filters.forEach { f ->
                                FilterChip(
                                    selected = eventFilter == f,
                                    onClick = { eventFilter = f },
                                    label = { Text(f) },
                                    modifier = Modifier.testTag("event_filter_$f")
                                )
                            }
                        }
                        if (events.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearEvents() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("clear_events_button")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear event feed")
                            }
                        }
                    }

                    val filtered = if (eventFilter == "All") {
                        events
                    } else {
                        events.filter { it.shortLabel().equals(eventFilter, ignoreCase = true) }
                    }

                    if (filtered.isEmpty()) {
                        EmptyPlaceholder(
                            icon = Icons.Default.Terminal,
                            title = "No Automation Events Yet",
                            subtitle = "Start a session on the Engine tab to monitor live scrolling, refreshes, stealth injections, and recoveries."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered) { event ->
                                EventRowCard(event = event, timeFormatter = timeFormatter)
                            }
                        }
                    }
                }

                1 -> {
                    if (history.isEmpty()) {
                        EmptyPlaceholder(
                            icon = Icons.Default.History,
                            title = "No Session History",
                            subtitle = "Completed automation sessions will be recorded here with full telemetry and one-tap config reload."
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.clearSessionHistory() },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Text("Clear All")
                            }
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(history, key = { it.id }) { item ->
                                HistorySessionCard(
                                    item = item,
                                    dateFormatter = dateFormatter,
                                    onLoadConfig = {
                                        viewModel.loadSessionIntoDraft(item)
                                        onSessionLoaded()
                                    },
                                    onDelete = { viewModel.deleteSessionHistory(item.id) }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    if (schedules.isEmpty()) {
                        EmptyPlaceholder(
                            icon = Icons.Default.Schedule,
                            title = "No Scheduled Sessions",
                            subtitle = "Create recurring background automation schedules powered by WorkManager."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(schedules, key = { it.id }) { task ->
                                ScheduledTaskCard(
                                    task = task,
                                    dateFormatter = dateFormatter,
                                    onToggle = { viewModel.toggleScheduledTask(task) },
                                    onDelete = { viewModel.deleteScheduledTask(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == 2) {
            FloatingActionButton(
                onClick = { showScheduleDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_schedule_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add scheduled task")
            }
        }
    }

    if (showScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showScheduleDialog = false },
            onSave = { title, url, mode, tabs, interval ->
                viewModel.addScheduledTask(title, url, mode, tabs, interval)
                showScheduleDialog = false
            }
        )
    }
}

@Composable
private fun EventRowCard(
    event: AutomationEvent,
    timeFormatter: SimpleDateFormat
) {
    val label = event.shortLabel()
    val badgeColor = when (label) {
        "Error", "Memory" -> StatusError
        "Pause", "Recovery" -> StatusPaused
        "Proxy", "Stealth", "Spoof" -> ElectricCyan
        "Scroll", "Refresh", "JS" -> StatusRunning
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = badgeColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Text(
                text = event.describe(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = timeFormatter.format(Date(event.timestamp)),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistorySessionCard(
    item: SessionHistoryEntity,
    dateFormatter: SimpleDateFormat,
    onLoadConfig: () -> Unit,
    onDelete: () -> Unit
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
                Text(
                    text = item.url,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dateFormatter.format(Date(item.endedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${AutomationMode.fromString(item.mode).label} \u2022 ${item.tabCount} tabs \u2022 ${item.runtimeSeconds}s \u2022 ${item.totalScrolls} scrolls \u2022 ${item.totalRefreshes} refreshes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onLoadConfig,
                    modifier = Modifier.testTag("load_history_${item.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Load Config")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_history_${item.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete session")
                }
            }
        }
    }
}

@Composable
private fun ScheduledTaskCard(
    task: ScheduledTask,
    dateFormatter: SimpleDateFormat,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = task.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = task.targetUrl,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.mode.label} \u2022 Every ${task.intervalMinutes}m \u2022 Next: ${dateFormatter.format(Date(task.nextRunAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = task.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_schedule_${task.id}")
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_schedule_${task.id}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete schedule")
            }
        }
    }
}

@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, AutomationMode, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf("Hourly Session") }
    var url by remember { mutableStateOf("https://example.com") }
    var tabs by remember { mutableStateOf("3") }
    var intervalMin by remember { mutableStateOf("60") }
    var selectedMode by remember { mutableStateOf(AutomationMode.SIMPLE_SCROLL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Scheduled Run") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Target URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tabs,
                        onValueChange = { tabs = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Tabs") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = intervalMin,
                        onValueChange = { intervalMin = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Every (min)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AutomationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        url,
                        selectedMode,
                        tabs.toIntOrNull() ?: 3,
                        intervalMin.toIntOrNull() ?: 60
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                modifier = Modifier.size(48.dp)
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
