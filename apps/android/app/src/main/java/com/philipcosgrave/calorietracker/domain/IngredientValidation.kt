package com.philipcosgrave.calorietracker.domain

fun validIngredientFields(name: String, quantity: String, unit: String, calories: String,
    protein: String, carbs: String, fat: String, requireAllMacros: Boolean = false): Boolean =
    name.isNotBlank() && unit.isNotBlank() &&
        quantity.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true &&
        calories.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true &&
        listOf(protein, carbs, fat).all { value ->
            (!requireAllMacros && value.isBlank()) || value.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true
        }
