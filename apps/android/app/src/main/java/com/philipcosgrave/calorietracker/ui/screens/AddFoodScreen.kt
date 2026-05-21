package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.SortMode
import com.philipcosgrave.calorietracker.ui.components.FoodSearchRow
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.SortMenu
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddFoodScreen(
    date: LocalDate,
    foods: List<FoodItem>,
    onBack: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onQuickCalories: () -> Unit,
    onScanBarcode: () -> Unit,
    onAddIngredient: () -> Unit,
    onAddRecipe: () -> Unit,
    onSelectFood: (FoodItem) -> Unit,
    onDeleteFood: (FoodItem) -> Unit,
    onEditFood: (FoodItem) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var activeKind by remember { mutableStateOf(FoodKind.Ingredient) }
    var sortMode by remember { mutableStateOf(SortMode.Recent) }
    val isSearching = search.isNotBlank() || barcode.isNotBlank()
    val results = foods
        .filter { isSearching || it.kind == activeKind }
        .filter { item ->
            search.isBlank() ||
                item.name.contains(search, ignoreCase = true) ||
                item.brand.contains(search, ignoreCase = true) ||
                item.components.any { it.item.name.contains(search, ignoreCase = true) }
        }
        .filter { barcode.isBlank() || it.barcode.contains(barcode) }
        .let { list ->
            when (sortMode) {
                SortMode.Recent -> list.sortedWith(compareBy<FoodItem> { it.lastUsedDaysAgo }.thenBy { it.name })
                SortMode.Frequency -> list.sortedWith(compareByDescending<FoodItem> { it.frequency }.thenBy { it.name })
                SortMode.Alphabetical -> list.sortedBy { it.name }
            }
        }

    Page {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Add Food", style = MaterialTheme.typography.headlineMedium)
                Text(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            }
            TextButton(onClick = onOpenSyncSettings) { Text("Sync") }
            TextButton(onClick = onBack) { Text("Back") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Search foods", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Button(onClick = onQuickCalories, modifier = Modifier.size(48.dp)) { Text("123") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onScanBarcode) { Text("Barcode") }
                }
                OutlinedTextField(search, { search = it }, label = { Text("Search ingredients or recipes") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodKind.entries.forEach { kind ->
                        FilterChip(
                            selected = activeKind == kind,
                            onClick = { activeKind = kind },
                            label = { Text(kind.name) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddIngredient) { Text("Add ingredient") }
                    Button(onClick = onAddRecipe) { Text("Add recipe") }
                    Spacer(modifier = Modifier.weight(1f))
                    SortMenu(sortMode, { sortMode = it })
                }
                if (results.isEmpty()) {
                    Text("No matching foods.")
                } else {
                    results.forEach { item ->
                        FoodSearchRow(
                            item = item,
                            showCalories = true,
                            onClick = { onSelectFood(item) },
                            onEdit = { onEditFood(item) },
                            onDelete = { onDeleteFood(item) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 1000)
@Composable
private fun AddFoodScreenPreview() {
    PreviewData.Theme {
        AddFoodScreen(
            date = PreviewData.date,
            foods = PreviewData.foods,
            onBack = {},
            onOpenSyncSettings = {},
            onQuickCalories = {},
            onScanBarcode = {},
            onAddIngredient = {},
            onAddRecipe = {},
            onSelectFood = {},
            onDeleteFood = {},
            onEditFood = {},
        )
    }
}
