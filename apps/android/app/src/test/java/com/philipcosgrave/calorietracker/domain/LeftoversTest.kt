package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.data.local.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LeftoversTest {
    private val date = LocalDate.of(2026, 9, 8)
    private val rice = FoodItem("rice", FoodKind.Ingredient, "Rice", servingQuantity = 100.0, servingUnit = "g", nutrients = Nutrients(130.0, 3.0, 28.0, 0.2))
    private val soup = FoodItem("soup", FoodKind.Ingredient, "Soup", servingQuantity = 250.0, servingUnit = "ml", nutrients = Nutrients(100.0, 5.0, 12.0, 2.0))
    private val entries = listOf(
        DiaryEntry("a", rice, date, Meal.Dinner, 2.0, 200.0, "g"),
        DiaryEntry("b", soup, date, Meal.Dinner, 1.5, 375.0, "ml"),
    )

    @Test fun movingAPercentageConservesEveryAmountAndMacro() {
        val split = splitLeftover(entries, 30.0, "Dinner")
        entries.indices.forEach { i ->
            assertEquals(entries[i].loggedAmount, split.remaining[i].loggedAmount + split.leftover.entries[i].loggedAmount, 1e-9)
            assertEquals(entries[i].servingMultiplier, split.remaining[i].servingMultiplier + split.leftover.entries[i].servingMultiplier, 1e-9)
            assertEquals(entries[i].food, split.leftover.entries[i].food)
            assertEquals(entries[i].loggedUnit, split.leftover.entries[i].loggedUnit)
        }
        assertEquals(60.0, split.leftover.entries[0].loggedAmount, 1e-9)
        assertEquals(112.5, split.leftover.entries[1].loggedAmount, 1e-9)
        // Verify stored values, before the app rounds each displayed portion to one decimal.
        val nutrients: List<(Nutrients) -> Double> = listOf({ it.calories }, { it.proteinGrams }, { it.carbohydrateGrams }, { it.fatGrams })
        nutrients.forEach { value ->
            val original = entries.sumOf { value(it.food.nutrients) * it.servingMultiplier }
            val combined = (split.remaining + split.leftover.entries).sumOf { value(it.food.nutrients) * it.servingMultiplier }
            assertEquals(original, combined, 1e-9)
        }
    }
    @Test fun onlySelectedFoodsAreMovedAndWholePortionsRemoveOriginals() {
        val split = splitLeftover(listOf(entries[1]), 100.0, "Soup")
        assertTrue(split.remaining.isEmpty())
        assertEquals(listOf(entries[1]), split.leftover.entries)
        assertEquals(200.0, entries[0].loggedAmount, 0.0)
    }
    @Test fun storedLeftoverRestoresSeparateFoodsAtDestinationWithStableNewIds() {
        val leftover = splitLeftover(entries, 25.0, "Lunch tomorrow").leftover
        val restored = leftover.toRecord("owner").toLeftover()
        assertEquals(leftover, restored)
        assertEquals(date, restored.date)
        val used = leftoverDiaryEntries(restored, date.plusDays(1), Meal.Lunch)
        assertEquals(2, used.size)
        assertEquals(2, used.map { it.id }.distinct().size)
        assertTrue(used.none { it.id in entries.map { old -> old.id } })
        assertTrue(used.all { it.date == date.plusDays(1) && it.meal == Meal.Lunch })
        assertEquals(restored.entries.map { it.food }, used.map { it.food })
        assertEquals(restored.entries.map { it.loggedAmount }, used.map { it.loggedAmount })
        assertEquals(used, leftoverDiaryEntries(restored, date.plusDays(1), Meal.Lunch))
    }
    @Test fun invalidSelectionsAreRejected() {
        for (percent in listOf(0.0, -1.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertThrows(IllegalArgumentException::class.java) { splitLeftover(entries, percent, "Food") }
        assertThrows(IllegalArgumentException::class.java) { splitLeftover(emptyList(), 50.0, "Food") }
        assertThrows(IllegalArgumentException::class.java) { splitLeftover(entries + entries[0], 50.0, "Food") }
        assertThrows(IllegalArgumentException::class.java) { splitLeftover(entries, 50.0, " ") }
        assertThrows(IllegalArgumentException::class.java) { splitLeftover(listOf(entries[0], entries[1].copy(meal = Meal.Lunch)), 50.0, "Food") }
    }
}
