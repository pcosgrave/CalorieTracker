package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.foodTitle
import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.ui.components.*

@Composable
fun ManageFoodsScreen(foods: List<FoodItem>, onBack: () -> Unit, onAddFood: () -> Unit,
    onAddRecipe: () -> Unit, onEdit: (FoodItem) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var recipes by rememberSaveable { mutableStateOf(false) }
    Page {
        PageHeader("Foods & recipes", onBack = onBack)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onAddFood, modifier = Modifier.weight(1f)) { Text("Add food") }
            OutlinedButton(onClick = onAddRecipe, modifier = Modifier.weight(1f)) { Text("Add recipe") }
        }
        AppFormField(query, { query = it }, "Search known foods and recipes", Modifier.fillMaxWidth())
        AppSegmentedControl(listOf("Foods", "Recipes"), if (recipes) 1 else 0, { recipes = it == 1 })
        Text("Tap an item to view, edit, or delete it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        val results = foods.filter { (it.kind == FoodKind.Recipe) == recipes &&
            (it.name.contains(query, true) || it.brand.contains(query, true)) }.sortedBy { it.name.lowercase() }
        if (results.isEmpty()) Text("No ${if (recipes) "recipes" else "foods"} found.")
        results.forEach { food ->
            key(food.id) {
                FoodSearchRow(
                    item = food.copy(name = foodTitle(food.name)),
                    showCalories = true,
                    onClick = { onEdit(food) },
                )
            }
        }
    }
}
