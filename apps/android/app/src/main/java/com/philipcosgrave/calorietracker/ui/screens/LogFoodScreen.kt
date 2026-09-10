package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.philipcosgrave.calorietracker.domain.amountInBaseUnits
import com.philipcosgrave.calorietracker.domain.amountInGrams
import com.philipcosgrave.calorietracker.domain.amountUnits
import com.philipcosgrave.calorietracker.domain.formatAmount
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.compatibleMeasurementUnits
import com.philipcosgrave.calorietracker.domain.createId
import com.philipcosgrave.calorietracker.domain.convertAmount
import com.philipcosgrave.calorietracker.domain.scale
import com.philipcosgrave.calorietracker.domain.withAdjustedComponents
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.DatePillsRow
import com.philipcosgrave.calorietracker.ui.components.MealPicker
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.UnitPicker
import com.philipcosgrave.calorietracker.ui.components.isDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.components.normalizeDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import java.time.LocalDate

@Composable
fun LogFoodScreen(food: FoodItem, date: LocalDate, existingEntry: DiaryEntry? = null,
    onBack: () -> Unit, onLog: suspend (DiaryEntry, Boolean) -> Unit,
    onEditNutrition: () -> Unit = {}, destinationMeal: Meal? = null,
    recipeDestination: Boolean = false, wholePortionOnly: Boolean = false, subtitle: String? = null,
) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val recipe = food.kind == com.philipcosgrave.calorietracker.model.FoodKind.Recipe
    var amount by rememberSaveable(food.id, existingEntry?.id) { mutableStateOf((existingEntry?.loggedAmount ?: if (recipe && !wholePortionOnly) 1.0 else food.servingQuantity).toString()) }
    var unit by rememberSaveable(food.id, existingEntry?.id) { mutableStateOf(existingEntry?.loggedUnit ?: food.servingUnit) }
    var mealName by rememberSaveable(food.id, existingEntry?.id) { mutableStateOf((existingEntry?.meal ?: destinationMeal ?: com.philipcosgrave.calorietracker.domain.inferMealForTime(java.time.LocalTime.now())).name) }
    var dateText by rememberSaveable(food.id, existingEntry?.id) { mutableStateOf((existingEntry?.date ?: date).toString()) }
    val meal = Meal.valueOf(mealName)
    var components by remember(food.id) { mutableStateOf(food.components) }
    var instructionsVisible by rememberSaveable { mutableStateOf(false) }
    var cookStep by rememberSaveable { mutableStateOf(-1) }
    var nutritionExpanded by rememberSaveable { mutableStateOf(false) }
    val number = amount.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    val adjustedFood = if (wholePortionOnly || components.isEmpty()) food else food.withAdjustedComponents(components)
    val converted = number?.let { adjustedFood.amountInBaseUnits(it, unit) }
    val multiplier = (converted ?: 0.0) / food.servingQuantity
    val nutrients = adjustedFood.nutrients.scale(multiplier)
    val totals = com.philipcosgrave.calorietracker.model.Totals(nutrients.calories, nutrients.proteinGrams, nutrients.carbohydrateGrams, nutrients.fatGrams)
    if (instructionsVisible) {
        androidx.activity.compose.BackHandler { if (cookStep >= 0) cookStep = -1 else instructionsVisible = false }
        com.philipcosgrave.calorietracker.ui.components.Page(spacing = 8.dp) {
            PageHeader(if (cookStep >= 0) "Cook Mode" else "Instructions", onBack = { if (cookStep >= 0) cookStep = -1 else instructionsVisible = false })
            if (cookStep >= 0) {
                Text("Step ${cookStep + 1} of ${food.instructions.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.philipcosgrave.calorietracker.ui.components.FoodPhoto(food.photoPath, Modifier.fillMaxWidth().height(200.dp))
                Text(food.instructions[cookStep], style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(enabled = cookStep > 0, onClick = { cookStep-- }) { Text("Previous") }
                    Button(onClick = { if (cookStep < food.instructions.lastIndex) cookStep++ else { cookStep = -1; instructionsVisible = false } }) { Text(if (cookStep < food.instructions.lastIndex) "Next Step" else "Done") }
                }
            } else {
                food.instructions.forEachIndexed { index, step -> com.philipcosgrave.calorietracker.ui.components.AppCardContainer { Text("${index + 1}. $step") } }
                Button(onClick = { cookStep = 0 }, Modifier.fillMaxWidth()) { Text("Start cooking") }
            }
        }
        return
    }
    com.philipcosgrave.calorietracker.ui.components.Page(spacing = 8.dp) {
        PageHeader(if (existingEntry != null) "Edit Food" else if (recipe) "Add Recipe" else "Food Details", onBack = onBack)
        com.philipcosgrave.calorietracker.ui.components.FoodPhoto(food.photoPath, Modifier.fillMaxWidth().height(120.dp))
        Text(com.philipcosgrave.calorietracker.domain.foodTitle(food.name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (food.description.isNotBlank()) Text(food.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        com.philipcosgrave.calorietracker.ui.components.NutritionSummary(totals)
        if (!recipeDestination) {
            Text("Add to Meal", fontWeight = FontWeight.Bold)
            MealPicker(meal, { mealName = it.name })
        }
        if (!wholePortionOnly) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (recipe) "Servings" else "Amount", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                if (!recipe && food.servingWeightGrams != null && food.servingUnit != "g") TextButton(onClick = {
                    val current = number ?: return@TextButton
                    val next = if (unit == "g") food.amountInBaseUnits(current, unit) else food.amountInGrams(current, unit)
                    if (next != null) { amount = next.toString(); unit = if (unit == "g") food.servingUnit else "g" }
                }) { Text(if (unit == "g") "⇄ Switch to servings" else "⇄ Switch to grams") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), modifier = Modifier.width(40.dp), onClick = { amount = ((number ?: 1.0) - 1.0).coerceAtLeast(.1).toString() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) { Text("−") }
                OutlinedTextField(amount, { if (isDecimalNumberInput(it)) amount = it }, Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedButton(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), modifier = Modifier.width(40.dp), onClick = { amount = ((number ?: 0.0) + 1).toString() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) { Text("+") }
                UnitPicker(unit, { newUnit ->
                    val current = number ?: return@UnitPicker
                    val base = food.amountInBaseUnits(current, unit)
                    val serving = food.servingOptions.firstOrNull { it.description == newUnit }
                    val next = if (serving != null) food.amountInGrams(current, unit)?.div(serving.grams) else if (newUnit == "g") food.amountInGrams(current, unit) else base?.let { convertAmount(it, food.servingUnit, newUnit) }
                    if (next != null) { amount = next.toString(); unit = newUnit }
                }, Modifier.weight(1f), food.amountUnits())
            }
            if (!recipe) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1.0, 2.0, 4.0).forEach { count ->
                    val baseAmount = food.servingQuantity * count
                    OutlinedButton(onClick = { unit = food.servingUnit; amount = baseAmount.toString() }, modifier = Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${formatNumber(baseAmount)} ${food.servingUnit}", style = MaterialTheme.typography.labelSmall)
                            Text("${formatNumber(food.nutrients.calories * count)} kcal", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            food.amountInGrams(number ?: 0.0, unit)?.let { grams -> Text("${formatNumber(grams)} g", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else Text("Use entire leftover", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (components.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ingredients", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                if (!wholePortionOnly) TextButton(onClick = { components = food.components }) { Text("Reset") }
            }
            components.forEachIndexed { index, component ->
                if (wholePortionOnly) Row(Modifier.fillMaxWidth()) { Text(component.item.name, Modifier.weight(1f)); Text("${formatAmount(component.amount)} ${component.unit}") }
                else EditableRecipeLogComponent(component.copy(amount = component.amount * multiplier), onAmountChange = { text ->
                    text.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && multiplier > 0 }?.let { value -> components = components.mapIndexed { i, c -> if (i == index) c.copy(amount = value / multiplier) else c } }
                }, onUnitChange = { newUnit ->
                    val next = convertAmount(component.amount, component.unit, newUnit)
                    if (next != null) components = components.mapIndexed { i, c -> if (i == index) c.copy(amount = next, unit = newUnit) else c }
                })
            }
        }
        if (food.instructions.isNotEmpty()) OutlinedButton(onClick = { instructionsVisible = true }, modifier = Modifier.fillMaxWidth()) { Text("Instructions ›") }
        TextButton(onClick = { nutritionExpanded = !nutritionExpanded }) { Text(if (nutritionExpanded) "Hide nutrition details" else "View full nutrition details ⌄") }
        if (nutritionExpanded) com.philipcosgrave.calorietracker.ui.components.AdditionalNutrition(nutrients)
        if (!recipeDestination) DatePillsRow(LocalDate.parse(dateText), LocalDate.now(), { dateText = it.toString() })
        if (!recipe && !wholePortionOnly) TextButton(onClick = onEditNutrition) { Text("Edit food / scan nutrition label") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !saving && converted != null && converted > 0, onClick = { saving = true; scope.launch {
            try { onLog(DiaryEntry(existingEntry?.id ?: createId("entry"), adjustedFood, LocalDate.parse(dateText), meal, multiplier, number!!, unit), false) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "Could not save food. Please retry." }
            finally { saving = false }
        } }, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Saving…" else if (existingEntry != null) "Save changes" else if (recipeDestination) "Add ingredient" else "Add to ${meal.label}") }
    }
}

@Composable
private fun EditableRecipeLogComponent(
    component: RecipeComponent,
    onAmountChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
) {
    var amountText by remember(component.item.id, component.amount, component.unit) { mutableStateOf(formatAmount(component.amount)) }
    val unitOptions = remember(component.unit, component.item.servingUnit) {
        buildList {
            if (component.unit.isNotBlank()) add(component.unit)
            addAll(compatibleMeasurementUnits(component.item.servingUnit).filterNot { it in this })
            if (component.item.servingUnit.isNotBlank() && component.item.servingUnit !in this) add(component.item.servingUnit)
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
                val normalized = normalizeDecimalNumberInput(it)
                if (isDecimalNumberInput(normalized)) {
                    amountText = normalized
                    onAmountChange(normalized)
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(84.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            existingEntry = null,
            onBack = {},
            onLog = { _, _ -> },
        )
    }
}
