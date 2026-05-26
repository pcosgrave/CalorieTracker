package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.Meal
import java.time.LocalTime
import java.util.Locale

data class VoiceFoodCommand(
    val amount: Double?,
    val unit: String?,
    val ingredientQuery: String,
    val mealOverride: Meal? = null,
)

private data class NumberParseResult(
    val value: Double,
    val consumedTokens: Int,
)

private val voiceUnitAliases = mapOf(
    "g" to "gram",
    "gram" to "gram",
    "grams" to "gram",
    "kg" to "kg",
    "kilo" to "kg",
    "kilos" to "kg",
    "kilogram" to "kg",
    "kilograms" to "kg",
    "mg" to "milligram",
    "milligram" to "milligram",
    "milligrams" to "milligram",
    "oz" to "oz",
    "ounce" to "oz",
    "ounces" to "oz",
    "fluid ounce" to "fl oz",
    "fluid ounces" to "fl oz",
    "fl oz" to "fl oz",
    "lb" to "lb",
    "lbs" to "lb",
    "pound" to "lb",
    "pounds" to "lb",
    "ml" to "ml",
    "milliliter" to "ml",
    "milliliters" to "ml",
    "l" to "liter",
    "liter" to "liter",
    "liters" to "liter",
    "cup" to "cup",
    "cups" to "cup",
    "mug" to "cup",
    "mugs" to "cup",
    "glass" to "cup",
    "glasses" to "cup",
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
    "slice" to "slice",
    "slices" to "slice",
    "piece" to "piece",
    "pieces" to "piece",
    "bowl" to "bowl",
    "bowls" to "bowl",
    "wrap" to "wrap",
    "wraps" to "wrap",
    "medium" to "medium",
)

private val spokenNumberTokens = mapOf(
    "zero" to 0.0,
    "a" to 1.0,
    "an" to 1.0,
    "one" to 1.0,
    "two" to 2.0,
    "three" to 3.0,
    "four" to 4.0,
    "five" to 5.0,
    "six" to 6.0,
    "seven" to 7.0,
    "eight" to 8.0,
    "nine" to 9.0,
    "ten" to 10.0,
    "half" to 0.5,
    "quarter" to 0.25,
)

private val ignorableVoiceLeadIns = setOf(
    "add",
    "had",
    "at",
    "log",
    "please",
    "hey",
    "bitewise",
)

fun parseVoiceFoodCommand(spokenText: String): VoiceFoodCommand? {
    val normalized = normalizeVoiceTranscript(spokenText)
    if (normalized.isBlank()) return null

    val (withoutMealHint, mealOverride) = extractMealHint(normalized)
    val core = stripLeadInTokens(withoutMealHint).trim()
    if (core.isBlank()) return null

    parseLeadingAmountPhrase(core, mealOverride)?.let { return it }
    parseTrailingAmountPhrase(core, mealOverride)?.let { return it }

    return VoiceFoodCommand(
        amount = null,
        unit = null,
        ingredientQuery = core.trim(),
        mealOverride = mealOverride,
    )
}

fun inferMealForTime(time: LocalTime): Meal =
    when {
        !time.isBefore(LocalTime.of(7, 0)) && time.isBefore(LocalTime.of(10, 0)) -> Meal.Breakfast
        !time.isBefore(LocalTime.of(10, 0)) && time.isBefore(LocalTime.of(14, 0)) -> Meal.Lunch
        !time.isBefore(LocalTime.of(16, 0)) && time.isBefore(LocalTime.of(20, 0)) -> Meal.Dinner
        else -> Meal.Snack
    }

fun normalizeVoiceUnit(rawUnit: String): String? {
    val compact = rawUnit.trim().lowercase(Locale.CANADA)
    return voiceUnitAliases[compact] ?: measurementUnits.firstOrNull { it.equals(compact, ignoreCase = true) }
}

