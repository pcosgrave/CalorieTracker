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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.createId
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.UnitPicker
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import com.philipcosgrave.calorietracker.ui.preview.PreviewTheme

@Composable
fun NewIngredientScreen(existing: FoodItem?, onBack: () -> Unit, onSave: (FoodItem) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var brand by remember(existing?.id) { mutableStateOf(existing?.brand.orEmpty()) }
    var barcode by remember(existing?.id) { mutableStateOf(existing?.barcode.orEmpty()) }
    var servingQuantity by remember(existing?.id) { mutableStateOf(existing?.servingQuantity?.let(::formatNumber) ?: "1") }
    var servingUnit by remember(existing?.id) { mutableStateOf(existing?.servingUnit ?: "serving") }
    var calories by remember(existing?.id) { mutableStateOf(existing?.nutrients?.calories?.let(::formatNumber).orEmpty()) }
    var protein by remember(existing?.id) { mutableStateOf(existing?.nutrients?.proteinGrams?.let(::formatNumber).orEmpty()) }
    var carbs by remember(existing?.id) { mutableStateOf(existing?.nutrients?.carbohydrateGrams?.let(::formatNumber).orEmpty()) }
    var fat by remember(existing?.id) { mutableStateOf(existing?.nutrients?.fatGrams?.let(::formatNumber).orEmpty()) }

    Page {
        Text(if (existing == null) "Add New Food" else "Edit Food", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(brand, { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(servingQuantity, { servingQuantity = it }, label = { Text("Serving size") }, modifier = Modifier.weight(1f))
                    UnitPicker(servingUnit, { servingUnit = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(calories, { calories = it }, label = { Text("Calories") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(protein, { protein = it }, label = { Text("Protein") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbs") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(fat, { fat = it }, label = { Text("Fat") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        enabled = name.isNotBlank() && brand.isNotBlank() && (calories.toDoubleOrNull() ?: 0.0) > 0,
                        onClick = {
                            onSave(
                                FoodItem(
                                    id = existing?.id ?: createId("custom"),
                                    kind = FoodKind.Ingredient,
                                    name = name.trim(),
                                    brand = brand.trim(),
                                    barcode = barcode.trim(),
                                    servingQuantity = servingQuantity.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0,
                                    servingUnit = servingUnit,
                                    nutrients = Nutrients(
                                        calories = calories.toDoubleOrNull() ?: 0.0,
                                        proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                                        carbohydrateGrams = carbs.toDoubleOrNull() ?: 0.0,
                                        fatGrams = fat.toDoubleOrNull() ?: 0.0,
                                    ),
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save Food")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun NewIngredientScreenPreview() {
    PreviewTheme {
        NewIngredientScreen(
            existing = PreviewData.food,
            onBack = {},
            onSave = {},
        )
    }
}
