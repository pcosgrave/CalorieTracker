package com.philipcosgrave.calorietracker.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.domain.*
import com.philipcosgrave.calorietracker.ui.components.*
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable fun CopyMealsScreen(targetDate: LocalDate, entries: List<DiaryEntry>, onBack: () -> Unit, onCopy: suspend (LocalDate, Set<Meal>) -> Unit) {
    var sourceText by rememberSaveable { mutableStateOf((entries.filter { it.date != targetDate }.maxByOrNull { it.date }?.date ?: targetDate).toString()) }
    val source = LocalDate.parse(sourceText)
    var selected by rememberSaveable(sourceText) { mutableStateOf(emptyList<String>()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Page {
        PageHeader("Copy meals", onBack = onBack)
        Text("Choose meals to copy to $targetDate")
        DatePillsRow(source, LocalDate.now(), { sourceText = it.toString() })
        Meal.entries.forEach { meal ->
            val foods = entries.filter { it.date == source && it.meal == meal }
            OutlinedCard(onClick = { if (!saving && foods.isNotEmpty()) selected = if (meal.name in selected) selected - meal.name else selected + meal.name }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(meal.name in selected, null, enabled = !saving && foods.isNotEmpty())
                    Column(Modifier.weight(1f).padding(start = 8.dp)) { Text(meal.label); Text("${foods.size} items · ${formatNumber(totalsForEntries(foods).calories)} kcal", color = AppMuted) }
                }
                if (meal.name in selected) foods.forEach { Text(it.food.name, Modifier.padding(start = 24.dp, bottom = 8.dp)) }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        AppPrimaryButton(if (saving) "Copying…" else "Copy selected meals", { saving = true; scope.launch {
            try { onCopy(source, selected.map(Meal::valueOf).toSet()) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { error = "Could not copy meals. Please retry." }
            finally { saving = false }
        } }, Modifier.fillMaxWidth(), enabled = !saving && selected.isNotEmpty())
    }
}
