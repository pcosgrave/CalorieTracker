package com.philipcosgrave.calorietracker.data.repository

import com.philipcosgrave.calorietracker.model.Meal

data class ParsedDiaryEntryDraft(
    val foodName: String,
    val calories: Double,
    val meal: Meal,
)

interface AiLogRepository {
    suspend fun parseTranscript(transcript: String): List<ParsedDiaryEntryDraft>
}

class FakeAiLogRepository : AiLogRepository {
    override suspend fun parseTranscript(transcript: String): List<ParsedDiaryEntryDraft> =
        listOf(
            ParsedDiaryEntryDraft(foodName = "Greek yogurt", calories = 170.0, meal = Meal.Breakfast),
            ParsedDiaryEntryDraft(foodName = "Chicken wrap", calories = 520.0, meal = Meal.Lunch),
            ParsedDiaryEntryDraft(foodName = "Almonds", calories = 180.0, meal = Meal.Snack),
        )
}