fun normalizeVoiceTranscript(text: String): String =
    text
        .trim()
        .lowercase(Locale.CANADA)
        .replace(Regex("""[^\w.\s-]"""), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

fun normalizeVoiceSearchQuery(text: String): String =
    text
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { singularizeToken(it) }
        .trim()

private fun parseLeadingAmountPhrase(
    text: String,
    mealOverride: Meal?,
): VoiceFoodCommand? {
    val tokens = text.split(" ").filter { it.isNotBlank() }
    val amountResult = parseNumberTokens(tokens, 0) ?: return null
    val afterAmount = tokens.drop(amountResult.consumedTokens)
    if (afterAmount.isEmpty()) return null

    val unit = normalizeVoiceUnit(afterAmount.first())
    return if (unit != null) {
        val ingredientTokens = stripIngredientLeadInTokens(afterAmount.drop(1))
        if (ingredientTokens.isEmpty()) {
            VoiceFoodCommand(
                amount = amountResult.value,
                unit = null,
                ingredientQuery = afterAmount.first(),
                mealOverride = mealOverride,
            )
        } else {
        VoiceFoodCommand(
            amount = amountResult.value,
            unit = unit,
            ingredientQuery = stripIngredientLeadInTokens(ingredientTokens).joinToString(" "),
            mealOverride = mealOverride,
        )
    }
    } else {
        VoiceFoodCommand(
            amount = amountResult.value,
            unit = null,
            ingredientQuery = afterAmount.joinToString(" "),
            mealOverride = mealOverride,
        )
    }
}

private fun parseTrailingAmountPhrase(
    text: String,
    mealOverride: Meal?,
): VoiceFoodCommand? {
    val tokens = text.split(" ").filter { it.isNotBlank() }
    if (tokens.size < 3) return null

    for (startIndex in 1 until tokens.lastIndex) {
        val amountResult = parseNumberTokens(tokens, startIndex) ?: continue
        val unitIndex = startIndex + amountResult.consumedTokens
        if (unitIndex >= tokens.size) continue
        val unit = normalizeVoiceUnit(tokens[unitIndex]) ?: continue
        val ingredientTokens = tokens.take(startIndex)
        if (ingredientTokens.isEmpty()) continue
        return VoiceFoodCommand(
            amount = amountResult.value,
            unit = unit,
            ingredientQuery = ingredientTokens.joinToString(" "),
            mealOverride = mealOverride,
        )
    }

    return null
}

private fun parseNumberTokens(
    tokens: List<String>,
    startIndex: Int,
): NumberParseResult? {
    if (startIndex >= tokens.size) return null
    val firstToken = tokens[startIndex]

    firstToken.toDoubleOrNull()?.let {
        return NumberParseResult(value = it, consumedTokens = 1)
    }

    if (firstToken == ".") {
        return NumberParseResult(value = 0.0, consumedTokens = 1)
    }

    if (firstToken == "point" && startIndex + 1 < tokens.size) {
        val decimalDigits = parseDecimalDigits(tokens.drop(startIndex + 1)) ?: return null
        return NumberParseResult(value = "0.$decimalDigits".toDouble(), consumedTokens = decimalDigits.length + 1)
    }

    val direct = spokenNumberTokens[firstToken]
    if (direct != null) {
        if (startIndex + 3 < tokens.size &&
            tokens[startIndex + 1] == "and" &&
            tokens[startIndex + 2] == "a" &&
            tokens[startIndex + 3] == "half"
        ) {
            return NumberParseResult(value = direct + 0.5, consumedTokens = 4)
        }
        if (startIndex + 2 < tokens.size &&
            tokens[startIndex + 1] == "and" &&
            tokens[startIndex + 2] == "half"
        ) {
            return NumberParseResult(value = direct + 0.5, consumedTokens = 3)
        }
        return NumberParseResult(value = direct, consumedTokens = 1)
    }

    return null
}

private fun parseDecimalDigits(tokens: List<String>): String? {
    val digitTokens = mutableListOf<String>()
    for (token in tokens) {
        val digit = when (token) {
            "zero" -> "0"
            "one" -> "1"
            "two" -> "2"
            "three" -> "3"
            "four" -> "4"
            "five" -> "5"
            "six" -> "6"
            "seven" -> "7"
            "eight" -> "8"
            "nine" -> "9"
            else -> null
        } ?: break
        digitTokens += digit
    }
    return digitTokens.takeIf { it.isNotEmpty() }?.joinToString("")
}

private fun extractMealHint(text: String): Pair<String, Meal?> {
    val hints = listOf(
        "for breakfast" to Meal.Breakfast,
        "to breakfast" to Meal.Breakfast,
        "for lunch" to Meal.Lunch,
        "to lunch" to Meal.Lunch,
        "for dinner" to Meal.Dinner,
        "to dinner" to Meal.Dinner,
        "as a snack" to Meal.Snack,
        "for snack" to Meal.Snack,
        "to snack" to Meal.Snack,
    )

    val match = hints.firstOrNull { text.contains(it.first) } ?: return text to null
    return text.replace(match.first, "").replace(Regex("\\s+"), " ").trim() to match.second
}

private fun stripIngredientLeadInTokens(tokens: List<String>): List<String> =
    tokens.dropWhile { it == "of" || it == "a" || it == "an" }

private fun stripLeadInTokens(text: String): String {
    val tokens = text.split(" ").filter { it.isNotBlank() }
    val cleaned = tokens.dropWhile { it in ignorableVoiceLeadIns }
    return cleaned.joinToString(" ")
}

private fun singularizeToken(token: String): String =
    when {
        token.endsWith("ies") && token.length > 3 -> token.dropLast(3) + "y"
        token.endsWith("es") && token.length > 3 && !token.endsWith("ses") -> token.dropLast(2)
        token.endsWith("s") && token.length > 2 && !token.endsWith("ss") -> token.dropLast(1)
        else -> token
    }
