package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CalorieTrackerMathTest {

    @Test
    fun test_formatNumber_with_integer_value_returns_integer_string() {
        assertEquals("10", formatNumber(10.0))
        assertEquals("100", formatNumber(100.0))
        assertEquals("0", formatNumber(0.0))
    }

    @Test
    fun test_formatNumber_with_decimal_value_returns_one_decimal() {
        assertEquals("10.5", formatNumber(10.5))
        assertEquals("1.2", formatNumber(1.2))
        assertEquals("0.5", formatNumber(0.5))
    }

    @Test
    fun test_roundOne_with_decimal_values() {
        assertEquals(10.5, roundOne(10.55), 0.01)
        assertEquals(1.2, roundOne(1.25), 0.01)
        assertEquals(0.5, roundOne(0.55), 0.01)
    }

    @Test
    fun test_convertAmount_same_unit_returns_same_value() {
        val result = convertAmount(100.0, "gram", "gram")
        assertEquals(100.0, result)
    }

    @Test
    fun test_convertAmount_grams_to_ml() {
        // Note: this is a simplified conversion for testing
        val result = convertAmount(100.0, "gram", "ml")
        assertNotNull(result)
        assertTrue(result > 0)
    }

    @Test
    fun test_convertAmount_unknown_units_returns_null() {
        val result = convertAmount(100.0, "invalid_unit", "ml")
        assertNull(result)
    }

    @Test
    fun test_compatibleMeasurementUnits_returns_valid_units() {
        val gramsUnits = compatibleMeasurementUnits("gram")
        assertTrue(gramsUnits.contains("gram"))
        assertTrue(gramsUnits.contains("kg"))
        assertTrue(gramsUnits.contains("milligram"))
    }

    @Test
    fun test_compatibleMeasurementUnits_empty_returns_serving() {
        val result = compatibleMeasurementUnits("")
        assertEquals(listOf("serving"), result)
    }

    @Test
    fun test_createId_generates_unique_string() {
        val id1 = createId("food")
        val id2 = createId("food")
        assertNotNull(id1)
        assertNotNull(id2)
        assertNotEquals(id1, id2)
    }

    @Test
    fun test_scale_nutrients_with_multiplier() {
        val nutrients = Nutrients(100.0, 10.0, 20.0, 5.0)
        val scaled = nutrients.scale(2.0)
        assertEquals(200.0, scaled.calories)
        assertEquals(20.0, scaled.proteinGrams)
        assertEquals(40.0, scaled.carbohydrateGrams)
        assertEquals(10.0, scaled.fatGrams)
    }

    @Test
    fun test_scale_nutrients_with_half_multiplier() {
        val nutrients = Nutrients(200.0, 20.0, 40.0, 10.0)
        val scaled = nutrients.scale(0.5)
        assertEquals(100.0, scaled.calories)
        assertEquals(10.0, scaled.proteinGrams)
        assertEquals(20.0, scaled.carbohydrateGrams)
        assertEquals(5.0, scaled.fatGrams)
    }

    @Test
    fun test_totals_rounded() {
        val totals = Totals(100.55, 10.25, 20.35, 5.45)
        val rounded = totals.rounded()
        assertEquals(100.5, rounded.calories)
        assertEquals(10.2, rounded.protein)
        assertEquals(20.3, rounded.carbs)
        assertEquals(5.4, rounded.fat)
    }

    @Test
    fun test_totalsForEntries_aggregates_multiple_entries() {
        val entry1 = DiaryEntry(
            id = "1",
            food = FoodItem(
                id = "food1",
                kind = FoodKind.Ingredient,
                name = "Apple",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(52.0, 0.3, 14.0, 0.3)
            ),
            date = LocalDate.now(),
            meal = Meal.Breakfast,
            servingMultiplier = 1.0
        )

        val entry2 = DiaryEntry(
            id = "2",
            food = FoodItem(
                id = "food2",
                kind = FoodKind.Ingredient,
                name = "Banana",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(89.0, 1.1, 23.0, 0.3)
            ),
            date = LocalDate.now(),
            meal = Meal.Snack,
            servingMultiplier = 1.0
        )

        val totals = totalsForEntries(listOf(entry1, entry2))
        assertEquals(141.0, totals.calories)
        assertEquals(1.4, totals.protein)
        assertEquals(37.0, totals.carbs)
        assertEquals(0.6, totals.fat)
    }

    @Test
    fun test_totalComponents_aggregates_recipe_components() {
        val components = listOf(
            RecipeComponent(
                item = FoodItem(
                    id = "chicken",
                    kind = FoodKind.Ingredient,
                    name = "Chicken Breast",
                    servingQuantity = 100.0,
                    servingUnit = "gram",
                    nutrients = Nutrients(165.0, 31.0, 0.0, 3.6)
                ),
                amount = 200.0,
                unit = "gram"
            ),
            RecipeComponent(
                item = FoodItem(
                    id = "rice",
                    kind = FoodKind.Ingredient,
                    name = "White Rice",
                    servingQuantity = 100.0,
                    servingUnit = "gram",
                    nutrients = Nutrients(130.0, 2.6, 28.0, 0.3)
                ),
                amount = 150.0,
                unit = "gram"
            )
        )

        val totals = totalComponents(components)
        assertTrue(totals.calories > 0)
        assertTrue(totals.protein > 0)
    }

    @Test
    fun test_foodItem_componentSummary() {
        val foodItem = FoodItem(
            id = "recipe1",
            kind = FoodKind.Recipe,
            name = "Pancakes",
            servingQuantity = 1.0,
            servingUnit = "serving",
            nutrients = Nutrients(400.0, 15.0, 60.0, 20.0),
            components = listOf(
                RecipeComponent(
                    item = FoodItem(
                        id = "flour",
                        kind = FoodKind.Ingredient,
                        name = "All-purpose flour",
                        servingQuantity = 100.0,
                        servingUnit = "gram",
                        nutrients = Nutrients(340.0, 13.0, 75.0, 0.6)
                    ),
                    amount = 100.0,
                    unit = "gram"
                )
            )
        )

        val summary = foodItem.componentSummary()
        assertTrue(summary.contains("All-purpose flour"))
    }

    @Test
    fun test_foodItem_toRecipeDraft() {
        val foodItem = FoodItem(
            id = "recipe1",
            kind = FoodKind.Recipe,
            name = "Pancakes",
            servingQuantity = 1.0,
            servingUnit = "serving",
            nutrients = Nutrients(400.0, 15.0, 60.0, 20.0),
            components = emptyList()
        )

        val draft = foodItem.toRecipeDraft()
        assertEquals("Pancakes", draft.name)
        assertEquals("1", draft.servingQuantity)
        assertEquals("serving", draft.servingUnit)
    }

    @Test
    fun test_recipeDraft_toFoodItem() {
        val draft = RecipeDraft(
            name = "Smoothie",
            brand = "My Recipe",
            servingQuantity = "1",
            servingUnit = "serving",
            components = emptyList()
        )

        val foodItem = draft.toFoodItem()
        assertEquals("Smoothie", foodItem.name)
        assertEquals("My Recipe", foodItem.brand)
        assertEquals(FoodKind.Recipe, foodItem.kind)
    }

    @Test
    fun test_nowIsoString_returns_valid_iso_string() {
        val timestamp = nowIsoString()
        assertTrue(timestamp.startsWith("20")) // Year 20XX
        assertEquals("T", timestamp[10]) // ISO format has T separator
    }

    @Test
    fun test_createSyncMetadata_creates_valid_metadata() {
        val metadata = createSyncMetadata(
            recordId = "test-id",
            deviceId = "device-1",
            version = 1
        )

        assertEquals("test-id", metadata.recordId)
        assertEquals(1, metadata.version)
        assertEquals(SyncStatus.LocalOnly, metadata.syncStatus)
        assertNotNull(metadata.updatedAt)
    }

    @Test
    fun test_markPendingSync_updates_metadata() {
        val metadata = createSyncMetadata("test-id", "device-1", version = 1)
        val pending = metadata.markPendingSync(updatedAt = "2024-01-01T00:00:00Z")

        assertEquals(2, pending.version)
        assertEquals(SyncStatus.PendingPush, pending.syncStatus)
    }

    @Test
    fun test_markPendingSync_with_deletion() {
        val metadata = createSyncMetadata("test-id", "device-1")
        val deleted = metadata.markPendingSync(updatedAt = "2024-01-01T00:00:00Z", deletedAt = "2024-01-02T00:00:00Z")

        assertNotNull(deleted.deletedAt)
        assertEquals(SyncStatus.PendingPush, deleted.syncStatus)
    }

    @Test
    fun test_createFoodRecord_creates_food_record() {
        val food = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val record = createFoodRecord(food, "device-1")

        assertEquals("food-1", record.food.id)
        assertEquals(SyncStatus.PendingPush, record.sync.syncStatus)
        assertEquals("device-1", record.sync.deviceId)
    }

    @Test
    fun test_createBarcodeAliasRecord_creates_alias() {
        val food = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            barcode = "123456789",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-1")

        assertNotNull(alias)
        assertEquals("123456789", alias.barcode)
        assertEquals("user-1", alias.ownerUserId)
        assertEquals("private", alias.visibility)
    }

    @Test
    fun test_createBarcodeAliasRecord_no_barcode_returns_null() {
        val food = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            barcode = "", // Empty barcode
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-1")
        assertNull(alias)
    }

    @Test
    fun test_createChangeEnvelope_creates_envelope() {
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-1",
            payload = mapOf("name" to "Test")
        )

        assertEquals("change-", envelope.changeId)
        assertEquals(SyncEntityType.FoodProduct, envelope.entityType)
        assertEquals(SyncOperation.Upsert, envelope.operation)
        assertEquals("device-1", envelope.deviceId)
    }

    @Test
    fun test_totalsForEntries_empty_list_returns_zero_totals() {
        val totals = totalsForEntries(emptyList())
        assertEquals(0.0, totals.calories)
        assertEquals(0.0, totals.protein)
        assertEquals(0.0, totals.carbs)
        assertEquals(0.0, totals.fat)
    }

    @Test
    fun test_scale_with_zero_multiplier() {
        val nutrients = Nutrients(100.0, 10.0, 20.0, 5.0)
        val scaled = nutrients.scale(0.0)
        assertEquals(0.0, scaled.calories)
        assertEquals(0.0, scaled.proteinGrams)
        assertEquals(0.0, scaled.carbohydrateGrams)
        assertEquals(0.0, scaled.fatGrams)
    }

    @Test
    fun test_scale_with_negative_multiplier() {
        val nutrients = Nutrients(100.0, 10.0, 20.0, 5.0)
        val scaled = nutrients.scale(-1.0)
        assertEquals(-100.0, scaled.calories)
    }

    @Test
    fun test_compatibleMeasurementUnits_invalid_unit_returns_unit_itself() {
        val result = compatibleMeasurementUnits("invalid")
        assertEquals(listOf("invalid"), result)
    }

    @Test
    fun test_measurementUnits_contains_all_expected_units() {
        assertTrue(measurementUnits.contains("gram"))
        assertTrue(measurementUnits.contains("kg"))
        assertTrue(measurementUnits.contains("ml"))
        assertTrue(measurementUnits.contains("cup"))
        assertTrue(measurementUnits.contains("serving"))
    }
}
