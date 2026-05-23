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
import com.philipcosgrave.calorietracker.data.health.HealthConnectAvailability
import com.philipcosgrave.calorietracker.model.AuthSession
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.preview.PreviewData

@Composable
fun SyncSettingsScreen(
    settings: SyncSettings,
    pendingChangeCount: Int,
    authSession: AuthSession?,
    healthConnectAvailability: HealthConnectAvailability,
    healthConnectPermissionGranted: Boolean,
    healthConnectExportEnabled: Boolean,
    onBack: () -> Unit,
    onSave: (SyncSettings) -> Unit,
    onSyncNow: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onConnectHealthConnect: () -> Unit,
    onSetHealthConnectExportEnabled: (Boolean) -> Unit,
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

                Text(
                    if (authSession != null) {
                        "Signed in as ${authSession.email ?: authSession.name ?: authSession.userSub}"
                    } else {
                        "Not signed in. Sign in to sync against your Cognito account."
                    },
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

                Text("Health Connect", style = MaterialTheme.typography.titleMedium)
                when (healthConnectAvailability) {
                    HealthConnectAvailability.Available -> {
                        if (healthConnectPermissionGranted) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Export logged meals")
                                    Text("Write nutrition records so other Health Connect apps can display your food logs.")
                                }
                                Switch(
                                    checked = healthConnectExportEnabled,
                                    onCheckedChange = onSetHealthConnectExportEnabled,
                                )
                            }
                        } else {
                            Text("Health Connect is available, but CalorieTracker still needs permission to write nutrition records.")
                            Button(onClick = onConnectHealthConnect, modifier = Modifier.fillMaxWidth()) {
                                Text("Connect Health Connect")
                            }
                        }
                    }

                    HealthConnectAvailability.UpdateRequired -> {
                        Text("Install or update Health Connect before nutrition logs can be shared with connected health apps.")
                        Button(onClick = onConnectHealthConnect, modifier = Modifier.fillMaxWidth()) {
                            Text("Install or update")
                        }
                    }

                    HealthConnectAvailability.Unavailable -> {
                        Text("Health Connect is not available on this device, so food logs stay local to CalorieTracker.")
                    }
                }

                Text("Pending local changes: $pendingChangeCount")
                Text("Last successful sync: ${settings.lastSuccessfulSyncAt ?: "Never"}")

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(
                        onClick = if (authSession != null) onSignOut else onSignIn,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (authSession != null) "Sign out" else "Sign in")
                    }
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
                        enabled = authSession != null && syncEnabled && apiBaseUrl.isNotBlank(),
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
    PreviewData.Theme {
        SyncSettingsScreen(
            settings = PreviewData.syncSettings,
            pendingChangeCount = 4,
            authSession = null,
            healthConnectAvailability = PreviewData.healthConnectAvailability,
            healthConnectPermissionGranted = true,
            healthConnectExportEnabled = true,
            onBack = {},
            onSave = {},
            onSyncNow = {},
            onSignIn = {},
            onSignOut = {},
            onConnectHealthConnect = {},
            onSetHealthConnectExportEnabled = {},
        )
    }
}
