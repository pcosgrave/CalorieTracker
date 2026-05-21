package com.philipcosgrave.calorietracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.round
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieTrackerApp()
        }
    }
}

private enum class Meal(val label: String) {
    Breakfast("Breakfast"),
    Lunch("Lunch"),
    Dinner("Dinner"),
    Snack("Snack"),
}

private enum class FoodKind {
    Ingredient,
    Recipe,
}

private enum class SortMode(val label: String) {
    Recent("Recent"),
    Frequency("Frequency"),
    Alphabetical("Alphabetical"),
}

private enum class Screen {
    Diary,
    AddFood,
    QuickCalories,
    NewIngredient,
    RecipeBuilder,
    LogFood,
}

private data class Nutrients(
    val calories: Double,
    val proteinGrams: Double = 0.0,
    val carbohydrateGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
)

private data class FoodItem(
    val id: String,
    val kind: FoodKind,
    val name: String,
    val brand: String = "",
    val barcode: String = "",
    val servingQuantity: Double,
    val servingUnit: String,
    val nutrients: Nutrients,
    val components: List<RecipeComponent> = emptyList(),
    val frequency: Int = 0,
    val lastUsedDaysAgo: Int = 0,
) {
    val servingLabel: String
        get() = "${formatNumber(servingQuantity)} $servingUnit"
}

private data class RecipeComponent(
    val item: FoodItem,
    val amount: Double,
    val unit: String,
)

private data class DiaryEntry(
    val id: String,
    val food: FoodItem,
    val date: LocalDate,
    val meal: Meal,
    val servingMultiplier: Double,
)

private data class RecipeDraft(
    val name: String = "",
    val brand: String = "",
    val servingQuantity: String = "1",
    val servingUnit: String = "serving",
    val components: List<RecipeComponent> = emptyList(),
)

private data class Totals(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
)

private val measurementUnits = listOf(
    "bar",
    "bottle",
    "box",
    "can",
    "container",
    "cup",
    "fl oz",
    "gram",
    "jar",
    "kg",
    "lb",
    "liter",
    "milligram",
    "ml",
    "oz",
    "package",
    "pint",
    "quart",
    "service",
    "serving",
    "tbsp",
    "tsp",
)

private val seedFoods = listOf(
    FoodItem(
        id = "ingredient-coffee",
        kind = FoodKind.Ingredient,
        name = "Coffee",
        servingQuantity = 16.0,
        servingUnit = "oz",
        nutrients = Nutrients(calories = 9.0, proteinGrams = 0.5),
        frequency = 18,
    ),
    FoodItem(
        id = "ingredient-greek-yogurt",
        kind = FoodKind.Ingredient,
        name = "Greek yogurt",
        brand = "Plain",
        barcode = "012345678905",
        servingQuantity = 1.0,
        servingUnit = "cup",
        nutrients = Nutrients(calories = 140.0, proteinGrams = 20.0, carbohydrateGrams = 8.0, fatGrams = 3.0),
        frequency = 14,
        lastUsedDaysAgo = 1,
    ),
    FoodItem(
        id = "ingredient-banana",
        kind = FoodKind.Ingredient,
        name = "Banana",
        servingQuantity = 1.0,
        servingUnit = "medium",
        nutrients = Nutrients(calories = 105.0, proteinGrams = 1.3, carbohydrateGrams = 27.0, fatGrams = 0.4),
        frequency = 10,
        lastUsedDaysAgo = 2,
    ),
    FoodItem(
        id = "ingredient-oats",
        kind = FoodKind.Ingredient,
        name = "Rolled oats",
        servingQuantity = 0.5,
        servingUnit = "cup",
        nutrients = Nutrients(calories = 150.0, proteinGrams = 5.0, carbohydrateGrams = 27.0, fatGrams = 3.0),
        frequency = 7,
        lastUsedDaysAgo = 4,
    ),
)

