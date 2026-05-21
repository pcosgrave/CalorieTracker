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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.ui.components.DateStepper
import com.philipcosgrave.calorietracker.ui.components.MealPicker
import com.philipcosgrave.calorietracker.ui.components.Page
import java.time.LocalDate

@Composable
fun QuickCaloriesScreen(
    date: LocalDate,
    onBack: () -> Unit,
    onSave: (Double, Meal, LocalDate) -> Unit,
) {
    var calories by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(Meal.Snack) }
    var selectedDate by remember { mutableStateOf(date) }

    Page {
        Text("Quick Calories", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateStepper(selectedDate, { selectedDate = it })
                OutlinedTextField(calories, { calories = it }, label = { Text("Calories") }, modifier = Modifier.fillMaxWidth())
                MealPicker(meal, { meal = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onSave(calories.toDoubleOrNull() ?: 0.0, meal, selectedDate) },
                        enabled = (calories.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Log calories")
                    }
                }
            }
        }
    }
}
