package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class IngredientValidationTest {
    @Test fun zeroCalorieIngredientCanBeSavedAndLogged() {
        assertTrue(validIngredientFields("Vinegar", "100", "g", "0", "0", "0", "0", true))
        assertTrue(validIngredientFields("Vinegar", "100", "g", "0", "", "", ""))
        val food = FoodItem("vinegar", FoodKind.Ingredient, "Vinegar", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(0.0))
        val draft = linkReviewFood(PhotoFoodDraft(name = "Vinegar", grams = "20"), food)
        val entry = photoDiaryEntries(listOf(draft), listOf(food), LocalDate.now(), Meal.Lunch).single()
        assertEquals(0.0, entry.food.nutrients.calories * entry.servingMultiplier, 0.0)
    }
    @Test fun missingCaloriesAndInvalidNumbersStillRequireCorrection() {
        for (cal in listOf("", "-1", "NaN", "Infinity"))
            assertFalse(validIngredientFields("Food", "100", "g", cal, "0", "0", "0"))
        assertFalse(validIngredientFields("Food", "0", "g", "0", "0", "0", "0"))
        assertFalse(validIngredientFields("Food", "100", "g", "0", "", "0", "0", true))
    }
}
