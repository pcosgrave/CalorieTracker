package com.philipcosgrave.calorietracker.domain

import org.json.JSONObject

data class NutritionLabel(
    val name: String?, val brand: String?,
    val quantity: String, val unit: String,
    val calories: String, val protein: String, val carbs: String, val fat: String,
)

/** All values belong to one printed column. Missing numbers remain blank, never invented zeros. */
fun parseNutritionLabel(text: String): NutritionLabel {
    val root = JSONObject(text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
    require(root.optBoolean("labelFound")) { "No readable nutrition label found. Please retake the photo." }
    fun number(key: String): Double? = root.optDouble(key).takeIf { it.isFinite() && it >= 0 }
    val quantity = number("quantity")?.takeIf { it > 0 }
    val unit = when (root.optString("unit").lowercase()) {
        "g", "gram", "grams" -> "gram"
        "ml" -> "ml"
        "serving" -> "serving"
        else -> ""
    }
    require(quantity != null && unit.isNotBlank()) { "The label's serving size is unclear. Retake the photo including the serving size and nutrition column." }
    val calories = number("caloriesKcal") ?: number("energyKj")?.div(4.184)
    require(listOf(calories, number("proteinGrams"), number("carbohydrateGrams"), number("fatGrams")).any { it != null }) {
        "The nutrition numbers could not be read. Please take a clearer photo."
    }
    fun optionalText(key: String) = if (root.isNull(key)) null else root.optString(key).trim().takeIf { it.isNotBlank() }?.take(200)
    // Preserve label decimals; display rounding for meal totals is inappropriate for transcribing a label.
    fun value(number: Double?) = number?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty()
    return NutritionLabel(optionalText("name"), optionalText("brand"), value(quantity), unit,
        value(calories), value(number("proteinGrams")), value(number("carbohydrateGrams")), value(number("fatGrams")))
}
