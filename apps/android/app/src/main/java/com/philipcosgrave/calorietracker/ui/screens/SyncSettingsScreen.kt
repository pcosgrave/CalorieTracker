package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import com.philipcosgrave.calorietracker.data.health.HealthConnectAvailability
import com.philipcosgrave.calorietracker.model.AuthSession
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppFormField
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.AppPrimaryButton
import com.philipcosgrave.calorietracker.ui.components.BiteWiseIcon
import com.philipcosgrave.calorietracker.ui.components.FoodPhotoPicker
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.SectionDivider
import com.philipcosgrave.calorietracker.ui.components.appCardColor
import com.philipcosgrave.calorietracker.ui.components.appSoftColor
import com.philipcosgrave.calorietracker.ui.components.isDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.components.isDigitsOnlyInput
import com.philipcosgrave.calorietracker.ui.components.normalizeDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import kotlinx.coroutines.delay
import java.time.LocalDate
import android.app.DatePickerDialog

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
    onDeleteAccount: () -> Unit,
    onConnectHealthConnect: () -> Unit,
    onSetHealthConnectExportEnabled: (Boolean) -> Unit,
    onImportWeightHistory: () -> Unit,
    onImportNutritionHistory: () -> Unit,
    onManageFoods: () -> Unit = {},
) {
    return SettingsOverviewScreen(
        settings = settings,
        authSession = authSession,
        healthConnectAvailability = healthConnectAvailability,
        healthConnectPermissionGranted = healthConnectPermissionGranted,
        healthConnectExportEnabled = healthConnectExportEnabled,
        onBack = onBack,
        onSave = onSave,
        onSyncNow = onSyncNow,
        onSignIn = onSignIn,
        onSignOut = onSignOut,
        onConnectHealthConnect = onConnectHealthConnect,
        onSetHealthConnectExportEnabled = onSetHealthConnectExportEnabled,
        onManageFoods = onManageFoods,
    )

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var syncEnabled by remember(settings) { mutableStateOf(settings.syncEnabled) }
    var apiBaseUrl by remember(settings) { mutableStateOf(settings.apiBaseUrl.orEmpty()) }
    var backupMode by remember(settings) { mutableStateOf(settings.backupMode) }
    var calorieTargetMin by remember(settings) { mutableStateOf(settings.calorieTargetMin.toString()) }
    var calorieTargetMax by remember(settings) { mutableStateOf(settings.calorieTargetMax.toString()) }
    var stepGoal by remember(settings) { mutableStateOf(settings.dailyStepGoal?.toString().orEmpty()) }
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
        dailyStepGoal = stepGoal.toIntOrNull()?.takeIf { it > 0 },
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
        AppPrimaryButton("Manage foods & recipes", onClick = onManageFoods, modifier = Modifier.fillMaxWidth())

        AppCardContainer {
            Text("Goals · Daily Calorie Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            AppFormField(value = stepGoal, onValueChange = { if (isDigitsOnlyInput(it)) stepGoal = it },
                label = "Daily step goal (optional)", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("Weight Goal & Units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    val normalized = normalizeDecimalNumberInput(it)
                    if (isDecimalNumberInput(normalized)) {
                        goalWeightText = normalized
                    }
                },
                label = "Goal Weight (${weightUnitLabel(weightUnit)})",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            SectionDivider()
            Text("Health & Devices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        Text("Mirror Health Connect data")
                        Text("Write meal logs and weight entries to Health Connect.", color = AppMuted)
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
            Text("Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            var accountExpanded by remember { mutableStateOf(false) }
            TextButton(onClick = { accountExpanded = !accountExpanded }) { Text(if (accountExpanded) "Hide account settings" else "Account & sync settings ›") }
            if (accountExpanded) {
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
            TextButton(
                onClick = onSyncNow,
                enabled = authSession != null && syncEnabled && apiBaseUrl.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Sync now", color = AppBlue)
            }
            Text("Pending local changes: $pendingChangeCount", color = AppMuted)
            Text("Last successful sync: ${settings.lastSuccessfulSyncAt ?: "Never"}", color = AppMuted)

            if (authSession == null) {
                AppPrimaryButton(
                    text = "Sign in",
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = onSignInWithGoogle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign in with Google", color = AppBlue)
                }
            } else {
                AppPrimaryButton(
                    text = "Sign out",
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { showDeleteConfirm = !showDeleteConfirm },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete account", color = Color(0xFFE55A5A))
                }
                if (showDeleteConfirm) {
                    Text(
                        "This permanently deletes your BiteWise account and synced data.",
                        color = Color(0xFFE55A5A),
                    )
                    AppPrimaryButton(
                        text = "Delete account permanently",
                        onClick = onDeleteAccount,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE55A5A),
                            contentColor = Color.White,
                        ),
                    )
                }
                Text(
                    "Cloud sync is connected to your signed-in account.",
                    color = AppMuted,
                )
            }
            }


        }
    }
}

