package com.philipcosgrave.calorietracker.data

import com.philipcosgrave.calorietracker.data.remote.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CnfNutritionParserTest {
    private fun values(calories: Double = 0.0) = listOf(
        "Energy (kCal)" to calories, "Protein" to 0.0,
        "Carbohydrate" to 0.1, "Total Fat" to 0.0,
    ).map { (name, value) -> JSONObject().put("nutrient_web_name", name).put("nutrient_value", value) }

    @Test fun zeroCaloriesAreValidAndDecimalsArePreserved() {
        val nutrients = parseCnfNutrients(values())!!
        assertEquals(0.0, nutrients.calories, 0.0)
        assertEquals(0.1, nutrients.carbohydrateGrams, 0.0)
    }
    @Test fun missingValuesAreNotInventedAsZero() {
        assertNull(parseCnfNutrients(emptyList()))
        for (i in 0..3) assertNull(parseCnfNutrients(values().filterIndexed { index, _ -> index != i }))
        assertNull(parseCnfNutrients(values(-1.0)))
    }
    @Test fun matchingHandlesWordOrderAndPluralsButPreservesPreparation() {
        assertTrue(matchesNutritionQuery("Chicken breast", "Chicken, breast, cooked"))
        assertTrue(matchesNutritionQuery("apple", "Apples, raw, with skin"))
        assertFalse(matchesNutritionQuery("Chicken raw", "Chicken, breast, cooked"))
        assertFalse(matchesNutritionQuery("apple", "Pineapple, raw"))
    }
}
