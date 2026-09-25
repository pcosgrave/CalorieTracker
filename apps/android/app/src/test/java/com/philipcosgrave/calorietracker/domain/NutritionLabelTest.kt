package com.philipcosgrave.calorietracker.domain

import org.junit.Assert.*
import org.junit.Test

class NutritionLabelTest {
    @Test fun preservesServingColumnDecimalsAndZero() {
        val label = parseNutritionLabel("""{"labelFound":true,"quantity":30,"unit":"g","caloriesKcal":120,"proteinGrams":2.75,"carbohydrateGrams":20,"fatGrams":0}""")
        assertEquals("30", label.quantity)
        assertEquals("gram", label.unit)
        assertEquals("120", label.calories)
        assertEquals("2.75", label.protein)
        assertEquals("0", label.fat)
        assertNull(label.name)
    }

    @Test fun convertsKilojoulesAndPreservesMissingFields() {
        val label = parseNutritionLabel("""{"labelFound":true,"quantity":100,"unit":"ml","energyKj":418.4,"proteinGrams":null}""")
        assertEquals(100.0, label.calories.toDouble(), 0.0001)
        assertEquals("", label.protein)
        assertEquals("", label.carbs)
        assertEquals("ml", label.unit)
    }

    @Test fun zeroCalorieLabelIsValidAndServingOnlyIsSupported() {
        val label = parseNutritionLabel("""{"labelFound":true,"quantity":1,"unit":"serving","caloriesKcal":0,"energyKj":10}""")
        assertEquals("0", label.calories)
        assertEquals("serving", label.unit)
    }

    @Test fun rejectsMissingServingBasisInsteadOfMixingWithOldFood() {
        for (json in listOf(
            """{"labelFound":false}""",
            """{"labelFound":true,"caloriesKcal":100}""",
            """{"labelFound":true,"quantity":0,"unit":"g","caloriesKcal":100}""",
            """{"labelFound":true,"quantity":100,"unit":"g"}""",
        )) assertThrows(IllegalArgumentException::class.java) { parseNutritionLabel(json) }
    }

    @Test fun negativeNutrientsRemainBlankAndVisibleNamesAreReturned() {
        val label = parseNutritionLabel("""{"labelFound":true,"name":"Oats","brand":null,"quantity":100,"unit":"grams","caloriesKcal":350,"fatGrams":-1}""")
        assertEquals("Oats", label.name)
        assertNull(label.brand)
        assertEquals("", label.fat)
    }
}
