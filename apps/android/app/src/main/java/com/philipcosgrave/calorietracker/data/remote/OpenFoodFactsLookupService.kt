package com.philipcosgrave.calorietracker.data.remote

import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val OpenFoodFactsBaseUrl = "https://world.openfoodfacts.org/api/v2/product"
private const val OpenFoodFactsUserAgent = "CalorieTracker/0.1 (https://github.com/philipcosgrave/CalorieTracker)"

class OpenFoodFactsLookupService {
    suspend fun lookupFoodByBarcode(barcode: String): FoodItem? = withContext(Dispatchers.IO) {
        val json = getJsonObject("$OpenFoodFactsBaseUrl/$barcode.json") ?: return@withContext null
        if (json.optInt("status") != 1) {
            return@withContext null
        }

        val product = json.optJSONObject("product") ?: return@withContext null
        val name = product.optString("product_name").trim()
        if (name.isBlank()) {
            return@withContext null
        }

        val nutriments = product.optJSONObject("nutriments") ?: JSONObject()
        val servingQuantity = product.optDouble("serving_quantity").takeIf { it > 0 } ?: 100.0
        val servingUnit = product.optString("serving_quantity_unit").trim()
            .ifBlank { parseServingUnit(product.optString("serving_size")) }
            .ifBlank { "g" }

        FoodItem(
            id = "off-$barcode",
            kind = FoodKind.Ingredient,
            name = name,
            brand = product.optString("brands").trim().ifBlank { "" },
            barcode = barcode,
            servingQuantity = servingQuantity,
            servingUnit = servingUnit,
            nutrients = Nutrients(
                calories = firstPositive(
                    nutriments.optDouble("energy-kcal_serving"),
                    nutriments.optDouble("energy-kcal"),
                    nutriments.optDouble("energy-kcal_100g"),
                    nutriments.optDouble("energy-kcal_value"),
                ),
                proteinGrams = firstPositive(
                    nutriments.optDouble("proteins_serving"),
                    nutriments.optDouble("proteins"),
                    nutriments.optDouble("proteins_100g"),
                ),
                carbohydrateGrams = firstPositive(
                    nutriments.optDouble("carbohydrates_serving"),
                    nutriments.optDouble("carbohydrates"),
                    nutriments.optDouble("carbohydrates_100g"),
                ),
                fatGrams = firstPositive(
                    nutriments.optDouble("fat_serving"),
                    nutriments.optDouble("fat"),
                    nutriments.optDouble("fat_100g"),
                ),
            ),
        )
    }

    private fun parseServingUnit(servingSize: String): String {
        val trimmed = servingSize.trim()
        if (trimmed.isBlank()) return ""
        val match = Regex("""^([0-9]+(?:\.[0-9]+)?)\s*([^\d].*)$""").find(trimmed)
        return match?.groupValues?.getOrNull(2)?.trim().orEmpty()
    }

    private fun firstPositive(vararg values: Double): Double {
        return values.firstOrNull { !it.isNaN() && it > 0 } ?: 0.0
    }

    private fun getJsonObject(url: String): JSONObject? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", OpenFoodFactsUserAgent)
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                null
            } else {
                JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
