package com.philipcosgrave.calorietracker.ui

import androidx.compose.runtime.saveable.listSaver
import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.data.local.*
import java.time.LocalDate

/** Screen-scoped, saveable destination. DiaryEntry is reused as the portion-bearing result. */
data class AcquisitionDestination(val date: LocalDate, val meal: Meal, val recipe: Boolean = false)
val AcquisitionSaver = listSaver<AcquisitionDestination, String>(
    save = { listOf(it.date.toString(), it.meal.name, it.recipe.toString()) },
    restore = { AcquisitionDestination(LocalDate.parse(it[0]), Meal.valueOf(it[1]), it[2].toBoolean()) },
)
val SelectedFoodSaver = listSaver<FoodItem?, String>(save = { listOf(it?.toJsonString().orEmpty()) }, restore = { it[0].takeIf(String::isNotBlank)?.let(::foodItemFromJsonString) })
val RecipeDraftSaver = listSaver<RecipeDraft, String>(
    save = { draft -> listOf(draft.name, draft.brand, draft.servingQuantity, draft.servingUnit,
        "@metadata" + org.json.JSONObject().put("photoPath", draft.photoPath).put("instructions", org.json.JSONArray(draft.instructions)).put("description", draft.description).put("unresolved", org.json.JSONArray(draft.unresolvedIngredients)).put("prepMinutes", draft.prepMinutes).put("totalMinutes", draft.totalMinutes).toString()) + draft.components.map {
        DiaryEntry("draft", it.item, LocalDate.of(2000, 1, 1), Meal.Lunch, 1.0, it.amount, it.unit).toJsonString()
    } },
    restore = { values ->
        val meta = values.getOrNull(4)?.takeIf { it.startsWith("@metadata") }?.removePrefix("@metadata")?.let { org.json.JSONObject(it) }
        RecipeDraft(values[0], values[1], values[2], values[3], values.drop(if (meta == null) 4 else 5).map {
            val entry = diaryEntryFromJsonString(it); RecipeComponent(entry.food, entry.loggedAmount, entry.loggedUnit)
        }, prepMinutes = meta?.optInt("prepMinutes")?.takeIf { it > 0 }, totalMinutes = meta?.optInt("totalMinutes")?.takeIf { it > 0 }, photoPath = meta?.optString("photoPath")?.takeIf { it.isNotBlank() && it != "null" },
            instructions = meta?.optJSONArray("instructions")?.let { array -> List(array.length()) { array.getString(it) } }.orEmpty(), description = meta?.optString("description").orEmpty(), unresolvedIngredients = meta?.optJSONArray("unresolved")?.let { array -> List(array.length()) { array.getString(it) } }.orEmpty())
    },
)
fun acquisitionEntries(destination: AcquisitionDestination, entries: List<DiaryEntry>) = entries.map { it.copy(date = destination.date, meal = destination.meal) }
fun acquisitionIngredients(entries: List<DiaryEntry>) = entries.map { RecipeComponent(it.food, it.loggedAmount, it.loggedUnit) }

val DiaryEntrySaver = listSaver<DiaryEntry?, String>(save = { listOf(it?.toJsonString().orEmpty()) }, restore = { it[0].takeIf(String::isNotBlank)?.let(::diaryEntryFromJsonString) })