private enum class SettingsEditor { Calories, Weight, HealthConnect, Fitbit, Profile, Subscription, Units, Notifications }
private data class NotificationToggles(val daily: Boolean = true, val weekly: Boolean = true, val insights: Boolean = true, val productUpdates: Boolean = false)
private data class HealthImportToggles(val activity: Boolean = true, val weight: Boolean = true, val nutrition: Boolean = true)

@Composable
private fun ReferenceSettingsEditorPage(
    editor: SettingsEditor,
    settings: SyncSettings,
    authSession: AuthSession?,
    healthConnectAvailability: HealthConnectAvailability,
    healthConnectPermissionGranted: Boolean,
    calorieMin: String,
    calorieMax: String,
    weightText: String,
    weightUnit: SyncSettings.WeightUnit,
    heightUnit: SyncSettings.HeightUnit,
    foodUnitSystem: SyncSettings.FoodUnitSystem,
    targetDate: String?,
    profileName: String,
    profileEmail: String,
    notifications: NotificationToggles,
    healthImports: HealthImportToggles,
    profilePhotoPath: String?,
    timezone: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onCalorieMinChange: (String) -> Unit,
    onCalorieMaxChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onWeightUnitChange: (SyncSettings.WeightUnit) -> Unit,
    onHeightUnitChange: (SyncSettings.HeightUnit) -> Unit,
    onFoodUnitSystemChange: (SyncSettings.FoodUnitSystem) -> Unit,
    onTargetDateChange: (String?) -> Unit,
    onProfileNameChange: (String) -> Unit,
    onProfileEmailChange: (String) -> Unit,
    onNotificationsChange: (NotificationToggles) -> Unit,
    onHealthImportsChange: (HealthImportToggles) -> Unit,
    onProfilePhotoChange: (String?) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onConnectHealthConnect: () -> Unit,
    onSyncNow: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val title = when (editor) {
        SettingsEditor.Calories -> "Calorie Target"
        SettingsEditor.Weight -> "Weight Goal"
        SettingsEditor.HealthConnect -> "Health Connect"
        SettingsEditor.Fitbit -> "Fitbit"
        SettingsEditor.Profile -> "Edit Profile"
        SettingsEditor.Subscription -> "Subscription"
        SettingsEditor.Units -> "Units"
        SettingsEditor.Notifications -> "Notifications"
    }
    Page(spacing = 12.dp) {
        PageHeader(title, onBack = onBack)
        when (editor) {
            SettingsEditor.Calories -> {
                ReferenceIntro("Set your daily calorie range.", "We'll use this to track your progress.")
                Text("${calorieMin.ifBlank { "0" }} – ${calorieMax.ifBlank { "0" }}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium)
                Text("kcal per day", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = AppMuted, style = MaterialTheme.typography.bodySmall)
                val rangeStart = calorieMin.toFloatOrNull()?.coerceIn(1200f, 2800f) ?: 1800f
                val rangeEnd = calorieMax.toFloatOrNull()?.coerceIn(rangeStart, 2800f) ?: 2200f
                RangeSlider(
                    value = rangeStart..rangeEnd,
                    onValueChange = { range -> onCalorieMinChange(range.start.toInt().toString()); onCalorieMaxChange(range.endInclusive.toInt().toString()) },
                    valueRange = 1200f..2800f,
                    steps = 15,
                    colors = SliderDefaults.colors(activeTrackColor = AppBlue, activeTickColor = AppBlue, thumbColor = MaterialTheme.colorScheme.surface, inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = .55f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("1,200", "1,800", "2,200", "2,800").forEach { Text(it, color = AppMuted, style = MaterialTheme.typography.labelSmall) } }
                Spacer(Modifier.height(8.dp))
            }
            SettingsEditor.Weight -> {
                ReferenceIntro("Set your goal and we'll track", "your progress over time.")
                ReferenceField("Goal weight", weightText, weightUnitLabel(weightUnit), onValueChange = onWeightChange)
                ReferenceDateField(targetDate, onTargetDateChange)
                Spacer(Modifier.height(8.dp))
            }
            SettingsEditor.HealthConnect -> {
                if (healthConnectPermissionGranted) {
                    ReferenceStatusHeader("✓", "Connected", "Health Connect is sharing your selected health data.")
                    ReferenceToggle("Import steps & activity", healthImports.activity, { onHealthImportsChange(healthImports.copy(activity = it)) })
                    ReferenceToggle("Import weight", healthImports.weight, { onHealthImportsChange(healthImports.copy(weight = it)) })
                    ReferenceToggle("Import nutrition", healthImports.nutrition, { onHealthImportsChange(healthImports.copy(nutrition = it)) })
                    Text("Last synced\nToday, 9:12 AM", color = AppMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                    AppPrimaryButton("Sync now", onSyncNow, modifier = Modifier.fillMaxWidth())
                } else {
                    ReferenceStatusHeader("♥", "Connect Health Connect", "Allow BiteWise to access your health data?")
                    ReferencePanel { Text("♧  Steps, activity, and workouts\n♧  Weight and body composition\n♧  Nutrition data (if available)", color = AppMuted, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight) }
                    AppPrimaryButton("Connect", onConnectHealthConnect, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
            }
            SettingsEditor.Fitbit -> {
                ReferenceStatusHeader("⁙", "Fitbit", "Fitbit connection is coming soon.")
                ReferencePanel { Text("When available, you’ll be able to import activity and weight data from Fitbit.", color = AppMuted) }
                Spacer(Modifier.height(8.dp))
            }
            SettingsEditor.Profile -> {
                FoodPhotoPicker(profilePhotoPath, onProfilePhotoChange)
                Text("Tap the photo to change it", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = AppMuted, style = MaterialTheme.typography.labelSmall)
                ReferenceField("Name", profileName, onValueChange = onProfileNameChange)
                ReferenceField("Email", profileEmail, onValueChange = onProfileEmailChange)
                ReferenceTimezoneField(timezone, onTimezoneChange)
                if (authSession == null) TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in to sync profile") } else TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
                Spacer(Modifier.height(8.dp))
            }
            SettingsEditor.Subscription -> {
                ReferenceStatusHeader("♕", "BiteWise Free", "Core tracking features")
                ReferencePanel {
                    Text("✓  Food diary\n✓  Barcode scanning\n✓  Basic analytics", color = AppBlue, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
                    Text("Upgrade to Plus", fontWeight = FontWeight.Bold)
                    Text("AI food recognition, advanced analytics, and more.", color = AppMuted, style = MaterialTheme.typography.bodySmall)
                    AppPrimaryButton("Upgrade", onClick = {}, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
            }
            SettingsEditor.Units -> {
                Text("Weight", fontWeight = FontWeight.Bold)
                ReferenceSegment(listOf("kg", "lb"), if (weightUnit == SyncSettings.WeightUnit.Kilograms) 0 else 1) { onWeightUnitChange(if (it == 0) SyncSettings.WeightUnit.Kilograms else SyncSettings.WeightUnit.Pounds) }
                Text("Height", fontWeight = FontWeight.Bold)
                ReferenceSegment(listOf("cm", "ft/in"), if (heightUnit == SyncSettings.HeightUnit.Centimeters) 0 else 1) { onHeightUnitChange(if (it == 0) SyncSettings.HeightUnit.Centimeters else SyncSettings.HeightUnit.FeetInches) }
                Text("Food quantities", fontWeight = FontWeight.Bold)
                ReferenceSegment(listOf("Metric (g, ml)", "Imperial (oz, cups)"), if (foodUnitSystem == SyncSettings.FoodUnitSystem.Metric) 0 else 1) { onFoodUnitSystemChange(if (it == 0) SyncSettings.FoodUnitSystem.Metric else SyncSettings.FoodUnitSystem.Imperial) }
                Spacer(Modifier.height(8.dp))
            }
            SettingsEditor.Notifications -> {
                ReferenceToggle("Daily reminder", notifications.daily, { onNotificationsChange(notifications.copy(daily = it)) }, "Log your meals · 7:00 PM")
                ReferenceToggle("Weekly summary", notifications.weekly, { onNotificationsChange(notifications.copy(weekly = it)) }, "Your progress each week")
                ReferenceToggle("Tips & insights", notifications.insights, { onNotificationsChange(notifications.copy(insights = it)) }, "Helpful suggestions")
                ReferenceToggle("Product updates", notifications.productUpdates, { onNotificationsChange(notifications.copy(productUpdates = it)) }, "New features and news")
                Spacer(Modifier.height(8.dp))
            }
        }
        AppPrimaryButton("Save", onSave, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ReferenceIntro(primary: String, secondary: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(primary, textAlign = TextAlign.Center, color = AppMuted)
        Text(secondary, textAlign = TextAlign.Center, color = AppMuted)
    }
}

@Composable
private fun ReferenceStatusHeader(symbol: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(Modifier.size(66.dp), shape = CircleShape, color = AppBlue.copy(alpha = .16f), border = BorderStroke(1.dp, AppBlue)) { Box(contentAlignment = Alignment.Center) { Text(symbol, color = AppBlue, style = MaterialTheme.typography.headlineMedium) } }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = AppMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReferencePanel(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .4f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun ReferenceField(label: String, value: String, suffix: String = "", onValueChange: ((String) -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = AppMuted, style = MaterialTheme.typography.labelSmall)
        OutlinedTextField(value = value, onValueChange = { onValueChange?.invoke(it) }, readOnly = onValueChange == null, singleLine = true, trailingIcon = { if (suffix.isNotEmpty()) Text(suffix, color = AppMuted, modifier = Modifier.padding(end = 8.dp)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppBlue, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = .55f), focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)))
    }
}

@Composable
private fun ReferenceDateField(value: String?, onChange: (String?) -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val selectedDate = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    fun showCalendar() {
        DatePickerDialog(context, { _, year, month, day -> onChange(LocalDate.of(year, month + 1, day).toString()) }, selectedDate?.year ?: today.year, (selectedDate?.monthValue ?: today.monthValue) - 1, selectedDate?.dayOfMonth ?: today.dayOfMonth).show()
    }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Target date (optional)", color = AppMuted, style = MaterialTheme.typography.labelSmall)
        Surface(Modifier.fillMaxWidth().height(56.dp).clickable(onClick = ::showCalendar), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selectedDate?.toString() ?: "Select a target date", modifier = Modifier.weight(1f), color = if (selectedDate == null) AppMuted else Color.Unspecified)
                Text("▣", color = AppMuted)
            }
        }
    }
}

@Composable
private fun ReferenceTimezoneField(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Time zone", color = AppMuted, style = MaterialTheme.typography.labelSmall)
        Box {
            Surface(Modifier.fillMaxWidth().height(56.dp).clickable { expanded = true }, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Text(value, modifier = Modifier.weight(1f)); Text("⌄", color = AppMuted) }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                listOf("UTC-05:00 Toronto", "UTC-08:00 Vancouver", "UTC-06:00 Winnipeg", "UTC-04:00 Halifax").forEach { timezone ->
                    DropdownMenuItem(text = { Text(timezone) }, onClick = { onChange(timezone); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun ReferenceSegment(options: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), RoundedCornerShape(9.dp)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, option ->
            val active = index == selected
            Box(Modifier.weight(1f).height(34.dp).background(if (active) AppBlue else Color.Transparent, RoundedCornerShape(7.dp)).clickable { onSelected(index) }, contentAlignment = Alignment.Center) { Text(option, color = if (active) Color.White else AppMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun ReferenceToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); subtitle?.let { Text(it, color = AppMuted, style = MaterialTheme.typography.bodySmall) } }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsEditorPage(
    editor: SettingsEditor,
    settings: SyncSettings,
    authSession: AuthSession?,
    healthConnectAvailability: HealthConnectAvailability,
    healthConnectPermissionGranted: Boolean,
    calorieMin: String,
    calorieMax: String,
    weightText: String,
    weightUnit: SyncSettings.WeightUnit,
    profileName: String,
    profileEmail: String,
    notificationsEnabled: Boolean,
    draftHealthExport: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onCalorieMinChange: (String) -> Unit,
    onCalorieMaxChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onWeightUnitChange: (SyncSettings.WeightUnit) -> Unit,
    onProfileNameChange: (String) -> Unit,
    onProfileEmailChange: (String) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onHealthExportChange: (Boolean) -> Unit,
    onConnectHealthConnect: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val title = when (editor) {
        SettingsEditor.Calories -> "Calorie Target"
        SettingsEditor.Weight -> "Weight Goal"
        SettingsEditor.HealthConnect -> "Health Connect"
        SettingsEditor.Fitbit -> "Fitbit"
        SettingsEditor.Profile -> "Edit Profile"
        SettingsEditor.Subscription -> "Subscription"
        SettingsEditor.Units -> "Units"
        SettingsEditor.Notifications -> "Notifications"
    }
    Page {
        PageHeader(title, onBack = onBack)
        when (editor) {
            SettingsEditor.Calories -> {
                Text("Set your daily calorie range.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("We'll use this to track your progress.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = AppMuted)
                Text("${calorieMin.ifBlank { "0" }} – ${calorieMax.ifBlank { "0" }}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.displaySmall)
                Text("kcal per day", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = AppMuted)
                AppCardContainer {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(calorieMin, onCalorieMinChange, label = { Text("Minimum") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(calorieMax, onCalorieMaxChange, label = { Text("Maximum") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }
            SettingsEditor.Weight -> {
                Text("Set your goal weight and we'll track your progress over time.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = AppMuted)
                AppCardContainer {
                    Text("Goal weight", fontWeight = FontWeight.Bold)
                    OutlinedTextField(weightText, onWeightChange, label = { Text("Weight (${weightUnitLabel(weightUnit)})") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    WeightUnitPicker(weightUnit, onWeightUnitChange)
                }
            }
            SettingsEditor.HealthConnect -> {
                AppCardContainer {
                    Text(if (healthConnectPermissionGranted) "✓ Connected" else "Connect Health Connect", color = if (healthConnectPermissionGranted) MaterialTheme.colorScheme.primary else Color.Unspecified, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Allow BiteWise to access your health data?", color = AppMuted)
                    Text("♧ Steps, activity, and workouts\n♧ Weight and body composition\n♧ Nutrition data (if available)", color = AppMuted)
                    Button(onClick = onConnectHealthConnect, modifier = Modifier.fillMaxWidth()) { Text(if (healthConnectPermissionGranted) "Manage connection" else "Connect") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Import nutrition", modifier = Modifier.weight(1f)); Switch(draftHealthExport, onHealthExportChange) }
                }
            }
            SettingsEditor.Fitbit -> AppCardContainer {
                Text("Fitbit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Fitbit connection is not available yet.", color = AppMuted)
            }
            SettingsEditor.Profile -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Surface(Modifier.size(84.dp), shape = CircleShape, color = appSoftColor()) { Box(contentAlignment = Alignment.Center) { Text("♙", style = MaterialTheme.typography.displaySmall) } } }
                Text("Change photo", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = AppMuted)
                AppCardContainer {
                    OutlinedTextField(profileName, onProfileNameChange, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(profileEmail, onProfileEmailChange, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Text("Time zone\nUTC-05:00 Toronto", color = AppMuted)
                    if (authSession == null) TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in") } else TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
                }
            }
            SettingsEditor.Subscription -> AppCardContainer {
                Text("♕", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.displaySmall)
                Text("BiteWise Free", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Core tracking features", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = AppMuted)
                Text("✓ Food diary\n✓ Barcode scanning\n✓ Basic analytics", color = AppMuted)
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Upgrade") }
            }
            SettingsEditor.Units -> AppCardContainer {
                Text("Weight", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onWeightUnitChange(SyncSettings.WeightUnit.Kilograms) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (weightUnit == SyncSettings.WeightUnit.Kilograms) MaterialTheme.colorScheme.primary else appCardColor())) { Text("kg") }
                    Button(onClick = { onWeightUnitChange(SyncSettings.WeightUnit.Pounds) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (weightUnit == SyncSettings.WeightUnit.Pounds) MaterialTheme.colorScheme.primary else appCardColor())) { Text("lb") }
                }
                Text("Height\ncm", color = AppMuted)
                Text("Food quantities\nMetric (g, ml)", color = AppMuted)
            }
            SettingsEditor.Notifications -> AppCardContainer {
                SettingsToggleRow("Daily reminder", "Log your meals", notificationsEnabled, onNotificationsChange)
                SettingsToggleRow("Weekly summary", "Your progress each week", notificationsEnabled, onNotificationsChange)
                SettingsToggleRow("Tips & insights", "Helpful suggestions", notificationsEnabled, onNotificationsChange)
                SettingsToggleRow("Product updates", "New features and news", notificationsEnabled, onNotificationsChange)
            }
        }
        if (editor != SettingsEditor.HealthConnect || !healthConnectPermissionGranted) {
            AppPrimaryButton("Save", onClick = onSave, modifier = Modifier.fillMaxWidth())
        } else {
            AppPrimaryButton("Save", onClick = onSave, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title); Text(subtitle, color = AppMuted, style = MaterialTheme.typography.bodySmall) }
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun SettingsOverviewScreen(
    settings: SyncSettings,
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
    onManageFoods: () -> Unit,
) {
    var editor by remember { mutableStateOf<SettingsEditor?>(null) }
    var savedProfileName by remember { mutableStateOf(authSession?.name.orEmpty()) }
    var savedProfileEmail by remember { mutableStateOf(authSession?.email.orEmpty()) }
    var savedProfilePhoto by remember(settings) { mutableStateOf(settings.profilePhotoPath) }
    var savedTimezone by remember(settings) { mutableStateOf(settings.profileTimezone) }
    var profileName by remember { mutableStateOf(savedProfileName) }
    var profileEmail by remember { mutableStateOf(savedProfileEmail) }
    var profilePhoto by remember { mutableStateOf(savedProfilePhoto) }
    var timezone by remember { mutableStateOf(savedTimezone) }
    var savedNotifications by remember(settings) { mutableStateOf(NotificationToggles(settings.notificationDaily, settings.notificationWeekly, settings.notificationInsights, settings.notificationProductUpdates)) }
    var notifications by remember { mutableStateOf(savedNotifications) }
    var calorieMin by remember(settings) { mutableStateOf(settings.calorieTargetMin.toString()) }
    var calorieMax by remember(settings) { mutableStateOf(settings.calorieTargetMax.toString()) }
    var weightText by remember(settings) { mutableStateOf(settings.goalWeightKg?.let { formatWeightForUnit(it, settings.weightUnit) }.orEmpty()) }
    var weightUnit by remember(settings) { mutableStateOf(settings.weightUnit) }
    var heightUnit by remember(settings) { mutableStateOf(settings.heightUnit) }
    var foodUnitSystem by remember(settings) { mutableStateOf(settings.foodUnitSystem) }
    var targetDate by remember(settings) { mutableStateOf(settings.goalTargetDate) }
    var savedHealthImports by remember(settings) { mutableStateOf(HealthImportToggles(settings.healthImportActivity, settings.healthImportWeight, settings.healthImportNutrition)) }
    var healthImports by remember(settings) { mutableStateOf(savedHealthImports) }
    // Retained solely for the legacy editor branch below; active flows use the independent toggle state above.
    var savedNotificationsEnabled by remember { mutableStateOf(savedNotifications.daily) }
    var notificationsEnabled by remember { mutableStateOf(notifications.daily) }
    var draftHealthExport by remember { mutableStateOf(healthImports.nutrition) }

    Page(spacing = 18.dp) {
        PageHeader("Settings", onBack = onBack)
        SettingsGroup("Goals") {
            SettingsRow("Target", "Daily Calorie Target", "${settings.calorieTargetMin} – ${settings.calorieTargetMax} kcal") { editor = SettingsEditor.Calories }
            SettingsRow("Scale", "Weight Goal", settings.goalWeightKg?.let { "${formatWeightForUnit(it, settings.weightUnit)} ${weightUnitLabel(settings.weightUnit)}" } ?: "Not set") { editor = SettingsEditor.Weight }
        }
        SettingsGroup("Health & Devices") {
            SettingsRow("Health", "Health Connect", if (healthConnectPermissionGranted) "Connected" else "Not connected", status = healthConnectPermissionGranted) { healthImports = savedHealthImports; editor = SettingsEditor.HealthConnect }
            SettingsRow("Fitbit", "Fitbit", "Not connected") { editor = SettingsEditor.Fitbit }
        }
        SettingsGroup("Account") {
            SettingsRow("Profile", "Profile", savedProfileEmail.ifBlank { "Sign in to manage your profile" }) { profileName = savedProfileName; profileEmail = savedProfileEmail; profilePhoto = savedProfilePhoto; timezone = savedTimezone; editor = SettingsEditor.Profile }
            SettingsRow("Subscription", "Subscription", "BiteWise Free") { editor = SettingsEditor.Subscription }
        }
        SettingsGroup("Preferences") {
            SettingsRow("Units", "Units", "${if (foodUnitSystem == SyncSettings.FoodUnitSystem.Metric) "Metric" else "Imperial"} (${weightUnitLabel(weightUnit)}, ${if (heightUnit == SyncSettings.HeightUnit.Centimeters) "cm" else "ft/in"})") { weightUnit = settings.weightUnit; heightUnit = settings.heightUnit; foodUnitSystem = settings.foodUnitSystem; editor = SettingsEditor.Units }
            SettingsRow("Notifications", "Notifications", if (savedNotifications.daily || savedNotifications.weekly || savedNotifications.insights || savedNotifications.productUpdates) "Enabled" else "Disabled") { notifications = savedNotifications; editor = SettingsEditor.Notifications }
        }
    }

    editor?.let { currentEditor ->
        ReferenceSettingsEditorPage(
            editor = currentEditor,
            settings = settings,
            authSession = authSession,
            healthConnectAvailability = healthConnectAvailability,
            healthConnectPermissionGranted = healthConnectPermissionGranted,
            calorieMin = calorieMin,
            calorieMax = calorieMax,
            weightText = weightText,
            weightUnit = weightUnit,
            heightUnit = heightUnit,
            foodUnitSystem = foodUnitSystem,
            targetDate = targetDate,
            profileName = profileName,
            profileEmail = profileEmail,
            notifications = notifications,
            healthImports = healthImports,
            profilePhotoPath = profilePhoto,
            timezone = timezone,
            onBack = { editor = null },
            onSave = {
                when (currentEditor) {
                    SettingsEditor.Calories -> {
                        val min = calorieMin.toIntOrNull()?.coerceAtLeast(0) ?: settings.calorieTargetMin
                        val max = calorieMax.toIntOrNull()?.coerceAtLeast(min) ?: settings.calorieTargetMax.coerceAtLeast(min)
                        onSave(settings.copy(calorieTargetMin = min, calorieTargetMax = max))
                    }
                    SettingsEditor.Weight -> onSave(settings.copy(weightUnit = weightUnit, goalWeightKg = weightText.toDoubleOrNull()?.let { convertWeightToKg(it, weightUnit) }, goalTargetDate = targetDate))
                    SettingsEditor.Units -> onSave(settings.copy(weightUnit = weightUnit, heightUnit = heightUnit, foodUnitSystem = foodUnitSystem))
                    SettingsEditor.HealthConnect -> { savedHealthImports = healthImports; onSave(settings.copy(healthImportActivity = healthImports.activity, healthImportWeight = healthImports.weight, healthImportNutrition = healthImports.nutrition)); onSetHealthConnectExportEnabled(healthImports.nutrition) }
                    SettingsEditor.Profile -> { savedProfileName = profileName; savedProfileEmail = profileEmail; savedProfilePhoto = profilePhoto; savedTimezone = timezone; onSave(settings.copy(profilePhotoPath = profilePhoto, profileTimezone = timezone)) }
                    SettingsEditor.Notifications -> { savedNotifications = notifications; onSave(settings.copy(notificationDaily = notifications.daily, notificationWeekly = notifications.weekly, notificationInsights = notifications.insights, notificationProductUpdates = notifications.productUpdates)) }
                    else -> Unit
                }
                editor = null
            },
            onCalorieMinChange = { if (isDigitsOnlyInput(it)) calorieMin = it },
            onCalorieMaxChange = { if (isDigitsOnlyInput(it)) calorieMax = it },
            onWeightChange = { value -> val normalized = normalizeDecimalNumberInput(value); if (isDecimalNumberInput(normalized)) weightText = normalized },
            onWeightUnitChange = { next -> weightText.toDoubleOrNull()?.let { weightText = formatWeightForUnit(convertWeightToKg(it, weightUnit), next) }; weightUnit = next },
            onHeightUnitChange = { heightUnit = it },
            onFoodUnitSystemChange = { foodUnitSystem = it },
            onTargetDateChange = { targetDate = it },
            onProfileNameChange = { profileName = it },
            onProfileEmailChange = { profileEmail = it },
            onNotificationsChange = { notifications = it },
            onHealthImportsChange = { healthImports = it },
            onProfilePhotoChange = { profilePhoto = it },
            onTimezoneChange = { timezone = it },
            onConnectHealthConnect = onConnectHealthConnect,
            onSyncNow = onSyncNow,
            onSignIn = onSignIn,
            onSignOut = onSignOut,
        )
        return
    }

    when (editor) {
        SettingsEditor.Calories -> SettingsEditorDialog("Daily Calorie Target", onDismiss = { editor = null }, onSave = {
            val min = calorieMin.toIntOrNull()?.coerceAtLeast(0) ?: settings.calorieTargetMin
            val max = calorieMax.toIntOrNull()?.coerceAtLeast(min) ?: settings.calorieTargetMax.coerceAtLeast(min)
            onSave(settings.copy(calorieTargetMin = min, calorieTargetMax = max)); editor = null
        }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(calorieMin, { if (isDigitsOnlyInput(it)) calorieMin = it }, label = { Text("Minimum kcal") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(calorieMax, { if (isDigitsOnlyInput(it)) calorieMax = it }, label = { Text("Maximum kcal") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        SettingsEditor.Weight -> SettingsEditorDialog("Weight Goal", onDismiss = { editor = null }, onSave = {
            onSave(settings.copy(weightUnit = weightUnit, goalWeightKg = weightText.toDoubleOrNull()?.let { convertWeightToKg(it, weightUnit) })); editor = null
        }) {
            OutlinedTextField(weightText, { value -> val normalized = normalizeDecimalNumberInput(value); if (isDecimalNumberInput(normalized)) weightText = normalized }, label = { Text("Goal weight (${weightUnitLabel(weightUnit)})") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            WeightUnitPicker(weightUnit) { next -> weightText.toDoubleOrNull()?.let { weightText = formatWeightForUnit(convertWeightToKg(it, weightUnit), next) }; weightUnit = next }
        }
        SettingsEditor.HealthConnect -> SettingsEditorDialog("Health Connect", onDismiss = { editor = null }, onSave = { onSetHealthConnectExportEnabled(draftHealthExport); editor = null }) {
            Text(if (healthConnectPermissionGranted) "Connected" else "Not connected", color = if (healthConnectPermissionGranted) MaterialTheme.colorScheme.primary else AppMuted, fontWeight = FontWeight.Bold)
            Text("Import steps, activity, weight, and nutrition data.", color = AppMuted)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Mirror nutrition data", modifier = Modifier.weight(1f)); Switch(draftHealthExport, { draftHealthExport = it }) }
            Button(onClick = onConnectHealthConnect, modifier = Modifier.fillMaxWidth()) { Text(if (healthConnectPermissionGranted) "Manage connection" else "Connect") }
        }
        SettingsEditor.Fitbit -> SettingsEditorDialog("Fitbit", onDismiss = { editor = null }, onSave = { editor = null }) {
            Text("Fitbit connection is not available yet.", color = AppMuted)
        }
        SettingsEditor.Profile -> SettingsEditorDialog("Edit Profile", onDismiss = { editor = null }, onSave = { savedProfileName = profileName; savedProfileEmail = profileEmail; editor = null }) {
            OutlinedTextField(profileName, { profileName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(profileEmail, { profileEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            if (authSession == null) TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in to sync your profile") } else TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        }
        SettingsEditor.Subscription -> SettingsEditorDialog("Subscription", onDismiss = { editor = null }, onSave = { editor = null }) {
            Text("BiteWise Free", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Food diary, barcode scanning, and basic analytics.", color = AppMuted)
            Text("Subscription management will be available here.", color = AppMuted)
        }
        SettingsEditor.Units -> SettingsEditorDialog("Units", onDismiss = { editor = null }, onSave = { onSave(settings.copy(weightUnit = weightUnit)); editor = null }) {
            Text("Weight unit", fontWeight = FontWeight.Bold)
            WeightUnitPicker(weightUnit) { weightUnit = it }
            Text("Food quantities use metric units in the food log.", color = AppMuted)
        }
        SettingsEditor.Notifications -> SettingsEditorDialog("Notifications", onDismiss = { editor = null }, onSave = { savedNotificationsEnabled = notificationsEnabled; editor = null }) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Daily reminders"); Text("Meal and progress reminders", color = AppMuted) }; Switch(notificationsEnabled, { notificationsEnabled = it }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Weekly summary"); Text("Your progress each week", color = AppMuted) }; Switch(notificationsEnabled, { notificationsEnabled = it }) }
        }
        null -> Unit
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .42f)),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(icon: String, title: String, subtitle: String, status: Boolean = false, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().height(64.dp).clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(34.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .55f)) {
            Box(contentAlignment = Alignment.Center) { BiteWiseIcon(icon, tint = if (status) AppBlue else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp)) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = if (status) MaterialTheme.colorScheme.primary else AppMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = AppMuted)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .28f), thickness = 1.dp, modifier = Modifier.padding(start = 60.dp))
    }
}

@Composable
private fun SettingsEditorDialog(title: String, onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }, confirmButton = { Button(onClick = onSave) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } })
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
            onDeleteAccount = {},
            onConnectHealthConnect = {},
            onSetHealthConnectExportEnabled = {},
            onImportWeightHistory = {},
            onImportNutritionHistory = {},
        )
    }
}
