package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*

data class RecipeTextIngredient(val name: String, val amount: Double?, val unit: String?)
fun parseRecipeIngredient(line: String): RecipeTextIngredient {
    val fractions = mapOf('½' to " 1/2", '¼' to " 1/4", '¾' to " 3/4", '⅓' to " 1/3", '⅔' to " 2/3", '⅛' to " 1/8")
    val normalized = line.trim().trimStart('•', '-', ' ').map { fractions[it] ?: it.toString() }.joinToString("").trim()
    val amountMatch = Regex("^(\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:\\.\\d+)?)\\s*(.*)$").find(normalized)
        ?: return RecipeTextIngredient(foodTitle(normalized), null, null)
    val amount = amountMatch.groupValues[1].split(Regex("\\s+")).sumOf { part ->
        if ('/' in part) { val pieces = part.split('/'); pieces[0].toDouble() / pieces[1].toDouble() } else part.toDouble()
    }.takeIf { it.isFinite() && it > 0 }
    val rest = amountMatch.groupValues[2]
    val units = mapOf("g" to "g", "gram" to "g", "grams" to "g", "kg" to "kg", "oz" to "oz", "ounces" to "oz", "lb" to "lb", "lbs" to "lb", "ml" to "ml", "l" to "liter", "cup" to "cup", "cups" to "cup", "tbsp" to "tbsp", "tablespoon" to "tbsp", "tablespoons" to "tbsp", "tsp" to "tsp", "teaspoon" to "tsp", "teaspoons" to "tsp")
    val first = rest.substringBefore(' ').trimEnd('.').lowercase()
    val unit = units[first]
    val name = if (unit != null) rest.substringAfter(' ', "") else rest
    return RecipeTextIngredient(foodTitle(name.trim().removePrefix("of ")), amount, unit)
}

fun recipeComponentFromLine(line: String, food: FoodItem): RecipeComponent? {
    val parsed = parseRecipeIngredient(line)
    val amount = parsed.amount ?: return null
    // A bare count cannot be interpreted as grams or millilitres.
    if (parsed.unit == null && (convertAmount(1.0, food.servingUnit, "g") != null || convertAmount(1.0, food.servingUnit, "ml") != null)) return null
    val unit = parsed.unit ?: food.servingUnit
    if (food.amountInBaseUnits(amount, unit) == null) return null
    return RecipeComponent(food, amount, unit)
}

fun parseRecipeText(text: String, foods: List<FoodItem>): RecipeDraft {
    val lines = text.lines().map(String::trim).filter(String::isNotBlank)
    var name = ""
    var servings = "1"
    var section = "title"
    val ingredients = mutableListOf<String>()
    val instructions = mutableListOf<String>()
    for (line in lines) {
        val lower = line.lowercase().trimEnd(':')
        if (lower in listOf("ingredients", "ingredient list")) { section = "ingredients"; continue }
        if (lower in listOf("instructions", "directions", "method", "preparation")) { section = "instructions"; continue }
        val servingMatch = Regex("(?i)(?:serves|servings|yield)\\s*:?\\s*(\\d+)|^(\\d+)\\s+servings$").find(line)
        if (servingMatch != null) { servings = servingMatch.groupValues.drop(1).first { it.isNotBlank() }; continue }
        if (name.isBlank() && section == "title") { name = line; continue }
        if (section == "instructions") {
            if (Regex("^\\d+[.)]\\s").containsMatchIn(line) || instructions.isEmpty()) instructions += line.replace(Regex("^\\d+[.)]\\s*"), "")
            else instructions[instructions.lastIndex] = instructions.last() + " " + line
        } else if (section == "ingredients" || parseRecipeIngredient(line).amount != null) ingredients += line
    }
    fun normalized(value: String) = value.lowercase().replace(Regex("[^a-z0-9 ]"), " ").split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { it.removeSuffix("s") }
    val matched = mutableListOf<RecipeComponent>()
    val unresolved = mutableListOf<String>()
    ingredients.forEach { line ->
        val item = parseRecipeIngredient(line)
        val food = foods.singleOrNull { normalized(it.name) == normalized(item.name) }
        val component = food?.let { recipeComponentFromLine(line, it) }
        if (component != null) matched += component else unresolved += line
    }
    return RecipeDraft(name = foodTitle(name), servingQuantity = servings, servingUnit = "serving", components = matched, instructions = instructions, unresolvedIngredients = unresolved)
}
