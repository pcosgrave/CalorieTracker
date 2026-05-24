package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.totalsForEntries
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.DatePillsRow
import com.philipcosgrave.calorietracker.ui.components.DiaryEntryRow
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.appBorderColor
import com.philipcosgrave.calorietracker.ui.components.appSoftColor
import com.philipcosgrave.calorietracker.ui.components.isDigitsOnlyInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import java.time.LocalDate
import kotlin.math.max

@Composable
fun DiaryScreen(
    selectedDate: LocalDate,
    entries: List<DiaryEntry>,
    targetRangeMin: Int,
    targetRangeMax: Int,
    onDateChange: (LocalDate) -> Unit,
    onBack: () -> Unit,
    onAddFood: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onUpdateEntry: (DiaryEntry) -> Unit,
) {
    val today = LocalDate.now()
    val selectedEntries = entries.filter { it.date == selectedDate }
    val totals = totalsForEntries(selectedEntries)
    val rangeMin = targetRangeMin.coerceAtLeast(0)
    val rangeMax = max(targetRangeMax, rangeMin + 1)
    val overflowSpan = max(rangeMax - rangeMin, 200)
    val currentCalories = totals.calories.toFloat()
    val currentProgress =
        when {
            currentCalories <= rangeMin -> {
                if (rangeMin == 0) 0f else (currentCalories / rangeMin.toFloat()) * (1f / 3f)
            }

            currentCalories <= rangeMax -> {
                val inRangeProgress = (currentCalories - rangeMin.toFloat()) / (rangeMax - rangeMin).toFloat()
                (1f / 3f) + inRangeProgress * (1f / 3f)
            }

            else -> {
                val overProgress = ((currentCalories - rangeMax.toFloat()) / overflowSpan.toFloat()).coerceIn(0f, 1f)
                (2f / 3f) + overProgress * (1f / 3f)
            }
        }.coerceIn(0f, 1f)
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    Page {
        PageHeader(
            title = "Food Log",
            onBack = onBack,
            actions = {
                TextButton(onClick = onOpenSyncSettings) {
                    Text("...", color = AppMuted, style = MaterialTheme.typography.titleLarge)
                }
            },
        )

        AppCardContainer {
            DatePillsRow(
                selectedDate = selectedDate,
                today = today,
                onDateChange = onDateChange,
                modifier = Modifier.fillMaxWidth(),
            )

            AppCardContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent, RoundedCornerShape(28.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(appSoftColor(), RoundedCornerShape(32.dp))
                        .padding(horizontal = 20.dp, vertical = 15.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            formatNumber(totals.calories),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text("$rangeMin-$rangeMax target", color = AppMuted)
                    }
                }

                IntakeRangeBar(
                    currentProgress = currentProgress,
                    lowerTarget = rangeMin,
                    upperTarget = rangeMax,
                )

                MacroRow(
                    protein = totals.protein,
                    carbs = totals.carbs,
                    fat = totals.fat,
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(60.dp)
                        .background(AppBlue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = onAddFood) {
                        Text("+", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }

        editingEntry?.let { entry ->
            EditEntryCard(
                entry = entry,
                onCancel = { editingEntry = null },
                onSave = {
                    onUpdateEntry(it)
                    editingEntry = null
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Meal Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Meal.entries.forEach { meal ->
                val mealEntries = selectedEntries.filter { it.meal == meal }
                AppCardContainer {
                    if (mealEntries.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                meal.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Text("0 cal", color = AppMuted)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                meal.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${formatNumber(totalsForEntries(mealEntries).calories)} cal", fontWeight = FontWeight.Bold)
                        }
                        mealEntries.forEach { entry ->
                            DiaryEntryRow(entry = entry, onEdit = { editingEntry = entry }, onDelete = { onDeleteEntry(entry) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntakeRangeBar(
    currentProgress: Float,
    lowerTarget: Int,
    upperTarget: Int,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        val barWidth = maxWidth
        val firstBreak = maxWidth / 3f
        val secondBreak = firstBreak * 2f
        val markerOffset = ((barWidth - 18.dp) * currentProgress).coerceIn(0.dp, barWidth - 18.dp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(appBorderColor(), RoundedCornerShape(999.dp)),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFFF0D58A), RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFF9BE2AB)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFFFFC4BA), RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)),
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = firstBreak - 1.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.White),
                )
                Box(
                    modifier = Modifier
                        .padding(start = secondBreak - 1.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.White),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = markerOffset)
                        .size(18.dp)
                        .background(AppBlue, CircleShape),
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "$lowerTarget",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(72.dp)
                        .align(Alignment.CenterStart)
                        .absoluteOffset((firstBreak - 36.dp).coerceAtLeast(0.dp), 0.dp)
                )
                Text(
                    "$upperTarget",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(72.dp)
                        .align(Alignment.CenterStart)
                        .absoluteOffset((secondBreak - 36.dp).coerceAtLeast(0.dp), 0.dp)
                )
            }
        }
    }
}

@Composable
private fun MacroRow(protein: Double, carbs: Double, fat: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MacroStat("${formatNumber(protein)}g", "PROTEIN", Modifier.weight(1f))
        MacroStat("${formatNumber(carbs)}g", "CARBS", Modifier.weight(1f))
        MacroStat("${formatNumber(fat)}g", "FAT", Modifier.weight(1f))
    }
}

@Composable
private fun MacroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(appBorderColor()),
                )
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(label, color = AppMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 1200)
@Composable
private fun DiaryScreenPreview() {
    PreviewData.Theme {
        DiaryScreen(
            selectedDate = PreviewData.date,
            entries = PreviewData.diaryEntries,
            targetRangeMin = PreviewData.syncSettings.calorieTargetMin,
            targetRangeMax = PreviewData.syncSettings.calorieTargetMax,
            onDateChange = {},
            onBack = {},
            onAddFood = {},
            onOpenSyncSettings = {},
            onDeleteEntry = {},
            onUpdateEntry = {},
        )
    }
}

@Composable
private fun EditEntryCard(entry: DiaryEntry, onCancel: () -> Unit, onSave: (DiaryEntry) -> Unit) {
    var servings by remember(entry.id) { mutableStateOf(formatNumber(entry.servingMultiplier)) }
    AppCardContainer {
        Text("Adjust serving", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        com.philipcosgrave.calorietracker.ui.components.AppFormField(
            value = servings,
            onValueChange = {
                if (isDigitsOnlyInput(it)) {
                    servings = it
                }
            },
            label = "Servings",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel", color = AppMuted) }
            com.philipcosgrave.calorietracker.ui.components.AppPrimaryButton(
                text = "Save",
                onClick = {
                    onSave(
                        entry.copy(
                            servingMultiplier = servings.toDoubleOrNull()?.coerceAtLeast(0.1) ?: entry.servingMultiplier,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
