package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.createId
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppFormField
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.AppPrimaryButton
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.UnitPicker
import com.philipcosgrave.calorietracker.ui.components.isDigitsOnlyInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData

@Composable
fun AddIngredientScreen(existing: FoodItem?, onBack: () -> Unit, onSave: (FoodItem) -> Unit) {
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
        PageHeader("Add Ingredient", onBack = onBack)
        AppCardContainer {
            AppFormField(name, { name = it }, "Food Name", Modifier.fillMaxWidth())
            AppFormField(brand, { brand = it }, "Brand (Optional)", Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppFormField(servingQuantity, {
                    if (isDigitsOnlyInput(it)) {
                        servingQuantity = it
                    }
                }, "Serving Size", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                UnitPicker(
                    value = servingUnit,
                    onChange = { servingUnit = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppFormField(calories, {
                    if (isDigitsOnlyInput(it)) {
                        calories = it
                    }
                }, "Calories (kcal)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                AppFormField(barcode, {
                    if (isDigitsOnlyInput(it)) {
                        barcode = it
                    }
                }, "Barcode / UPC", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppFormField(protein, {
                    if (isDigitsOnlyInput(it)) {
                        protein = it
                    }
                }, "Protein (g)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                AppFormField(carbs, {
                    if (isDigitsOnlyInput(it)) {
                        carbs = it
                    }
                }, "Carbs (g)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                AppFormField(fat, {
                    if (isDigitsOnlyInput(it)) {
                        fat = it
                    }
                }, "Fat (g)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            AppPrimaryButton(
                text = "Save Food",
                enabled = name.isNotBlank() && servingUnit.isNotBlank() && (calories.toDoubleOrNull() ?: 0.0) > 0,
                onClick = {
                    onSave(
                        FoodItem(
                            id = existing?.id ?: createId("custom"),
                            kind = FoodKind.Ingredient,
                            name = name.trim(),
                            brand = brand.trim(),
                            barcode = barcode.trim(),
                            servingQuantity = servingQuantity.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0,
                            servingUnit = servingUnit.trim(),
                            nutrients = Nutrients(
                                calories = calories.toDoubleOrNull() ?: 0.0,
                                proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                                carbohydrateGrams = carbs.toDoubleOrNull() ?: 0.0,
                                fatGrams = fat.toDoubleOrNull() ?: 0.0,
                            ),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun AddIngredientScreenPreview() {
    PreviewData.Theme {
        AddIngredientScreen(
            existing = PreviewData.food,
            onBack = {},
            onSave = {},
        )
    }
}
