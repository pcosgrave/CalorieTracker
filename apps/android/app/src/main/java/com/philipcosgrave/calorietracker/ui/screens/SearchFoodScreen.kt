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
import androidx.compose.runtime.LaunchedEffect
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
import com.philipcosgrave.calorietracker.domain.frequencyForMeal
import com.philipcosgrave.calorietracker.domain.inferMealForTime
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
import java.time.Instant
import java.time.LocalTime

@Composable
fun SearchFoodScreen(
    date: LocalDate,
    foods: List<FoodItem>,
    isSignedIn: Boolean,
    initialSearchQuery: String = "",
    voiceSearchNotice: String? = null,
    onBack: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onQuickCalories: () -> Unit,
    onScanBarcode: () -> Unit,
    onAddIngredient: () -> Unit,
    onAddRecipe: () -> Unit,
    onSelectFood: (FoodItem) -> Unit,
    onQuickLogFood: (FoodItem) -> Unit,
    personalOnlineResults: List<FoodItem>,
    communityResults: List<FoodItem>,
    canadianResults: List<FoodItem>,
    remoteSearchQuery: String,
    isSearchingRemote: Boolean,
    onSearchOnlineFoods: (String) -> Unit,
    onImportRemoteFood: (FoodItem) -> Unit,
    onDeleteFood: (FoodItem) -> Unit,
    onEditFood: (FoodItem) -> Unit,
    onPublishToCommunity: (FoodItem) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var activeKind by remember { mutableStateOf(FoodKind.Ingredient) }
    var sortMode by remember { mutableStateOf(SortMode.Recent) }
    var addMenuExpanded by remember { mutableStateOf(false) }
    val currentMeal = remember { inferMealForTime(LocalTime.now()) }
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
                SortMode.Recent -> list.sortedWith(
                    compareByDescending<FoodItem> {
                        it.lastUsedAt?.let(Instant::parse)?.toEpochMilli() ?: Long.MIN_VALUE
                    }.thenBy { it.lastUsedDaysAgo }.thenBy { it.name }
                )
                SortMode.MealTime -> list.sortedWith(
                    compareByDescending<FoodItem> { it.frequencyForMeal(currentMeal) }
                        .thenByDescending { it.frequency }
                        .thenByDescending { it.lastUsedAt?.let(Instant::parse)?.toEpochMilli() ?: Long.MIN_VALUE }
                        .thenBy { it.name }
                )
                SortMode.Frequency -> list.sortedWith(compareByDescending<FoodItem> { it.frequency }.thenBy { it.name })
                SortMode.Alphabetical -> list.sortedBy { it.name }
            }
        }
    val shouldShowRemoteSearch =
        search.trim().length >= 3 && activeKind == FoodKind.Ingredient && results.size < 2
    val showingRemoteResultsForCurrentSearch =
        remoteSearchQuery.equals(search.trim(), ignoreCase = true)

    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery.isNotBlank() && initialSearchQuery != search) {
            search = initialSearchQuery
        }
    }

    Page {
        PageHeader(
            title = "Search foods",
            onBack = onBack,
            actions = {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(AppBlue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { addMenuExpanded = true }) {
                        Text("+", color = Color.White, style = MaterialTheme.typography.titleLarge)
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

            if (!voiceSearchNotice.isNullOrBlank()) {
                Text(
                    voiceSearchNotice,
                    color = AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sortMode.label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                            TextButton(onClick = { onSearchOnlineFoods(search) }) {
                                Text(
                                    if (isSignedIn) "Search online foods"
                                    else "Sign in to search saved and community foods",
                                    color = AppBlue,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                if (isSignedIn) {
                                    "Search order: your cloud foods, then community foods, then the Canadian Nutrient File."
                                } else {
                                    "Sign in from Settings to search your saved foods and the community catalog. Scanning a barcode is still better when available."
                                },
                                color = AppMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    if (isSearchingRemote) {
                        Text("Searching online foods...", color = AppMuted)
                    } else if (showingRemoteResultsForCurrentSearch &&
                        (personalOnlineResults.isNotEmpty() || communityResults.isNotEmpty() || canadianResults.isNotEmpty())
                    ) {
                        if (personalOnlineResults.isNotEmpty()) {
                            Text("Your cloud foods", fontWeight = FontWeight.Bold, color = AppMuted)
                            personalOnlineResults.forEach { item ->
                                FoodSearchRow(
                                    item = item,
                                    showCalories = true,
                                    onClick = { onImportRemoteFood(item) },
                                    onDoubleClick = { onImportRemoteFood(item) },
                                )
                            }
                        }

                        if (communityResults.isNotEmpty()) {
                            Text("Community foods", fontWeight = FontWeight.Bold, color = AppMuted)
                            communityResults.forEach { item ->
                                FoodSearchRow(
                                    item = item,
                                    showCalories = true,
                                    onClick = { onImportRemoteFood(item) },
                                    onDoubleClick = { onImportRemoteFood(item) },
                                )
                            }
                        }

                        if (canadianResults.isNotEmpty()) {
                            Text("Canadian Nutrient File", fontWeight = FontWeight.Bold, color = AppMuted)
                            canadianResults.forEach { item ->
                                FoodSearchRow(
                                    item = item,
                                    showCalories = true,
                                    onClick = { onImportRemoteFood(item) },
                                    onDoubleClick = { onImportRemoteFood(item) },
                                )
                            }
                        }
                    } else {
                        Text(
                            if (showingRemoteResultsForCurrentSearch) {
                                "No online or Canadian Nutrient File matches found."
                            } else {
                                "No online food matches loaded yet."
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
private fun SearchFoodScreenPreview() {
    PreviewData.Theme {
        SearchFoodScreen(
            date = PreviewData.date,
            foods = PreviewData.foods,
            isSignedIn = true,
            initialSearchQuery = "onion",
            voiceSearchNotice = "Showing search results for what voice logging heard.",
            onBack = {},
            onOpenSyncSettings = {},
            onQuickCalories = {},
            onScanBarcode = {},
            onAddIngredient = {},
            onAddRecipe = {},
            onSelectFood = {},
            onQuickLogFood = {},
            personalOnlineResults = emptyList(),
            communityResults = emptyList(),
            canadianResults = emptyList(),
            remoteSearchQuery = "",
            isSearchingRemote = false,
            onSearchOnlineFoods = {},
            onImportRemoteFood = {},
            onDeleteFood = {},
            onEditFood = {},
            onPublishToCommunity = {},
        )
    }
}
