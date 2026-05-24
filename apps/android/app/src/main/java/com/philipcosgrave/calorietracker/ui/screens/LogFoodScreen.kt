package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.measurementUnits
import com.philipcosgrave.calorietracker.domain.scale
import com.philipcosgrave.calorietracker.domain.withAdjustedComponents
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.DateStepper
import com.philipcosgrave.calorietracker.ui.components.MealPicker
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.UnitPicker
import com.philipcosgrave.calorietracker.ui.components.isDigitsOnlyInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import java.time.LocalDate

@Composable
fun LogFoodScreen(
    food: FoodItem,
    date: LocalDate,
    onBack: () -> Unit,
    onLog: (Meal, LocalDate, FoodItem, Double, Boolean) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.background.luminance() < 0.5f
    var amount by remember(food.id) { mutableStateOf(formatNumber(food.servingQuantity)) }
    var unit by remember(food.id) { mutableStateOf(food.servingUnit) }
    var meal by remember { mutableStateOf(Meal.Breakfast) }
    var selectedDate by remember { mutableStateOf(date) }
    var components by remember(food.id) { mutableStateOf(food.components) }
    val amountNumber = amount.toDoubleOrNull()?.coerceAtLeast(0.1) ?: food.servingQuantity
    val adjustedFood = if (components.isEmpty()) food else food.withAdjustedComponents(components)
    val adjusted = adjustedFood.nutrients.scale(amountNumber / adjustedFood.servingQuantity.coerceAtLeast(0.1))

    fun updateComponent(index: Int, amountText: String? = null, unitValue: String? = null) {
        components = components.mapIndexed { componentIndex, component ->
            if (componentIndex != index) {
                component
            } else {
                component.copy(
                    amount = amountText?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: component.amount,
                    unit = unitValue ?: component.unit,
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader("Add foods", onBack = onBack)
        Text(food.name, style = MaterialTheme.typography.headlineSmall, color = colorScheme.onBackground)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Serving size", color = colorScheme.onBackground)
            OutlinedTextField(
                amount,
                {
                    if (isDigitsOnlyInput(it)) {
                        amount = it
                    }
                },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            UnitPicker(unit, { unit = it }, Modifier.weight(1f))
            Text("${formatNumber(adjusted.calories)} cals.", color = colorScheme.onBackground, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meal & Snacks Time", style = MaterialTheme.typography.titleLarge, color = colorScheme.onBackground)
            MealPicker(meal, { meal = it }, darkMode = isDarkTheme)
        }
        if (components.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recipe ingredients", color = colorScheme.onBackground, fontWeight = FontWeight.Bold)
                components.forEachIndexed { index, component ->
                    EditableRecipeLogComponent(
                        component = component,
                        onAmountChange = { updateComponent(index, amountText = it) },
                        onUnitChange = { updateComponent(index, unitValue = it) },
                    )
                }
            }
        }
        DateStepper(selectedDate, { selectedDate = it }, darkMode = isDarkTheme)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onLog(meal, selectedDate, adjustedFood, amountNumber, true) }, modifier = Modifier.weight(1f)) {
                Text("Log & add more")
            }
            Button(onClick = { onLog(meal, selectedDate, adjustedFood, amountNumber, false) }, modifier = Modifier.weight(1f)) {
                Text("Log this")
            }
        }
    }
}

@Composable
private fun EditableRecipeLogComponent(
    component: RecipeComponent,
    onAmountChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
) {
    var amountText by remember(component.item.id) { mutableStateOf(formatNumber(component.amount)) }
    val unitOptions = remember(component.unit, component.item.servingUnit) {
        buildList {
            if (component.unit.isNotBlank()) add(component.unit)
            if (component.item.servingUnit.isNotBlank() && component.item.servingUnit !in this) add(component.item.servingUnit)
            addAll(measurementUnits.filterNot { it in this })
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(component.item.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(
                component.item.brand.ifBlank { component.item.servingUnit },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedTextField(
            value = amountText,
            onValueChange = {
                if (isDigitsOnlyInput(it)) {
                    amountText = it
                    onAmountChange(it)
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(84.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        CompactUnitPicker(
            value = component.unit,
            onChange = onUnitChange,
            units = unitOptions,
            modifier = Modifier.width(108.dp),
        )
    }
}

@Composable
private fun CompactUnitPicker(
    value: String,
    onChange: (String) -> Unit,
    units: List<String>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(value) { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit) },
                    onClick = {
                        expanded = false
                        onChange(unit)
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun LogFoodScreenPreview() {
    PreviewData.Theme {
        LogFoodScreen(
            food = PreviewData.foods.last(),
            date = PreviewData.date,
            onBack = {},
            onLog = { _, _, _, _, _ -> },
        )
    }
}
