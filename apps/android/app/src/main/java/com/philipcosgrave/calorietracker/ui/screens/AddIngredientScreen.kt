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
import com.philipcosgrave.calorietracker.domain.formatAmount
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
    initialBarcode: String = "",
    onAnalyzeLabel: (suspend (android.graphics.Bitmap, (String) -> Unit) -> com.philipcosgrave.calorietracker.domain.NutritionLabel)? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    autoSearchNutrition: Boolean = false,
    onSearchNutrition: (suspend (String) -> List<FoodItem>)? = null,
    knownFoods: List<FoodItem> = emptyList(),
    initialLabel: com.philipcosgrave.calorietracker.domain.NutritionLabel? = null,
) {
    val localAnalyzer = remember { com.philipcosgrave.calorietracker.data.local.LocalPhotoFoodAnalyzer() }
    val labelAnalyzer: suspend (android.graphics.Bitmap, (String) -> Unit) -> com.philipcosgrave.calorietracker.domain.NutritionLabel = onAnalyzeLabel ?: { bitmap: android.graphics.Bitmap, status: (String) -> Unit -> localAnalyzer.analyzeLabel(bitmap, status) }
    var photoNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var nameCamera by rememberSaveable { mutableStateOf(false) }
    var referenceReview by rememberSaveable(stateSaver = com.philipcosgrave.calorietracker.ui.SelectedFoodSaver) { mutableStateOf<FoodItem?>(null) }
    var sourceFood by rememberSaveable(stateSaver = com.philipcosgrave.calorietracker.ui.SelectedFoodSaver) { mutableStateOf(existing) }
    var speaking by rememberSaveable { mutableStateOf(false) }
    var optionalNutritionExpanded by rememberSaveable { mutableStateOf(false) }
    var additionalJson by rememberSaveable(existing?.id) { mutableStateOf(org.json.JSONObject(existing?.nutrients?.additional.orEmpty().mapValues { formatAmount(it.value) }).toString()) }
    var step by rememberSaveable(existing?.id) { mutableStateOf(0) }
    var photoPath by rememberSaveable(existing?.id) { mutableStateOf(existing?.photoPath) }
    var weightGrams by rememberSaveable(existing?.id) { mutableStateOf(existing?.servingWeightGrams?.toString().orEmpty()) }
    var hasServing by rememberSaveable(existing?.id) { mutableStateOf(existing != null && com.philipcosgrave.calorietracker.domain.convertAmount(1.0, existing.servingUnit, "g") == null) }
    var name by rememberSaveable(existing?.id) { mutableStateOf(foodTitle(existing?.name ?: initialName)) }
    var brand by rememberSaveable(existing?.id) { mutableStateOf(existing?.brand.orEmpty()) }
    var barcode by rememberSaveable(existing?.id) { mutableStateOf(existing?.barcode ?: initialBarcode) }
    var servingQuantity by rememberSaveable(existing?.id) { mutableStateOf(existing?.servingQuantity?.let(::formatAmount) ?: initialLabel?.quantity ?: "100") }
    var servingUnit by rememberSaveable(existing?.id) { mutableStateOf(existing?.servingUnit ?: initialLabel?.unit ?: "g") }
    var calories by rememberSaveable(existing?.id) { mutableStateOf(existing?.nutrients?.calories?.let(::formatAmount) ?: initialLabel?.calories.orEmpty()) }
    var protein by rememberSaveable(existing?.id) { mutableStateOf(existing?.nutrients?.proteinGrams?.let(::formatAmount) ?: initialLabel?.protein.orEmpty()) }
    var carbs by rememberSaveable(existing?.id) { mutableStateOf(existing?.nutrients?.carbohydrateGrams?.let(::formatAmount) ?: initialLabel?.carbs.orEmpty()) }
    var fat by rememberSaveable(existing?.id) { mutableStateOf(existing?.nutrients?.fatGrams?.let(::formatAmount) ?: initialLabel?.fat.orEmpty()) }
    var lastAppliedLookupId by rememberSaveable(existing?.id) { mutableStateOf<String?>(null) }
    var showLabelCamera by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var showBarcodeCamera by rememberSaveable { mutableStateOf(false) }
    var labelApplied by rememberSaveable(existing?.id) { mutableStateOf(initialLabel != null) }

    val nutritionScope = rememberCoroutineScope()
    var searchingNutrition by remember { mutableStateOf(false) }
    var nutritionResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var nutritionMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var autoSearchStarted by rememberSaveable { mutableStateOf(false) }
    fun formSnapshot() = listOf(name, brand, barcode, servingQuantity, servingUnit, calories, protein, carbs, fat, labelApplied.toString())
    fun applyNutrition(food: FoodItem) {
        name = foodTitle(food.name)
        servingQuantity = formatAmount(food.servingQuantity)
        servingUnit = food.servingUnit
        hasServing = com.philipcosgrave.calorietracker.domain.convertAmount(1.0, food.servingUnit, "g") == null
        weightGrams = food.servingWeightGrams?.let(::formatAmount).orEmpty()
        fun exact(value: Double) = java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        calories = exact(food.nutrients.calories)
        protein = exact(food.nutrients.proteinGrams)
        carbs = exact(food.nutrients.carbohydrateGrams)
        fat = exact(food.nutrients.fatGrams)
        labelApplied = false
        nutritionResults = emptyList()
        nutritionMessage = "Filled from ${food.name}. Review before saving."
    }
    suspend fun searchNutrition() {
        if (searchingNutrition || name.trim().length < 2) return
        val query = name.trim()
        val snapshot = formSnapshot()
        searchingNutrition = true
        nutritionMessage = null
        nutritionResults = emptyList()
        try {
            val results = withTimeout(45_000) { onSearchNutrition?.invoke(query).orEmpty() }
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
        if (onSearchNutrition != null && autoSearchNutrition && !autoSearchStarted && name.isNotBlank()) {
            autoSearchStarted = true
            searchNutrition()
        }
    }

    if (nameCamera) {
        NutritionLabelCamera(onBack = { nameCamera = false }, onParsed = { result ->
            photoNames = result.name.orEmpty().split("\n").filter { it.isNotBlank() }; nameCamera = false
            if (photoNames.size == 1) { name = foodTitle(photoNames.first()); photoNames = emptyList() }
        }, onAnalyze = { bitmap, status ->
            val names = localAnalyzer.analyze(bitmap, knownFoods, status).map { it.name }.distinct()
            com.philipcosgrave.calorietracker.domain.NutritionLabel(name = names.joinToString("\n"), brand = null, quantity = "", unit = "g", calories = "", protein = "", carbs = "", fat = "")
        }, title = "Identify food", guidance = "Take a photo to identify the food name, then choose its nutrition from your foods or CNF. The image stays in memory.")
        return
    }
    if (speaking) {
        SpeakFoodScreen(onBack = { speaking = false }, onReview = { name = foodTitle(it); speaking = false })
        return
    }
    referenceReview?.let { food ->
        androidx.activity.compose.BackHandler { referenceReview = null }
        FoodDetailsScreen(food, { referenceReview = null }, {
            sourceFood = food
            applyNutrition(food)
            additionalJson = org.json.JSONObject(food.nutrients.additional.mapValues { formatAmount(it.value) }).toString()
            referenceReview = null; step = 1
        }, {
            if (!isSaving) onSave(if (food.id.startsWith("cnf:")) food.copy(id = createId("custom")) else food)
        }, if (isSaving) "Saving…" else if (food.id.startsWith("cnf:")) "Add Ingredient" else "Use Ingredient")
        return
    }
    if (showBarcodeCamera) {
        androidx.activity.compose.BackHandler { showBarcodeCamera = false }
        BarcodeScannerScreen(onBack = { showBarcodeCamera = false }, onBarcodeDetected = { code -> barcode = code; showBarcodeCamera = false })
        return
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
        weightGrams = lookup.servingWeightGrams?.toString().orEmpty()
        hasServing = com.philipcosgrave.calorietracker.domain.convertAmount(1.0, lookup.servingUnit, "g") == null
        calories = formatNumber(lookup.nutrients.calories)
        protein = formatNumber(lookup.nutrients.proteinGrams)
        carbs = formatNumber(lookup.nutrients.carbohydrateGrams)
        fat = formatNumber(lookup.nutrients.fatGrams)
        lastAppliedLookupId = lookup.id
        labelApplied = false
    }

    if (showLabelCamera) {
        NutritionLabelCamera(
            onAnalyze = labelAnalyzer,            onBack = { showLabelCamera = false },
            onParsed = { label ->
                nutritionResults = emptyList()
                nutritionMessage = null
                if (name.isBlank()) name = foodTitle(label.name.orEmpty())
                if (brand.isBlank()) brand = label.brand.orEmpty()
                servingQuantity = label.quantity
                servingUnit = label.unit
                hasServing = com.philipcosgrave.calorietracker.domain.convertAmount(1.0, label.unit, "g") == null
                weightGrams = ""
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

    fun previous() { if (step > 0) step-- else onBack() }
    androidx.activity.compose.BackHandler { previous() }
    val validWeight = weightGrams.isBlank() || weightGrams.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    Page {
        PageHeader(if (existing != null) "Edit Food" else listOf("New Food", "Serving information", "Nutrition information")[step], onBack = ::previous)
        Text("${step + 1} of 3", color = AppMuted)
        saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (step == 0) {
            com.philipcosgrave.calorietracker.ui.components.FoodPhotoPicker(photoPath) { photoPath = it }
            photoNames.forEach { candidate -> TextButton(onClick = { name = foodTitle(candidate); photoNames = emptyList() }) { Text(candidate) } }
            AppFormField(name, { name = it }, "Food Name", Modifier.fillMaxWidth())
            if (existing == null) com.philipcosgrave.calorietracker.ui.components.ReferenceLookupResults(name, knownFoods,
                onSelect = { referenceReview = it }, showLocal = true, onEmpty = {
                    TextButton(onClick = { showBarcodeCamera = true }) { Text("Scan Barcode") }
                    TextButton(onClick = { showLabelCamera = true }) { Text("Scan Nutrition Label") }
                    TextButton(onClick = { nameCamera = true }) { Text("Take a Photo") }
                    TextButton(onClick = { speaking = true }) { Text("Speak Food Name") }
                })
            AppFormField(brand, { brand = it }, "Brand (Optional)", Modifier.fillMaxWidth())
            AppPrimaryButton(if (existing == null) "Enter Manually" else "Next", { step = 1 }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && !isSaving)
        }
        if (step == 1) {
            Text("Use weight alone, or define a familiar serving such as a slice.", color = AppMuted)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Define a serving (optional)", Modifier.weight(1f))
                androidx.compose.material3.Switch(hasServing, { enabled ->
                    hasServing = enabled
                    if (!enabled) {
                        val grams = com.philipcosgrave.calorietracker.domain.convertAmount(servingQuantity.toDoubleOrNull() ?: 100.0, servingUnit, "g") ?: weightGrams.toDoubleOrNull()
                        servingQuantity = grams?.toString().orEmpty()
                        servingUnit = "g"; weightGrams = ""
                    } else if (servingUnit == "g") {
                        weightGrams = servingQuantity; servingQuantity = "1"; servingUnit = "serving"
                    }
                })
            }
            AppCardContainer {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppFormField(servingQuantity, { if (isDecimalNumberInput(it)) servingQuantity = it }, if (hasServing) "Serving size" else "Nutrition weight", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    if (hasServing) AppFormField(servingUnit, { servingUnit = it }, "Unit (slice, piece…)", Modifier.weight(1f))
                    else Text("g", Modifier.weight(1f))
                }
                if (hasServing) AppFormField(weightGrams, { if (isDecimalNumberInput(it)) weightGrams = it }, "Weight of this serving in g (optional)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (hasServing && weightGrams.isNotBlank()) Text("$servingQuantity $servingUnit = $weightGrams g")
            }
            AppPrimaryButton("Next", { step = 2 }, Modifier.fillMaxWidth(), enabled = validWeight && servingQuantity.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true && servingUnit.isNotBlank())
        }
        if (step == 2) {
            Text("Enter nutrition for $servingQuantity $servingUnit${if (hasServing && weightGrams.isNotBlank()) " ($weightGrams g)" else ""}.", color = AppMuted)
            if (labelApplied) Text("Check the fields read from the label before saving.", color = AppMuted)
            AppCardContainer {
                AppFormField(calories, { if (isDecimalNumberInput(it)) calories = it }, "Calories (kcal)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                AppFormField(protein, { if (isDecimalNumberInput(it)) protein = it }, "Protein (g)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                AppFormField(fat, { if (isDecimalNumberInput(it)) fat = it }, "Fat (g)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                AppFormField(carbs, { if (isDecimalNumberInput(it)) carbs = it }, "Carbs (g)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            TextButton(onClick = { optionalNutritionExpanded = !optionalNutritionExpanded }) { Text("Additional nutrition (optional)") }
            if (optionalNutritionExpanded) {
                val extras = org.json.JSONObject(additionalJson)
                com.philipcosgrave.calorietracker.model.additionalNutrientFields.forEach { (key, field) ->
                    AppFormField(if (extras.has(key)) extras.getString(key) else "", { value ->
                        if (isDecimalNumberInput(value)) {
                            val changed = org.json.JSONObject(additionalJson)
                            val parsed = value.toDoubleOrNull()
                            if (value.isNotBlank()) changed.put(key, value) else changed.remove(key)
                            additionalJson = changed.toString()
                        }
                    }, "${field.first} (${field.second})", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
            if (onSearchNutrition != null) {
                TextButton(enabled = !searchingNutrition, onClick = { nutritionScope.launch { searchNutrition() } }) { Text(if (searchingNutrition) "Searching…" else "Find nutrition for 100 g") }
                nutritionResults.forEach { result -> TextButton(onClick = { applyNutrition(result); hasServing = false; weightGrams = "" }) { Text(result.name) } }
                nutritionMessage?.let { Text(it, color = AppMuted) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFormField(barcode, { if (isDigitsOnlyInput(it)) barcode = it }, "Barcode / UPC (optional)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                TextButton(onClick = { showBarcodeCamera = true }) { Text("Scan") }
                TextButton(enabled = barcode.length >= 4 && !isLookingUpBarcode, onClick = { onLookupBarcode(barcode) }) { Text("Find") }
            }
            TextButton(enabled = !isSaving, onClick = { showLabelCamera = true }) { Text("Scan nutrition label") }
            AppPrimaryButton(if (isSaving) "Saving…" else "Save Food", {
                onSave(FoodItem(id = existing?.id ?: createId("custom"), kind = FoodKind.Ingredient,
                    name = foodTitle(name), brand = brand.trim(), barcode = barcode.trim(),
                    servingQuantity = servingQuantity.toDouble(), servingUnit = servingUnit.trim(),
                    servingWeightGrams = weightGrams.toDoubleOrNull(), photoPath = photoPath,
                    source = sourceFood?.source, sourceId = sourceFood?.sourceId, sourceVersion = sourceFood?.sourceVersion,
                    servingOptions = sourceFood?.servingOptions.orEmpty(),

                    nutrients = Nutrients(calories.toDouble(), protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0, org.json.JSONObject(additionalJson).let { obj -> obj.keys().asSequence().mapNotNull { key -> obj.optString(key).toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }?.let { key to it } }.toMap() })))
            }, Modifier.fillMaxWidth(), enabled = !isSaving && !isLookingUpBarcode && !searchingNutrition && validWeight && validIngredientFields(name, servingQuantity, servingUnit, calories, protein, carbs, fat, labelApplied))
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
