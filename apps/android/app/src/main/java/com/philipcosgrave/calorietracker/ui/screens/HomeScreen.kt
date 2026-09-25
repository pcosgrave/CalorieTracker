package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.philipcosgrave.calorietracker.ui.components.DatePillsRow
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import java.time.LocalDate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.model.HealthDashboardMetrics
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.appBorderStrongColor
import com.philipcosgrave.calorietracker.ui.components.appCardColor
import com.philipcosgrave.calorietracker.ui.components.appSoftColor
import com.philipcosgrave.calorietracker.ui.preview.PreviewData

@Composable
fun HomeScreen(
    caloriesLogged: Double,
    selectedDate: LocalDate = LocalDate.now(),
    onDateChange: (LocalDate) -> Unit = {},
    calorieTarget: Int = 2000,
    healthMetrics: HealthDashboardMetrics,
    latestWeightKg: Double?,
    weightUnit: SyncSettings.WeightUnit,
    goalWeightKg: Double? = null,
    startWeightKg: Double? = null,
    dailyStepGoal: Int? = null,
    onOpenFoodLog: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenSyncSettings: () -> Unit,
) {
    val weightText = latestWeightKg?.let {
        "${formatNumber(convertWeightFromKg(it, weightUnit))} ${weightUnitLabel(weightUnit)}"
    } ?: "--"

    var expandedHealth by rememberSaveable { mutableStateOf(false) }
    Page {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("BiteWise", Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onOpenSyncSettings) { Text("⚙") }
        }
        DatePillsRow(selectedDate, LocalDate.now(), onDateChange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.philipcosgrave.calorietracker.ui.components.DailyMetricRing("Calories", formatNumber(caloriesLogged), "of $calorieTarget kcal", AppBlue, (caloriesLogged / calorieTarget.coerceAtLeast(1)).toFloat(), Modifier.weight(1f), onOpenFoodLog)
            com.philipcosgrave.calorietracker.ui.components.DailyMetricRing("Weight", weightText, goalWeightKg?.let { "Goal ${formatNumber(convertWeightFromKg(it, weightUnit))} ${weightUnitLabel(weightUnit)}" } ?: "Set weight goal", Color(0xFF9D88FF), progress = com.philipcosgrave.calorietracker.domain.weightGoalProgress(startWeightKg, latestWeightKg, goalWeightKg), modifier = Modifier.weight(1f), onClick = onOpenWeight)
            com.philipcosgrave.calorietracker.ui.components.DailyMetricRing("Steps", healthMetrics.steps?.toString() ?: "—", dailyStepGoal?.let { "of $it steps" } ?: "Set step goal", Color(0xFF51B8FF), progress = dailyStepGoal?.takeIf { it > 0 }?.let { target -> healthMetrics.steps?.let { it.toFloat() / target } }, modifier = Modifier.weight(1f), onClick = onOpenSyncSettings)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Imported from Health Connect", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { expandedHealth = !expandedHealth }) { Text(if (expandedHealth) "Less ‹" else "View all ›", style = MaterialTheme.typography.labelSmall) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HealthMetricTile("Steps", healthMetrics.steps?.toString() ?: "—", "↗", Color(0xFF51B8FF), Modifier.weight(1f))
            HealthMetricTile("Heart rate", healthMetrics.heartRateBpm?.let { "$it bpm" } ?: "—", "♥", Color(0xFFFF7591), Modifier.weight(1f))
            HealthMetricTile("Energy", healthMetrics.caloriesBurned?.let { "${formatNumber(it)} kcal" } ?: "—", "♨", Color(0xFF9D88FF), Modifier.weight(1f))
        }
        if (expandedHealth) AppCardContainer {
            Text("Health Connect", fontWeight = FontWeight.Bold)
            Text("Steps, heart rate and energy burned for today. A dash means no reading is available.", color = AppMuted)
            TextButton(onClick = onOpenSyncSettings) { Text("Manage connection") }
        }
    }
}

@Composable
private fun HealthMetricTile(label: String, value: String, icon: String, accent: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, appBorderStrongColor()), colors = CardDefaults.cardColors(containerColor = appCardColor())) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = accent); Text(label, color = AppMuted, style = MaterialTheme.typography.labelSmall)
            }
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun convertWeightFromKg(weightKg: Double, unit: SyncSettings.WeightUnit): Double =
    when (unit) {
        SyncSettings.WeightUnit.Kilograms -> weightKg
        SyncSettings.WeightUnit.Pounds -> weightKg * 2.2046226218
    }

private fun weightUnitLabel(unit: SyncSettings.WeightUnit): String =
    when (unit) {
        SyncSettings.WeightUnit.Kilograms -> "kg"
        SyncSettings.WeightUnit.Pounds -> "lb"
    }

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun HomeScreenPreview() {
    PreviewData.Theme {
        HomeScreen(
            caloriesLogged = PreviewData.totals.calories,
            healthMetrics = PreviewData.healthMetrics,
            latestWeightKg = PreviewData.weightEntries.lastOrNull()?.weightKg,
            weightUnit = PreviewData.syncSettings.weightUnit,
            onOpenFoodLog = {},
            onOpenWeight = {},
            onOpenSyncSettings = {},
        )
    }
}
