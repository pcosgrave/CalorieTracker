package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.Meal
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class VoiceFoodLoggingTest {

    @Test
    fun test_parseVoiceFoodCommand_valid_command() {
        val command = parseVoiceFoodCommand("add 150 grams chicken breast")

        assertNotNull(command)
        assertEquals(150.0, command.amount)
        assertEquals("gram", command.unit)
        assertEquals("chicken breast", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_with_decimal_amount() {
        val command = parseVoiceFoodCommand("add 2.5 cups rice")

        assertNotNull(command)
        assertEquals(2.5, command.amount)
        assertEquals("cup", command.unit)
        assertEquals("rice", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_with_kilograms() {
        val command = parseVoiceFoodCommand("add 0.5 kg broccoli")

        assertNotNull(command)
        assertEquals(0.5, command.amount)
        assertEquals("kg", command.unit)
        assertEquals("broccoli", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_kilograms_to_kg() {
        val command = parseVoiceFoodCommand("add 1 kilogram carrots")

        assertNotNull(command)
        assertEquals("kg", command.unit)
        assertEquals("carrots", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_kilograms_to_kg_plural() {
        val command = parseVoiceFoodCommand("add 2 kilograms spinach")

        assertNotNull(command)
        assertEquals("kg", command.unit)
        assertEquals("spinach", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_ounces_to_oz() {
        val command = parseVoiceFoodCommand("add 8 ounces cheese")

        assertNotNull(command)
        assertEquals("oz", command.unit)
        assertEquals("cheese", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_ounces_to_oz_plural() {
        val command = parseVoiceFoodCommand("add 16 ounces milk")

        assertNotNull(command)
        assertEquals("oz", command.unit)
        assertEquals("milk", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_pounds_to_lb() {
        val command = parseVoiceFoodCommand("add 2 lbs meat")

        assertNotNull(command)
        assertEquals("lb", command.unit)
        assertEquals("meat", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_pounds_to_lb_plural() {
        val command = parseVoiceFoodCommand("add 3 pounds potatoes")

        assertNotNull(command)
        assertEquals("lb", command.unit)
        assertEquals("potatoes", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_ml_to_ml() {
        val command = parseVoiceFoodCommand("add 500 milliliters water")

        assertNotNull(command)
        assertEquals("ml", command.unit)
        assertEquals("water", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_liter_to_liter() {
        val command = parseVoiceFoodCommand("add 1 liter soda")

        assertNotNull(command)
        assertEquals("liter", command.unit)
        assertEquals("soda", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_liters_to_liter() {
        val command = parseVoiceFoodCommand("add 2 liters juice")

        assertNotNull(command)
        assertEquals("liter", command.unit)
        assertEquals("juice", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_cup_to_cup() {
        val command = parseVoiceFoodCommand("add 2 cups flour")

        assertNotNull(command)
        assertEquals("cup", command.unit)
        assertEquals("flour", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_cups_to_cup() {
        val command = parseVoiceFoodCommand("add 3 cups sugar")

        assertNotNull(command)
        assertEquals("cup", command.unit)
        assertEquals("sugar", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_tablespoon_to_tbsp() {
        val command = parseVoiceFoodCommand("add 2 tablespoons oil")

        assertNotNull(command)
        assertEquals("tbsp", command.unit)
        assertEquals("oil", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_tablespoons_to_tbsp() {
        val command = parseVoiceFoodCommand("add 3 tablespoons butter")

        assertNotNull(command)
        assertEquals("tbsp", command.unit)
        assertEquals("butter", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_teaspoon_to_tsp() {
        val command = parseVoiceFoodCommand("add 1 teaspoon salt")

        assertNotNull(command)
        assertEquals("tsp", command.unit)
        assertEquals("salt", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_teaspoons_to_tsp() {
        val command = parseVoiceFoodCommand("add 2 teaspoons pepper")

        assertNotNull(command)
        assertEquals("tsp", command.unit)
        assertEquals("pepper", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_serving_to_serving() {
        val command = parseVoiceFoodCommand("add 1 serving pasta")

        assertNotNull(command)
        assertEquals("serving", command.unit)
        assertEquals("pasta", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_servings_to_serving() {
        val command = parseVoiceFoodCommand("add 2 servings rice")

        assertNotNull(command)
        assertEquals("serving", command.unit)
        assertEquals("rice", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_egg_to_eggs() {
        val command = parseVoiceFoodCommand("add 2 eggs")

        assertNotNull(command)
        assertEquals("eggs", command.unit)
        assertEquals("eggs", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_eggs_to_eggs() {
        val command = parseVoiceFoodCommand("add 3 eggs")

        assertNotNull(command)
        assertEquals("eggs", command.unit)
        assertEquals("eggs", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_no_amount_returns_null() {
        val command = parseVoiceFoodCommand("add chicken breast")
        assertNull(command)
    }

    @Test
    fun test_parseVoiceFoodCommand_no_unit_returns_null() {
        val command = parseVoiceFoodCommand("add 150 chicken breast")
        assertNull(command)
    }

    @Test
    fun test_parseVoiceFoodCommand_blank_ingredient_returns_null() {
        val command = parseVoiceFoodCommand("add 150 grams")
        assertNull(command)
    }

    @Test
    fun test_parseVoiceFoodCommand_invalid_amount_returns_null() {
        val command = parseVoiceFoodCommand("add abc grams chicken")
        assertNull(command)
    }

    @Test
    fun test_parseVoiceFoodCommand_with_extra_whitespace() {
        val command = parseVoiceFoodCommand("add   100   grams   chicken")

        assertNotNull(command)
        assertEquals(100.0, command.amount)
        assertEquals("gram", command.unit)
        assertEquals("chicken", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_without_add_prefix() {
        val command = parseVoiceFoodCommand("100 grams chicken")

        assertNotNull(command)
        assertEquals(100.0, command.amount)
    }

    @Test
    fun test_parseVoiceFoodCommand_trims_ingredient() {
        val command = parseVoiceFoodCommand("add 100 grams  chicken breast  ")

        assertNotNull(command)
        assertEquals("chicken breast", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_with_capital_letters() {
        val command = parseVoiceFoodCommand("add 100 Grams Chicken Breast")

        assertNotNull(command)
        assertEquals("gram", command.unit)
        assertEquals("chicken breast", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_with_mixed_case() {
        val command = parseVoiceFoodCommand("Add 50 GmS SpInAcH")

        assertNotNull(command)
        assertEquals("gram", command.unit)
        assertEquals("spinach", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_wrap_to_wrap() {
        val command = parseVoiceFoodCommand("add 1 wrap turkey")

        assertNotNull(command)
        assertEquals("wrap", command.unit)
        assertEquals("turkey", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_wraps_to_wrap() {
        val command = parseVoiceFoodCommand("add 2 wraps chicken")

        assertNotNull(command)
        assertEquals("wrap", command.unit)
        assertEquals("chicken", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_bowl_to_bowl() {
        val command = parseVoiceFoodCommand("add 1 bowl soup")

        assertNotNull(command)
        assertEquals("bowl", command.unit)
        assertEquals("soup", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_bowls_to_bowl() {
        val command = parseVoiceFoodCommand("add 2 bowls cereal")

        assertNotNull(command)
        assertEquals("bowl", command.unit)
        assertEquals("cereal", command.ingredientQuery)
    }

    @Test
    fun test_parseVoiceFoodCommand_normalizes_medium_to_medium() {
        val command = parseVoiceFoodCommand("add 1 medium avocado")

        assertNotNull(command)
        assertEquals("medium", command.unit)
        assertEquals("avocado", command.ingredientQuery)
    }

    @Test
    fun test_inferMealForTime_breakfast() {
        val meal = inferMealForTime(LocalTime.of(8, 0))
        assertEquals(Meal.Breakfast, meal)
    }

    @Test
    fun test_inferMealForTime_lunch() {
        val meal = inferMealForTime(LocalTime.of(12, 0))
        assertEquals(Meal.Lunch, meal)
    }

    @Test
    fun test_inferMealForTime_dinner() {
        val meal = inferMealForTime(LocalTime.of(18, 0))
        assertEquals(Meal.Dinner, meal)
    }

    @Test
    fun test_inferMealForTime_snack() {
        val meal = inferMealForTime(LocalTime.of(15, 0))
        assertEquals(Meal.Snack, meal)
    }

    @Test
    fun test_inferMealForTime_very_early_morning_is_snack() {
        val meal = inferMealForTime(LocalTime.of(4, 0))
        assertEquals(Meal.Snack, meal)
    }

    @Test
    fun test_inferMealForTime_after_dinner_is_snack() {
        val meal = inferMealForTime(LocalTime.of(21, 0))
        assertEquals(Meal.Snack, meal)
    }

    @Test
    fun test_inferMealForTime_midnight_is_snack() {
        val meal = inferMealForTime(LocalTime.of(0, 0))
        assertEquals(Meal.Snack, meal)
    }

    @Test
    fun test_normalizeVoiceUnit_returns_valid_unit() {
        assertEquals("gram", normalizeVoiceUnit("grams"))
        assertEquals("oz", normalizeVoiceUnit("ounces"))
        assertEquals("lb", normalizeVoiceUnit("pounds"))
    }

    @Test
    fun test_normalizeVoiceUnit_unknown_unit_returns_unit_itself() {
        assertEquals("cup", normalizeVoiceUnit("cup"))
        assertEquals("liter", normalizeVoiceUnit("liter"))
    }

    @Test
    fun test_normalizeVoiceUnit_empty_string_returns_null() {
        assertNull(normalizeVoiceUnit(""))
    }

    @Test
    fun test_normalizeVoiceUnit_very_long_ingredient_name() {
        val command = parseVoiceFoodCommand("add 100 grams very long ingredient name with spaces")

        assertNotNull(command)
        assertEquals("very long ingredient name with spaces", command.ingredientQuery)
    }

    @Test
    fun test_normalizeVoiceUnit_abbreviations() {
        assertEquals("milligram", normalizeVoiceUnit("mg"))
        assertEquals("milliliter", normalizeVoiceUnit("ml"))
        assertEquals("liter", normalizeVoiceUnit("l"))
    }
}
