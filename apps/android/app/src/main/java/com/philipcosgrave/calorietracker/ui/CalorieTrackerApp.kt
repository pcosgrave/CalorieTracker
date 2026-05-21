package com.philipcosgrave.calorietracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.philipcosgrave.calorietracker.data.readDiaryEntries
import com.philipcosgrave.calorietracker.data.readFoodItems
import com.philipcosgrave.calorietracker.data.readStringList
import com.philipcosgrave.calorietracker.data.seedFoods
import com.philipcosgrave.calorietracker.data.seedRecipes
import com.philipcosgrave.calorietracker.data.writeDiaryEntries
import com.philipcosgrave.calorietracker.data.writeFoodItems
import com.philipcosgrave.calorietracker.data.writeStringList
import com.philipcosgrave.calorietracker.domain.createId
import com.philipcosgrave.calorietracker.domain.toFoodItem
import com.philipcosgrave.calorietracker.domain.toRecipeDraft
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.RecipeDraft
import com.philipcosgrave.calorietracker.ui.screens.AddFoodScreen
import com.philipcosgrave.calorietracker.ui.screens.DiaryScreen
import com.philipcosgrave.calorietracker.ui.screens.LogFoodScreen
import com.philipcosgrave.calorietracker.ui.screens.NewIngredientScreen
import com.philipcosgrave.calorietracker.ui.screens.QuickCaloriesScreen
import com.philipcosgrave.calorietracker.ui.screens.RecipeBuilderScreen
import java.time.LocalDate

@Composable
fun CalorieTrackerApp() {
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
    var screen by remember { mutableStateOf(AppScreen.Diary) }
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            AppScreen.Diary -> DiaryScreen(
                selectedDate = selectedDate,
                entries = diary,
                onDateChange = { selectedDate = it },
                onAddFood = { screen = AppScreen.AddFood },
                onDeleteEntry = { diary.remove(it) },
                onUpdateEntry = { updated ->
                    val index = diary.indexOfFirst { it.id == updated.id }
                    if (index >= 0) diary[index] = updated
                },
            )

            AppScreen.AddFood -> AddFoodScreen(
                date = selectedDate,
                foods = customFoods + recipes + seedFoods.filterNot { hiddenSeedIds.contains(it.id) },
                onBack = { screen = AppScreen.Diary },
                onQuickCalories = { screen = AppScreen.QuickCalories },
                onAddIngredient = {
                    editingFood = null
                    screen = AppScreen.NewIngredient
                },
                onAddRecipe = {
                    recipeDraft = RecipeDraft()
                    parentRecipeDraft = null
                    screen = AppScreen.RecipeBuilder
                },
                onSelectFood = {
                    selectedFood = it
                    screen = AppScreen.LogFood
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
                        screen = AppScreen.RecipeBuilder
                    } else {
                        editingFood = item
                        screen = AppScreen.NewIngredient
                    }
                },
            )

            AppScreen.QuickCalories -> QuickCaloriesScreen(
                date = selectedDate,
                onBack = { screen = AppScreen.AddFood },
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
                    screen = AppScreen.Diary
                },
            )

            AppScreen.NewIngredient -> NewIngredientScreen(
                existing = editingFood,
                onBack = { screen = AppScreen.AddFood },
                onSave = { item ->
                    val index = customFoods.indexOfFirst { it.id == item.id }
                    if (index >= 0) customFoods[index] = item else customFoods.add(0, item)
                    editingFood = null
                    screen = if (parentRecipeDraft != null) AppScreen.RecipeBuilder else AppScreen.AddFood
                },
            )

            AppScreen.RecipeBuilder -> RecipeBuilderScreen(
                draft = recipeDraft,
                foods = customFoods + recipes.filter { it.id != editingFood?.id } + seedFoods,
                onDraftChange = { recipeDraft = it },
                onBack = {
                    parentRecipeDraft = null
                    editingFood = null
                    screen = AppScreen.AddFood
                },
                onAddIngredient = {
                    editingFood = null
                    screen = AppScreen.NewIngredient
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
                        screen = AppScreen.AddFood
                    }
                },
            )

            AppScreen.LogFood -> selectedFood?.let { food ->
                LogFoodScreen(
                    food = food,
                    date = selectedDate,
                    onBack = { screen = AppScreen.AddFood },
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
                        screen = if (addMore) AppScreen.AddFood else AppScreen.Diary
                    },
                )
            } ?: run {
                screen = AppScreen.AddFood
            }
        }
    }
}
