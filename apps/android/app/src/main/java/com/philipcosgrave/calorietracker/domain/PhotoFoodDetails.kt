package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.FoodItem
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/** Accept a single-food response or select the requested food from a model-returned list. */
fun parsePhotoFoodDetails(name: String, text: String, foods: List<FoodItem>): PhotoFoodDraft {
    val root = JSONObject(text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
    val items = root.optJSONArray("items")
    val index = if (items != null) (0 until items.length()).singleOrNull {
        items.optJSONObject(it)?.optString("name")?.trim()?.equals(name.trim(), true) == true
    } ?: if (items.length() == 1 && items.getJSONObject(0).optString("name").isBlank()) 0 else
        error("No unambiguous details returned for $name") else 0
    val item = if (items != null) items.getJSONObject(index) else root
    require(item.has("estimatedGrams") || root.optJSONObject("scale") != null) { "No food details returned" }
    item.put("name", foodTitle(name))
    val scale = root.optJSONObject("scale")?.let { original ->
        val target = original.optInt("itemIndex", -1)
        when {
            target == index -> JSONObject(original.toString()).put("itemIndex", 0)
            target == -1 -> original
            else -> null // A scale assigned to another food must not become this food's weight.
        }
    }
    return parsePhotoFoods(JSONObject().put("items", JSONArray().put(item)).put("scale", scale).toString(), foods).single()
}

/** Keep every discovered food in review even if a detail request fails; never invent missing values. */
suspend fun readPhotoFoodDetails(
    names: List<String>, foods: List<FoodItem>,
    request: suspend (name: String, index: Int, retry: Boolean) -> String,
): List<PhotoFoodDraft> = names.mapIndexed { index, name ->
    var result: PhotoFoodDraft? = null
    for (attempt in 0..1) {
        try {
            result = parsePhotoFoodDetails(name, request(name, index, attempt > 0), foods)
            break
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // One bounded retry with a shorter response instruction, then leave an editable row.
        }
    }
    result ?: PhotoFoodDraft(
        name = foodTitle(name), grams = "", weightSource = "Weight needs review",
        notes = "Identified in the photo, but its details could not be read. Enter a weight and review nutrition, or swipe to remove.",
        matchedFoodId = foods.filter { it.name.trim().equals(name.trim(), true) }.singleOrNull()?.id,
    ).let { draft -> draft.matchedFood(foods)?.let { linkReviewFood(draft, it) } ?: draft }
}
