package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import com.philipcosgrave.calorietracker.data.remote.CanadianNutrientFileLookupService
import com.philipcosgrave.calorietracker.data.remote.nutritionSearchWords
import com.philipcosgrave.calorietracker.domain.validIngredientFields
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.philipcosgrave.calorietracker.domain.foodTitle
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
import com.philipcosgrave.calorietracker.ui.components.isDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.components.isDigitsOnlyInput
import com.philipcosgrave.calorietracker.ui.components.normalizeDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData

@Composable
fun AddIngredientScreen(
    existing: FoodItem?,
    barcodeLookupResult: FoodItem? = null,
    isLookingUpBarcode: Boolean = false,
    onBack: () -> Unit,
    onSave: (FoodItem) -> Unit,
    onLookupBarcode: (String) -> Unit = {},
    initialName: String = "",
    isSaving: Boolean = false,
    saveError: String? = null,
    autoSearchNutrition: Boolean = false,
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(foodTitle(existing?.name ?: initialName)) }
    var brand by rememberSaveable(existing?.id) { mutableStateOf(existing?.brand.orEmpty()) }
    var barcode by rememberSaveable(existing?.id) { mutableStateOf(existing?.barcode.orEmpty()) }
    var servingQuantity by rememberSaveable(existing?.id) { mutableStateOf(existing?.servingQuantity?.let(::formatNumber) ?: "1") }
    var servingUnit by rememberSaveable(existing?.id) { mutableStateOf(existing?.servingUnit ?: "serving") }
    var calories by rememberSaveable(existing?.id) { mutableStateOf(existing?.nutrients?.calories?.let(::formatNumber).orEmpty()) }
    var protein by rememberSaveable(existing?.id) { mutableStateOf(existing?.nutrients?.proteinGrams?.let(::formatNumber).orEmpty()) }
    var carbs by rememberSaveable(existing?.id) { mutableStateOf(existing?.nutrients?.carbohydrateGrams?.let(::formatNumber).orEmpty()) }
    var fat by rememberSaveable(existing?.id) { mutableStateOf(existing?.nutrients?.fatGrams?.let(::formatNumber).orEmpty()) }
    var lastAppliedLookupId by rememberSaveable(existing?.id) { mutableStateOf<String?>(null) }
    var showLabelCamera by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var labelApplied by rememberSaveable(existing?.id) { mutableStateOf(false) }

    val nutritionService = remember { CanadianNutrientFileLookupService() }
    val nutritionScope = rememberCoroutineScope()
    var searchingNutrition by remember { mutableStateOf(false) }
    var nutritionResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var nutritionMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var autoSearchStarted by rememberSaveable { mutableStateOf(false) }
    fun formSnapshot() = listOf(name, brand, barcode, servingQuantity, servingUnit, calories, protein, carbs, fat, labelApplied.toString())
    fun applyNutrition(food: FoodItem) {
        name = foodTitle(food.name)
        servingQuantity = "100"
        servingUnit = "g"
        fun exact(value: Double) = java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        calories = exact(food.nutrients.calories)
        protein = exact(food.nutrients.proteinGrams)
        carbs = exact(food.nutrients.carbohydrateGrams)
        fat = exact(food.nutrients.fatGrams)
        labelApplied = false
        nutritionResults = emptyList()
        nutritionMessage = "Filled from Canadian Nutrient File: ${food.name} (100 g). Review before saving."
    }
    suspend fun searchNutrition() {
        if (searchingNutrition || name.trim().length < 2) return
        val query = name.trim()
        val snapshot = formSnapshot()
        searchingNutrition = true
        nutritionMessage = null
        nutritionResults = emptyList()
        try {
            val results = withTimeout(45_000) { nutritionService.searchFoodsByName(query, limit = 6, per100Grams = true) }
            // A late response must never replace manual edits, UPC data, or a scanned label.
            if (snapshot != formSnapshot()) {
                nutritionMessage = "Your fields changed during the search. Search again to use the updated food name."
                return
            }
            val exact = results.singleOrNull { nutritionSearchWords(it.name) == nutritionSearchWords(query) }
            if (exact != null && listOf(calories, protein, carbs, fat).all { it.isBlank() }) applyNutrition(exact)
            else {
                nutritionResults = results
                nutritionMessage = if (results.isEmpty()) "No complete nutrition match found. Try a simpler food name, enter values, or scan a label."
                    else "Choose the matching food to fill nutrition for 100 g (Canadian Nutrient File)."
            }
        } catch (cancelled: CancellationException) {
            if (cancelled is kotlinx.coroutines.TimeoutCancellationException) nutritionMessage = "Nutrition search timed out. Retry or scan a label."
            else throw cancelled
        } catch (_: Exception) {
            nutritionMessage = "Nutrition search unavailable. Retry, enter values, or scan a label."
        } finally { searchingNutrition = false }
    }
    LaunchedEffect(Unit) {
        if (autoSearchNutrition && !autoSearchStarted && name.isNotBlank()) {
            autoSearchStarted = true
            searchNutrition()
        }
    }

    LaunchedEffect(barcodeLookupResult?.id) {
        val lookup = barcodeLookupResult ?: return@LaunchedEffect
        if (lookup.id == lastAppliedLookupId) return@LaunchedEffect
        nutritionResults = emptyList()
        nutritionMessage = null
        name = foodTitle(lookup.name)
        brand = lookup.brand
        if (barcode.isBlank()) {
            barcode = lookup.barcode
        }
        servingQuantity = formatNumber(lookup.servingQuantity)
        servingUnit = lookup.servingUnit
        calories = formatNumber(lookup.nutrients.calories)
        protein = formatNumber(lookup.nutrients.proteinGrams)
        carbs = formatNumber(lookup.nutrients.carbohydrateGrams)
        fat = formatNumber(lookup.nutrients.fatGrams)
        lastAppliedLookupId = lookup.id
        labelApplied = false
    }

    if (showLabelCamera) {
        NutritionLabelCamera(
            onBack = { showLabelCamera = false },
            onParsed = { label ->
                nutritionResults = emptyList()
                nutritionMessage = null
                if (name.isBlank()) name = foodTitle(label.name.orEmpty())
                if (brand.isBlank()) brand = label.brand.orEmpty()
                servingQuantity = label.quantity
                servingUnit = label.unit
                calories = label.calories
                protein = label.protein
                carbs = label.carbs
                fat = label.fat
                labelApplied = true
                showLabelCamera = false
            },
        )
        return
    }

    Page {
        PageHeader(if (existing == null) "Add Ingredient" else "Edit Ingredient", onBack = onBack)
        saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        AppCardContainer {
            AppPrimaryButton(
                text = "📷 Scan nutrition label",
                enabled = !isLookingUpBarcode && !isSaving,
                onClick = { showLabelCamera = true },
                modifier = Modifier.fillMaxWidth(),
            )
            if (labelApplied) {
                Text("Label fields filled. Check the serving size and values before saving; complete any blank nutrition fields.", color = AppMuted)
            }
            AppFormField(name, { name = it; nutritionResults = emptyList(); nutritionMessage = null }, "Food Name", Modifier.fillMaxWidth())
            AppPrimaryButton(
                text = if (searchingNutrition) "Searching nutrition..." else "Find nutrition for 100 g",
                enabled = !searchingNutrition && !isLookingUpBarcode && !isSaving && name.trim().length >= 2,
                onClick = { nutritionScope.launch { searchNutrition() } },
                modifier = Modifier.fillMaxWidth(),
            )
            nutritionMessage?.let { Text(it, color = AppMuted) }
            nutritionResults.forEach { result ->
                TextButton(enabled = !isSaving && !isLookingUpBarcode, onClick = { applyNutrition(result) }) {
                    Text("${foodTitle(result.name)} · ${formatNumber(result.nutrients.calories)} kcal / 100 g")
                }
            }
            AppFormField(brand, { brand = it }, "Brand (Optional)", Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppFormField(servingQuantity, {
                    val normalized = normalizeDecimalNumberInput(it)
                    if (isDecimalNumberInput(normalized)) {
                        servingQuantity = normalized
                    }
                }, "Serving Size", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                UnitPicker(
                    value = servingUnit,
                    onChange = { servingUnit = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                AppFormField(calories, {
                    val normalized = normalizeDecimalNumberInput(it)
                    if (isDecimalNumberInput(normalized)) {
                        calories = normalized
                    }
                }, "Calories (kcal)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                AppFormField(fat, {
                    val normalized = normalizeDecimalNumberInput(it)
                    if (isDecimalNumberInput(normalized)) {
                        fat = normalized
                    }
                }, "Fat (g)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppFormField(protein, {
                    val normalized = normalizeDecimalNumberInput(it)
                    if (isDecimalNumberInput(normalized)) {
                        protein = normalized
                    }
                }, "Protein (g)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                AppFormField(carbs, {
                    val normalized = normalizeDecimalNumberInput(it)
                    if (isDecimalNumberInput(normalized)) {
                        carbs = normalized
                    }
                }, "Carbs (g)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppFormField(barcode, {
                    if (isDigitsOnlyInput(it)) {
                        barcode = it
                    }
                }, "Barcode / UPC", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                AppPrimaryButton(
                    text = if (isLookingUpBarcode) "..." else "Find",
                    enabled = barcode.length >= 4 && !isLookingUpBarcode && !isSaving,
                    onClick = { onLookupBarcode(barcode.trim()) },
                )
            }

            AppPrimaryButton(
                text = if (isSaving) "Saving..." else "Save Food",
                enabled = !isSaving && !isLookingUpBarcode && !searchingNutrition &&
                    validIngredientFields(name, servingQuantity, servingUnit, calories, protein, carbs, fat, labelApplied),
                onClick = {
                    onSave(
                        FoodItem(
                            id = existing?.id ?: createId("custom"),
                            kind = FoodKind.Ingredient,
                            name = foodTitle(name),
                            brand = brand.trim(),
                            barcode = barcode.trim(),
                            servingQuantity = servingQuantity.toDouble(),
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
