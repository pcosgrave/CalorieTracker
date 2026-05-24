package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.SortMode
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppBorder
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppFormField
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.AppSegmentedControl
import com.philipcosgrave.calorietracker.ui.components.FoodSearchRow
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.SortMenu
import com.philipcosgrave.calorietracker.ui.components.appBorderColor
import com.philipcosgrave.calorietracker.ui.components.appCardColor
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import java.time.LocalDate

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
    onQuickLogFood: (FoodItem) -> Unit,
    remoteSearchResults: List<FoodItem>,
    remoteSearchQuery: String,
    isSearchingRemote: Boolean,
    onSearchOpenFoodFacts: (String) -> Unit,
    onImportRemoteFood: (FoodItem) -> Unit,
    onDeleteFood: (FoodItem) -> Unit,
    onEditFood: (FoodItem) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var activeKind by remember { mutableStateOf(FoodKind.Ingredient) }
    var sortMode by remember { mutableStateOf(SortMode.Recent) }
    var addMenuExpanded by remember { mutableStateOf(false) }
    val results = foods
        .filter { it.kind == activeKind }
        .filter { item ->
            search.isBlank() ||
                item.name.contains(search, ignoreCase = true) ||
                item.brand.contains(search, ignoreCase = true) ||
                item.components.any { it.item.name.contains(search, ignoreCase = true) }
        }
        .let { list ->
            when (sortMode) {
                SortMode.Recent -> list.sortedWith(compareBy<FoodItem> { it.lastUsedDaysAgo }.thenBy { it.name })
                SortMode.Frequency -> list.sortedWith(compareByDescending<FoodItem> { it.frequency }.thenBy { it.name })
                SortMode.Alphabetical -> list.sortedBy { it.name }
            }
        }
    val shouldShowRemoteSearch =
        search.trim().length >= 3 && activeKind == FoodKind.Ingredient && results.size < 2
    val showingRemoteResultsForCurrentSearch =
        remoteSearchQuery.equals(search.trim(), ignoreCase = true)

    Page {
        PageHeader(
            title = "Search foods",
            onBack = onBack,
            actions = {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(appCardColor(), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { addMenuExpanded = true }) {
                        Text("+", color = AppBlue, style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(expanded = addMenuExpanded, onDismissRequest = { addMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Quick add calories") },
                            onClick = {
                                addMenuExpanded = false
                                onQuickCalories()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Add ingredient") },
                            onClick = {
                                addMenuExpanded = false
                                onAddIngredient()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Add recipe") },
                            onClick = {
                                addMenuExpanded = false
                                onAddRecipe()
                            },
                        )
                    }
                }
            },
        )

        AppCardContainer {
            Box(modifier = Modifier.fillMaxWidth()) {
                AppFormField(
                    value = search,
                    onValueChange = { search = it },
                    label = "Search ingredients or recipes...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                )
                FloatingActionButton(
                    onClick = onScanBarcode,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 8.dp, end = 12.dp)
                        .size(44.dp),
                    containerColor = AppBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        "Scan",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                SortMenu(value = sortMode, onChange = { sortMode = it })
            }

            AppSegmentedControl(
                options = listOf("Ingredient", "Recipe"),
                selectedIndex = if (activeKind == FoodKind.Ingredient) 0 else 1,
                onSelectedIndexChange = { activeKind = if (it == 0) FoodKind.Ingredient else FoodKind.Recipe },
            )

            if (results.isEmpty()) {
                Text("No matching foods.", color = AppMuted)
            } else {
                results.forEach { item ->
                    FoodSearchRow(
                        item = item,
                        showCalories = true,
                        onClick = { onSelectFood(item) },
                        onDoubleClick = { onQuickLogFood(item) },
                        onEdit = { onEditFood(item) },
                        onDelete = { onDeleteFood(item) },
                    )
                }
            }

            if (shouldShowRemoteSearch) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(appCardColor(), RoundedCornerShape(18.dp))
                            .border(1.dp, appBorderColor(), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { onSearchOpenFoodFacts(search) }) {
                                Text("Search Open Food Facts", color = AppBlue, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Packaged foods scan better when a barcode is available.",
                                color = AppMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    if (isSearchingRemote) {
                        Text("Searching Open Food Facts...", color = AppMuted)
                    } else if (showingRemoteResultsForCurrentSearch && remoteSearchResults.isNotEmpty()) {
                        Text("Open Food Facts", fontWeight = FontWeight.Bold, color = AppMuted)
                        remoteSearchResults.forEach { item ->
                            FoodSearchRow(
                                item = item,
                                showCalories = true,
                                onClick = { onImportRemoteFood(item) },
                                onDoubleClick = { onImportRemoteFood(item) },
                            )
                        }
                    } else {
                        Text(
                            if (showingRemoteResultsForCurrentSearch) {
                                "No Open Food Facts matches found."
                            } else {
                                "No Open Food Facts matches loaded yet."
                            },
                            color = AppMuted,
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
            onQuickLogFood = {},
            remoteSearchResults = emptyList(),
            remoteSearchQuery = "",
            isSearchingRemote = false,
            onSearchOpenFoodFacts = {},
            onImportRemoteFood = {},
            onDeleteFood = {},
            onEditFood = {},
        )
    }
}
