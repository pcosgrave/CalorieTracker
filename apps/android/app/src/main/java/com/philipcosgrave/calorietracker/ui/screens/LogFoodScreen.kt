package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.scale
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.ui.components.DateStepper
import com.philipcosgrave.calorietracker.ui.components.MealPicker
import com.philipcosgrave.calorietracker.ui.components.UnitPicker
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import com.philipcosgrave.calorietracker.ui.preview.PreviewTheme
import java.time.LocalDate

@Composable
fun LogFoodScreen(
    food: FoodItem,
    date: LocalDate,
    onBack: () -> Unit,
    onLog: (Meal, LocalDate, Double, Boolean) -> Unit,
) {
    var amount by remember(food.id) { mutableStateOf(formatNumber(food.servingQuantity)) }
    var unit by remember(food.id) { mutableStateOf(food.servingUnit) }
    var meal by remember { mutableStateOf(Meal.Breakfast) }
    var selectedDate by remember { mutableStateOf(date) }
    val amountNumber = amount.toDoubleOrNull()?.coerceAtLeast(0.1) ?: food.servingQuantity
    val adjusted = food.nutrients.scale(amountNumber / food.servingQuantity.coerceAtLeast(0.1))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("< Back") }
        Text(food.name, style = MaterialTheme.typography.headlineSmall, color = Color(0xFFF5F1E8))
        Text("NUTRITION FACTS", color = Color(0xFF00D1FF), fontWeight = FontWeight.Bold)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Serving size", color = Color.White)
            OutlinedTextField(amount, { amount = it }, modifier = Modifier.weight(1f))
            UnitPicker(unit, { unit = it }, Modifier.weight(1f))
            Text("${formatNumber(adjusted.calories)} cals.", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meal & Snacks Time", style = MaterialTheme.typography.titleLarge, color = Color.White)
            MealPicker(meal, { meal = it }, darkMode = true)
        }
        if (food.components.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recipe ingredients", color = Color.White, fontWeight = FontWeight.Bold)
                food.components.forEach { component ->
                    Row {
                        Text("* ${component.item.name}", color = Color(0xFFD5D0C7), modifier = Modifier.weight(1f))
                        Text("Serving: ${formatNumber(component.amount)} ${component.unit}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        DateStepper(selectedDate, { selectedDate = it }, darkMode = true)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onLog(meal, selectedDate, amountNumber, true) }, modifier = Modifier.weight(1f)) {
                Text("Log & add more")
            }
            Button(onClick = { onLog(meal, selectedDate, amountNumber, false) }, modifier = Modifier.weight(1f)) {
                Text("Log this")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun LogFoodScreenPreview() {
    PreviewTheme {
        LogFoodScreen(
            food = PreviewData.foods.last(),
            date = PreviewData.date,
            onBack = {},
            onLog = { _, _, _, _ -> },
        )
    }
}
