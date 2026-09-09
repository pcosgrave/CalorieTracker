package com.philipcosgrave.calorietracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.*
import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CreateLeftoverDialog(entries: List<DiaryEntry>, onDismiss: () -> Unit,
    onCreate: suspend (List<DiaryEntry>, Double, String) -> Unit) {
    var selected by remember { mutableStateOf(entries.map { it.id }.toSet()) }
    var name by remember { mutableStateOf("${entries.first().meal.label} leftovers") }
    var percent by remember { mutableStateOf("50") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val chosen = entries.filter { it.id in selected }
    val percentage = percent.toDoubleOrNull()
    val valid = chosen.isNotEmpty() && name.isNotBlank() && percentage != null && percentage.isFinite() && percentage > 0 && percentage <= 100
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Create leftover") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Leftover name") }, enabled = !saving)
            Text("Meal date: ${entries.first().date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}")
            Text("Select foods and the percentage to move out of this meal.")
            entries.forEach { entry ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(entry.id in selected, { checked -> selected = if (checked) selected + entry.id else selected - entry.id }, enabled = !saving)
                    Text("${entry.food.name} · ${formatNumber(entry.loggedAmount)} ${entry.loggedUnit}", Modifier.weight(1f))
                }
            }
            OutlinedTextField(percent, { percent = it.replace(',', '.') }, label = { Text("Percentage to save (%)") }, enabled = !saving,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
            if (valid) {
                val split = splitLeftover(chosen, percentage!!, name)
                Text("Save ${formatNumber(totalsForEntries(split.leftover.entries).calories)} kcal for later. Leave ${formatNumber(100 - percentage)}% of each selected item in this meal.")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = valid && !saving, onClick = {
            saving = true
            scope.launch {
                try { onCreate(chosen, percentage!!, name); onDismiss() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (exception: Exception) { error = exception.message ?: "Could not save leftover. Try again." }
                finally { saving = false }
            }
        }) { Text(if (saving) "Saving..." else "Save leftover") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun LeftoversScreen(leftovers: List<Leftover>, initialDate: LocalDate, onBack: () -> Unit,
    onUse: suspend (Leftover, LocalDate, Meal) -> Unit) {
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    BackHandler { if (!saving) onBack() }
    Page {
        PageHeader("Leftovers", onBack = { if (!saving) onBack() })
        Text("Tap a leftover to review its ingredients and add it to a meal.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (leftovers.isEmpty()) Text("No leftovers saved. Use Create leftover beside Meal Log.")
        leftovers.forEach { leftover ->
            key(leftover.id) {
                var dateText by rememberSaveable { mutableStateOf(initialDate.toString()) }
                var mealName by rememberSaveable { mutableStateOf(Meal.Lunch.name) }
                val date = LocalDate.parse(dateText)
                val meal = Meal.valueOf(mealName)
                val expanded = expandedId == leftover.id
                AppCardContainer {
                    Row(Modifier.fillMaxWidth().clickable(enabled = !saving, onClickLabel = "Review leftover") {
                        expandedId = if (expanded) null else leftover.id
                    }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(leftover.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(leftover.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(if (expanded) "⌃" else "›", style = MaterialTheme.typography.headlineSmall)
                    }
                    if (expanded) {
                        HorizontalDivider()
                        Text("Leftover ingredients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        leftover.entries.forEach { entry ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.food.name, fontWeight = FontWeight.Medium)
                                    Text("${formatNumber(entry.loggedAmount)} ${entry.loggedUnit}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("${formatNumber(entry.food.nutrients.calories * entry.servingMultiplier)} kcal")
                            }
                        }
                        val totals = totalsForEntries(leftover.entries)
                        Text("${formatNumber(totals.calories)} kcal · Protein ${formatNumber(totals.protein)} g · Carbs ${formatNumber(totals.carbs)} g · Fat ${formatNumber(totals.fat)} g")
                        if (!saving) {
                            DatePillsRow(date, LocalDate.now(), { if (!it.isAfter(LocalDate.now())) dateText = it.toString() })
                            MealPicker(meal, { mealName = it.name })
                        }
                        AppPrimaryButton(text = if (saving) "Saving..." else "Add to ${meal.label}", enabled = !saving, modifier = Modifier.fillMaxWidth(), onClick = {
                            saving = true
                            error = null
                            scope.launch {
                                try { onUse(leftover, date, meal); expandedId = null }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (exception: Exception) { error = exception.message ?: "Could not use leftover. Try again." }
                                finally { saving = false }
                            }
                        })
                    }
                }
            }
        }
    }
}
