package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FoodReviewTest {
    @Test fun capitalizesWordStartsWithoutDamagingAcronyms() {
        assertEquals("Grilled Chicken (Cooked)", foodTitle(" grilled chicken (cooked) "))
        assertEquals("USDA Low-Fat Milk", foodTitle("USDA low-fat milk"))
    }
    @Test fun dictatedVolumeIsNotTreatedAsGrams() {
        val food = FoodItem("milk", FoodKind.Ingredient, "Milk", servingQuantity = 100.0, servingUnit = "ml", nutrients = Nutrients(50.0))
        val draft = PhotoFoodDraft(name = "Milk", grams = "250", matchedFoodId = food.id, amountUnit = "ml")
        val entry = photoDiaryEntries(listOf(draft), listOf(food), LocalDate.now(), Meal.Breakfast).single()
        assertEquals("ml", entry.loggedUnit)
        assertEquals(250.0, entry.loggedAmount, 0.001)
        assertEquals(2.5, entry.servingMultiplier, 0.001)
        assertFalse(draft.copy(amountUnit = "g").isValid(listOf(food)))
    }
    @Test fun dictatedServingsUseExistingFoodNutrition() {
        val food = FoodItem("bar", FoodKind.Ingredient, "Bar", servingQuantity = 1.0, servingUnit = "serving", nutrients = Nutrients(200.0))
        val draft = PhotoFoodDraft(name = "Bar", grams = "2", matchedFoodId = food.id, amountUnit = "serving")
        val entry = photoDiaryEntries(listOf(draft), listOf(food), LocalDate.now(), Meal.Snack).single()
        assertEquals(400.0, entry.servingMultiplier * entry.food.nutrients.calories, 0.001)
    }
}
