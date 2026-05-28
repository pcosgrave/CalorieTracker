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
    healthMetrics: HealthDashboardMetrics,
    latestWeightKg: Double?,
    weightUnit: SyncSettings.WeightUnit,
    onOpenFoodLog: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenSyncSettings: () -> Unit,
) {
    val weightText = latestWeightKg?.let {
        "${formatNumber(convertWeightFromKg(it, weightUnit))} ${weightUnitLabel(weightUnit)}"
    } ?: "--"

    Page {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Today",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "A cleaner view of your meals, movement, and trends.",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onOpenSyncSettings) {
                Text("Settings", color = AppBlue, fontWeight = FontWeight.Bold)
            }
        }

        HeroSnapshotCard(
            caloriesLogged = caloriesLogged,
            onOpenFoodLog = onOpenFoodLog,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightCard(
                title = "Weight",
                value = weightText,
                subtitle = if (latestWeightKg != null) "Latest logged" else "No weight logged yet",
                accent = Color(0xFF36C15B),
                modifier = Modifier.weight(1f),
                onClick = onOpenWeight,
            )
        }

        SourcePanel(
            eyebrow = "Google Health",
            title = "Imported health data",
            body = "These metrics come from Health Connect and connected health apps. May not be accurate.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricStripCard(
                    label = "Steps",
                    value = healthMetrics.steps?.toString() ?: "--",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricStripCard(
                    label = "Heart Rate",
                    value = healthMetrics.heartRateBpm?.let { "$it bpm" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
                MetricStripCard(
                    label = "Calories Burned",
                    value = healthMetrics.caloriesBurned?.let { "${formatNumber(it)} cal" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SourcePanel(
    eyebrow: String,
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = appSoftColor()),
        border = androidx.compose.foundation.BorderStroke(1.dp, appBorderStrongColor()),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = {
                SectionHeader(
                    eyebrow = eyebrow,
                    title = title,
                    body = body,
                )
                content()
            },
        )
    }
}

@Composable
private fun SectionHeader(
    eyebrow: String,
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            eyebrow.uppercase(),
            color = AppBlue,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            body,
            color = AppMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HeroSnapshotCard(
    caloriesLogged: Double,
    onOpenFoodLog: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFoodLog),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AppBlue.copy(alpha = 0.95f),
                            Color(0xFF163E87),
                        ),
                    ),
                    shape = RoundedCornerShape(30.dp),
                )
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Calories Logged",
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${formatNumber(caloriesLogged)} cal",
                            color = Color.White,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = appCardColor()),
        border = androidx.compose.foundation.BorderStroke(1.dp, appBorderStrongColor()),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    title,
                    color = accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                subtitle,
                color = AppMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MetricStripCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appSoftColor()),
        border = androidx.compose.foundation.BorderStroke(1.dp, appBorderStrongColor()),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                label,
                color = AppMuted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun SummaryNoteCard(
    headline: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = appCardColor()),
        border = androidx.compose.foundation.BorderStroke(1.dp, appBorderStrongColor()),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Daily Focus",
                color = AppBlue,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                headline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                body,
                color = AppMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppBlue,
                                Color(0xFF6F8BFF),
                                Color(0xFF9AC3FF),
                            ),
                        ),
                        RoundedCornerShape(999.dp),
                    ),
            )
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
