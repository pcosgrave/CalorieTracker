package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.philipcosgrave.calorietracker.ui.components.ScrollablePillSelector
import com.philipcosgrave.calorietracker.ui.components.SectionDivider
import com.philipcosgrave.calorietracker.ui.components.appBorderStrongColor
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
    onEditWeight: (WeightEntry) -> Unit,
    onDeleteWeight: (WeightEntry) -> Unit,
) {
    val today = LocalDate.now()
    val overallLatest = remember(weights) { weights.maxByOrNull { it.date } }
    var range by remember { mutableStateOf(WeightChartRange.Daily) }
    var selectedDailyDate by remember { mutableStateOf(today) }
    var selectedWeekStart by remember { mutableStateOf(startOfWeek(today)) }
    var selectedMonthStart by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var pendingDeleteWeight by remember { mutableStateOf<WeightEntry?>(null) }

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
    val chartPoints = remember(
        weights,
        range,
        weightUnit,
        selectedDailyDate,
        selectedWeekStart,
        selectedMonthStart,
    ) {
        buildChartPoints(
            weights = weights,
            range = range,
            weightUnit = weightUnit,
            selectedDailyDate = selectedDailyDate,
            selectedWeekStart = selectedWeekStart,
            selectedMonthStart = selectedMonthStart,
        )
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
                    onEdit = onEditWeight,
                    onDelete = onDeleteWeight,
                )
            } else {
                WeightTrendChart(
                    points = chartPoints.map { it.second },
                    labels = displayedChartLabels(chartPoints.map { it.first }, range),
                    goalValue = goalWeightKg?.let { convertWeightFromKg(it, weightUnit) },
                    unitLabel = weightUnitLabel(weightUnit),
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
                    WeightHistoryRow(
                        entry = entry,
                        weightUnit = weightUnit,
                        onEdit = onEditWeight,
                        onDelete = { pendingDeleteWeight = it },
                    )
                }
            }
        }
    }

    pendingDeleteWeight?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDeleteWeight = null },
            title = { Text("Delete weight?") },
            text = { Text("Delete the weight recorded on ${entry.date.month.name.take(3)} ${entry.date.dayOfMonth}, ${entry.date.year}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteWeight = null
                        onDeleteWeight(entry)
                    },
                ) {
                    Text("Delete", color = androidx.compose.ui.graphics.Color(0xFFFF5449), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteWeight = null }) {
                    Text("Cancel")
                }
            },
        )
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
    when (range) {
        WeightChartRange.Daily -> {
            val selected = minOf(selectedDailyDate, today)
            val earliest = minOf(selected, today.minusDays(60))
            val options = remember(selected, today) {
                generateSequence(earliest) { current ->
                    current.takeIf { it.isBefore(today) }?.plusDays(1)
                }.toList()
            }
            ScrollablePillSelector(
                options = options,
                selectedOption = selected,
                currentOption = today,
                currentLabel = "Today",
                onSelect = onSelectDay,
                labelForOption = { date -> "${date.month.name.take(3)} ${date.dayOfMonth}" },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        WeightChartRange.Weekly -> {
            val currentWeek = startOfWeek(today)
            val selected = minOf(selectedWeekStart, currentWeek)
            val earliest = minOf(selected, currentWeek.minusWeeks(16))
            val options = remember(selected, currentWeek) {
                generateSequence(earliest) { current ->
                    current.takeIf { it.isBefore(currentWeek) }?.plusWeeks(1)
                }.toList()
            }
            ScrollablePillSelector(
                options = options,
                selectedOption = selected,
                currentOption = currentWeek,
                currentLabel = "This week",
                onSelect = onSelectWeek,
                labelForOption = { week -> "W${week.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}" },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        WeightChartRange.Monthly -> {
            val currentMonth = today.withDayOfMonth(1)
            val selected = minOf(selectedMonthStart, currentMonth)
            val earliest = minOf(selected, currentMonth.minusMonths(12))
            val options = remember(selected, currentMonth) {
                generateSequence(earliest) { current ->
                    current.takeIf { it.isBefore(currentMonth) }?.plusMonths(1)
                }.toList()
            }
            ScrollablePillSelector(
                options = options,
                selectedOption = selected,
                currentOption = currentMonth,
                currentLabel = "This month",
                onSelect = onSelectMonth,
                labelForOption = { month -> month.month.name.take(3) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DailyWeightSummary(
    entry: WeightEntry?,
    isCurrentWeight: Boolean,
    weightUnit: SyncSettings.WeightUnit,
    goalWeightKg: Double?,
    onEdit: (WeightEntry) -> Unit,
    onDelete: (WeightEntry) -> Unit,
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
    points: List<Double?>,
    labels: List<String>,
    goalValue: Double?,
    unitLabel: String,
) {
    AppCardContainer {
        val knownPoints = points.filterNotNull()
        if (knownPoints.isEmpty()) {
            Text("No weights logged for this period.", color = AppMuted)
            return@AppCardContainer
        }

        val rawMinValue = listOfNotNull(knownPoints.minOrNull(), goalValue).minOrNull() ?: 0.0
        val rawMaxValue = listOfNotNull(knownPoints.maxOrNull(), goalValue).maxOrNull() ?: rawMinValue
        val rawSpan = (rawMaxValue - rawMinValue).takeIf { it > 0.1 } ?: 1.0
        val lowerPadding = goalValue?.let { maxOf(rawSpan * 0.2, 1.0) } ?: maxOf(rawSpan * 0.1, 0.5)
        val upperPadding = maxOf(rawSpan * 0.1, 0.5)
        val minValue = goalValue?.let { minOf(rawMinValue, it - lowerPadding) } ?: (rawMinValue - lowerPadding)
        val maxValue = maxOf(rawMaxValue, (goalValue ?: rawMaxValue) + upperPadding)
        val span = (maxValue - minValue).takeIf { it > 0.1 } ?: 1.0
        val topTick = maxValue
        val middleTick = minValue + (span / 2.0)
        val bottomTick = minValue

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier.height(220.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(topTick, middleTick, bottomTick).forEachIndexed { index, tick ->
                    Text(
                        if (index == 0) "${formatNumber(tick)} $unitLabel" else formatNumber(tick),
                        color = AppMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                ) {
                    val leftPad = 4f
                    val rightPad = 4f
                    val topPad = 20f
                    val bottomPad = 26f
                    val usableWidth = size.width - leftPad - rightPad
                    val usableHeight = size.height - topPad - bottomPad
                    val stepX = if (points.size <= 1) 0f else usableWidth / (points.size - 1)

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
                    var hasKnownPoint = false
                    points.forEachIndexed { index, value ->
                        val x = leftPad + stepX * index
                        if (value != null) {
                            val normalized = ((value - minValue) / span).toFloat()
                            val y = topPad + usableHeight - usableHeight * normalized
                            if (!hasKnownPoint) {
                                path.moveTo(x, y)
                                hasKnownPoint = true
                            } else {
                                path.lineTo(x, y)
                            }
                            drawCircle(color = AppBlue, radius = 7f, center = Offset(x, y))
                        }
                    }
                    if (hasKnownPoint) {
                        drawPath(path = path, color = AppBlue, style = Stroke(width = 6f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    labels.forEach { label ->
                        Text(label, color = AppMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        goalValue?.let {
            Text(
                "Goal baseline: ${formatNumber(it)} $unitLabel",
                color = androidx.compose.ui.graphics.Color(0xFF36C15B),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightHistoryRow(
    entry: WeightEntry,
    weightUnit: SyncSettings.WeightUnit,
    onEdit: (WeightEntry) -> Unit,
    onDelete: (WeightEntry) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit(entry)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete(entry)
                    false
                }
                else -> false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(androidx.compose.ui.graphics.Color(0xFF4CAF50), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit", tint = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(androidx.compose.ui.graphics.Color(0xFFFF5449), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
                else -> Unit
            }
        },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = appSoftColor()),
            border = androidx.compose.foundation.BorderStroke(1.dp, appBorderStrongColor()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${entry.date.month.name.take(3)} ${entry.date.dayOfMonth}, ${entry.date.year}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 18.dp),
                )
                Text(
                    "${formatNumber(convertWeightFromKg(entry.weightKg, weightUnit))} ${weightUnitLabel(weightUnit)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 18.dp),
                )
            }
        }
    }
    SectionDivider()
}

private fun buildChartPoints(
    weights: List<WeightEntry>,
    range: WeightChartRange,
    weightUnit: SyncSettings.WeightUnit,
    selectedDailyDate: LocalDate,
    selectedWeekStart: LocalDate,
    selectedMonthStart: LocalDate,
): List<Pair<String, Double?>> {
    if (weights.isEmpty()) return emptyList()
    val weightsByDate = weights
        .groupBy { it.date }
        .mapValues { (_, items) -> items.maxByOrNull { it.date } }

    return when (range) {
        WeightChartRange.Daily -> emptyList()
        WeightChartRange.Weekly -> {
            val weekStart = selectedWeekStart.minusDays(1)
            val weekEnd = selectedWeekStart.plusDays(6)
            generateSequence(weekStart) { current ->
                current.takeIf { it.isBefore(weekEnd) }?.plusDays(1)
            }.map { date ->
                date.dayOfWeek.name.take(3) to weightsByDate[date]?.let { entry ->
                    convertWeightFromKg(entry.weightKg, weightUnit)
                }
            }.toList()
        }

        WeightChartRange.Monthly -> {
            val monthEnd = selectedMonthStart.plusMonths(1).minusDays(1)
            generateSequence(selectedMonthStart) { current ->
                current.takeIf { it.isBefore(monthEnd) }?.plusDays(1)
            }.map { date ->
                date.dayOfMonth.toString() to weightsByDate[date]?.let { entry ->
                    convertWeightFromKg(entry.weightKg, weightUnit)
                }
            }.toList()
        }
    }
}

private fun displayedChartLabels(labels: List<String>, range: WeightChartRange): List<String> {
    if (labels.isEmpty()) return labels
    return when (range) {
        WeightChartRange.Daily -> labels
        WeightChartRange.Weekly -> labels
        WeightChartRange.Monthly -> {
            val maxVisibleLabels = 7
            if (labels.size <= maxVisibleLabels) {
                labels
            } else {
                val step = ((labels.size - 1).toDouble() / (maxVisibleLabels - 1)).toInt().coerceAtLeast(1)
                labels.mapIndexed { index, label ->
                    when {
                        index == 0 -> label
                        index == labels.lastIndex -> label
                        index % step == 0 -> label
                        else -> ""
                    }
                }
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
            onEditWeight = {},
            onDeleteWeight = {},
        )
    }
}
