package com.philipcosgrave.calorietracker.model

import java.time.LocalDate

enum class Meal(val label: String) {
    Breakfast("Breakfast"),
    Lunch("Lunch"),
    Dinner("Dinner"),
    Snack("Snack"),
}

enum class FoodKind {
    Ingredient,
    Recipe,
}

enum class SortMode(val label: String) {
    Recent("Recent"),
    MealTime("Frequent now"),
    Frequency("Frequency"),
    Alphabetical("Alphabetical"),
}

data class Nutrients(
    val calories: Double,
    val proteinGrams: Double = 0.0,
    val carbohydrateGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
)

data class FoodItem(
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
    val breakfastFrequency: Int = 0,
    val lunchFrequency: Int = 0,
    val dinnerFrequency: Int = 0,
    val snackFrequency: Int = 0,
    val lastUsedAt: String? = null,
    val lastUsedDaysAgo: Int = 0,
    val isUserCreated: Boolean = true,
)

data class RecipeComponent(
    val item: FoodItem,
    val amount: Double,
    val unit: String,
)

data class DiaryEntry(
    val id: String,
    val food: FoodItem,
    val date: LocalDate,
    val meal: Meal,
    val servingMultiplier: Double,
    val loggedAmount: Double = food.servingQuantity * servingMultiplier,
    val loggedUnit: String = food.servingUnit,
)

data class RecipeDraft(
    val name: String = "",
    val brand: String = "",
    val servingQuantity: String = "1",
    val servingUnit: String = "serving",
    val components: List<RecipeComponent> = emptyList(),
)

data class Totals(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
)

data class WeightEntry(
    val id: String,
    val date: LocalDate,
    val weightKg: Double,
)

enum class WeightChartRange(val label: String) {
    Daily("Daily"),
    Weekly("Weekly"),
    Monthly("Monthly"),
}

data class HealthDashboardMetrics(
    val steps: Long? = null,
    val heartRateBpm: Long? = null,
    val caloriesBurned: Double? = null,
)
