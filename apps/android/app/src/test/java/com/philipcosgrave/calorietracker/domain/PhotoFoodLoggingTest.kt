package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PhotoFoodLoggingTest {
    private val items = """[{"name":"Rice","estimatedGrams":100,"nutritionPer100g":{"calories":130,"protein":3,"carbs":28,"fat":0}},
        {"name":"Chicken","estimatedGrams":80,"nutritionPer100g":{"calories":165,"protein":31,"carbs":0,"fat":4}}]"""
    private fun parse(scale: String) = parsePhotoFoods("""{"items":$items,"scale":$scale}""", emptyList())

    @Test fun scaleAppliesOnlyToAttributedFoodAndConvertsKilograms() {
        val drafts = parse("""{"value":0.215,"unit":"kg","itemIndex":1}""")
        assertEquals(100.0, drafts[0].grams.toDouble(), 0.001)
        assertEquals(215.0, drafts[1].grams.toDouble(), 0.001)
        assertTrue(drafts[1].weightSource.startsWith("Scale reading"))
    }

    @Test fun plateTotalIsNeverAppliedToEachFood() {
        val drafts = parse("""{"value":320,"unit":"g","itemIndex":-1}""")
        assertEquals(listOf(100.0, 80.0), drafts.map { it.grams.toDouble() })
        assertTrue(drafts.all { it.notes.contains("320") && it.weightSource == "Estimated weight" })
    }

    @Test fun unreadableOrUnsupportedScaleRetainsVisualEstimate() {
        for (scale in listOf("null", """{"value":-1,"unit":"g","itemIndex":0}""", """{"value":10,"unit":"unknown","itemIndex":0}""")) {
            assertEquals(100.0, parse(scale)[0].grams.toDouble(), 0.001)
        }
        assertEquals(56.7, parse("""{"value":2,"unit":"oz","itemIndex":0}""")[0].grams.toDouble(), 0.01)
    }

    @Test fun editedWeightsUseEachSavedFoodsNutrition() {
        val foods = listOf(
            FoodItem("rice", FoodKind.Ingredient, "Rice", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(120.0)),
            FoodItem("chicken", FoodKind.Ingredient, "Chicken", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(160.0)),
        )
        val drafts = parsePhotoFoods("""{"items":$items}""", foods).map { it.copy(grams = "200") }
        val date = LocalDate.of(2026, 9, 8)
        val entries = photoDiaryEntries(drafts, foods, date, Meal.Dinner)
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.date == date && it.meal == Meal.Dinner && it.loggedAmount == 200.0 })
        assertEquals(240.0, entries[0].food.nutrients.calories * entries[0].servingMultiplier, 0.001)
        assertEquals(entries.map { it.id }, photoDiaryEntries(drafts, foods, date, Meal.Dinner).map { it.id })
        assertEquals(320.0, entries[1].food.nutrients.calories * entries[1].servingMultiplier, 0.001)
        assertEquals(foods, entries.map { it.food })
        assertTrue(drafts.all { it.calories.isBlank() && it.protein.isBlank() && it.carbs.isBlank() && it.fat.isBlank() })
    }

    @Test fun savedNutritionWinsAndIsConvertedFromKilogramServing() {
        val saved = FoodItem("rice", FoodKind.Ingredient, "Rice", servingQuantity = 1.0, servingUnit = "kg", nutrients = Nutrients(1300.0))
        val draft = parsePhotoFoods("""{"items":$items}""", listOf(saved)).first()
        assertEquals("rice", draft.matchedFoodId)
        val entry = photoDiaryEntries(listOf(draft), listOf(saved), LocalDate.now(), Meal.Lunch).single()
        assertEquals(saved, entry.food)
        assertEquals(0.1, entry.servingMultiplier, 0.0001)
    }

    @Test fun gramAliasesWorkWithExistingFoodsAndUnitPicker() {
        assertEquals(100.0, convertAmount(100.0, "g", "gram")!!, 0.001)
        assertEquals(1.0, convertAmount(1000.0, "mg", "g")!!, 0.001)
        assertTrue(compatibleMeasurementUnits("g").containsAll(listOf("g", "kg", "oz")))
        assertNull(convertAmount(100.0, "g", "ml"))
    }

    @Test fun ambiguousNamesAreNotSilentlyMatched() {
        val food = FoodItem("one", FoodKind.Ingredient, "Rice", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(130.0))
        val result = parsePhotoFoods("""{"items":$items}""", listOf(food, food.copy(id = "two")))
        assertNull(result.first().matchedFoodId)
    }

    @Test fun missingNutritionAndInvalidWeightsCannotBeSaved() {
        val draft = parse("null").first()
        for (weight in listOf("", "0", "-1", "NaN", "Infinity", "100001")) {
            assertFalse(draft.copy(grams = weight).isValid(emptyList()))
        }
        assertFalse(draft.copy(calories = "").isValid(emptyList()))
        assertFalse(draft.copy(protein = "NaN").isValid(emptyList()))
        val missing = parsePhotoFoods("""{"items":[{"name":"Mystery food"}]}""", emptyList()).single()
        assertEquals("", missing.grams)
        assertEquals("", missing.calories)
        assertFalse(missing.isValid(emptyList()))
    }

    @Test fun emptyAndMalformedResponsesAreNotSaved() {
        assertTrue(parsePhotoFoods("""{"items":[]}""", emptyList()).isEmpty())
        assertThrows(Exception::class.java) { parsePhotoFoods("not JSON", emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { photoDiaryEntries(emptyList(), emptyList(), LocalDate.now(), Meal.Snack) }
    }
    @Test fun unknownFoodCannotBeSavedEvenWithModelNutrition() {
        val draft = PhotoFoodDraft(name = "Unknown", grams = "100", calories = "130", protein = "3", carbs = "28", fat = "0")
        assertFalse(draft.isValid(emptyList()))
        assertThrows(IllegalArgumentException::class.java) {
            photoDiaryEntries(listOf(draft), emptyList(), LocalDate.now(), Meal.Lunch)
        }
    }

    @Test fun addingFoodLinksSavedNutritionAndPreservesCompatiblePhotoWeight() {
        val draft = PhotoFoodDraft(name = "rice", grams = "215", weightSource = "Scale reading")
        val food = FoodItem("new", FoodKind.Ingredient, "Rice", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(120.0))
        val linked = linkReviewFood(draft, food)
        assertEquals(draft.id, linked.id)
        assertEquals("215", linked.grams)
        assertEquals(draft.weightSource, linked.weightSource)
        assertTrue(linked.isValid(listOf(food)))
        val entry = photoDiaryEntries(listOf(linked), listOf(food), LocalDate.now(), Meal.Lunch).single()
        assertEquals(258.0, entry.food.nutrients.calories * entry.servingMultiplier, 0.001)
    }

    @Test fun incompatibleServingUnitsRequireAnAmountInsteadOfReusingGrams() {
        val food = FoodItem("new", FoodKind.Ingredient, "Soup", servingQuantity = 1.0, servingUnit = "serving", nutrients = Nutrients(120.0))
        val linked = linkReviewFood(PhotoFoodDraft(name = "Soup", grams = "215"), food)
        assertEquals("", linked.grams)
        assertEquals("serving", linked.amountUnit)
        assertFalse(linked.isValid(listOf(food)))
        assertTrue(linked.copy(grams = "1").isValid(listOf(food)))
    }

    @Test fun knownServingBasedFoodIsMatchedWithoutInventingAGramsConversion() {
        val food = FoodItem("rice", FoodKind.Ingredient, "Rice", servingQuantity = 1.0, servingUnit = "serving", nutrients = Nutrients(120.0))
        val draft = parsePhotoFoods("""{"items":$items}""", listOf(food)).first()
        assertEquals(food.id, draft.matchedFoodId)
        assertEquals("serving", draft.amountUnit)
        assertEquals("", draft.grams)
        assertFalse(draft.isValid(listOf(food)))
    }

}
