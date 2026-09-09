package com.philipcosgrave.calorietracker.domain

import org.json.JSONObject

fun parsePhotoFoodNames(text: String): List<String> {
    val root = JSONObject(text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
    require(!root.optBoolean("tooMany")) { "There are too many foods to read together. Please photograph a smaller group." }
    val names = root.getJSONArray("foods")
    require(names.length() <= 12) { "Please photograph a smaller group of foods." }
    return List(names.length()) { index ->
        names.getString(index).trim().also {
            require(it.isNotBlank() && it.length <= 120) { "A food name could not be read. Please try another photo." }
        }
    }.filterNot(::isPhotoNonFoodObject).distinctBy { it.lowercase() }
}

/** Reject obvious objects without rejecting edible names such as mug cake or rice paper. */
internal fun isPhotoNonFoodObject(name: String): Boolean {
    val normalized = name.lowercase().replace(Regex("[^a-z ]"), " ").trim()
    val words = normalized.split(Regex("\\s+")).filterNot {
        it in setOf("a", "an", "the", "empty", "white", "black", "red", "blue", "green", "small", "large", "ceramic", "plastic", "wooden", "metal", "stainless", "steel", "glass", "coffee", "tea", "dinner", "kitchen", "digital")
    }
    val objects = setOf("mug", "mugs", "cup", "cups", "bowl", "bowls", "plate", "plates", "fork", "forks", "knife", "knives", "spoon", "spoons", "cutlery", "utensils", "napkin", "napkins", "table", "tablecloth", "placemat", "scale", "scales", "bottle", "bottles", "container", "containers", "packaging", "wrapper", "wrappers", "phone", "hand", "hands", "chopsticks", "straw", "lid", "lids", "tray")
    return normalized == "glass" || words.isNotEmpty() && words.all { it in objects }
}