private fun seedRecipes(): List<FoodItem> {
    val yogurt = seedFoods.first { it.id == "ingredient-greek-yogurt" }
    val banana = seedFoods.first { it.id == "ingredient-banana" }
    val oats = seedFoods.first { it.id == "ingredient-oats" }
    val eggs = FoodItem(
        id = "ingredient-eggs",
        kind = FoodKind.Ingredient,
        name = "Eggs",
        servingQuantity = 2.0,
        servingUnit = "eggs",
        nutrients = Nutrients(calories = 140.0, proteinGrams = 12.0, carbohydrateGrams = 1.0, fatGrams = 10.0),
    )
    val tortilla = FoodItem(
        id = "ingredient-tortilla",
        kind = FoodKind.Ingredient,
        name = "Tortilla",
        servingQuantity = 1.0,
        servingUnit = "wrap",
        nutrients = Nutrients(calories = 180.0, proteinGrams = 5.0, carbohydrateGrams = 30.0, fatGrams = 4.0),
    )

    return listOf(
        FoodItem(
            id = "recipe-yogurt-bowl",
            kind = FoodKind.Recipe,
            name = "Yogurt banana bowl",
            servingQuantity = 1.0,
            servingUnit = "bowl",
            nutrients = Nutrients(calories = 395.0, proteinGrams = 26.0, carbohydrateGrams = 62.0, fatGrams = 6.4),
            components = listOf(
                RecipeComponent(yogurt, 1.0, "cup"),
                RecipeComponent(banana, 1.0, "medium"),
                RecipeComponent(oats, 0.5, "cup"),
            ),
            frequency = 8,
            lastUsedDaysAgo = 1,
        ),
        FoodItem(
            id = "recipe-breakfast-wrap",
            kind = FoodKind.Recipe,
            name = "Breakfast egg wrap",
            servingQuantity = 1.0,
            servingUnit = "wrap",
            nutrients = Nutrients(calories = 430.0, proteinGrams = 25.0, carbohydrateGrams = 38.0, fatGrams = 19.0),
            components = listOf(
                RecipeComponent(eggs, 2.0, "eggs"),
                RecipeComponent(tortilla, 1.0, "wrap"),
            ),
            frequency = 5,
            lastUsedDaysAgo = 6,
        ),
    )
}

