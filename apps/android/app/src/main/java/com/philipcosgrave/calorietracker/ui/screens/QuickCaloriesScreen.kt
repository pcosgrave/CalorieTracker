package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.DatePillsRow
import com.philipcosgrave.calorietracker.ui.components.DateStepper
import com.philipcosgrave.calorietracker.ui.components.MealPicker
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.isDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.components.normalizeDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun QuickCaloriesScreen(
    date: LocalDate,
    onBack: () -> Unit,
    onSave: suspend (Double, Meal, LocalDate) -> Unit,
    destinationMeal: Meal? = null,
    recipeDestination: Boolean = false,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    var calories by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(destinationMeal ?: com.philipcosgrave.calorietracker.domain.inferMealForTime(java.time.LocalTime.now())) }
    var selectedDate by remember { mutableStateOf(date) }

    Page {
        PageHeader("Quick Calories", onBack = onBack)
        AppCardContainer(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(calories,
                    {
                        val normalized = normalizeDecimalNumberInput(it)
                        if (isDecimalNumberInput(normalized))
                        {
                            calories = normalized
                        }
                    },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth())
                if (!recipeDestination) MealPicker(meal, { meal = it })
                else Text(if (recipeDestination) "Add to recipe" else "${meal.label} · $selectedDate")

                if (!recipeDestination) DatePillsRow(selectedDate,
                    today = today,
                    onDateChange = { selectedDate = it })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { saving = true; scope.launch {
                            try { onSave(calories.toDouble(), meal, selectedDate) }
                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (_: Exception) { error = "Could not add calories. Please retry." }
                            finally { saving = false }
                        } },
                        enabled = !saving && calories.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Log calories")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 700)
@Composable
private fun QuickCaloriesScreenPreview() {
    PreviewData.Theme {
        QuickCaloriesScreen(
            date = PreviewData.date,
            onBack = {},
            onSave = { _, _, _ -> },
        )
    }
}
