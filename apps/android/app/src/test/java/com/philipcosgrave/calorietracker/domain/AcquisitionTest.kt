package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.ui.*
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AcquisitionTest {
    private val food = FoodItem("apple", FoodKind.Ingredient, "Apple", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(52.0))
    private val date = LocalDate.of(2026, 9, 8)
    private val scope = SaverScope { true }
    @Test fun destinationRestoresDateMealAndRecipeContext() {
        for (recipe in listOf(false, true)) {
            val destination = AcquisitionDestination(date, Meal.Dinner, recipe)
            val saved = with(AcquisitionSaver) { scope.save(destination) }!!
            assertEquals(destination, AcquisitionSaver.restore(saved))
        }
    }
    @Test fun searchAndCaptureResultsUseRequestedMealAndDate() {
        val result = DiaryEntry("result", food, LocalDate.now(), Meal.Breakfast, 1.5, 150.0, "g")
        val actual = acquisitionEntries(AcquisitionDestination(date, Meal.Lunch), listOf(result)).single()
        assertEquals(date, actual.date)
        assertEquals(Meal.Lunch, actual.meal)
        assertEquals(78.0, actual.food.nutrients.calories * actual.servingMultiplier, 0.0001)
        assertEquals(result.food, actual.food)
    }
    @Test fun recipeResultsArePortionBearingComponents() {
        val result = DiaryEntry("result", food, date, Meal.Lunch, 1.5, 150.0, "g")
        assertEquals(listOf(RecipeComponent(food, 150.0, "g")), acquisitionIngredients(listOf(result)))
    }
    @Test fun recipeDraftAndSelectedFoodSurviveRestoration() {
        val draft = RecipeDraft("Lunch", "", "2", "serving", listOf(RecipeComponent(food, 150.0, "g")))
        assertEquals(draft, RecipeDraftSaver.restore(with(RecipeDraftSaver) { scope.save(draft) }!!))
        assertEquals(food, SelectedFoodSaver.restore(with(SelectedFoodSaver) { scope.save(food) }!!))
    }
    @Test fun localVoiceKeepsUnknownFoodForReviewWithoutInventingNutrition() {
        val drafts = voiceReviewDrafts("100 grams apple and 50 grams mystery", listOf(food))
        assertEquals(2, drafts.size)
        assertEquals(food.id, drafts[0].matchedFoodId)
        assertEquals("100", drafts[0].grams)
        assertNull(drafts[1].matchedFoodId)
        assertTrue(drafts[1].calories.isBlank())
        assertFalse(drafts[1].isValid(listOf(food)))
    }
}
