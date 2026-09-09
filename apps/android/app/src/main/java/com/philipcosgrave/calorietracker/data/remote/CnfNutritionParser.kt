package com.philipcosgrave.calorietracker.data.remote

import com.philipcosgrave.calorietracker.model.Nutrients
import org.json.JSONObject

/** CNF values are per 100 g edible portion. Missing nutrients are not zero. */
internal fun parseCnfNutrients(items: List<JSONObject>): Nutrients? {
    fun number(vararg names: String): Double? = items.firstNotNullOfOrNull { item ->
        if (names.any { it.equals(item.optString("nutrient_web_name").trim(), true) })
            item.optDouble("nutrient_value").takeIf { it.isFinite() && it >= 0 } else null
    }
    return Nutrients(
        calories = number("Energy (kCal)", "Calories") ?: return null,
        proteinGrams = number("Protein") ?: return null,
        carbohydrateGrams = number("Carbohydrate", "Carbohydrate, total (by difference)") ?: return null,
        fatGrams = number("Total Fat", "Fat (total)", "Total lipid (fat)", "Fat, total") ?: return null,
    )
}

internal fun nutritionSearchWords(text: String): List<String> =
    Regex("[a-z0-9]+").findAll(text.lowercase()).map { it.value.removeSuffix("s") }.toList()

/** Match words in any order (e.g. chicken breast against chicken, breast, cooked). */
internal fun matchesNutritionQuery(query: String, description: String): Boolean {
    val words = nutritionSearchWords(query)
    val target = nutritionSearchWords(description).toSet()
    return words.isNotEmpty() && words.all { it in target }
}
