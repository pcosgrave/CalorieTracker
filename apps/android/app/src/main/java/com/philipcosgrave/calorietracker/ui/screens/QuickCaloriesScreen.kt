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
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import java.time.LocalDate

@Composable
fun QuickCaloriesScreen(
    date: LocalDate,
    onBack: () -> Unit,
    onSave: (Double, Meal, LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    var calories by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(Meal.Snack) }
    var selectedDate by remember { mutableStateOf(date) }

    Page {
        PageHeader("Quick Calories", onBack = onBack)
        AppCardContainer(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(calories,
                    {
                        if (it.isEmpty() || it.all { it.isDigit() })
                        {
                            calories = it
                        }
                    },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth())
                MealPicker(meal, { meal = it })

                DatePillsRow(selectedDate,
                    today = today,
                    onDateChange = { selectedDate = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
