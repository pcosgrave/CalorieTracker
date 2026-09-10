package com.philipcosgrave.calorietracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.philipcosgrave.calorietracker.data.local.*
import com.philipcosgrave.calorietracker.domain.*
import com.philipcosgrave.calorietracker.model.*
import kotlinx.coroutines.*

@Composable
fun ReferenceLookupResults(query: String, localFoods: List<FoodItem>, onSelect: (FoodItem) -> Unit,
    showLocal: Boolean = false, onEmpty: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val service = remember { IngredientSearch(SqliteReferenceFoodRepository(context), LocalFoodNameNormalizer()) }
    var results by remember { mutableStateOf<List<ReferenceFood>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val local = localFoods.filter { it.kind == FoodKind.Ingredient && FoodSearchMatching.score(query, it.name) > 0 }
        .sortedByDescending { FoodSearchMatching.score(query, it.name) }.take(15)
    LaunchedEffect(query, localFoods) {
        results = emptyList(); failed = false
        if (query.trim().length < 2) { searching = false; return@LaunchedEffect }
        searching = true
        delay(250)
        try { results = service.search(query, localFoods) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
        finally { searching = false }
    }
    if (query.trim().length >= 2) {
        if (showLocal && local.isNotEmpty()) {
            Text("Your foods")
            local.forEach { food -> TextButton(onClick = { onSelect(food) }) { Text(food.name) } }
        }
        if (searching) Text("Searching foods on this device…")
        if (results.isNotEmpty()) {
            Text("Health Canada · CNF 2026")
            results.forEach { ref ->
                OutlinedButton(onClick = { onSelect(ref.toLocalFood(ref.id)) }, Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) { Text(ref.name); Text("${formatNumber(ref.nutritionPer100g.calories)} kcal · 100 g") }
                }
            }
        }
        if (!searching && results.isEmpty() && local.isEmpty()) {
            Text(if (failed) "Reference search is unavailable. You can still add this food below." else "No matching food found. Try another name or choose how to add it.")
            onEmpty()
        }
    }
}
