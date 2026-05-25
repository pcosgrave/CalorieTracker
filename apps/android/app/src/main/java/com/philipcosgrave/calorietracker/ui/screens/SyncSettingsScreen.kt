package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.data.health.HealthConnectAvailability
import com.philipcosgrave.calorietracker.model.AuthSession
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppFormField
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.SectionDivider
import com.philipcosgrave.calorietracker.ui.components.appCardColor
import com.philipcosgrave.calorietracker.ui.components.appSoftColor
import com.philipcosgrave.calorietracker.ui.components.isDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.components.isDigitsOnlyInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import kotlinx.coroutines.delay

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
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onConnectHealthConnect: () -> Unit,
    onSetHealthConnectExportEnabled: (Boolean) -> Unit,
    onImportWeightHistory: () -> Unit,
    onImportNutritionHistory: () -> Unit,
) {
    var syncEnabled by remember(settings) { mutableStateOf(settings.syncEnabled) }
    var apiBaseUrl by remember(settings) { mutableStateOf(settings.apiBaseUrl.orEmpty()) }
    var backupMode by remember(settings) { mutableStateOf(settings.backupMode) }
    var calorieTargetMin by remember(settings) { mutableStateOf(settings.calorieTargetMin.toString()) }
    var calorieTargetMax by remember(settings) { mutableStateOf(settings.calorieTargetMax.toString()) }
    var weightUnit by remember(settings) { mutableStateOf(settings.weightUnit) }
    var goalWeightText by remember(settings) {
        mutableStateOf(settings.goalWeightKg?.let { formatWeightForUnit(it, settings.weightUnit) } ?: "")
    }
    val draftSettings = settings.copy(
        syncEnabled = syncEnabled,
        backupMode = backupMode,
        apiBaseUrl = apiBaseUrl.trim().ifBlank { null },
        calorieTargetMin = calorieTargetMin.toIntOrNull()?.coerceAtLeast(0) ?: settings.calorieTargetMin,
        calorieTargetMax = maxOf(
            calorieTargetMax.toIntOrNull()?.coerceAtLeast(0) ?: settings.calorieTargetMax,
            calorieTargetMin.toIntOrNull()?.coerceAtLeast(0) ?: settings.calorieTargetMin,
        ),
        weightUnit = weightUnit,
        goalWeightKg = goalWeightText.toDoubleOrNull()?.let { convertWeightToKg(it, weightUnit) },
    )

    LaunchedEffect(draftSettings) {
        if (draftSettings != settings) {
            delay(350)
            onSave(draftSettings)
        }
    }

    Page {
        PageHeader("Settings", onBack = onBack)

        AppCardContainer {
            Text("Health Connect", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(appSoftColor(), RoundedCornerShape(18.dp)),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("Health Connect", color = AppMuted)
                    val statusText = when (healthConnectAvailability) {
                        HealthConnectAvailability.Available -> if (healthConnectPermissionGranted) "Ready" else "Permission needed"
                        HealthConnectAvailability.UpdateRequired -> "Update required"
                        HealthConnectAvailability.Unavailable -> "Unavailable"
                    }
                    Text("Status: $statusText")
                    if (healthConnectPermissionGranted) {
                        Text(
                            if (healthConnectExportEnabled) "Nutrition export is on." else "Nutrition export is off.",
                            color = AppMuted,
                        )
                    }
                }
            }

            com.philipcosgrave.calorietracker.ui.components.AppPrimaryButton(
                text = when {
                    healthConnectAvailability == HealthConnectAvailability.UpdateRequired -> "Install or Update Health Connect"
                    healthConnectPermissionGranted -> if (healthConnectExportEnabled) "Health Connect Enabled" else "Reconnect Health Connect"
                    else -> "Connect Health Connect"
                },
                onClick = onConnectHealthConnect,
                modifier = Modifier.fillMaxWidth(),
            )

            if (healthConnectPermissionGranted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mirror meal logs")
                        Text("Write new, edited, and deleted diary logs to Health Connect.", color = AppMuted)
                    }
                    Switch(checked = healthConnectExportEnabled, onCheckedChange = onSetHealthConnectExportEnabled)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(
                        onClick = onImportWeightHistory,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Import weight", color = AppBlue, textAlign = TextAlign.Center)
                    }
                    TextButton(
                        onClick = onImportNutritionHistory,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Import food logs", color = AppBlue, textAlign = TextAlign.Center)
                    }
                }
            }

            SectionDivider()
            Text("Calorie Target Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppFormField(
                    value = calorieTargetMin,
                    onValueChange = {
                        if (isDigitsOnlyInput(it)) {
                            calorieTargetMin = it
                        }
                    },
                    label = "Min kcal",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                AppFormField(
                    value = calorieTargetMax,
                    onValueChange = {
                        if (isDigitsOnlyInput(it)) {
                            calorieTargetMax = it
                        }
                    },
                    label = "Max kcal",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            SectionDivider()
            Text("Weight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Weight Unit", modifier = Modifier.weight(1f), color = AppMuted)
                WeightUnitPicker(
                    value = weightUnit,
                    onChange = { nextUnit ->
                        val currentValue = goalWeightText.toDoubleOrNull()
                        val currentKg = currentValue?.let { convertWeightToKg(it, weightUnit) }
                        weightUnit = nextUnit
                        goalWeightText = currentKg?.let { formatWeightForUnit(it, nextUnit) } ?: ""
                    },
                )
            }

            AppFormField(
                value = goalWeightText,
                onValueChange = {
                    if (isDecimalNumberInput(it)) {
                        goalWeightText = it
                    }
                },
                label = "Goal Weight (${weightUnitLabel(weightUnit)})",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            SectionDivider()
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Enable Cloud Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Sync your data with Health Connect. Syncs automatically in background.", color = AppMuted)
                }
                Switch(checked = syncEnabled, onCheckedChange = { syncEnabled = it })
            }
            if (authSession != null) {
                Text("Signed in as ${authSession.email ?: authSession.name ?: authSession.userSub}")
            } else {
                Text("Sign in to sync against your private cloud backup.", color = AppMuted)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backup Mode", modifier = Modifier.weight(1f), color = AppMuted)
                BackupModePicker(value = backupMode, onChange = { backupMode = it })
            }

            Text("Pending local changes: $pendingChangeCount", color = AppMuted)
            Text("Last successful sync: ${settings.lastSuccessfulSyncAt ?: "Never"}", color = AppMuted)

            TextButton(
                onClick = if (authSession != null) onSignOut else onSignIn,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (authSession != null) "Sign out" else "Sign in", color = AppMuted)
            }

            if (authSession == null) {
                TextButton(
                    onClick = onSignInWithGoogle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign in with Google", color = AppBlue)
                }
            }

            TextButton(
                onClick = onSyncNow,
                enabled = authSession != null && syncEnabled && apiBaseUrl.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Sync now", color = AppBlue)
            }
        }
    }
}

