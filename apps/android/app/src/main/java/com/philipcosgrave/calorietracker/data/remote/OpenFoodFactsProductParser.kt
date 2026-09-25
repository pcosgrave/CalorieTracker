package com.philipcosgrave.calorietracker.data.remote

import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import org.json.JSONObject

/** Keep every nutrient on the same serving basis; zero is a valid returned value. */
internal fun parseOpenFoodFactsProduct(product: JSONObject): FoodItem? {
    val name = product.optString("product_name").trim()
    if (name.isBlank()) return null
    val nutriments = product.optJSONObject("nutriments") ?: return null
    fun number(key: String): Double? = nutriments.optDouble(key).takeIf { it.isFinite() && it >= 0 }
    fun energy(suffix: String): Double? = number("energy-kcal$suffix")
        ?: number("energy-kj$suffix")?.div(4.184)
        ?: number("energy$suffix")?.div(4.184)
    val size = Regex("""(?i)(\d+(?:\.\d+)?)\s*(g|ml)\b""").find(product.optString("serving_size"))
    val quantity = product.optDouble("serving_quantity").takeIf { it.isFinite() && it > 0 }
        ?: size?.groupValues?.get(1)?.toDoubleOrNull()
    val unit = product.optString("serving_quantity_unit").trim().lowercase()
        .ifBlank { size?.groupValues?.get(2)?.lowercase().orEmpty() }
    val hasMeasuredServing = quantity != null && unit in listOf("g", "ml")
    val hasPer100 = energy("_100g") != null
    val hasPerServing = energy("_serving") != null
    if (!hasPer100 && !hasPerServing) return null
    val useServing = hasMeasuredServing || (!hasPer100 && hasPerServing)
    val servingQuantity = if (useServing && hasMeasuredServing) quantity!! else if (useServing) 1.0 else 100.0
    val servingUnit = if (hasMeasuredServing) unit else if (useServing) "serving" else
        if (product.optString("nutrition_data_per") == "100ml" || unit == "ml") "ml" else "g"
    fun nutrient(key: String): Double? {
        fun read(suffix: String) = if (key == "energy") energy(suffix) else number("$key$suffix")
        return if (useServing) read("_serving")
            ?: if (hasMeasuredServing) read("_100g")?.times(servingQuantity / 100.0) else null
        else read("_100g")
    }
    // Missing nutrition should open manual entry, not silently create a zero-nutrition food.
    val calories = nutrient("energy") ?: return null
    val protein = nutrient("proteins") ?: return null
    val carbs = nutrient("carbohydrates") ?: return null
    val fat = nutrient("fat") ?: return null
    val barcode = product.optString("code").trim()
    return FoodItem(
        id = "off-${barcode.ifBlank { name.lowercase().replace("\\s+".toRegex(), "-") }}",
        kind = FoodKind.Ingredient, name = name, brand = product.optString("brands").trim(), barcode = barcode,
        servingQuantity = servingQuantity, servingUnit = servingUnit,
        nutrients = Nutrients(calories, protein, carbs, fat), isUserCreated = false,
    )
}
