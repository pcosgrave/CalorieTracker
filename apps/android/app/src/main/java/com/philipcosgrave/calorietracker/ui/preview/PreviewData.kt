package com.philipcosgrave.calorietracker.ui.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.RecipeDraft
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.model.Totals
import com.philipcosgrave.calorietracker.data.health.HealthConnectAvailability
import com.philipcosgrave.calorietracker.ui.theme.CalorieTrackerTheme
import java.time.LocalDate

object PreviewData {
    private val previewYogurt = FoodItem(
        id = "preview-yogurt",
        kind = FoodKind.Ingredient,
        name = "Greek yogurt",
        brand = "Plain",
        servingQuantity = 1.0,
        servingUnit = "cup",
        nutrients = Nutrients(calories = 140.0, proteinGrams = 20.0, carbohydrateGrams = 8.0, fatGrams = 3.0),
        frequency = 8,
        lastUsedDaysAgo = 1,
    )
    private val previewBanana = FoodItem(
        id = "preview-banana",
        kind = FoodKind.Ingredient,
        name = "Banana",
        brand = "Fresh",
        servingQuantity = 1.0,
        servingUnit = "banana",
        nutrients = Nutrients(calories = 105.0, proteinGrams = 1.3, carbohydrateGrams = 27.0, fatGrams = 0.4),
        frequency = 6,
        lastUsedDaysAgo = 0,
    )
    private val previewOats = FoodItem(
        id = "preview-oats",
        kind = FoodKind.Ingredient,
        name = "Rolled oats",
        brand = "Store brand",
        servingQuantity = 0.5,
        servingUnit = "cup",
        nutrients = Nutrients(calories = 150.0, proteinGrams = 5.0, carbohydrateGrams = 27.0, fatGrams = 3.0),
        frequency = 4,
        lastUsedDaysAgo = 3,
    )
    private val previewRecipeComponents = listOf(
        RecipeComponent(item = previewYogurt, amount = 1.0, unit = "cup"),
        RecipeComponent(item = previewBanana, amount = 1.0, unit = "banana"),
        RecipeComponent(item = previewOats, amount = 0.5, unit = "cup"),
    )
    private val previewRecipe = FoodItem(
        id = "preview-recipe",
        kind = FoodKind.Recipe,
        name = "Yogurt banana bowl",
        brand = "Home recipe",
        servingQuantity = 1.0,
        servingUnit = "bowl",
        nutrients = Nutrients(calories = 395.0, proteinGrams = 26.3, carbohydrateGrams = 62.0, fatGrams = 6.4),
        components = previewRecipeComponents,
        frequency = 3,
        lastUsedDaysAgo = 2,
    )

    val date: LocalDate = LocalDate.of(2026, 5, 21)
    val food: FoodItem = previewYogurt
    val foods: List<FoodItem> = listOf(previewYogurt, previewBanana, previewOats, previewRecipe)
    val recipeComponent: RecipeComponent = previewRecipeComponents.first()
    val recipeDraft: RecipeDraft = RecipeDraft(
        name = "Yogurt banana bowl",
        brand = "Home recipe",
        servingQuantity = "1",
        servingUnit = "bowl",
        components = previewRecipeComponents,
    )
    val diaryEntries: List<DiaryEntry> = listOf(
        DiaryEntry(
            id = "entry-breakfast",
            food = previewRecipe,
            date = date,
            meal = Meal.Breakfast,
            servingMultiplier = 1.0,
        ),
        DiaryEntry(
            id = "entry-snack",
            food = previewYogurt,
            date = date,
            meal = Meal.Snack,
            servingMultiplier = 1.5,
        ),
    )
    val totals: Totals = Totals(calories = 605.0, protein = 56.3, carbs = 74.0, fat = 10.9)
    val syncSettings: SyncSettings = SyncSettings(
        syncEnabled = true,
        backupMode = SyncSettings.BackupMode.AutomaticBackup,
        apiBaseUrl = "https://api.calorietracker.dev",
        lastSuccessfulSyncAt = "2026-05-21T09:30:00Z",
    )
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.Available

    @Composable
    fun Theme(content: @Composable () -> Unit) {
        CalorieTrackerTheme(dynamicColor = false) {
            Surface {
                content()
            }
        }
    }
}
