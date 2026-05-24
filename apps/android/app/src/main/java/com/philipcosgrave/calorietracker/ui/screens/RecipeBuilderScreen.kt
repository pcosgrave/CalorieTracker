package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.philipcosgrave.calorietracker.ui.components.FoodSearchRow
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.RecipeComponentRow
import com.philipcosgrave.calorietracker.ui.components.UnitPicker
import com.philipcosgrave.calorietracker.ui.components.isDigitsOnlyInput
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
    val totals = totalComponents(draft.components)
    val draftServingUnits = remember(draft.servingUnit) {
        buildList {
            if (draft.servingUnit.isNotBlank()) add(draft.servingUnit)
            addAll(measurementUnits.filterNot { it in this })
        }
    }

    Page {
        PageHeader("Add Recipe", onBack = onBack)

        AppCardContainer {
            AppFormField(draft.name, { onDraftChange(draft.copy(name = it)) }, "Recipe Name", Modifier.fillMaxWidth())
            AppFormField(draft.brand, { onDraftChange(draft.copy(brand = it)) }, "Brand (Optional)", Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppFormField(
                    draft.servingQuantity,
                    {
                        if (isDigitsOnlyInput(it)) {
                            onDraftChange(draft.copy(servingQuantity = it))
                        }
                    },
                    "Servings",
                    Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                UnitPicker(draft.servingUnit, { onDraftChange(draft.copy(servingUnit = it)) }, Modifier.weight(1f), draftServingUnits)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F6FA), RoundedCornerShape(18.dp)),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("Nutrition", color = AppMuted)
                    Text(
                        "${formatNumber(totals.calories)} Cal • ${formatNumber(totals.protein)}g Protein",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            draft.components.forEachIndexed { index, component ->
                RecipeComponentRow(
                    component = component,
                    onChange = { updated ->
                        onDraftChange(draft.copy(components = draft.components.mapIndexed { i, c -> if (i == index) updated else c }))
                    },
                    onRemove = {
                        onDraftChange(draft.copy(components = draft.components.filterIndexed { i, _ -> i != index }))
                    },
                )
            }

            DashedAddCard("Tap + or Search to add items", onClick = onAddIngredient)
            DashedAddCard("Tap + or Search to add items", onClick = onStartNestedRecipe)

            AppFormField(search, { search = it }, "Search ingredients or recipes...", Modifier.fillMaxWidth())
            results.take(6).forEach { item ->
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

            AppPrimaryButton(
                text = "Save Recipe",
                onClick = { onSave(draft) },
                enabled = draft.name.isNotBlank() && draft.components.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DashedAddCard(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("+", style = MaterialTheme.typography.titleLarge)
            Text(label, color = AppMuted)
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
