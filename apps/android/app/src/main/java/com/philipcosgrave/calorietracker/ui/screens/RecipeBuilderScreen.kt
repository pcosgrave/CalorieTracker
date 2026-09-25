package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
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
    initialStep: Int = 0,
    onStepChange: (Int) -> Unit = {},
) {
    var scanning by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val step = initialStep
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
    androidx.activity.compose.BackHandler { if (step > 0) onStepChange(step - 1) else onBack() }
    Page {
        PageHeader("${if (draft.name.isBlank()) "Create" else "Edit"} Recipe", onBack = { if (step > 0) onStepChange(step - 1) else onBack() })
        RecipeFlowProgress(step) { if (it <= step) onStepChange(it) }
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
            RecipeFlowNavigation(onBack = onBack, onNext = { onStepChange(1) }, nextEnabled = draft.name.isNotBlank() && draft.servingQuantity.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true)
        } else if (step == 1) {
            com.philipcosgrave.calorietracker.ui.components.NutritionSummary(totals)
            Text("Nutrition for the whole recipe", color = AppMuted)
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
            RecipeFlowNavigation(onBack = { onStepChange(0) }, onNext = { onStepChange(2) }, nextEnabled = draft.components.isNotEmpty() && draft.unresolvedIngredients.isEmpty() && draft.components.all { it.amount.isFinite() && it.amount > 0 })
        } else if (step == 2) {
            Text("Instructions are optional.", color = AppMuted)
            AppFormField(draft.prepMinutes?.toString().orEmpty(), { value -> if (value.all(Char::isDigit)) onDraftChange(draft.copy(prepMinutes = value.toIntOrNull()?.takeIf { it > 0 })) }, "Prep time (minutes, optional)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            AppFormField(draft.totalMinutes?.toString().orEmpty(), { value -> if (value.all(Char::isDigit)) onDraftChange(draft.copy(totalMinutes = value.toIntOrNull()?.takeIf { it > 0 })) }, "Total time (minutes, optional)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            draft.instructions.forEachIndexed { index, instruction ->
                AppFormField(instruction, { value -> onDraftChange(draft.copy(instructions = draft.instructions.mapIndexed { i, old -> if (i == index) value else old })) }, "Step ${index + 1}", Modifier.fillMaxWidth())
                androidx.compose.material3.TextButton(onClick = { onDraftChange(draft.copy(instructions = draft.instructions.filterIndexed { i, _ -> i != index })) }) { Text("Remove step") }
            }
            androidx.compose.material3.OutlinedButton(onClick = { onDraftChange(draft.copy(instructions = draft.instructions + "")) }, Modifier.fillMaxWidth()) { Text("＋ Add step") }
            RecipeFlowNavigation(onBack = { onStepChange(1) }, onNext = { onStepChange(3) }, nextEnabled = true)
        } else {
            AppCardContainer {
                Text(draft.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (draft.description.isNotBlank()) Text(draft.description, color = AppMuted)
                Text("${draft.servingQuantity} servings", color = AppMuted)
            }
            com.philipcosgrave.calorietracker.ui.components.NutritionSummary(totals)
            Text("${draft.components.size} ingredients · ${draft.instructions.count { it.isNotBlank() }} steps", color = AppMuted)
            RecipeFlowNavigation(onBack = { onStepChange(2) }, onNext = { onSave(draft.copy(instructions = draft.instructions.map(String::trim).filter(String::isNotBlank))) }, nextLabel = if (isSaving) "Saving…" else "Save Recipe", nextEnabled = !isSaving)
        }
    }
}

@Composable
private fun RecipeFlowProgress(step: Int, onClick: (Int) -> Unit) {
    val labels = listOf("Basic", "Ingredients", "Instructions", "Review")
    val activeLineColor = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().size(1.dp, 20.dp)) {
            val segment = size.width / labels.size
            val y = size.height / 2
            repeat(labels.lastIndex) { index ->
                drawLine(if (index < step) activeLineColor else AppBorder, Offset(segment * (index + .5f), y), Offset(segment * (index + 1.5f), y), 2.dp.toPx(), StrokeCap.Round)
            }
        }
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Column(
                modifier = Modifier.weight(1f).clickable(enabled = index <= step) { onClick(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(20.dp).background(if (index <= step) MaterialTheme.colorScheme.primary else AppBorder, CircleShape), contentAlignment = Alignment.Center) {
                    Text(if (index < step) "✓" else "${index + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (index <= step) MaterialTheme.colorScheme.onSurface else AppMuted, textAlign = TextAlign.Center)
            }
        }
    }
    }
}

@Composable
private fun RecipeFlowNavigation(onBack: () -> Unit, onNext: () -> Unit, nextLabel: String = "Next", nextEnabled: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.material3.OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
        androidx.compose.material3.Button(onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f)) { Text(nextLabel) }
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
