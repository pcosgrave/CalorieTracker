package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DashboardAndSpeechTest {
    @Test fun rangeBarBreaksAlignWithTargetsAndClampOverflow() {
        assertEquals(0f, calorieRangeProgress(-1.0, 1800, 2200), .0001f)
        assertEquals(1f / 3, calorieRangeProgress(1800.0, 1800, 2200), .0001f)
        assertEquals(2f / 3, calorieRangeProgress(2200.0, 1800, 2200), .0001f)
        assertEquals(1f, calorieRangeProgress(9000.0, 1800, 2200), .0001f)
        assertTrue(calorieRangeProgress(0.0, 0, 0).isFinite())
    }
    @Test fun weightProgressWorksForLossGainAndMissingTargets() {
        assertEquals(.5f, weightGoalProgress(100.0, 90.0, 80.0)!!, .0001f)
        assertEquals(.5f, weightGoalProgress(60.0, 65.0, 70.0)!!, .0001f)
        assertEquals(1f, weightGoalProgress(100.0, 70.0, 80.0)!!, .0001f)
        assertEquals(0f, weightGoalProgress(100.0, 110.0, 80.0)!!, .0001f)
        assertNull(weightGoalProgress(100.0, 90.0, null))
    }
    @Test fun finalSpeechReplacesPartialAndOnlyDeliversOnce() {
        val session = SpeechCaptureSession()
        session.partial("one apple")
        assertEquals("one apple and banana", session.finish("one apple and banana"))
        assertNull(session.finish("duplicate"))
        session.partial("late callback")
        assertEquals("one apple and banana", session.transcript)
    }
    @Test fun stoppingCanUsePartialAndCancellationIgnoresLateCallbacks() {
        val stopped = SpeechCaptureSession()
        stopped.partial("150 grams of chicken")
        assertEquals("150 grams of chicken", stopped.finish())
        val cancelled = SpeechCaptureSession()
        cancelled.partial("apple")
        cancelled.cancel()
        assertNull(cancelled.finish("banana"))
        assertNull(SpeechCaptureSession().finish())
    }
    @Test fun leftoverDetailsUseEachOriginalPortion() {
        val apple = FoodItem("apple", FoodKind.Ingredient, "Apple", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(52.0, 1.0, 14.0, .2))
        val chicken = FoodItem("chicken", FoodKind.Ingredient, "Chicken", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(165.0, 31.0, 0.0, 3.6))
        val date = LocalDate.of(2026, 9, 8)
        val entries = listOf(DiaryEntry("a", apple, date, Meal.Lunch, .5), DiaryEntry("c", chicken, date, Meal.Lunch, 1.5))
        val leftover = Leftover("leftover", "Lunch remaining", entries)
        val display = leftoverAsFood(leftover)
        assertEquals(273.5, display.nutrients.calories, .0001)
        assertEquals(listOf(50.0, 150.0), display.components.map { it.amount })
        val consumed = leftoverDiaryEntries(leftover, date.plusDays(1), Meal.Dinner)
        assertEquals(listOf(apple, chicken), consumed.map { it.food })
        assertEquals(listOf(.5, 1.5), consumed.map { it.servingMultiplier })
        assertTrue(consumed.all { it.date == date.plusDays(1) && it.meal == Meal.Dinner })
    }
}
