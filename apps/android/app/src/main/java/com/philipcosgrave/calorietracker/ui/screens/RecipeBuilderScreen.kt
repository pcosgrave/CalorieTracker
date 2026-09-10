package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.measurementUnits
import com.philipcosgrave.calorietracker.domain.totalComponents
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.RecipeDraft
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppBorder
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppFormField
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.AppPrimaryButton
import com.philipcosgrave.calorietracker.ui.components.SectionDivider
import com.philipcosgrave.calorietracker.ui.components.FoodSearchRow
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.RecipeComponentRow
import com.philipcosgrave.calorietracker.ui.components.UnitPicker
import com.philipcosgrave.calorietracker.ui.components.appSoftColor
import com.philipcosgrave.calorietracker.ui.components.isDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.components.isDigitsOnlyInput
import com.philipcosgrave.calorietracker.ui.components.normalizeDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData

@Composable
fun RecipeBuilderScreen(
    draft: RecipeDraft,
    foods: List<FoodItem>,
    onDraftChange: (RecipeDraft) -> Unit,
    onBack: () -> Unit,
    onAddIngredient: () -> Unit,
    onStartNestedRecipe: () -> Unit,
    onSave: (RecipeDraft) -> Unit,
    onSaveIngredient: suspend (FoodItem) -> Unit = {},
    isSaving: Boolean = false,
    saveError: String? = null,
) {
    var scanning by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var step by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    var unresolvedIndex by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Int?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var resolveError by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    if (scanning) {
        RecipeScanScreen(onBack = { scanning = false }, onExtracted = { text, image ->
            onDraftChange(com.philipcosgrave.calorietracker.domain.parseRecipeText(text, foods).copy(photoPath = image)); scanning = false
        })
        return
    }
    val unresolved = unresolvedIndex?.let { draft.unresolvedIngredients.getOrNull(it) }
    if (unresolved != null) {
        val parsed = com.philipcosgrave.calorietracker.domain.parseRecipeIngredient(unresolved)
        AddIngredientScreen(knownFoods = foods, existing = null, initialName = parsed.name, onBack = { unresolvedIndex = null }, isSaving = resolving, saveError = resolveError,
            onSave = { food -> resolving = true; scope.launch {
                try {
                    onSaveIngredient(food)
                    val component = com.philipcosgrave.calorietracker.domain.recipeComponentFromLine(unresolved, food)
                        ?: RecipeComponent(food, 0.0, food.servingUnit)
                    onDraftChange(draft.copy(components = draft.components + component, unresolvedIngredients = draft.unresolvedIngredients.filterIndexed { index, _ -> index != unresolvedIndex }))
                    unresolvedIndex = null; resolveError = null
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { resolveError = "Could not save ingredient. Please retry." }
                finally { resolving = false }
            } })
        return
    }
    val totals = totalComponents(draft.components)
    androidx.activity.compose.BackHandler { if (step > 0) step-- else onBack() }
    Page {
        PageHeader(if (step == 0) "Review Recipe" else "Add Instructions", onBack = { if (step > 0) step-- else onBack() })
        saveError?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        if (step == 0) {
            if (draft.name.isBlank() && draft.components.isEmpty()) {
                AppPrimaryButton("Create from a photo", { scanning = true }, Modifier.fillMaxWidth())
                Text("Or create manually below", color = AppMuted)
            }
            com.philipcosgrave.calorietracker.ui.components.FoodPhotoPicker(draft.photoPath) { onDraftChange(draft.copy(photoPath = it)) }
            AppFormField(draft.name, { onDraftChange(draft.copy(name = it)) }, "Recipe name", Modifier.fillMaxWidth())
            AppFormField(draft.description, { onDraftChange(draft.copy(description = it)) }, "Description (optional)", Modifier.fillMaxWidth())
            AppFormField(draft.servingQuantity, { if (isDecimalNumberInput(it)) onDraftChange(draft.copy(servingQuantity = it)) }, "Servings", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            AppFormField(draft.prepMinutes?.toString().orEmpty(), { value -> if (value.all(Char::isDigit)) onDraftChange(draft.copy(prepMinutes = value.toIntOrNull()?.takeIf { it > 0 })) }, "Prep time (minutes, optional)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            AppFormField(draft.totalMinutes?.toString().orEmpty(), { value -> if (value.all(Char::isDigit)) onDraftChange(draft.copy(totalMinutes = value.toIntOrNull()?.takeIf { it > 0 })) }, "Total time (minutes, optional)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("Ingredients", fontWeight = FontWeight.Bold)
            if (draft.components.any { it.amount <= 0 }) Text("Enter the amount for ingredients shown as zero.", color = MaterialTheme.colorScheme.error)
            AppPrimaryButton("＋ Add Ingredient", onAddIngredient, Modifier.fillMaxWidth())
            draft.components.forEachIndexed { index, component ->
                RecipeComponentRow(component = component,
                    onChange = { updated -> onDraftChange(draft.copy(components = draft.components.mapIndexed { i, c -> if (i == index) updated else c })) },
                    onRemove = { onDraftChange(draft.copy(components = draft.components.filterIndexed { i, _ -> i != index })) })
            }
            draft.unresolvedIngredients.forEachIndexed { index, line ->
                AppCardContainer {
                    Text(line)
                    Text("Choose a known food or add its nutrition before saving.", color = AppMuted)
                    foods.filter { it.name.contains(com.philipcosgrave.calorietracker.domain.parseRecipeIngredient(line).name, true) }.take(3).forEach { food ->
                        androidx.compose.material3.TextButton(onClick = {
                            val component = com.philipcosgrave.calorietracker.domain.recipeComponentFromLine(line, food) ?: RecipeComponent(food, 0.0, food.servingUnit)
                            onDraftChange(draft.copy(components = draft.components + component, unresolvedIngredients = draft.unresolvedIngredients.filterIndexed { i, _ -> i != index }))
                        }) { Text("Use ${food.name} · review amount") }
                    }
                    Row {
                        androidx.compose.material3.TextButton(onClick = { unresolvedIndex = index }) { Text("Add food") }
                        androidx.compose.material3.TextButton(onClick = { onDraftChange(draft.copy(unresolvedIngredients = draft.unresolvedIngredients.filterIndexed { i, _ -> i != index })) }) { Text("Remove") }
                    }
                }
            }
            com.philipcosgrave.calorietracker.ui.components.NutritionSummary(totals)
            Text("Nutrition for the whole recipe", color = AppMuted)
            AppPrimaryButton("Next", { step = 1 }, Modifier.fillMaxWidth(), enabled = draft.name.isNotBlank() && draft.components.isNotEmpty() && draft.unresolvedIngredients.isEmpty() && draft.components.all { it.amount.isFinite() && it.amount > 0 } && draft.servingQuantity.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true)
        } else {
            Text("Instructions are optional.", color = AppMuted)
            draft.instructions.forEachIndexed { index, instruction ->
                AppFormField(instruction, { value -> onDraftChange(draft.copy(instructions = draft.instructions.mapIndexed { i, old -> if (i == index) value else old })) }, "Step ${index + 1}", Modifier.fillMaxWidth())
                androidx.compose.material3.TextButton(onClick = { onDraftChange(draft.copy(instructions = draft.instructions.filterIndexed { i, _ -> i != index })) }) { Text("Remove step") }
            }
            androidx.compose.material3.OutlinedButton(onClick = { onDraftChange(draft.copy(instructions = draft.instructions + "")) }, Modifier.fillMaxWidth()) { Text("＋ Add step") }
            AppPrimaryButton(if (isSaving) "Saving…" else "Save Recipe", { onSave(draft.copy(instructions = draft.instructions.map(String::trim).filter(String::isNotBlank))) }, Modifier.fillMaxWidth(), enabled = !isSaving)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 1100)
@Composable
private fun RecipeBuilderScreenPreview() {
    PreviewData.Theme {
        RecipeBuilderScreen(
            draft = PreviewData.recipeDraft,
            foods = PreviewData.foods,
            onDraftChange = {},
            onBack = {},
            onAddIngredient = {},
            onStartNestedRecipe = {},
            onSave = {},
        )
    }
}
