package com.philipcosgrave.calorietracker.data.remote

import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val CanadianNutrientFileBaseUrl =
    "https://food-nutrition.canada.ca/api/canadian-nutrient-file"

class CanadianNutrientFileLookupService {
    private var cachedFoods: List<CnfFoodSummary>? = null

    suspend fun searchFoodsByName(query: String, limit: Int = 8): List<FoodItem> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return@withContext emptyList()

        val foods = cachedFoods ?: fetchFoodSummaries().also { cachedFoods = it }
        val matches = foods
            .asSequence()
            .filter { it.description.contains(normalizedQuery, ignoreCase = true) }
            .sortedWith(
                compareBy<CnfFoodSummary>(
                    { !it.description.startsWith(normalizedQuery, ignoreCase = true) },
                    { it.description.length },
                ),
            )
            .take(limit)
            .toList()

        matches.mapNotNull { fetchFoodDetails(it) }
    }

    private fun fetchFoodSummaries(): List<CnfFoodSummary> {
        val response = getJson("$CanadianNutrientFileBaseUrl/food/?lang=en&type=json") ?: return emptyList()
        return jsonArrayFromResponse(response).mapNotNull { item ->
            val foodCode = item.optInt("food_code")
            val description = item.optString("food_description").trim()
            if (foodCode <= 0 || description.isBlank()) {
                null
            } else {
                CnfFoodSummary(foodCode, description)
            }
        }
    }

    private fun fetchFoodDetails(summary: CnfFoodSummary): FoodItem? {
        val nutrientResponse = getJson(
            "$CanadianNutrientFileBaseUrl/nutrientamount/?lang=en&type=json&id=${summary.foodCode}",
        ) ?: return null
        val nutrients = parseNutrients(jsonArrayFromResponse(nutrientResponse))

        val servingResponse = getJson(
            "$CanadianNutrientFileBaseUrl/servingsize/?lang=en&type=json&id=${summary.foodCode}",
        )
        val serving = parseServing(jsonArrayFromResponse(servingResponse))

        return FoodItem(
            id = "cnf-${summary.foodCode}",
            kind = FoodKind.Ingredient,
            name = summary.description,
            brand = "",
            barcode = "",
            servingQuantity = serving.quantity,
            servingUnit = serving.unit,
            nutrients = nutrients.scale(serving.conversionFactor),
            isUserCreated = false,
        )
    }

    private fun parseNutrients(items: List<JSONObject>): Nutrients {
        var calories = 0.0
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0

        items.forEach { item ->
            val name = item.optString("nutrient_web_name").trim()
            val value = item.optDouble("nutrient_value").takeUnless { it.isNaN() } ?: 0.0
            when {
                name.equals("Energy (kCal)", ignoreCase = true) ||
                    name.equals("Calories", ignoreCase = true) -> calories = value
                name.equals("Protein", ignoreCase = true) -> protein = value
                name.equals("Carbohydrate", ignoreCase = true) -> carbs = value
                name.equals("Fat (total)", ignoreCase = true) ||
                    name.equals("Total lipid (fat)", ignoreCase = true) ||
                    name.equals("Fat, total", ignoreCase = true) -> fat = value
            }
        }

        return Nutrients(
            calories = calories,
            proteinGrams = protein,
            carbohydrateGrams = carbs,
            fatGrams = fat,
        )
    }

    private fun parseServing(items: List<JSONObject>): CnfServing {
        val preferred = items.firstOrNull()
        if (preferred == null) {
            return CnfServing(quantity = 100.0, unit = "g", conversionFactor = 1.0)
        }

        val measureName = preferred.optString("measure_name").trim()
        val conversionFactor = preferred.optDouble("conversion_factor_value")
            .takeUnless { it.isNaN() || it <= 0.0 }
            ?: 1.0

        val quantityMatch = Regex("""^\s*([0-9]+(?:\.[0-9]+)?)\s*(.*)$""").find(measureName)
        val quantity = quantityMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: conversionFactor * 100.0
        val unit = quantityMatch?.groupValues?.getOrNull(2)?.trim().orEmpty().ifBlank { "g" }
        return CnfServing(quantity = quantity, unit = unit, conversionFactor = conversionFactor)
    }

    private fun jsonArrayFromResponse(response: Any?): List<JSONObject> =
        when (response) {
            is JSONArray -> List(response.length()) { index -> response.optJSONObject(index) }.filterNotNull()
            is JSONObject -> {
                when {
                    response.has("result") -> {
                        val result = response.opt("result")
                        when (result) {
                            is JSONArray -> List(result.length()) { index -> result.optJSONObject(index) }.filterNotNull()
                            is JSONObject -> listOf(result)
                            else -> emptyList()
                        }
                    }
                    response.has("food_code") -> listOf(response)
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }

    private fun getJson(url: String): Any? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        return try {
            if (connection.responseCode !in 200..299) {
                null
            } else {
                val body = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                when {
                    body.startsWith("[") -> JSONArray(body)
                    body.startsWith("{") -> JSONObject(body)
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private data class CnfFoodSummary(
        val foodCode: Int,
        val description: String,
    )

    private data class CnfServing(
        val quantity: Double,
        val unit: String,
        val conversionFactor: Double,
    )

    private fun Nutrients.scale(multiplier: Double): Nutrients =
        Nutrients(
            calories = calories * multiplier,
            proteinGrams = proteinGrams * multiplier,
            carbohydrateGrams = carbohydrateGrams * multiplier,
            fatGrams = fatGrams * multiplier,
        )
}
