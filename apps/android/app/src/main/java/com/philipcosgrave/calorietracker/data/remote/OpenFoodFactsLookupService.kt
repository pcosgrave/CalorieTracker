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
private const val OpenFoodFactsSearchUrl = "https://world.openfoodfacts.org/cgi/search.pl"
private const val OpenFoodFactsUserAgent = "CalorieTracker/0.1 (https://github.com/philipcosgrave/CalorieTracker)"

class OpenFoodFactsLookupService {
    suspend fun lookupFoodByBarcode(barcode: String): FoodItem? = withContext(Dispatchers.IO) {
        val json = getJsonObject("$OpenFoodFactsBaseUrl/$barcode.json") ?: return@withContext null
        if (json.optInt("status") != 1) {
            return@withContext null
        }

        val product = json.optJSONObject("product") ?: return@withContext null
        parseFood(product)
    }

    suspend fun searchFoodsByName(query: String, limit: Int = 8): List<FoodItem> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return@withContext emptyList()

        val encodedQuery = java.net.URLEncoder.encode(normalizedQuery, Charsets.UTF_8.name())
        val json = getJsonObject(
            "$OpenFoodFactsSearchUrl?search_terms=$encodedQuery&search_simple=1&action=process&json=1&page_size=$limit",
        ) ?: return@withContext emptyList()

        val products = json.optJSONArray("products") ?: return@withContext emptyList()
        buildList {
            for (index in 0 until products.length()) {
                val product = products.optJSONObject(index) ?: continue
                val parsed = parseFood(product) ?: continue
                if (parsed.name.contains(normalizedQuery, ignoreCase = true) ||
                    parsed.brand.contains(normalizedQuery, ignoreCase = true)
                ) {
                    add(parsed)
                }
            }
        }.distinctBy { it.barcode.ifBlank { "${it.name}|${it.brand}" } }
    }

    private fun parseFood(product: JSONObject): FoodItem? {
        val name = product.optString("product_name").trim()
        if (name.isBlank()) {
            return null
        }

        val barcode = product.optString("code").trim()
        val nutriments = product.optJSONObject("nutriments") ?: JSONObject()
        val servingQuantity = product.optDouble("serving_quantity").takeIf { it > 0 } ?: 100.0
        val servingUnit = product.optString("serving_quantity_unit").trim()
            .ifBlank { parseServingUnit(product.optString("serving_size")) }
            .ifBlank { "g" }

        return FoodItem(
            id = "off-${barcode.ifBlank { name.lowercase().replace("\\s+".toRegex(), "-") }}",
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