@Composable
private fun CalorieTrackerApp() {
    val context = LocalContext.current
    val customFoods = remember { mutableStateListOf<FoodItem>().also { it.addAll(readFoodItems(context, "customFoods")) } }
    val recipes = remember {
        mutableStateListOf<FoodItem>().also {
            val storedRecipes = readFoodItems(context, "recipes")
            it.addAll(storedRecipes.ifEmpty { seedRecipes() })
        }
    }
    val diary = remember { mutableStateListOf<DiaryEntry>().also { it.addAll(readDiaryEntries(context)) } }
    val hiddenSeedIds = remember { mutableStateListOf<String>().also { it.addAll(readStringList(context, "hiddenSeedIds")) } }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var screen by remember { mutableStateOf(Screen.Diary) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var recipeDraft by remember { mutableStateOf(RecipeDraft()) }
    var parentRecipeDraft by remember { mutableStateOf<RecipeDraft?>(null) }
    var editingFood by remember { mutableStateOf<FoodItem?>(null) }

    LaunchedEffect(customFoods.toList(), recipes.toList(), diary.toList(), hiddenSeedIds.toList()) {
        writeFoodItems(context, "customFoods", customFoods)
        writeFoodItems(context, "recipes", recipes)
        writeDiaryEntries(context, diary)
        writeStringList(context, "hiddenSeedIds", hiddenSeedIds)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                Screen.Diary -> DiaryScreen(
                    selectedDate = selectedDate,
                    entries = diary,
                    onDateChange = { selectedDate = it },
                    onAddFood = { screen = Screen.AddFood },
                    onDeleteEntry = { diary.remove(it) },
                    onUpdateEntry = { updated ->
                        val index = diary.indexOfFirst { it.id == updated.id }
                        if (index >= 0) diary[index] = updated
                    },
                )

                Screen.AddFood -> AddFoodScreen(
                    date = selectedDate,
                    foods = customFoods + recipes + seedFoods.filterNot { hiddenSeedIds.contains(it.id) },
                    onBack = { screen = Screen.Diary },
                    onQuickCalories = { screen = Screen.QuickCalories },
                    onAddIngredient = {
                        editingFood = null
                        screen = Screen.NewIngredient
                    },
                    onAddRecipe = {
                        recipeDraft = RecipeDraft()
                        parentRecipeDraft = null
                        screen = Screen.RecipeBuilder
                    },
                    onSelectFood = {
                        selectedFood = it
                        screen = Screen.LogFood
                    },
                    onDeleteFood = { item ->
                        when {
                            customFoods.remove(item) -> Unit
                            recipes.remove(item) -> Unit
                            else -> hiddenSeedIds.add(item.id)
                        }
                    },
                    onEditFood = { item ->
                        if (item.kind == FoodKind.Recipe) {
                            recipeDraft = item.toRecipeDraft()
                            editingFood = item
                            screen = Screen.RecipeBuilder
                        } else {
                            editingFood = item
                            screen = Screen.NewIngredient
                        }
                    },
                )

                Screen.QuickCalories -> QuickCaloriesScreen(
                    date = selectedDate,
                    onBack = { screen = Screen.AddFood },
                    onSave = { calories, meal, date ->
                        diary.add(
                            0,
                            DiaryEntry(
                                id = createId("entry"),
                                food = FoodItem(
                                    id = createId("quick"),
                                    kind = FoodKind.Ingredient,
                                    name = "Quick calories",
                                    servingQuantity = 1.0,
                                    servingUnit = "entry",
                                    nutrients = Nutrients(calories = calories),
                                ),
                                date = date,
                                meal = meal,
                                servingMultiplier = 1.0,
                            ),
                        )
                        selectedDate = date
                        screen = Screen.Diary
                    },
                )

                Screen.NewIngredient -> NewIngredientScreen(
                    existing = editingFood,
                    onBack = { screen = Screen.AddFood },
                    onSave = { item ->
                        val index = customFoods.indexOfFirst { it.id == item.id }
                        if (index >= 0) customFoods[index] = item else customFoods.add(0, item)
                        editingFood = null
                        screen = if (parentRecipeDraft != null) Screen.RecipeBuilder else Screen.AddFood
                    },
                )

                Screen.RecipeBuilder -> RecipeBuilderScreen(
                    draft = recipeDraft,
                    foods = customFoods + recipes.filter { it.id != editingFood?.id } + seedFoods,
                    onDraftChange = { recipeDraft = it },
                    onBack = {
                        parentRecipeDraft = null
                        editingFood = null
                        screen = Screen.AddFood
                    },
                    onAddIngredient = {
                        editingFood = null
                        screen = Screen.NewIngredient
                    },
                    onStartNestedRecipe = {
                        parentRecipeDraft = recipeDraft
                        recipeDraft = RecipeDraft()
                    },
                    onSave = { draft ->
                        val recipe = draft.toFoodItem(editingFood?.id)
                        val index = recipes.indexOfFirst { it.id == recipe.id }
                        if (index >= 0) recipes[index] = recipe else recipes.add(0, recipe)

                        val parent = parentRecipeDraft
                        if (parent != null) {
                            recipeDraft = parent.copy(
                                components = parent.components + RecipeComponent(recipe, recipe.servingQuantity, recipe.servingUnit),
                            )
                            parentRecipeDraft = null
                        } else {
                            recipeDraft = RecipeDraft()
                            editingFood = null
                            screen = Screen.AddFood
                        }
                    },
                )

                Screen.LogFood -> selectedFood?.let { food ->
                    LogFoodScreen(
                        food = food,
                        date = selectedDate,
                        onBack = { screen = Screen.AddFood },
                        onLog = { meal, date, amount, addMore ->
                            val multiplier = amount / food.servingQuantity.coerceAtLeast(0.1)
                            diary.add(
                                0,
                                DiaryEntry(
                                    id = createId("entry"),
                                    food = food,
                                    date = date,
                                    meal = meal,
                                    servingMultiplier = multiplier,
                                ),
                            )
                            selectedDate = date
                            screen = if (addMore) Screen.AddFood else Screen.Diary
                        },
                    )
                } ?: run {
                    screen = Screen.AddFood
                }
            }
        }
    }
}

