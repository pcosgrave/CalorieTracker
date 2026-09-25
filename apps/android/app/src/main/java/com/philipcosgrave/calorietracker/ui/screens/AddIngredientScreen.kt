package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.philipcosgrave.calorietracker.model.ReferenceServing
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
    openCnfSearch: Boolean = false,
    initialLabel: com.philipcosgrave.calorietracker.domain.NutritionLabel? = null,
) {
    val localAnalyzer = remember { com.philipcosgrave.calorietracker.data.local.LocalPhotoFoodAnalyzer() }
    val labelAnalyzer: suspend (android.graphics.Bitmap, (String) -> Unit) -> com.philipcosgrave.calorietracker.domain.NutritionLabel = onAnalyzeLabel ?: { bitmap: android.graphics.Bitmap, status: (String) -> Unit -> localAnalyzer.analyzeLabel(bitmap, status) }
    var photoNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var cnfSearchOpen by rememberSaveable { mutableStateOf(openCnfSearch) }
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
    var servingOptions by remember { mutableStateOf(existing?.servingOptions.orEmpty()) }
    var newServingName by rememberSaveable(existing?.id) { mutableStateOf("") }
    var newServingSize by rememberSaveable(existing?.id) { mutableStateOf("1") }
    var newServingSizeUnit by rememberSaveable(existing?.id) { mutableStateOf("serving") }
    var newServingAmount by rememberSaveable(existing?.id) { mutableStateOf("") }
    var newServingUnit by rememberSaveable(existing?.id) { mutableStateOf(existing?.servingUnit ?: initialLabel?.unit ?: "g") }
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
        servingOptions = if (food.source?.contains("CNF", ignoreCase = true) == true || food.source?.contains("Canadian Nutrient", ignoreCase = true) == true) {
            com.philipcosgrave.calorietracker.domain.normalizedCnfServingOptions(food.servingOptions)
        } else {
            food.servingOptions
        }
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

    if (cnfSearchOpen) {
        androidx.activity.compose.BackHandler { onBack() }
        Page(spacing = 12.dp) {
            PageHeader("Search CNF Foods", onBack = onBack)
            FoodFlowProgress(step = 0, onStepClick = { target ->
                // CNF search is the Basic step of the same food flow. Later
                // steps are available only after the user selects a result.
                if (target == 0) step = 0
            })
            Text("Search the Canadian Nutrient File by food name.", color = AppMuted)
            com.philipcosgrave.calorietracker.ui.components.FoodPhotoPicker(photoPath) { photoPath = it }
            AppFormField(name, { name = it }, "Food name", Modifier.fillMaxWidth())
            com.philipcosgrave.calorietracker.ui.components.ReferenceLookupResults(
                query = name,
                localFoods = emptyList(),
                onSelect = { food ->
                    // A CNF match supplies the form values; it is not the final
                    // save action. Continue through Nutrition, Serving, and
                    // Review so the user can inspect or adjust every value.
                    sourceFood = food
                    applyNutrition(food)
                    additionalJson = org.json.JSONObject(food.nutrients.additional.mapValues { formatAmount(it.value) }).toString()
                    referenceReview = null
                    cnfSearchOpen = false
                    step = 1
                },
                onEmpty = { Text("Enter a food name to see matching CNF foods.", color = AppMuted) },
            )
            AppPrimaryButton(
                "Enter manually",
                { cnfSearchOpen = false; referenceReview = null; step = 0 },
                Modifier.fillMaxWidth(),
            )
        }
        return
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
        FoodDetailsScreen(food, { referenceReview = null; cnfSearchOpen = true }, {
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
        BarcodeScannerScreen(onBack = { showBarcodeCamera = false }, onBarcodeDetected = { code ->
            barcode = code
            showBarcodeCamera = false
            if (code.length >= 4) onLookupBarcode(code)
        })
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
                newServingSize = label.quantity
                newServingSizeUnit = label.unit
                newServingAmount = label.quantity
                newServingUnit = label.unit
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
    fun saveIngredient() {
        onSave(FoodItem(id = existing?.id ?: createId("custom"), kind = FoodKind.Ingredient,
            name = foodTitle(name), brand = brand.trim(), barcode = barcode.trim(),
            servingQuantity = servingQuantity.toDouble(), servingUnit = servingUnit.trim(),
            servingWeightGrams = weightGrams.toDoubleOrNull(), photoPath = photoPath,
            source = sourceFood?.source, sourceId = sourceFood?.sourceId, sourceVersion = sourceFood?.sourceVersion,
            servingOptions = servingOptions,
            nutrients = Nutrients(calories.toDouble(), protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0, org.json.JSONObject(additionalJson).let { obj -> obj.keys().asSequence().mapNotNull { key -> obj.optString(key).toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }?.let { key to it } }.toMap() })))
    }
    androidx.activity.compose.BackHandler { previous() }
    val validWeight = weightGrams.isBlank() || weightGrams.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    Page {
        PageHeader(if (existing != null) "Edit Food" else "Add Food", onBack = ::previous)
        FoodFlowProgress(step = step, onStepClick = { target -> if (target <= step) step = target })
        saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (step == 0) {
            com.philipcosgrave.calorietracker.ui.components.FoodPhotoPicker(photoPath) { photoPath = it }
            photoNames.forEach { candidate -> TextButton(onClick = { name = foodTitle(candidate); photoNames = emptyList() }) { Text(candidate) } }
            AppFormField(name, { name = it }, "Food Name", Modifier.fillMaxWidth())
            AppFormField(brand, { brand = it }, "Brand (Optional)", Modifier.fillMaxWidth())
            OutlinedTextField(
                value = barcode,
                onValueChange = { if (isDigitsOnlyInput(it)) barcode = it },
                label = { Text("Barcode / UPC (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    TextButton(onClick = { showBarcodeCamera = true }) { Text("▣", color = AppBlue, style = MaterialTheme.typography.titleLarge) }
                },
            )
            TextButton(enabled = barcode.length >= 4 && !isLookingUpBarcode, onClick = { onLookupBarcode(barcode) }, modifier = Modifier.align(Alignment.End)) { Text(if (isLookingUpBarcode) "Looking up…" else "Find barcode") }
            FoodFlowNavigation(onBack = ::previous, onNext = { step = 1 }, nextLabel = "Next", nextEnabled = name.isNotBlank() && !isSaving)
        }
        if (step == 2) {
            Text("How do you usually measure this food?", fontWeight = FontWeight.Bold)
            Text("Add familiar portions such as a can, scoop, slice, or cup. We use the equivalent amount to calculate nutrition.", color = AppMuted, style = MaterialTheme.typography.bodySmall)
            Text("Additional serving sizes", fontWeight = FontWeight.Bold)
            servingOptions.forEach { option ->
                AppCardContainer {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(option.description, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("= ${option.amount?.let { amount -> "${formatAmount(amount)} ${option.unit}" } ?: "${formatAmount(option.grams)} g"}", color = AppMuted)
                        TextButton(onClick = { servingOptions = servingOptions.filterNot { it.id == option.id } }) { Text("×") }
                    }
                }
            }
            AppCardContainer {
                Text("Add serving", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFormField(newServingSize, { if (isDecimalNumberInput(it)) newServingSize = it }, "Serving size", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    UnitPicker(newServingSizeUnit, { newServingSizeUnit = it }, Modifier.weight(1f), units = com.philipcosgrave.calorietracker.domain.servingUnits)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFormField(newServingAmount, { if (isDecimalNumberInput(it)) newServingAmount = it }, "Equals", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    UnitPicker(
                        newServingUnit,
                        { newServingUnit = it },
                        Modifier.weight(1f),
                        units = com.philipcosgrave.calorietracker.domain.compatibleMeasurementUnits(servingUnit),
                    )
                }
                val equivalentAmount = newServingAmount.toDoubleOrNull()
                val grams = equivalentAmount?.let { com.philipcosgrave.calorietracker.domain.convertAmount(it, newServingUnit, "g") }
                val baseEquivalent = equivalentAmount?.let { com.philipcosgrave.calorietracker.domain.convertAmount(it, newServingUnit, servingUnit) }
                if (newServingAmount.isNotBlank() && grams == null && baseEquivalent == null) Text("Choose a unit compatible with the nutrition-label unit, or enter an approximate weight.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = newServingSize.toDoubleOrNull()?.let { it > 0 } == true && newServingSizeUnit.isNotBlank() && equivalentAmount != null && (grams != null || baseEquivalent != null), onClick = {
                    servingOptions = servingOptions + ReferenceServing(createId("serving"), "${formatAmount(newServingSize.toDouble())} ${newServingSizeUnit.trim()}", grams ?: 0.0, equivalentAmount, newServingUnit)
                    newServingSize = "1"; newServingSizeUnit = "serving"; newServingAmount = ""; newServingUnit = servingUnit
                }) { Text("+ Add serving") }
            }
            FoodFlowNavigation(onBack = ::previous, onNext = { step = 3 }, nextLabel = "Next", nextEnabled = validWeight && servingQuantity.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true && servingUnit.isNotBlank())
        }
        if (step == 1) {
            AppPrimaryButton("Scan nutrition label", { showLabelCamera = true }, Modifier.fillMaxWidth(), enabled = !isSaving)
            Text("Enter the nutrition values exactly as they appear on the label.", color = AppMuted)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Per", fontWeight = FontWeight.Bold)
                AppFormField(servingQuantity, { if (isDecimalNumberInput(it)) servingQuantity = it }, "Amount", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                UnitPicker(servingUnit, { servingUnit = it }, Modifier.weight(1f), units = com.philipcosgrave.calorietracker.domain.nutritionMeasurementUnits)
            }
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
            FoodFlowNavigation(onBack = ::previous, onNext = {
                newServingUnit = com.philipcosgrave.calorietracker.domain.compatibleMeasurementUnits(servingUnit).firstOrNull() ?: servingUnit
                step = 2
            }, nextLabel = "Next", nextEnabled = !isSaving && !isLookingUpBarcode && !searchingNutrition && validIngredientFields(name, servingQuantity, servingUnit, calories, protein, carbs, fat, labelApplied))
        }
        if (step == 3) {
            AppCardContainer {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.philipcosgrave.calorietracker.ui.components.FoodPhoto(photoPath, Modifier.size(64.dp))
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text(foodTitle(name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (brand.isNotBlank()) Text(brand, color = AppMuted)
                        if (barcode.isNotBlank()) Text("UPC: $barcode", color = AppMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Nutrition (per ${formatAmount(servingQuantity.toDoubleOrNull() ?: 0.0)} $servingUnit)", fontWeight = FontWeight.Bold)
            Text("${formatAmount(calories.toDoubleOrNull() ?: 0.0)} kcal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReviewMacro("Protein", protein.toDoubleOrNull() ?: 0.0)
                ReviewMacro("Fat", fat.toDoubleOrNull() ?: 0.0)
                ReviewMacro("Carbs", carbs.toDoubleOrNull() ?: 0.0)
            }
            FoodFlowNavigation(onBack = ::previous, onNext = ::saveIngredient, nextLabel = if (isSaving) "Saving…" else "Save Food", nextEnabled = !isSaving && !isLookingUpBarcode && !searchingNutrition && validWeight && validIngredientFields(name, servingQuantity, servingUnit, calories, protein, carbs, fat, labelApplied))
        }
    }
}

@Composable
private fun ReviewMacro(label: String, value: Double) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${formatAmount(value)} g", fontWeight = FontWeight.Bold)
        Text(label, color = AppMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FoodFlowProgress(step: Int, onStepClick: (Int) -> Unit) {
    val labels = listOf("Basic", "Nutrition", "Serving", "Review")
    val completedColor = MaterialTheme.colorScheme.primary
    val pendingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Box(Modifier.fillMaxWidth().height(54.dp)) {
        Canvas(Modifier.fillMaxWidth().height(20.dp)) {
            val segment = size.width / labels.size
            val y = size.height / 2
            repeat(labels.lastIndex) { index ->
                drawLine(
                    color = if (index < step) completedColor else pendingColor,
                    start = Offset(segment * (index + .5f), y),
                    end = Offset(segment * (index + 1.5f), y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = index <= step) { onStepClick(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(if (index <= step) completedColor else pendingColor, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (index < step) "✓" else "${index + 1}",
                            color = if (index <= step) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(label, color = if (index <= step) MaterialTheme.colorScheme.onSurface else AppMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun FoodFlowNavigation(onBack: () -> Unit, onNext: () -> Unit, nextLabel: String, nextEnabled: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.material3.OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
        androidx.compose.material3.Button(onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f)) { Text(nextLabel) }
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