private fun formatWeightForUnit(weightKg: Double, unit: SyncSettings.WeightUnit): String =
    when (unit) {
        SyncSettings.WeightUnit.Kilograms -> weightKg
        SyncSettings.WeightUnit.Pounds -> weightKg * 2.2046226218
    }.let { java.lang.String.format(java.util.Locale.US, "%.1f", it).trimEnd('0').trimEnd('.') }

private fun convertWeightToKg(value: Double, unit: SyncSettings.WeightUnit): Double =
    when (unit) {
        SyncSettings.WeightUnit.Kilograms -> value
        SyncSettings.WeightUnit.Pounds -> value / 2.2046226218
    }

private fun weightUnitLabel(unit: SyncSettings.WeightUnit): String =
    when (unit) {
        SyncSettings.WeightUnit.Kilograms -> "kg"
        SyncSettings.WeightUnit.Pounds -> "lb"
    }

@Composable
private fun BackupModePicker(value: SyncSettings.BackupMode, onChange: (SyncSettings.BackupMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .background(appCardColor(), RoundedCornerShape(14.dp)),
        ) {
            Text(
                when (value) {
                    SyncSettings.BackupMode.Disabled -> "Off"
                    SyncSettings.BackupMode.ManualBackup -> "Manual"
                    SyncSettings.BackupMode.AutomaticBackup -> "Auto"
                },
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SyncSettings.BackupMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        expanded = false
                        onChange(mode)
                    },
                )
            }
        }
    }
}

@Composable
private fun WeightUnitPicker(value: SyncSettings.WeightUnit, onChange: (SyncSettings.WeightUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .background(appCardColor(), RoundedCornerShape(14.dp)),
        ) {
            Text(
                when (value) {
                    SyncSettings.WeightUnit.Kilograms -> "kg"
                    SyncSettings.WeightUnit.Pounds -> "lb"
                },
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SyncSettings.WeightUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (unit) {
                                SyncSettings.WeightUnit.Kilograms -> "Kilograms (kg)"
                                SyncSettings.WeightUnit.Pounds -> "Pounds (lb)"
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onChange(unit)
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
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
            onSignInWithGoogle = {},
            onSignOut = {},
            onConnectHealthConnect = {},
            onSetHealthConnectExportEnabled = {},
            onImportWeightHistory = {},
            onImportNutritionHistory = {},
        )
    }
}
