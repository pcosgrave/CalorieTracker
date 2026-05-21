package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import com.philipcosgrave.calorietracker.ui.preview.PreviewTheme

@Composable
fun SyncSettingsScreen(
    settings: SyncSettings,
    pendingChangeCount: Int,
    onBack: () -> Unit,
    onSave: (SyncSettings) -> Unit,
    onSyncNow: () -> Unit,
) {
    var syncEnabled by remember(settings) { mutableStateOf(settings.syncEnabled) }
    var apiBaseUrl by remember(settings) { mutableStateOf(settings.apiBaseUrl.orEmpty()) }
    var backupMode by remember(settings) { mutableStateOf(settings.backupMode) }

    Page {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sync Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable sync", style = MaterialTheme.typography.titleMedium)
                        Text("Local storage stays primary. Sync acts as backup and cross-device restore.")
                    }
                    Switch(checked = syncEnabled, onCheckedChange = { syncEnabled = it })
                }

                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = { apiBaseUrl = it },
                    label = { Text("API base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.example.com") },
                )

                Text("Backup mode", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SyncSettings.BackupMode.entries.forEach { mode ->
                        Button(onClick = { backupMode = mode }, modifier = Modifier.weight(1f)) {
                            Text(
                                when (mode) {
                                    SyncSettings.BackupMode.Disabled -> "Off"
                                    SyncSettings.BackupMode.ManualBackup -> "Manual"
                                    SyncSettings.BackupMode.AutomaticBackup -> "Auto"
                                },
                            )
                        }
                    }
                }

                Text("Pending local changes: $pendingChangeCount")
                Text("Last successful sync: ${settings.lastSuccessfulSyncAt ?: "Never"}")

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(
                        onClick = {
                            onSave(
                                settings.copy(
                                    syncEnabled = syncEnabled,
                                    backupMode = backupMode,
                                    apiBaseUrl = apiBaseUrl.trim().ifBlank { null },
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }
                    Button(
                        onClick = onSyncNow,
                        modifier = Modifier.weight(1f),
                        enabled = syncEnabled && apiBaseUrl.isNotBlank(),
                    ) {
                        Text("Sync now")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 700)
@Composable
private fun SyncSettingsScreenPreview() {
    PreviewTheme {
        SyncSettingsScreen(
            settings = PreviewData.syncSettings,
            pendingChangeCount = 4,
            onBack = {},
            onSave = {},
            onSyncNow = {},
        )
    }
}
