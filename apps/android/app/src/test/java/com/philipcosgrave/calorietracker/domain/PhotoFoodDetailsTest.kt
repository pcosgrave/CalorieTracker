package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PhotoFoodDetailsTest {
    private val foods = listOf(
        FoodItem("apple", FoodKind.Ingredient, "Apple", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(52.0)),
        FoodItem("banana", FoodKind.Ingredient, "Banana", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(89.0)),
    )
    private val detail = """{"estimatedGrams":180,"scale":null}"""

    @Test fun twoFoodsProduceTwoReviewRows() = runBlocking {
        val rows = readPhotoFoodDetails(listOf("apple", "banana"), foods) { _, _, _ -> detail }
        assertEquals(listOf("Apple", "Banana"), rows.map { it.name })
        assertTrue(rows.all { it.isValid(foods) })
    }

    @Test fun wholeMealReturnedForAppleSelectsAppleAndItsScaleIndex() {
        val row = parsePhotoFoodDetails("apple", """{"items":[{"name":"banana","estimatedGrams":90},{"name":"apple","estimatedGrams":180}],"scale":{"value":200,"unit":"g","itemIndex":1}}""", foods)
        assertEquals("Apple", row.name)
        assertEquals("200", row.grams)
    }

    @Test fun anotherFoodsScaleCannotBeAppliedToApple() {
        val row = parsePhotoFoodDetails("apple", """{"items":[{"name":"banana","estimatedGrams":90},{"name":"apple","estimatedGrams":180}],"scale":{"value":200,"unit":"g","itemIndex":0}}""", foods)
        assertEquals("180", row.grams)
    }

    @Test fun badAppleResponseDoesNotDiscardBanana() = runBlocking {
        var appleAttempts = 0
        val rows = readPhotoFoodDetails(listOf("apple", "banana"), foods) { name, _, _ ->
            if (name == "apple") { appleAttempts++; """{"items":[]}""" } else detail
        }
        assertEquals(2, appleAttempts)
        assertEquals(listOf("Apple", "Banana"), rows.map { it.name })
        assertEquals("", rows[0].grams)
        assertFalse(rows[0].isValid(foods))
        assertTrue(rows[1].isValid(foods))
    }

    @Test fun truncatedResponseRetriesWithoutDuplicatingTheFood() = runBlocking {
        val rows = readPhotoFoodDetails(listOf("apple"), foods) { _, _, retry -> if (retry) detail else "{" }
        assertEquals(1, rows.size)
        assertTrue(rows.single().isValid(foods))
    }

    @Test fun cancellationStillStopsAnalysis() = runBlocking {
        try {
            readPhotoFoodDetails(listOf("apple"), foods) { _, _, _ -> throw CancellationException("cancel") }
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }
}
