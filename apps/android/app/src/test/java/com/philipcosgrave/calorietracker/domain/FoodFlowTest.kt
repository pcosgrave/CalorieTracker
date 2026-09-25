package com.philipcosgrave.calorietracker.domain
import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.data.local.*
import com.philipcosgrave.calorietracker.ui.*
import androidx.compose.runtime.saveable.SaverScope
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FoodFlowTest {
    private val ham = FoodItem("ham", FoodKind.Ingredient, "Ham", servingQuantity = 2.0, servingUnit = "slice", nutrients = Nutrients(70.0, 12.0, 1.0, 2.0), servingWeightGrams = 55.0)
    @Test fun servingsAndGramsStayEquivalent() {
        assertEquals(1.0, ham.amountInBaseUnits(27.5, "g")!!, .00001)
        assertEquals(55.0, ham.amountInGrams(2.0, "slice")!!, .00001)
        assertEquals(35.0, ham.nutrients.calories * ham.amountInBaseUnits(27.5, "g")!! / ham.servingQuantity, .00001)
        assertNull(ham.copy(servingWeightGrams = null).amountInBaseUnits(27.5, "g"))
        assertEquals(listOf("slice", "g"), ham.amountUnits())
    }
    @Test fun optionalFieldsRoundTripAndLegacyFoodsStillLoad() {
        val enriched = ham.copy(photoPath = "/private/food.jpg", instructions = listOf("Cook", "Serve"), description = "Lunch")
        assertEquals(enriched, foodItemFromJsonString(enriched.toJsonString()))
        val legacy = JSONObject(ham.toJsonString()).apply { remove("servingWeightGrams"); remove("photoPath"); remove("instructions"); remove("description") }
        val restored = foodItemFromJsonString(legacy.toString())
        assertEquals(ham.nutrients, restored.nutrients)
        assertEquals(2.0, restored.servingQuantity, .0)
        assertNull(restored.servingWeightGrams)
        assertNull(restored.photoPath)
        assertTrue(restored.instructions.isEmpty())
    }
    @Test fun recipeImportPreservesUnknownIngredientsAndInstructions() {
        val chicken = FoodItem("chicken", FoodKind.Ingredient, "Chicken Breast", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(165.0))
        val recipe = parseRecipeText("""Creamy Chicken
Servings: 4
Ingredients
500 g chicken breast
1 1/2 cups cream
Salt to taste
Instructions
1. Cook chicken.
Stir until cooked.
2. Add cream.
""", listOf(chicken))
        assertEquals("Creamy Chicken", recipe.name)
        assertEquals("4", recipe.servingQuantity)
        assertEquals(500.0, recipe.components.single().amount, .0)
        assertEquals(2, recipe.unresolvedIngredients.size)
        assertEquals(listOf("Cook chicken. Stir until cooked.", "Add cream."), recipe.instructions)
        assertEquals(825.0, totalComponents(recipe.components).calories, .01)
    }
    @Test fun fractionAndCountParsingDoesNotInventWeight() {
        assertEquals(1.5, parseRecipeIngredient("1½ cups cream").amount!!, .00001)
        assertEquals(.25, parseRecipeIngredient("¼ tsp salt").amount!!, .00001)
        val gramsFood = ham.copy(servingUnit = "g", servingQuantity = 100.0, servingWeightGrams = null)
        assertNull(recipeComponentFromLine("2 ham", gramsFood))
        assertEquals(2.0, recipeComponentFromLine("2 ham", ham)!!.amount, .0)
    }
    @Test fun recipeDraftRestoresImagesInstructionsAndUnresolvedRows() {
        val draft = RecipeDraft(name = "Recipe", photoPath = "/private/photo.jpg", instructions = listOf("Cook", "Serve"), unresolvedIngredients = listOf("1 tsp spice"), components = listOf(RecipeComponent(ham, 2.0, "slice")))
        val scope = SaverScope { true }
        val saved = with(RecipeDraftSaver) { scope.save(draft) }!!
        assertEquals(draft, RecipeDraftSaver.restore(saved))
        val food = draft.copy(unresolvedIngredients = emptyList()).toFoodItem("recipe")
        assertEquals(draft.instructions, food.toRecipeDraft().instructions)
        assertEquals(draft.photoPath, food.photoPath)
    }
}
