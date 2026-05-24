package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.model.HealthDashboardMetrics
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.preview.PreviewData

@Composable
fun HomeScreen(
    caloriesLogged: Double,
    healthMetrics: HealthDashboardMetrics,
    latestWeightKg: Double?,
    weightUnit: SyncSettings.WeightUnit,
    onOpenFoodLog: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenSyncSettings: () -> Unit,
) {
    Page {
        Row {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Home", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Your daily health snapshot", color = AppMuted)
            }
            TextButton(onClick = onOpenSyncSettings) {
                Text("Settings", color = AppBlue)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardWidget(
                title = "Calories Logged",
                value = "${formatNumber(caloriesLogged)} cal",
                caption = "Open food log",
                modifier = Modifier.weight(1f),
                onClick = onOpenFoodLog,
            )
            DashboardWidget(
                title = "Weight",
                value = latestWeightKg?.let {
                    "${formatNumber(convertWeightFromKg(it, weightUnit))} ${weightUnitLabel(weightUnit)}"
                } ?: "--",
                caption = "View history",
                modifier = Modifier.weight(1f),
                onClick = onOpenWeight,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardWidget(
                title = "Steps",
                value = healthMetrics.steps?.toString() ?: "--",
                caption = "From Health Connect",
                modifier = Modifier.weight(1f),
            )
            DashboardWidget(
                title = "Heart Rate",
                value = healthMetrics.heartRateBpm?.let { "$it bpm" } ?: "--",
                caption = "Latest today",
                modifier = Modifier.weight(1f),
            )
        }

        DashboardWidget(
            title = "Calories Burned",
            value = healthMetrics.caloriesBurned?.let { "${formatNumber(it)} cal" } ?: "--",
            caption = "From Health Connect",
            modifier = Modifier.fillMaxWidth(),
        )
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

@Composable
private fun DashboardWidget(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    AppCardContainer(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(caption, color = AppMuted, style = MaterialTheme.typography.bodySmall)
    }
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
