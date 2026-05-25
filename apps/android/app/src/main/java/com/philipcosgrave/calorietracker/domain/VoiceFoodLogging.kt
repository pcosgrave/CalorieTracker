package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.Meal
import java.time.LocalTime

data class VoiceFoodCommand(
    val amount: Double,
    val unit: String,
    val ingredientQuery: String,
)

fun parseVoiceFoodCommand(spokenText: String): VoiceFoodCommand? {
    val normalized = spokenText
        .trim()
        .lowercase()
        .removePrefix("add ")
        .replace(Regex("\\s+"), " ")

    val match = Regex("""^(\d+(?:\.\d+)?)\s+(.+?)\s+([a-z][a-z\s-]*)$""").matchEntire(normalized) ?: return null
    val amount = match.groupValues[1].toDoubleOrNull() ?: return null
    val rawUnit = match.groupValues[2].trim()
    val ingredient = match.groupValues[3].trim()
    val unit = normalizeVoiceUnit(rawUnit) ?: return null

    if (ingredient.isBlank()) return null
    return VoiceFoodCommand(
        amount = amount,
        unit = unit,
        ingredientQuery = ingredient,
    )
}

fun inferMealForTime(time: LocalTime): Meal =
    when {
        !time.isBefore(LocalTime.of(7, 0)) && time.isBefore(LocalTime.of(10, 0)) -> Meal.Breakfast
        !time.isBefore(LocalTime.of(10, 0)) && time.isBefore(LocalTime.of(14, 0)) -> Meal.Lunch
        !time.isBefore(LocalTime.of(16, 0)) && time.isBefore(LocalTime.of(20, 0)) -> Meal.Dinner
        else -> Meal.Snack
    }

private fun normalizeVoiceUnit(rawUnit: String): String? {
    val compact = rawUnit.trim().lowercase()
    val knownAliases = mapOf(
        "g" to "gram",
        "gram" to "gram",
        "grams" to "gram",
        "kg" to "kg",
        "kilogram" to "kg",
        "kilograms" to "kg",
        "mg" to "milligram",
        "milligram" to "milligram",
        "milligrams" to "milligram",
        "oz" to "oz",
        "ounce" to "oz",
        "ounces" to "oz",
        "lb" to "lb",
        "lbs" to "lb",
        "pound" to "lb",
        "pounds" to "lb",
        "ml" to "ml",
        "milliliter" to "ml",
        "milliliters" to "ml",
        "liter" to "liter",
        "liters" to "liter",
        "l" to "liter",
        "cup" to "cup",
        "cups" to "cup",
        "tablespoon" to "tbsp",
        "tablespoons" to "tbsp",
        "tbsp" to "tbsp",
        "teaspoon" to "tsp",
        "teaspoons" to "tsp",
        "tsp" to "tsp",
        "serving" to "serving",
        "servings" to "serving",
        "egg" to "eggs",
        "eggs" to "eggs",
        "wrap" to "wrap",
        "wraps" to "wrap",
        "bowl" to "bowl",
        "bowls" to "bowl",
        "medium" to "medium",
    )
    return knownAliases[compact] ?: measurementUnits.firstOrNull { it.equals(compact, ignoreCase = true) }
}