@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun DiaryScreen(
    selectedDate: LocalDate,
    entries: List<DiaryEntry>,
    onDateChange: (LocalDate) -> Unit,
    onAddFood: () -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onUpdateEntry: (DiaryEntry) -> Unit,
) {
    val selectedEntries = entries.filter { it.date == selectedDate }
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    Page {
        Header()
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDateChange(selectedDate.minusDays(1)) }) { Text("<") }
                    Text(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Button(onClick = { onDateChange(selectedDate.plusDays(1)) }) { Text(">") }
                }
                TotalsGrid(totalsForEntries(selectedEntries))
                Button(onClick = onAddFood, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("+")
                }
            }
        }

        editingEntry?.let { entry ->
            EditEntryCard(
                entry = entry,
                onCancel = { editingEntry = null },
                onSave = {
                    onUpdateEntry(it)
                    editingEntry = null
                },
            )
        }

        Meal.entries.forEach { meal ->
            val mealEntries = selectedEntries.filter { it.meal == meal }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meal.label, style = MaterialTheme.typography.titleLarge)
                            Text("${mealEntries.size} foods", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${formatNumber(totalsForEntries(mealEntries).calories)} cal", fontWeight = FontWeight.Bold)
                    }
                    if (mealEntries.isEmpty()) {
                        Text("No food logged.")
                    } else {
                        mealEntries.forEach { entry ->
                            DiaryEntryRow(
                                entry = entry,
                                onEdit = { editingEntry = entry },
                                onDelete = { onDeleteEntry(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CalorieTracker", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Manual labels, private barcode shortcuts, cloud sync when signed in.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = { }) {
            Text("Sign in with Google")
        }
    }
}

@Composable
private fun TotalsGrid(totals: Totals) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Metric(formatNumber(totals.calories), "Calories", Modifier.weight(1f))
        Metric("${formatNumber(totals.protein)}g", "Protein", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Metric("${formatNumber(totals.carbs)}g", "Carbs", Modifier.weight(1f))
        Metric("${formatNumber(totals.fat)}g", "Fat", Modifier.weight(1f))
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DiaryEntryRow(entry: DiaryEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    val nutrients = entry.food.nutrients.scale(entry.servingMultiplier)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onEdit)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.food.name, fontWeight = FontWeight.Bold)
                Text("${entry.food.brand.ifBlank { "No brand" }} - ${entry.food.servingLabel} x ${formatNumber(entry.servingMultiplier)}")
            }
            Text("${formatNumber(nutrients.calories)} cal", fontWeight = FontWeight.Bold)
            OverflowMenu(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun EditEntryCard(entry: DiaryEntry, onCancel: () -> Unit, onSave: (DiaryEntry) -> Unit) {
    var name by remember(entry.id) { mutableStateOf(entry.food.name) }
    var brand by remember(entry.id) { mutableStateOf(entry.food.brand) }
    var calories by remember(entry.id) { mutableStateOf(formatNumber(entry.food.nutrients.calories)) }
    var servings by remember(entry.id) { mutableStateOf(formatNumber(entry.servingMultiplier)) }
    var meal by remember(entry.id) { mutableStateOf(entry.meal) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Edit logged food", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(brand, { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(calories, { calories = it }, label = { Text("Calories") }, modifier = Modifier.weight(1f))
                OutlinedTextField(servings, { servings = it }, label = { Text("Servings") }, modifier = Modifier.weight(1f))
            }
            MealPicker(meal = meal, onMealChange = { meal = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(
                            entry.copy(
                                meal = meal,
                                servingMultiplier = servings.toDoubleOrNull()?.coerceAtLeast(0.1) ?: entry.servingMultiplier,
                                food = entry.food.copy(
                                    name = name.ifBlank { entry.food.name },
                                    brand = brand,
                                    nutrients = entry.food.nutrients.copy(calories = calories.toDoubleOrNull() ?: entry.food.nutrients.calories),
                                ),
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun AddFoodScreen(
    date: LocalDate,
    foods: List<FoodItem>,
    onBack: () -> Unit,
    onQuickCalories: () -> Unit,
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
            TextButton(onClick = onBack) { Text("Back") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Search foods", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Button(onClick = onQuickCalories, modifier = Modifier.size(48.dp), shape = CircleShape) { Text("123") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { }) { Text("Barcode") }
                }
                OutlinedTextField(search, { search = it }, label = { Text("Search ingredients or recipes") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodKind.entries.forEach { kind ->
                        FilterChip(
                            selected = activeKind == kind,
                            onClick = { activeKind = kind },
                            label = { Text(if (kind == FoodKind.Ingredient) "Ingredients" else "Recipes") },
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

@Composable
private fun FoodSearchRow(
    item: FoodItem,
    showCalories: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KindIcon(item.kind)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text(
                    listOf(item.brand, item.componentSummary()).filter { it.isNotBlank() }.joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.servingLabel, style = MaterialTheme.typography.bodySmall)
                if (showCalories) Text("${formatNumber(item.nutrients.calories)} cal", fontWeight = FontWeight.Bold)
            }
            if (onEdit != null && onDelete != null) {
                OverflowMenu(onEdit = onEdit, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun KindIcon(kind: FoodKind) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (kind == FoodKind.Recipe) "B" else "K", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun OverflowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text("⋮") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() })
        }
    }
}

@Composable
private fun QuickCaloriesScreen(
    date: LocalDate,
    onBack: () -> Unit,
    onSave: (Double, Meal, LocalDate) -> Unit,
) {
    var calories by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(Meal.Snack) }
    var selectedDate by remember { mutableStateOf(date) }

    Page {
        Text("Quick Calories", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateStepper(selectedDate, { selectedDate = it })
                OutlinedTextField(calories, { calories = it }, label = { Text("Calories") }, modifier = Modifier.fillMaxWidth())
                MealPicker(meal, { meal = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onSave(calories.toDoubleOrNull() ?: 0.0, meal, selectedDate) },
                        enabled = (calories.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Log calories")
                    }
                }
            }
        }
    }
}

@Composable
private fun NewIngredientScreen(existing: FoodItem?, onBack: () -> Unit, onSave: (FoodItem) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var brand by remember(existing?.id) { mutableStateOf(existing?.brand.orEmpty()) }
    var barcode by remember(existing?.id) { mutableStateOf(existing?.barcode.orEmpty()) }
    var servingQuantity by remember(existing?.id) { mutableStateOf(existing?.servingQuantity?.let(::formatNumber) ?: "1") }
    var servingUnit by remember(existing?.id) { mutableStateOf(existing?.servingUnit ?: "serving") }
    var calories by remember(existing?.id) { mutableStateOf(existing?.nutrients?.calories?.let(::formatNumber).orEmpty()) }
    var protein by remember(existing?.id) { mutableStateOf(existing?.nutrients?.proteinGrams?.let(::formatNumber).orEmpty()) }
    var carbs by remember(existing?.id) { mutableStateOf(existing?.nutrients?.carbohydrateGrams?.let(::formatNumber).orEmpty()) }
    var fat by remember(existing?.id) { mutableStateOf(existing?.nutrients?.fatGrams?.let(::formatNumber).orEmpty()) }

    Page {
        Text(if (existing == null) "Add New Food" else "Edit Food", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(brand, { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(servingQuantity, { servingQuantity = it }, label = { Text("Serving size") }, modifier = Modifier.weight(1f))
                    UnitPicker(servingUnit, { servingUnit = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(calories, { calories = it }, label = { Text("Calories") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(protein, { protein = it }, label = { Text("Protein") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbs") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(fat, { fat = it }, label = { Text("Fat") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        enabled = name.isNotBlank() && brand.isNotBlank() && (calories.toDoubleOrNull() ?: 0.0) > 0,
                        onClick = {
                            onSave(
                                FoodItem(
                                    id = existing?.id ?: createId("custom"),
                                    kind = FoodKind.Ingredient,
                                    name = name.trim(),
                                    brand = brand.trim(),
                                    barcode = barcode.trim(),
                                    servingQuantity = servingQuantity.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0,
                                    servingUnit = servingUnit,
                                    nutrients = Nutrients(
                                        calories = calories.toDoubleOrNull() ?: 0.0,
                                        proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                                        carbohydrateGrams = carbs.toDoubleOrNull() ?: 0.0,
                                        fatGrams = fat.toDoubleOrNull() ?: 0.0,
                                    ),
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save Food")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeBuilderScreen(
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
                    UnitPicker(draft.servingUnit, { onDraftChange(draft.copy(servingUnit = it)) }, Modifier.weight(1f))
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
                    enabled = draft.name.isNotBlank() && draft.brand.isNotBlank() && draft.components.isNotEmpty(),
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

@Composable
private fun RecipeComponentRow(
    component: RecipeComponent,
    onChange: (RecipeComponent) -> Unit,
    onRemove: () -> Unit,
) {
    var amount by remember(component.item.id, component.amount) { mutableStateOf(formatNumber(component.amount)) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KindIcon(component.item.kind)
                Column(modifier = Modifier.weight(1f)) {
                    Text(component.item.name, fontWeight = FontWeight.Bold)
                    Text(component.item.servingLabel, style = MaterialTheme.typography.bodySmall)
                    if (component.item.components.isNotEmpty()) {
                        Text(component.item.componentSummary(), style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        onChange(component.copy(amount = it.toDoubleOrNull() ?: component.amount))
                    },
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
                )
                UnitPicker(component.unit, { onChange(component.copy(unit = it)) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LogFoodScreen(
    food: FoodItem,
    date: LocalDate,
    onBack: () -> Unit,
    onLog: (Meal, LocalDate, Double, Boolean) -> Unit,
) {
    var amount by remember(food.id) { mutableStateOf(formatNumber(food.servingQuantity)) }
    var unit by remember(food.id) { mutableStateOf(food.servingUnit) }
    var meal by remember { mutableStateOf(Meal.Breakfast) }
    var selectedDate by remember { mutableStateOf(date) }
    val amountNumber = amount.toDoubleOrNull()?.coerceAtLeast(0.1) ?: food.servingQuantity
    val adjusted = food.nutrients.scale(amountNumber / food.servingQuantity.coerceAtLeast(0.1))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("< Back") }
        Text(food.name, style = MaterialTheme.typography.headlineSmall, color = Color(0xFFF5F1E8))
        Text("NUTRITION FACTS", color = Color(0xFF00D1FF), fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Serving size", color = Color.White)
            OutlinedTextField(amount, { amount = it }, modifier = Modifier.weight(1f))
            UnitPicker(unit, { unit = it }, Modifier.weight(1f))
            Text("${formatNumber(adjusted.calories)} cals.", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meal & Snacks Time", style = MaterialTheme.typography.titleLarge, color = Color.White)
            MealPicker(meal, { meal = it }, darkMode = true)
        }
        if (food.components.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recipe ingredients", color = Color.White, fontWeight = FontWeight.Bold)
                food.components.forEach { component ->
                    Row {
                        Text("• ${component.item.name}", color = Color(0xFFD5D0C7), modifier = Modifier.weight(1f))
                        Text("Serving: ${formatNumber(component.amount)} ${component.unit}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        DateStepper(selectedDate, { selectedDate = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onLog(meal, selectedDate, amountNumber, true) }, modifier = Modifier.weight(1f)) {
                Text("Log & add more")
            }
            Button(onClick = { onLog(meal, selectedDate, amountNumber, false) }, modifier = Modifier.weight(1f)) {
                Text("Log this")
            }
        }
    }
}

@Composable
private fun MealPicker(meal: Meal, onMealChange: (Meal) -> Unit, darkMode: Boolean = false) {
    val textColor = if (darkMode) Color.White else Color.Unspecified
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Meal.entries.take(2).forEach { option ->
                MealChoice(option, meal == option, onMealChange, textColor, Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Meal.entries.drop(2).forEach { option ->
                MealChoice(option, meal == option, onMealChange, textColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MealChoice(option: Meal, selected: Boolean, onMealChange: (Meal) -> Unit, color: Color, modifier: Modifier) {
    Row(
        modifier = modifier.clickable { onMealChange(option) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = { onMealChange(option) })
        Text(option.label.uppercase(), color = color, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DateStepper(date: LocalDate, onDateChange: (LocalDate) -> Unit, darkMode: Boolean = false) {
    val textColor = if (darkMode) Color.White else Color.Unspecified
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Day", color = textColor, fontWeight = FontWeight.Bold)
        Button(onClick = { onDateChange(date.minusDays(1)) }) { Text("<") }
        Text(date.format(DateTimeFormatter.ISO_LOCAL_DATE), color = textColor, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Button(onClick = { onDateChange(date.plusDays(1)) }) { Text(">") }
    }
}

@Composable
private fun UnitPicker(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("Unit") },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            measurementUnits.forEach { unit ->
                DropdownMenuItem(text = { Text(unit) }, onClick = { expanded = false; onChange(unit) })
            }
        }
    }
}

@Composable
private fun SortMenu(value: SortMode, onChange: (SortMode) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text("≡")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Sort by") }, onClick = { })
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(if (mode == value) "✓ ${mode.label}" else mode.label) },
                    onClick = {
                        expanded = false
                        onChange(mode)
                    },
                )
            }
        }
    }
}

private fun RecipeDraft.toFoodItem(existingId: String? = null): FoodItem {
    val nutrients = totalComponents(components)
    val quantity = servingQuantity.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0
    return FoodItem(
        id = existingId ?: createId("recipe"),
        kind = FoodKind.Recipe,
        name = name.trim(),
        brand = brand.trim(),
        servingQuantity = quantity,
        servingUnit = servingUnit,
        nutrients = Nutrients(nutrients.calories, nutrients.protein, nutrients.carbs, nutrients.fat),
        components = components,
    )
}

private fun FoodItem.toRecipeDraft(): RecipeDraft = RecipeDraft(
    name = name,
    brand = brand,
    servingQuantity = formatNumber(servingQuantity),
    servingUnit = servingUnit,
    components = components,
)

private fun totalComponents(components: List<RecipeComponent>): Totals =
    components.fold(Totals()) { total, component ->
        val converted = convertAmount(component.amount, component.unit, component.item.servingUnit) ?: component.amount
        val nutrients = component.item.nutrients.scale(converted / component.item.servingQuantity.coerceAtLeast(0.1))
        Totals(
            calories = total.calories + nutrients.calories,
            protein = total.protein + nutrients.proteinGrams,
            carbs = total.carbs + nutrients.carbohydrateGrams,
            fat = total.fat + nutrients.fatGrams,
        )
    }.rounded()

private fun totalsForEntries(entries: List<DiaryEntry>): Totals =
    entries.fold(Totals()) { total, entry ->
        val nutrients = entry.food.nutrients.scale(entry.servingMultiplier)
        Totals(
            calories = total.calories + nutrients.calories,
            protein = total.protein + nutrients.proteinGrams,
            carbs = total.carbs + nutrients.carbohydrateGrams,
            fat = total.fat + nutrients.fatGrams,
        )
    }.rounded()

private fun Nutrients.scale(multiplier: Double): Nutrients = Nutrients(
    calories = roundOne(calories * multiplier),
    proteinGrams = roundOne(proteinGrams * multiplier),
    carbohydrateGrams = roundOne(carbohydrateGrams * multiplier),
    fatGrams = roundOne(fatGrams * multiplier),
)

private fun Totals.rounded(): Totals = Totals(
    calories = roundOne(calories),
    protein = roundOne(protein),
    carbs = roundOne(carbs),
    fat = roundOne(fat),
)

private fun FoodItem.componentSummary(): String =
    components.joinToString(", ") { "${it.item.name} - ${formatNumber(it.amount)} ${it.unit}" }

private val conversionGroups = listOf(
    mapOf("tsp" to 1.0, "tbsp" to 3.0, "fl oz" to 6.0, "cup" to 48.0, "pint" to 96.0, "quart" to 192.0, "ml" to 0.202884, "liter" to 202.884),
    mapOf("milligram" to 0.001, "gram" to 1.0, "kg" to 1000.0, "oz" to 28.3495, "lb" to 453.592),
)

private fun convertAmount(amount: Double, fromUnit: String, toUnit: String): Double? {
    if (fromUnit == toUnit) return amount
    val group = conversionGroups.firstOrNull { it.containsKey(fromUnit) && it.containsKey(toUnit) } ?: return null
    return amount * (group[fromUnit] ?: return null) / (group[toUnit] ?: return null)
}

private fun createId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

private fun roundOne(value: Double): Double = round(value * 10.0) / 10.0

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else roundOne(value).toString()

private fun prefs(context: Context) = context.getSharedPreferences("calorie-tracker", Context.MODE_PRIVATE)

private fun readFoodItems(context: Context, key: String): List<FoodItem> =
    runCatching {
        val stored = prefs(context).getString(key, "[]") ?: "[]"
        val array = JSONArray(stored)
        List(array.length()) { index -> foodItemFromJson(array.getJSONObject(index)) }
    }.getOrDefault(emptyList())

private fun writeFoodItems(context: Context, key: String, items: List<FoodItem>) {
    val array = JSONArray()
    items.forEach { array.put(it.toJson()) }
    prefs(context).edit().putString(key, array.toString()).apply()
}

private fun readDiaryEntries(context: Context): List<DiaryEntry> =
    runCatching {
        val stored = prefs(context).getString("diaryEntries", "[]") ?: "[]"
        val array = JSONArray(stored)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            DiaryEntry(
                id = item.getString("id"),
                food = foodItemFromJson(item.getJSONObject("food")),
                date = LocalDate.parse(item.getString("date")),
                meal = Meal.valueOf(item.getString("meal")),
                servingMultiplier = item.getDouble("servingMultiplier"),
            )
        }
    }.getOrDefault(emptyList())

private fun writeDiaryEntries(context: Context, entries: List<DiaryEntry>) {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(
            JSONObject()
                .put("id", entry.id)
                .put("food", entry.food.toJson())
                .put("date", entry.date.toString())
                .put("meal", entry.meal.name)
                .put("servingMultiplier", entry.servingMultiplier),
        )
    }
    prefs(context).edit().putString("diaryEntries", array.toString()).apply()
}

private fun readStringList(context: Context, key: String): List<String> =
    runCatching {
        val array = JSONArray(prefs(context).getString(key, "[]") ?: "[]")
        List(array.length()) { index -> array.getString(index) }
    }.getOrDefault(emptyList())

private fun writeStringList(context: Context, key: String, values: List<String>) {
    val array = JSONArray()
    values.forEach { array.put(it) }
    prefs(context).edit().putString(key, array.toString()).apply()
}

private fun FoodItem.toJson(): JSONObject {
    val componentsJson = JSONArray()
    components.forEach { componentsJson.put(it.toJson()) }
    return JSONObject()
        .put("id", id)
        .put("kind", kind.name)
        .put("name", name)
        .put("brand", brand)
        .put("barcode", barcode)
        .put("servingQuantity", servingQuantity)
        .put("servingUnit", servingUnit)
        .put("nutrients", nutrients.toJson())
        .put("components", componentsJson)
        .put("frequency", frequency)
        .put("lastUsedDaysAgo", lastUsedDaysAgo)
}

private fun RecipeComponent.toJson(): JSONObject =
    JSONObject()
        .put("item", item.toJson())
        .put("amount", amount)
        .put("unit", unit)

private fun Nutrients.toJson(): JSONObject =
    JSONObject()
        .put("calories", calories)
        .put("proteinGrams", proteinGrams)
        .put("carbohydrateGrams", carbohydrateGrams)
        .put("fatGrams", fatGrams)

private fun foodItemFromJson(json: JSONObject): FoodItem {
    val componentsJson = json.optJSONArray("components") ?: JSONArray()
    return FoodItem(
        id = json.getString("id"),
        kind = FoodKind.valueOf(json.getString("kind")),
        name = json.getString("name"),
        brand = json.optString("brand"),
        barcode = json.optString("barcode"),
        servingQuantity = json.getDouble("servingQuantity"),
        servingUnit = json.getString("servingUnit"),
        nutrients = nutrientsFromJson(json.getJSONObject("nutrients")),
        components = List(componentsJson.length()) { index -> recipeComponentFromJson(componentsJson.getJSONObject(index)) },
        frequency = json.optInt("frequency"),
        lastUsedDaysAgo = json.optInt("lastUsedDaysAgo"),
    )
}

private fun recipeComponentFromJson(json: JSONObject): RecipeComponent =
    RecipeComponent(
        item = foodItemFromJson(json.getJSONObject("item")),
        amount = json.getDouble("amount"),
        unit = json.getString("unit"),
    )

private fun nutrientsFromJson(json: JSONObject): Nutrients =
    Nutrients(
        calories = json.getDouble("calories"),
        proteinGrams = json.optDouble("proteinGrams"),
        carbohydrateGrams = json.optDouble("carbohydrateGrams"),
        fatGrams = json.optDouble("fatGrams"),
    )
