package com.philipcosgrave.calorietracker.domain

fun calorieRangeProgress(calories: Double, minimum: Int, maximum: Int): Float {
    val low = minimum.coerceAtLeast(0).toDouble()
    val high = maximum.toDouble().coerceAtLeast(low + 1)
    return (when {
        calories <= low -> if (low == 0.0) 0.0 else calories / low / 3
        calories <= high -> (1 + (calories - low) / (high - low)) / 3
        else -> (2 + ((calories - high) / (high - low).coerceAtLeast(200.0)).coerceIn(0.0, 1.0)) / 3
    }).toFloat().coerceIn(0f, 1f)
}

fun weightGoalProgress(start: Double?, current: Double?, goal: Double?): Float? {
    if (start == null || current == null || goal == null) return null
    if (start == goal) return if (current == goal) 1f else 0f
    return ((current - start) / (goal - start)).toFloat().coerceIn(0f, 1f)
}
