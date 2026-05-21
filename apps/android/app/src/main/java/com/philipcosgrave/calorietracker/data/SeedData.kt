package com.philipcosgrave.calorietracker.data

import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.RecipeComponent

val seedFoods = listOf(
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

fun seedRecipes(): List<FoodItem> {
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
