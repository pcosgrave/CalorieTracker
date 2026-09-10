package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import org.json.JSONObject
import java.time.LocalDate

data class PhotoFoodDraft(
    val id: String = createId("photo"),
    val name: String,
    val grams: String,
    val weightSource: String = "Estimated weight",
    val notes: String = "",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val matchedFoodId: String? = null,
    val amountUnit: String = "g",
) {
    fun matchedFood(foods: List<FoodItem>): FoodItem? = foods.firstOrNull { it.id == matchedFoodId }

    fun isValid(foods: List<FoodItem>): Boolean = name.isNotBlank() &&
        grams.toDoubleOrNull()?.let { it.isFinite() && it > 0 && it <= 100_000 } == true &&
        (matchedFood(foods)?.let { food ->
            food.servingQuantity.isFinite() && food.servingQuantity > 0 && food.amountInBaseUnits(1.0, amountUnit) != null
        } ?: false)
}

/** Scale attribution is separate from visual estimates so a plate total cannot be reused per item. */
fun parsePhotoFoods(text: String, foods: List<FoodItem>): List<PhotoFoodDraft> {
    val clean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    val root = JSONObject(clean)
    val items = root.getJSONArray("items")
    require(items.length() <= 30) { "Too many foods in this photo. Try a smaller group." }
    val scale = root.optJSONObject("scale")
    val scaleGrams = scale?.optDouble("value")?.let { value ->
        when (scale.optString("unit").lowercase()) {
            "g" -> value
            "kg" -> value * 1000
            "oz" -> value * 28.349523125
            "lb" -> value * 453.59237
            else -> null
        }
    }?.takeIf { it.isFinite() && it > 0 && it <= 100_000 }
    val scaleIndex = scale?.optInt("itemIndex", -1) ?: -1
    return List(items.length()) { index ->
        val item = items.getJSONObject(index)
        val name = foodTitle(item.getString("name").take(200))
        require(name.isNotBlank()) { "A food could not be identified. Please try another photo." }
        val useScale = scaleGrams != null && scaleIndex == index
        val grams = if (useScale) scaleGrams else item.optDouble("estimatedGrams")
            .takeIf { it.isFinite() && it > 0 && it <= 100_000 }
        val match = foods.filter {
            it.name.trim().equals(name, ignoreCase = true)
        }.singleOrNull()
        PhotoFoodDraft(
            name = name,
            grams = grams?.let(::formatNumber).orEmpty(),
            weightSource = if (useScale) "Scale reading — confirm the scale was tared" else "Estimated weight",
            notes = listOfNotNull(
                item.optString("notes").takeIf { it.isNotBlank() }?.take(500),
                if (scaleGrams != null && scaleIndex !in 0 until items.length())
                    "Scale shows ${formatNumber(scaleGrams)} g for the plate/group; individual weights are estimates."
                else null,
            ).joinToString(" "),
        ).let { draft -> if (match != null) linkReviewFood(draft, match) else draft }
    }
}

/** Stable IDs let a retried save address the same foods and entries. */
fun photoDiaryEntries(drafts: List<PhotoFoodDraft>, foods: List<FoodItem>, date: LocalDate, meal: Meal): List<DiaryEntry> {
    require(drafts.isNotEmpty() && drafts.all { it.isValid(foods) }) { "Review food names, weights, and nutrition before saving." }
    return drafts.map { draft ->
        val food = requireNotNull(draft.matchedFood(foods))
        val grams = draft.grams.toDouble()
        DiaryEntry(
            id = "photo-entry-${draft.id}", food = food, date = date, meal = meal,
            servingMultiplier = requireNotNull(food.amountInBaseUnits(grams, draft.amountUnit)) / food.servingQuantity,
            loggedAmount = grams, loggedUnit = draft.amountUnit,
        )
    }
}

/** Keep the pictured amount only when it can be expressed in the saved food's units. */
fun linkReviewFood(draft: PhotoFoodDraft, food: FoodItem): PhotoFoodDraft {
    val compatible = food.amountInBaseUnits(1.0, draft.amountUnit) != null
    return draft.copy(
        name = foodTitle(food.name), matchedFoodId = food.id,
        grams = if (compatible) draft.grams else "",
        amountUnit = if (compatible) draft.amountUnit else food.servingUnit,
        weightSource = if (compatible) draft.weightSource else "Enter amount in ${food.servingUnit}",
        calories = "", protein = "", carbs = "", fat = "",
    )
}
