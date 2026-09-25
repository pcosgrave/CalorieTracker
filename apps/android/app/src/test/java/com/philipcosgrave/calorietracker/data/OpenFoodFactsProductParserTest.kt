package com.philipcosgrave.calorietracker.data

import com.philipcosgrave.calorietracker.data.remote.parseOpenFoodFactsProduct
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class OpenFoodFactsProductParserTest {
    @Test fun per100gNutritionIsScaledToReturnedServing() {
        val food = parseOpenFoodFactsProduct(JSONObject("""{"product_name":"Cereal","code":"123","serving_quantity":30,"serving_size":"30 g",
            "nutriments":{"energy-kcal_100g":400,"proteins_100g":10,"carbohydrates_100g":80,"fat_100g":4}}"""))!!
        assertEquals(30.0, food.servingQuantity, 0.001)
        assertEquals("g", food.servingUnit)
        assertEquals(120.0, food.nutrients.calories, 0.001)
        assertEquals(3.0, food.nutrients.proteinGrams, 0.001)
        assertEquals(24.0, food.nutrients.carbohydrateGrams, 0.001)
        assertEquals("123", food.barcode)
    }

    @Test fun actualZeroServingValuesDoNotFallBackToPer100g() {
        val food = parseOpenFoodFactsProduct(JSONObject("""{"product_name":"Drink","serving_quantity":250,"serving_quantity_unit":"ml",
            "nutriments":{"energy-kcal_serving":0,"energy-kcal_100g":1,"proteins_serving":0,"proteins_100g":1,
            "carbohydrates_serving":0,"carbohydrates_100g":1,"fat_serving":0,"fat_100g":1}}"""))!!
        assertEquals(0.0, food.nutrients.calories, 0.001)
        assertEquals(0.0, food.nutrients.proteinGrams, 0.001)
        assertEquals("ml", food.servingUnit)
    }

    @Test fun noServingSizeUses100gAndConvertsKilojoules() {
        val food = parseOpenFoodFactsProduct(JSONObject("""{"product_name":"Food",
            "nutriments":{"energy_100g":418.4,"proteins_100g":2,"carbohydrates_100g":20,"fat_100g":1}}"""))!!
        assertEquals(100.0, food.servingQuantity, 0.001)
        assertEquals(100.0, food.nutrients.calories, 0.001)
    }

    @Test fun perServingOnlyWithoutMeasuredWeightUsesOneServing() {
        val food = parseOpenFoodFactsProduct(JSONObject("""{"product_name":"Food",
            "nutriments":{"energy-kcal_serving":90,"proteins_serving":2,"carbohydrates_serving":20,"fat_serving":1}}"""))!!
        assertEquals("serving", food.servingUnit)
        assertEquals(1.0, food.servingQuantity, 0.001)
        assertEquals(90.0, food.nutrients.calories, 0.001)
    }

    @Test fun incompleteNutritionIsNotAutoCreatedAsZero() {
        assertNull(parseOpenFoodFactsProduct(JSONObject("""{"product_name":"Unknown"}""")))
        assertNull(parseOpenFoodFactsProduct(JSONObject("""{"product_name":"Partial","nutriments":{"energy-kcal_100g":30}}""")))
    }
}
