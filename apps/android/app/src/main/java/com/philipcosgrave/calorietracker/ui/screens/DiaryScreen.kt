package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.totalsForEntries
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.AppSuccess
import com.philipcosgrave.calorietracker.ui.components.AppSoft
import com.philipcosgrave.calorietracker.ui.components.DiaryEntryRow
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.isDigitsOnlyInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val DailyGoalCalories = 2000.0

@Composable
fun DiaryScreen(
    selectedDate: LocalDate,
    entries: List<DiaryEntry>,
    onDateChange: (LocalDate) -> Unit,
    onAddFood: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onUpdateEntry: (DiaryEntry) -> Unit,
) {
    val selectedEntries = entries.filter { it.date == selectedDate }
    val totals = totalsForEntries(selectedEntries)
    val progress = (totals.calories / DailyGoalCalories).coerceIn(0.0, 1.0)
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    Page {
        AppCardContainer {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM d")), color = AppMuted)
                }
                TextButton(onClick = onOpenSyncSettings) {
                    Text("⋯", color = AppMuted, style = MaterialTheme.typography.titleLarge)
                }
            }

            DatePillsRow(selectedDate = selectedDate, onDateChange = onDateChange)

            AppCardContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent, RoundedCornerShape(28.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(AppSoft, RoundedCornerShape(32.dp))
                        .padding(horizontal = 34.dp, vertical = 30.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(formatNumber(totals.calories), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                        Text("of ${formatNumber(DailyGoalCalories)} Goal", color = AppMuted)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(Color(0xFFD7DAE5), RoundedCornerShape(999.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.toFloat())
                            .height(12.dp)
                            .background(AppSuccess, RoundedCornerShape(999.dp)),
                    )
                }
                Text("Daily Intake", modifier = Modifier.align(Alignment.CenterHorizontally), color = AppMuted)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroStat("14g", "PROTEIN", Modifier.weight(1f))
                    MacroStat("25g", "CARBS", Modifier.weight(1f))
                    MacroStat("6g", "FAT", Modifier.weight(1f))
                }

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
                            Text(meal.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("Add later", color = AppMuted)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(meal.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
private fun DatePillsRow(selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        (-1..1).forEach { offset ->
            val date = selectedDate.plusDays(offset.toLong())
            val selected = offset == 0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) AppBlue else Color.Transparent, RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(onClick = { onDateChange(date) }) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("MMM d")),
                        color = if (selected) Color.White else AppMuted,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
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
                .background(Color(0xFFE4E8F0)),
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
            onDateChange = {},
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
