package com.philipcosgrave.calorietracker.data.repository

import com.philipcosgrave.calorietracker.model.AiDiaryEntryDraft
import com.philipcosgrave.calorietracker.model.AiFoodLogResponse
import com.philipcosgrave.calorietracker.model.Meal
import java.time.LocalDate

class FakeAiFoodLogRepository : AiFoodLogRepository {
    override suspend fun parseFoodLog(
        transcript: String,
        date: LocalDate,
        fallbackMeal: Meal,
    ): AiFoodLogResponse =
        AiFoodLogResponse(
            entries = listOf(
                AiDiaryEntryDraft(foodName = "Greek yogurt", calories = 170.0, meal = Meal.Breakfast),
                AiDiaryEntryDraft(foodName = "Chicken wrap", calories = 520.0, meal = Meal.Lunch),
                AiDiaryEntryDraft(foodName = "Almonds", calories = 180.0, meal = fallbackMeal),
            ),
        )
}
