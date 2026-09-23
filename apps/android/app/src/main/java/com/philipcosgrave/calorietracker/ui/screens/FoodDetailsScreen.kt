package com.philipcosgrave.calorietracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.*
import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.ui.components.*

/** Reference view has no editable portion or meal state. Logging starts explicitly. */
@Composable
fun FoodDetailsScreen(food: FoodItem, onBack: () -> Unit, onEdit: () -> Unit,
    onLog: () -> Unit, primaryLabel: String = "Add to Meal", onEditIngredient: ((FoodItem) -> Unit)? = null,
    onDelete: (() -> Unit)? = null) {
    var section by rememberSaveable(food.id) { mutableStateOf("") }
    var ingredient by remember { mutableStateOf<FoodItem?>(null) }
    var cookStep by rememberSaveable(food.id) { mutableStateOf(-1) }
    var deleteConfirmationVisible by rememberSaveable(food.id) { mutableStateOf(false) }
    ingredient?.let { item ->
        BackHandler { ingredient = null }
        FoodDetailsScreen(item, { ingredient = null }, { onEditIngredient?.invoke(item) }, { ingredient = null }, "Back to Recipe", onEditIngredient)
        return
    }
    val recipe = food.kind == FoodKind.Recipe
    val recipeNutrients = if (recipe) {
        val totals = totalComponents(food.components)
        Nutrients(totals.calories, totals.protein, totals.carbs, totals.fat, componentAdditionalNutrients(food.components))
    } else food.nutrients
    val portion = if (recipe) recipeNutrients.scale(1.0 / food.servingQuantity.coerceAtLeast(0.1)) else recipeNutrients
    // Older CNF imports may have been saved before their raw measurement rows
    // were reduced to named portions. Keep the settings detail view consistent
    // with the editor without changing the user's stored food until they save it.
    val servingConversions = if (food.source?.contains("CNF", ignoreCase = true) == true ||
        food.source?.contains("Canadian Nutrient", ignoreCase = true) == true
    ) normalizedCnfServingOptions(food.servingOptions) else food.servingOptions
    BackHandler(enabled = section.isNotEmpty()) { if (cookStep >= 0) cookStep = -1 else section = "" }
    Page(spacing = 12.dp) {
        PageHeader(if (section.isNotEmpty()) section else if (recipe) "Recipe Details" else "Ingredient Details",
            onBack = { if (section.isNotEmpty()) { if (cookStep >= 0) cookStep = -1 else section = "" } else onBack() },
            actions = {
                if (section.isEmpty() && (primaryLabel != "Back to Recipe" || onEditIngredient != null)) {
                    IconButton(onClick = onEdit) { BiteWiseIcon("Edit", modifier = Modifier.size(22.dp)) }
                }
                if (section.isEmpty() && onDelete != null) {
                    IconButton(onClick = { deleteConfirmationVisible = true }) {
                        BiteWiseIcon("Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                    }
                }
            })
        when (section) {
            "Ingredients" -> food.components.forEach { component ->
                OutlinedCard(onClick = { ingredient = component.item }, Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp)) {
                        FoodPhoto(component.item.photoPath, Modifier.size(48.dp))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(component.item.name, fontWeight = FontWeight.Bold)
                            Text("${formatAmount(component.amount)} ${component.unit}")
                        }
                    }
                }
            }
            "Instructions" -> {
                if (food.instructions.isEmpty()) Text("No instructions added.")
                else if (cookStep >= 0) {
                    Text("Step ${cookStep + 1} of ${food.instructions.size}")
                    Text(food.instructions[cookStep], style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { cookStep-- }, enabled = cookStep > 0) { Text("Previous") }
                        Button(onClick = { if (cookStep < food.instructions.lastIndex) cookStep++ else cookStep = -1 }) { Text(if (cookStep < food.instructions.lastIndex) "Next Step" else "Done") }
                    }
                } else {
                    food.instructions.forEachIndexed { index, text -> AppCardContainer { Text("${index + 1}. $text") } }
                    AppPrimaryButton("Start cooking", { cookStep = 0 }, Modifier.fillMaxWidth())
                }
            }
            "Nutrition Details" -> AdditionalNutrition(portion)
            "Serving Conversions" -> {
                Text("Serving conversions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                servingConversions.forEach { option ->
                    AppCardContainer {
                        Text(option.description, fontWeight = FontWeight.Medium)
                        Text(
                            option.amount?.let { amount -> "= ${formatAmount(amount)} ${option.unit}" }
                                ?: "= ${formatAmount(option.grams)} g",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> {
                FoodPhoto(food.photoPath, Modifier.fillMaxWidth().height(180.dp))
                Text(food.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                food.source?.let { Text("Source: Health Canada / $it ${food.sourceVersion.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                if (food.brand.isNotBlank()) Text(food.brand)
                if (food.description.isNotBlank()) Text(food.description)
                food.servingWeightGrams?.let { Text("${food.servingLabel} = ${formatAmount(it)} g") }
                NutritionSummary(Totals(portion.calories, portion.proteinGrams, portion.carbohydrateGrams, portion.fatGrams))
                if (recipe) {
                    Text("Prep: ${food.prepMinutes?.let { "$it min" } ?: "Not specified"} · Total: ${food.totalMinutes?.let { "$it min" } ?: "Not specified"}")
                    OutlinedButton(onClick = { section = "Ingredients" }, Modifier.fillMaxWidth()) { Text("Ingredients ›") }
                    OutlinedButton(onClick = { section = "Instructions" }, Modifier.fillMaxWidth()) { Text("Instructions ›") }
                }
                OutlinedButton(onClick = { section = "Nutrition Details" }, modifier = Modifier.fillMaxWidth()) { Text("View full nutrition details ›") }
                if (food.barcode.isNotBlank()) Text("Barcode: ${food.barcode}")
                if (servingConversions.isNotEmpty()) {
                    OutlinedButton(onClick = { section = "Serving Conversions" }, modifier = Modifier.fillMaxWidth()) { Text("Serving conversions ›") }
                }
                AppPrimaryButton(primaryLabel, onLog, Modifier.fillMaxWidth())
            }
        }
    }
    if (deleteConfirmationVisible && onDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmationVisible = false },
            title = { Text("Delete ${food.name}?") },
            text = { Text("This will remove this ${if (recipe) "recipe" else "ingredient"} from your saved foods.") },
            confirmButton = {
                TextButton(onClick = { deleteConfirmationVisible = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmationVisible = false }) { Text("Cancel") } },
        )
    }
}
