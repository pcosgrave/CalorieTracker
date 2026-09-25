package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.FoodItem

/** Local interpretation only; unresolved names and quantities stay editable in shared review. */
fun voiceReviewDrafts(transcript: String, foods: List<FoodItem>): List<PhotoFoodDraft> =
    transcript.split(Regex(",|;|\\band\\b", RegexOption.IGNORE_CASE)).filter { it.isNotBlank() }.take(12).map { phrase ->
        val command = parseVoiceFoodCommand(phrase.trim())
        val name = command?.ingredientQuery ?: phrase.trim().replace(Regex("^(I had|I ate|I drank)\\s+", RegexOption.IGNORE_CASE), "")
        val known = foods.filter { it.name.equals(name, true) }.singleOrNull()
        val amount = command?.amount?.let { if (command.unit == null && known != null) it * known.servingQuantity else it }
            ?: known?.servingQuantity
        PhotoFoodDraft(name = foodTitle(name), grams = amount?.let(::formatNumber).orEmpty(),
            amountUnit = command?.unit ?: known?.servingUnit ?: "g", matchedFoodId = known?.id,
            weightSource = "Dictated amount", notes = if (known == null) "Review the food name and add its nutrition." else "")
    }
