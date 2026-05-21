package com.philipcosgrave.calorietracker.data

import android.content.Context
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.RecipeComponent
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

private fun prefs(context: Context) = context.getSharedPreferences("calorie-tracker", Context.MODE_PRIVATE)

fun readFoodItems(context: Context, key: String): List<FoodItem> =
    runCatching {
        val stored = prefs(context).getString(key, "[]") ?: "[]"
        val array = JSONArray(stored)
        List(array.length()) { index -> foodItemFromJson(array.getJSONObject(index)) }
    }.getOrDefault(emptyList())

fun writeFoodItems(context: Context, key: String, items: List<FoodItem>) {
    val array = JSONArray()
    items.forEach { array.put(it.toJson()) }
    prefs(context).edit().putString(key, array.toString()).apply()
}

fun readDiaryEntries(context: Context): List<DiaryEntry> =
    runCatching {
        val stored = prefs(context).getString("diaryEntries", "[]") ?: "[]"
        val array = JSONArray(stored)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            DiaryEntry(
                id = item.getString("id"),
                food = foodItemFromJson(item.getJSONObject("food")),
                date = LocalDate.parse(item.getString("date")),
                meal = Meal.valueOf(item.getString("meal")),
                servingMultiplier = item.getDouble("servingMultiplier"),
            )
        }
    }.getOrDefault(emptyList())

fun writeDiaryEntries(context: Context, entries: List<DiaryEntry>) {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(
            JSONObject()
                .put("id", entry.id)
                .put("food", entry.food.toJson())
                .put("date", entry.date.toString())
                .put("meal", entry.meal.name)
                .put("servingMultiplier", entry.servingMultiplier),
        )
    }
    prefs(context).edit().putString("diaryEntries", array.toString()).apply()
}

fun readStringList(context: Context, key: String): List<String> =
    runCatching {
        val array = JSONArray(prefs(context).getString(key, "[]") ?: "[]")
        List(array.length()) { index -> array.getString(index) }
    }.getOrDefault(emptyList())

fun writeStringList(context: Context, key: String, values: List<String>) {
    val array = JSONArray()
    values.forEach { array.put(it) }
    prefs(context).edit().putString(key, array.toString()).apply()
}

private fun FoodItem.toJson(): JSONObject {
    val componentsJson = JSONArray()
    components.forEach { componentsJson.put(it.toJson()) }
    return JSONObject()
        .put("id", id)
        .put("kind", kind.name)
        .put("name", name)
        .put("brand", brand)
        .put("barcode", barcode)
        .put("servingQuantity", servingQuantity)
        .put("servingUnit", servingUnit)
        .put("nutrients", nutrients.toJson())
        .put("components", componentsJson)
        .put("frequency", frequency)
        .put("lastUsedDaysAgo", lastUsedDaysAgo)
}

private fun RecipeComponent.toJson(): JSONObject =
    JSONObject()
        .put("item", item.toJson())
        .put("amount", amount)
        .put("unit", unit)

private fun Nutrients.toJson(): JSONObject =
    JSONObject()
        .put("calories", calories)
        .put("proteinGrams", proteinGrams)
        .put("carbohydrateGrams", carbohydrateGrams)
        .put("fatGrams", fatGrams)

private fun foodItemFromJson(json: JSONObject): FoodItem {
    val componentsJson = json.optJSONArray("components") ?: JSONArray()
    return FoodItem(
        id = json.getString("id"),
        kind = FoodKind.valueOf(json.getString("kind")),
        name = json.getString("name"),
        brand = json.optString("brand"),
        barcode = json.optString("barcode"),
        servingQuantity = json.getDouble("servingQuantity"),
        servingUnit = json.getString("servingUnit"),
        nutrients = nutrientsFromJson(json.getJSONObject("nutrients")),
        components = List(componentsJson.length()) { index -> recipeComponentFromJson(componentsJson.getJSONObject(index)) },
        frequency = json.optInt("frequency"),
        lastUsedDaysAgo = json.optInt("lastUsedDaysAgo"),
    )
}

private fun recipeComponentFromJson(json: JSONObject): RecipeComponent =
    RecipeComponent(
        item = foodItemFromJson(json.getJSONObject("item")),
        amount = json.getDouble("amount"),
        unit = json.getString("unit"),
    )

private fun nutrientsFromJson(json: JSONObject): Nutrients =
    Nutrients(
        calories = json.getDouble("calories"),
        proteinGrams = json.optDouble("proteinGrams"),
        carbohydrateGrams = json.optDouble("carbohydrateGrams"),
        fatGrams = json.optDouble("fatGrams"),
    )
