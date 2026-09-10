package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.data.local.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReferenceFoodTest {
    private val red = ReferenceFood("cnf:1", "CNF", "1", "Peppers, sweet, red, raw", "peppers sweet red raw", "Vegetables", Nutrients(31.0, 1.0, 6.0, .3, mapOf("sodium" to 4.0)), listOf(ReferenceServing("s", "1 medium", 119.0)))
    private val green = red.copy(id = "cnf:2", sourceFoodId = "2", name = "Peppers, sweet, green, raw")
    @Test fun aliasesAndExplicitAttributesRankCorrectly() {
        assertTrue(FoodSearchMatching.score("large red bell pepper", red.name) > FoodSearchMatching.score("large red bell pepper", green.name))
        assertTrue(FoodSearchMatching.score("cilantro", "Coriander leaves, raw") > 0)
        assertTrue(FoodSearchMatching.score("scallions", "Onions, green, raw") > 0)
        assertTrue(FoodSearchMatching.score("minced beef", "Beef, ground, lean, raw") > 0)
        assertTrue(FoodSearchMatching.score("aple", "Apple, raw") > 0)
        assertEquals(0, FoodSearchMatching.score("apple", "Pineapple, raw"))
    }
    @Test fun importedServingsAndOptionalNutritionRoundTrip() {
        val local = red.toLocalFood("new-local-id")
        val restored = foodItemFromJsonString(local.toJsonString())
        assertEquals(local, restored)
        assertEquals("2026", restored.sourceVersion)
        assertEquals(119.0, restored.amountInBaseUnits(1.0, "1 medium")!!, .00001)
        assertEquals(238.0, restored.amountInGrams(2.0, "1 medium")!!, .00001)
        assertEquals(8.0, restored.nutrients.scale(2.0).additional.getValue("sodium"), .0)
        assertFalse(restored.nutrients.additional.containsKey("iron"))
        assertEquals(31.0, red.nutritionPer100g.calories, .0)
        assertEquals(0.0, restored.copy(nutrients = Nutrients(0.0)).nutrients.calories, .0)
    }
    private fun repository(foods: List<ReferenceFood>) = object : ReferenceFoodRepository {
        override suspend fun search(query: String) = foods.filter { FoodSearchMatching.score(query, it.name) > 0 }
        override suspend fun getFood(id: String) = foods.firstOrNull { it.id == id }
    }
    @Test fun strongMatchesSkipAiAndImportedFoodsAreDeduplicated() = runBlocking {
        var calls = 0
        val search = IngredientSearch(repository(listOf(red, green)), FoodNameNormalizer { calls++; null })
        assertEquals(1, search.search("red bell pepper", emptyList()).size)
        assertEquals(0, calls)
        assertTrue(search.search("red bell pepper", listOf(red.toLocalFood("local"))).isEmpty())
        assertEquals(0, calls)
    }
    @Test fun weakQueryUsesCachedAiAndFailureKeepsDeterministicResults() = runBlocking {
        var calls = 0
        val search = IngredientSearch(repository(listOf(red)), FoodNameNormalizer { calls++; NormalizedFoodName("sweet pepper", listOf("red"), .9) })
        assertEquals(1, search.search("capsicum", emptyList()).size)
        assertEquals(1, search.search("capsicum", emptyList()).size)
        assertEquals(1, calls)
        val failing = IngredientSearch(repository(listOf(red)), FoodNameNormalizer { error("No AICore") })
        assertTrue(failing.search("unrecognized", emptyList()).isEmpty())
    }
    @Test fun ambiguousSearchKeepsChoicesAndRecipeExtrasRequireCompleteData() = runBlocking {
        val creams = listOf(red.copy(name = "Cream, 10%"), green.copy(name = "Cream, 35%"))
        assertEquals(2, IngredientSearch(repository(creams), FoodNameNormalizer { error("Must not call") }).search("cream", emptyList()).size)
        val local = red.toLocalFood("local")
        val recipe = RecipeDraft(name = "Peppers", components = listOf(RecipeComponent(local, 50.0, "g")), prepMinutes = 5, totalMinutes = 10).toFoodItem()
        assertEquals(2.0, recipe.nutrients.additional.getValue("sodium"), .0)
        assertEquals(5, recipe.toRecipeDraft().prepMinutes)
        assertTrue(componentAdditionalNutrients(recipe.components + RecipeComponent(local.copy(nutrients = Nutrients(31.0)), 50.0, "g")).isEmpty())
    }
}
