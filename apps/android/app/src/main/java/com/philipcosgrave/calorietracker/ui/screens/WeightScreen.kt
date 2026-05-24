package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.model.WeightChartRange
import com.philipcosgrave.calorietracker.model.WeightEntry
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.AppSegmentedControl
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.appSoftColor
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

@Composable
fun WeightScreen(
    weights: List<WeightEntry>,
    weightUnit: SyncSettings.WeightUnit,
    goalWeightKg: Double?,
    onBack: () -> Unit,
    onLogWeight: () -> Unit,
) {
    val today = LocalDate.now()
    val overallLatest = remember(weights) { weights.maxByOrNull { it.date } }
    var range by remember { mutableStateOf(WeightChartRange.Daily) }
    var selectedDailyDate by remember { mutableStateOf(today) }
    var selectedWeekStart by remember { mutableStateOf(startOfWeek(today)) }
    var selectedMonthStart by remember { mutableStateOf(today.withDayOfMonth(1)) }

    val selectedEntries = remember(weights, range, selectedDailyDate, selectedWeekStart, selectedMonthStart) {
        when (range) {
            WeightChartRange.Daily -> weights.filter { it.date == selectedDailyDate }
            WeightChartRange.Weekly -> weights.filter { it.date in selectedWeekStart..selectedWeekStart.plusDays(6) }
            WeightChartRange.Monthly -> weights.filter { it.date.year == selectedMonthStart.year && it.date.month == selectedMonthStart.month }
        }.sortedByDescending { it.date }
    }
    val effectiveDailyEntry = remember(weights, selectedDailyDate) {
        weights
            .filter { !it.date.isAfter(selectedDailyDate) }
            .maxByOrNull { it.date }
    }
    val historyEntries = remember(weights, range, selectedDailyDate, selectedEntries) {
        when (range) {
            WeightChartRange.Daily -> weights
                .filter { !it.date.isAfter(selectedDailyDate) }
                .sortedByDescending { it.date }
            else -> selectedEntries
        }
    }
    val chartPoints = remember(selectedEntries, range, weightUnit) {
        buildChartPoints(selectedEntries, range, weightUnit)
    }
    val latest = if (range == WeightChartRange.Daily) effectiveDailyEntry else selectedEntries.maxByOrNull { it.date }

    Page {
        PageHeader("Weight", onBack = onBack)

        AppCardContainer {
            AppSegmentedControl(
                options = WeightChartRange.entries.map { it.label },
                selectedIndex = range.ordinal,
                onSelectedIndexChange = { range = WeightChartRange.entries[it] },
            )

            WeightPeriodPillsRow(
                range = range,
                today = today,
                selectedDailyDate = selectedDailyDate,
                selectedWeekStart = selectedWeekStart,
                selectedMonthStart = selectedMonthStart,
                onSelectDay = { selectedDailyDate = it },
                onSelectWeek = { selectedWeekStart = it },
                onSelectMonth = { selectedMonthStart = it },
            )

            Text(
                when (range) {
                    WeightChartRange.Daily -> selectedDailyDateLabel(selectedDailyDate)
                    WeightChartRange.Weekly -> selectedWeekLabel(selectedWeekStart)
                    WeightChartRange.Monthly -> selectedMonthLabel(selectedMonthStart)
                },
                color = AppMuted,
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                latest?.let {
                    "Latest: ${formatNumber(convertWeightFromKg(it.weightKg, weightUnit))} ${weightUnitLabel(weightUnit)}"
                } ?: "No weight logged in this period",
                color = AppMuted,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (range == WeightChartRange.Daily) {
                DailyWeightSummary(
                    entry = effectiveDailyEntry,
                    isCurrentWeight = effectiveDailyEntry?.id == overallLatest?.id,
                    weightUnit = weightUnit,
                    goalWeightKg = goalWeightKg,
                )
            } else {
                WeightTrendChart(
                    points = chartPoints.map { it.second },
                    labels = chartPoints.map { it.first },
                    goalValue = goalWeightKg?.let { convertWeightFromKg(it, weightUnit) },
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(58.dp)
                    .background(AppBlue, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(onClick = onLogWeight) {
                    Text("+", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        AppCardContainer {
            Text("Logged Weights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (historyEntries.isEmpty()) {
                Text("No weights logged for this selection yet.", color = AppMuted)
            } else {
                historyEntries.forEach { entry ->
                    WeightHistoryRow(entry = entry, weightUnit = weightUnit)
                }
            }
        }
    }
}

@Composable
private fun WeightPeriodPillsRow(
    range: WeightChartRange,
    today: LocalDate,
    selectedDailyDate: LocalDate,
    selectedWeekStart: LocalDate,
    selectedMonthStart: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    onSelectWeek: (LocalDate) -> Unit,
    onSelectMonth: (LocalDate) -> Unit,
) {
    val options = when (range) {
        WeightChartRange.Daily -> buildList {
            var cursor = today
            repeat(5) {
                add(cursor)
                cursor = cursor.minusDays(1)
            }.let { }
        }.reversed()

        WeightChartRange.Weekly -> buildList {
            var cursor = startOfWeek(today)
            repeat(5) {
                add(cursor)
                cursor = cursor.minusWeeks(1)
            }.let { }
        }.reversed()

        WeightChartRange.Monthly -> buildList {
            var cursor = today.withDayOfMonth(1)
            repeat(5) {
                add(cursor)
                cursor = cursor.minusMonths(1)
            }.let { }
        }.reversed()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val selected =
                when (range) {
                    WeightChartRange.Daily -> option == selectedDailyDate
                    WeightChartRange.Weekly -> option == selectedWeekStart
                    WeightChartRange.Monthly -> option == selectedMonthStart
                }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) AppBlue else appSoftColor(),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable {
                        when (range) {
                            WeightChartRange.Daily -> onSelectDay(option)
                            WeightChartRange.Weekly -> onSelectWeek(option)
                            WeightChartRange.Monthly -> onSelectMonth(option)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (range) {
                        WeightChartRange.Daily -> "${option.month.name.take(3)} ${option.dayOfMonth}"
                        WeightChartRange.Weekly -> "W${option.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
                        WeightChartRange.Monthly -> option.month.name.take(3)
                    },
                    modifier = Modifier.align(Alignment.Center),
                    color = if (selected) androidx.compose.ui.graphics.Color.White else AppMuted,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun DailyWeightSummary(
    entry: WeightEntry?,
    isCurrentWeight: Boolean,
    weightUnit: SyncSettings.WeightUnit,
    goalWeightKg: Double?,
) {
    AppCardContainer {
        if (entry == null) {
            Text("No weights logged for this day.", color = AppMuted)
        } else {
            Text(
                "${formatNumber(convertWeightFromKg(entry.weightKg, weightUnit))} ${weightUnitLabel(weightUnit)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(if (isCurrentWeight) "Current weight" else "Logged weight", color = AppMuted)
            Text(
                "Recorded on ${entry.date.month.name.take(3)} ${entry.date.dayOfMonth}, ${entry.date.year}",
                color = AppMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            goalWeightKg?.let {
                Text(
                    "Goal: ${formatNumber(convertWeightFromKg(it, weightUnit))} ${weightUnitLabel(weightUnit)}",
                    color = AppMuted,
                )
            }
        }
    }
}

@Composable
private fun WeightTrendChart(
    points: List<Double>,
    labels: List<String>,
    goalValue: Double?,
) {
    AppCardContainer {
        if (points.isEmpty()) {
            Text("No weights logged for this period.", color = AppMuted)
            return@AppCardContainer
        }

        val minValue = listOfNotNull(points.minOrNull(), goalValue).minOrNull() ?: 0.0
        val maxValue = listOfNotNull(points.maxOrNull(), goalValue).maxOrNull() ?: minValue
        val span = (maxValue - minValue).takeIf { it > 0.1 } ?: 1.0

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            val leftPad = 20f
            val rightPad = 20f
            val topPad = 20f
            val bottomPad = 26f
            val usableWidth = size.width - leftPad - rightPad
            val usableHeight = size.height - topPad - bottomPad
            val stepX = if (points.size == 1) 0f else usableWidth / (points.size - 1)

            repeat(3) { index ->
                val y = topPad + usableHeight * (index / 2f)
                drawLine(
                    color = AppMuted.copy(alpha = 0.15f),
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 2f,
                )
            }

            goalValue?.let { goal ->
                val normalized = ((goal - minValue) / span).toFloat()
                val y = topPad + usableHeight - usableHeight * normalized
                drawLine(
                    color = androidx.compose.ui.graphics.Color(0xFF36C15B),
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 4f,
                )
            }

            val path = Path()
            points.forEachIndexed { index, value ->
                val normalized = ((value - minValue) / span).toFloat()
                val x = leftPad + stepX * index
                val y = topPad + usableHeight - usableHeight * normalized
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(color = AppBlue, radius = 7f, center = Offset(x, y))
            }
            drawPath(path = path, color = AppBlue, style = Stroke(width = 6f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.take(5).forEach { label ->
                Text(label, color = AppMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        goalValue?.let {
            Text(
                "Goal baseline: ${formatNumber(it)}",
                color = androidx.compose.ui.graphics.Color(0xFF36C15B),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun WeightHistoryRow(
    entry: WeightEntry,
    weightUnit: SyncSettings.WeightUnit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${entry.date.month.name.take(3)} ${entry.date.dayOfMonth}, ${entry.date.year}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "${formatNumber(convertWeightFromKg(entry.weightKg, weightUnit))} ${weightUnitLabel(weightUnit)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun buildChartPoints(
    entries: List<WeightEntry>,
    range: WeightChartRange,
    weightUnit: SyncSettings.WeightUnit,
): List<Pair<String, Double>> {
    if (entries.isEmpty()) return emptyList()
    return when (range) {
        WeightChartRange.Daily -> emptyList()
        WeightChartRange.Weekly -> {
            entries
                .groupBy { it.date }
                .toSortedMap()
                .map { (date, items) ->
                    date.dayOfWeek.name.take(3) to convertWeightFromKg(items.last().weightKg, weightUnit)
                }
        }

        WeightChartRange.Monthly -> {
            entries
                .groupBy { it.date }
                .toSortedMap()
                .map { (date, items) ->
                    date.dayOfMonth.toString() to convertWeightFromKg(items.last().weightKg, weightUnit)
                }
        }
    }
}

private fun startOfWeek(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun selectedDailyDateLabel(date: LocalDate): String =
    "${date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${date.month.name.take(3)} ${date.dayOfMonth}"

private fun selectedWeekLabel(weekStart: LocalDate): String =
    "Week ${weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)} of ${weekStart.get(IsoFields.WEEK_BASED_YEAR)}"

private fun selectedMonthLabel(monthStart: LocalDate): String =
    "${monthStart.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${monthStart.year}"

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
private fun WeightScreenPreview() {
    PreviewData.Theme {
        WeightScreen(
            weights = PreviewData.weightEntries,
            weightUnit = PreviewData.syncSettings.weightUnit,
            goalWeightKg = PreviewData.syncSettings.goalWeightKg,
            onBack = {},
            onLogWeight = {},
        )
    }
}
