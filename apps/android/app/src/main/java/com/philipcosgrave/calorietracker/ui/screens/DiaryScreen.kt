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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.totalsForEntries
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.ui.components.DiaryEntryRow
import com.philipcosgrave.calorietracker.ui.components.Header
import com.philipcosgrave.calorietracker.ui.components.MealPicker
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.TotalsGrid
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DiaryScreen(
    selectedDate: LocalDate,
    entries: List<DiaryEntry>,
    onDateChange: (LocalDate) -> Unit,
    onAddFood: () -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onUpdateEntry: (DiaryEntry) -> Unit,
) {
    val selectedEntries = entries.filter { it.date == selectedDate }
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    Page {
        Header()
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDateChange(selectedDate.minusDays(1)) }) { Text("<") }
                    Text(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Button(onClick = { onDateChange(selectedDate.plusDays(1)) }) { Text(">") }
                }
                TotalsGrid(totalsForEntries(selectedEntries))
                Button(onClick = onAddFood, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("+")
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

        Meal.entries.forEach { meal ->
            val mealEntries = selectedEntries.filter { it.meal == meal }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meal.label, style = MaterialTheme.typography.titleLarge)
                            Text("${mealEntries.size} foods", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${formatNumber(totalsForEntries(mealEntries).calories)} cal", fontWeight = FontWeight.Bold)
                    }
                    if (mealEntries.isEmpty()) {
                        Text("No food logged.")
                    } else {
                        mealEntries.forEach { entry ->
                            DiaryEntryRow(
                                entry = entry,
                                onEdit = { editingEntry = entry },
                                onDelete = { onDeleteEntry(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditEntryCard(entry: DiaryEntry, onCancel: () -> Unit, onSave: (DiaryEntry) -> Unit) {
    var name by remember(entry.id) { mutableStateOf(entry.food.name) }
    var brand by remember(entry.id) { mutableStateOf(entry.food.brand) }
    var calories by remember(entry.id) { mutableStateOf(formatNumber(entry.food.nutrients.calories)) }
    var servings by remember(entry.id) { mutableStateOf(formatNumber(entry.servingMultiplier)) }
    var meal by remember(entry.id) { mutableStateOf(entry.meal) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Edit logged food", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(brand, { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(calories, { calories = it }, label = { Text("Calories") }, modifier = Modifier.weight(1f))
                OutlinedTextField(servings, { servings = it }, label = { Text("Servings") }, modifier = Modifier.weight(1f))
            }
            MealPicker(meal = meal, onMealChange = { meal = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(
                            entry.copy(
                                meal = meal,
                                servingMultiplier = servings.toDoubleOrNull()?.coerceAtLeast(0.1) ?: entry.servingMultiplier,
                                food = entry.food.copy(
                                    name = name.ifBlank { entry.food.name },
                                    brand = brand,
                                    nutrients = entry.food.nutrients.copy(calories = calories.toDoubleOrNull() ?: entry.food.nutrients.calories),
                                ),
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
            }
        }
    }
}
