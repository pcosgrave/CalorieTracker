package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.measurementUnits
import com.philipcosgrave.calorietracker.domain.totalComponents
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.RecipeDraft
import com.philipcosgrave.calorietracker.ui.components.FoodSearchRow
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.RecipeComponentRow
import com.philipcosgrave.calorietracker.ui.components.UnitPicker
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
) {
    var search by remember { mutableStateOf("") }
    val results = foods.filter { item ->
        search.isBlank() ||
            item.name.contains(search, ignoreCase = true) ||
            item.brand.contains(search, ignoreCase = true) ||
            item.components.any { it.item.name.contains(search, ignoreCase = true) }
    }
    val draftServingUnits = remember(draft.servingUnit) {
        buildList {
            if (draft.servingUnit.isNotBlank()) add(draft.servingUnit)
            addAll(measurementUnits.filterNot { it in this })
        }
    }
    val totals = totalComponents(draft.components)

    Page {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Add Recipe", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(draft.name, { onDraftChange(draft.copy(name = it)) }, label = { Text("Recipe name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.brand, { onDraftChange(draft.copy(brand = it)) }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(draft.servingQuantity, { onDraftChange(draft.copy(servingQuantity = it)) }, label = { Text("Serving size") }, modifier = Modifier.weight(1f))
                    UnitPicker(draft.servingUnit, { onDraftChange(draft.copy(servingUnit = it)) }, Modifier.weight(1f), draftServingUnits)
                }
                Text("Recipe items", style = MaterialTheme.typography.titleLarge)
                Text("${formatNumber(totals.calories)} cal - ${formatNumber(totals.protein)}g protein - ${formatNumber(totals.carbs)}g carbs - ${formatNumber(totals.fat)}g fat")
                if (draft.components.isEmpty()) {
                    Text("No items added yet.")
                } else {
                    draft.components.forEachIndexed { index, component ->
                        RecipeComponentRow(
                            component = component,
                            onChange = { updated ->
                                onDraftChange(draft.copy(components = draft.components.mapIndexed { i, c -> if (i == index) updated else c }))
                            },
                            onRemove = { onDraftChange(draft.copy(components = draft.components.filterIndexed { i, _ -> i != index })) },
                        )
                    }
                }
                Button(
                    onClick = { onSave(draft) },
                    enabled = draft.name.isNotBlank() && draft.components.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save recipe")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Search foods", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Button(onClick = onAddIngredient) { Text("Add ingredient") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onStartNestedRecipe) { Text("Add recipe") }
                }
                OutlinedTextField(search, { search = it }, label = { Text("Search ingredients or recipes") }, modifier = Modifier.fillMaxWidth())
                if (results.isEmpty()) {
                    Text("No matching foods.")
                } else {
                    results.forEach { item ->
                        FoodSearchRow(
                            item = item,
                            showCalories = false,
                            onClick = {
                                onDraftChange(
                                    draft.copy(
                                        components = draft.components + RecipeComponent(item, item.servingQuantity, item.servingUnit),
                                    ),
                                )
                                search = ""
                            },
                        )
                    }
                }
            }
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
