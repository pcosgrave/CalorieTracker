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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch
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
    onOpenLeftovers: () -> Unit = {},
    leftovers: List<com.philipcosgrave.calorietracker.domain.Leftover> = emptyList(),
    initialLeftovers: Boolean = false,
    allowLeftovers: Boolean = true,
    destinationMeal: com.philipcosgrave.calorietracker.model.Meal? = null,
    onUseLeftover: suspend (com.philipcosgrave.calorietracker.domain.Leftover, LocalDate, com.philipcosgrave.calorietracker.model.Meal) -> Unit = { _, _, _ -> },
    localOnly: Boolean = true,
    onSelectFood: (FoodItem) -> Unit,
    onImportReference: suspend (FoodItem) -> FoodItem = { it },
    onPhoto: () -> Unit = {},
    onVoice: () -> Unit = {},
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
    var reference by remember { mutableStateOf<FoodItem?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    val referenceScope = androidx.compose.runtime.rememberCoroutineScope()
    reference?.let { food ->
        androidx.activity.compose.BackHandler { reference = null }
        FoodDetailsScreen(food, { reference = null }, { referenceScope.launch {
            if (!importing) { importing = true; try { onEditFood(onImportReference(food)); reference = null }
            catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { importError = "Could not save ingredient. Retry." } finally { importing = false } }
        } }, { referenceScope.launch {
            if (!importing) { importing = true; try { onSelectFood(onImportReference(food)); reference = null }
            catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { importError = "Could not save ingredient. Retry." } finally { importing = false } }
        } }, if (importing) "Saving…" else "Add Ingredient")
        importError?.let { message -> AlertDialog(onDismissRequest = { importError = null }, text = { Text(message) }, confirmButton = { TextButton(onClick = { importError = null }) { Text("OK") } }) }
        return
    }
    var showingLeftovers by rememberSaveable { mutableStateOf(initialLeftovers) }
    var selectedLeftoverId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedLeftover = leftovers.firstOrNull { it.id == selectedLeftoverId }
    if (selectedLeftover != null) {
        androidx.activity.compose.BackHandler { selectedLeftoverId = null }
        LogFoodScreen(food = com.philipcosgrave.calorietracker.domain.leftoverAsFood(selectedLeftover), date = date,
            destinationMeal = destinationMeal, wholePortionOnly = true, subtitle = "Saved ${selectedLeftover.date}",
            onBack = { selectedLeftoverId = null },
            onLog = { entry, _ -> onUseLeftover(selectedLeftover, entry.date, entry.meal); selectedLeftoverId = null })
        return
    }
    var search by rememberSaveable { mutableStateOf("") }
    var activeKind by rememberSaveable { mutableStateOf(FoodKind.Ingredient) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.Recent) }
    var addMenuExpanded by remember { mutableStateOf(false) }
    var expandedFoodId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteFood by remember { mutableStateOf<FoodItem?>(null) }
    val currentMeal = remember { inferMealForTime(LocalTime.now()) }
    val sortModeLabel = when (sortMode) {
        SortMode.MealTime -> "Frequent ${currentMeal.label}"
        else -> sortMode.label
    }
    val results = foods
        .filter { it.kind == activeKind }
        .filter { item ->
            search.isBlank() ||
                com.philipcosgrave.calorietracker.domain.FoodSearchMatching.score(search, item.name) > 0 ||
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
        !localOnly && search.trim().length >= 3 && activeKind == FoodKind.Ingredient && results.size < 2
    val showingRemoteResultsForCurrentSearch =
        remoteSearchQuery.equals(search.trim(), ignoreCase = true)

    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery.isNotBlank() && initialSearchQuery != search) {
            search = initialSearchQuery
        }
    }

    LaunchedEffect(search, activeKind, sortMode) {
        expandedFoodId = null
    }

    Page {
        PageHeader("Search Foods", onBack = onBack)

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
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { sortExpanded = true }) { Text(if (sortMode == SortMode.Frequency) "Frequent ⌄" else "Recent ⌄") }
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        DropdownMenuItem(text = { Text("Recent") }, onClick = { sortMode = SortMode.Recent; sortExpanded = false })
                        DropdownMenuItem(text = { Text("Frequent") }, onClick = { sortMode = SortMode.Frequency; sortExpanded = false })
                        // TODO: Enable when favorite-food storage is available.
                        DropdownMenuItem(text = { Text("Favorites · coming later") }, onClick = {}, enabled = false)
                    }
                }
            }

            AppSegmentedControl(
                options = if (allowLeftovers) listOf("Ingredient", "Recipe", "Leftovers") else listOf("Ingredient", "Recipe"),
                selectedIndex = if (showingLeftovers) 2 else if (activeKind == FoodKind.Ingredient) 0 else 1,
                onSelectedIndexChange = {
                    showingLeftovers = it == 2
                    if (it != 2) activeKind = if (it == 0) FoodKind.Ingredient else FoodKind.Recipe
                },
            )

            if (showingLeftovers) {
                val matching = leftovers.filter { search.isBlank() || it.name.contains(search, true) || it.entries.any { entry -> entry.food.name.contains(search, true) } }.sortedByDescending { it.date }
                if (matching.isEmpty()) Text("No saved leftovers.", color = AppMuted)
                matching.forEach { leftover ->
                    androidx.compose.material3.OutlinedCard(onClick = { selectedLeftoverId = leftover.id }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(leftover.name, fontWeight = FontWeight.Bold); Text(leftover.date.toString(), color = AppMuted, style = MaterialTheme.typography.bodySmall) }
                            Text("›")
                        }
                    }
                }
            } else {
                if (allowLeftovers || activeKind == FoodKind.Ingredient) TextButton(onClick = if (activeKind == FoodKind.Ingredient) onAddIngredient else onAddRecipe) {
                    Text(if (activeKind == FoodKind.Ingredient) "Create ingredient" else "Create recipe")
                }
            if (results.isEmpty()) {
                Text("No matching foods.", color = AppMuted)
            } else {
                results.forEach { item ->
                    FoodSearchRow(
                        item = item,
                        showCalories = true,
                        onClick = { onSelectFood(item) },
                        onDoubleClick = { onQuickLogFood(item) },
                        expanded = expandedFoodId == item.id,
                        onToggleExpanded = {
                            expandedFoodId = if (expandedFoodId == item.id) null else item.id
                        },
                        onEdit = { onEditFood(item) },
                        onDelete = { pendingDeleteFood = item },
                    )
                }
            }

            }
            if (!showingLeftovers && activeKind == FoodKind.Ingredient) {
                com.philipcosgrave.calorietracker.ui.components.ReferenceLookupResults(search, foods,
                    onSelect = { reference = it }, onEmpty = {
                        TextButton(onClick = onScanBarcode) { Text("Scan Barcode") }
                        TextButton(onClick = onPhoto) { Text("Scan Nutrition Label / Photo") }
                        TextButton(onClick = onVoice) { Text("Speak Food") }
                        TextButton(onClick = onAddIngredient) { Text("Enter Manually") }
                    })
            }
            if (!showingLeftovers && shouldShowRemoteSearch) {
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
                                    else "Search community and Canadian foods",
                                    color = AppBlue,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                if (isSignedIn) {
                                    "Search order: your cloud foods, then community foods, then the Canadian Nutrient File."
                                } else {
                                    "Search order: community foods, then the Canadian Nutrient File. Sign in to include your saved cloud foods."
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

    pendingDeleteFood?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteFood = null },
            title = { Text("Delete food?") },
            text = { Text("Delete ${item.name} from your saved foods?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteFood = null
                        onDeleteFood(item)
                    },
                ) {
                    Text("Delete", color = Color(0xFFFF5449), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFood = null }) {
                    Text("Cancel")
                }
            },
        )
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
