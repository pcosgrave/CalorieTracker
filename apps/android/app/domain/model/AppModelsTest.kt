package com.philipcosgrave.calorietracker.model

import org.junit.Assert.*
import org.junit.Test

class AppModelsTest {

    @Test
    fun test_AuthSession_creation_with_all_fields() {
        val session = AuthSession(
            userSub = "sub-123",
            accessToken = "access_token_abc",
            idToken = "id_token_def",
            email = "user@example.com",
            name = "Test User",
            expiresAtEpochMs = System.currentTimeMillis() + 3600000 // 1 hour
        )

        assertEquals("sub-123", session.userSub)
        assertEquals("access_token_abc", session.accessToken)
        assertEquals("id_token_def", session.idToken)
        assertEquals("user@example.com", session.email)
        assertEquals("Test User", session.name)
        assertTrue(session.expiresAtEpochMs > System.currentTimeMillis())
    }

    @Test
    fun test_AuthSession_creation_with_refresh_token() {
        val session = AuthSession(
            userSub = "sub-456",
            accessToken = "access_token_xyz",
            idToken = "id_token_zyx",
            refreshToken = "refresh_token_abc",
            expiresAtEpochMs = System.currentTimeMillis() + 7200000
        )

        assertEquals("sub-456", session.userSub)
        assertNotNull(session.refreshToken)
        assertEquals("refresh_token_abc", session.refreshToken)
    }

    @Test
    fun test_AuthSession_creation_without_optional_fields() {
        val session = AuthSession(
            userSub = "sub-789",
            accessToken = "access_token_new"
        )

        assertEquals("sub-789", session.userSub)
        assertEquals("access_token_new", session.accessToken)
        assertNull(session.idToken)
        assertNull(session.email)
        assertNull(session.name)
        assertNull(session.refreshToken)
        assertTrue(session.expiresAtEpochMs > 0)
    }

    @Test
    fun test_Meal_enum_values() {
        val meals = Meal.entries
        assertEquals(4, meals.size)

        val labels = Meal.entries.map { it.label }
        assertTrue(labels.contains("Breakfast"))
        assertTrue(labels.contains("Lunch"))
        assertTrue(labels.contains("Dinner"))
        assertTrue(labels.contains("Snack"))
    }

    @Test
    fun test_FoodKind_enum_values() {
        val kinds = FoodKind.entries
        assertEquals(2, kinds.size)

        val names = FoodKind.entries.map { it.name }
        assertTrue(names.contains("INGREDIENT"))
        assertTrue(names.contains("RECIPE"))
    }

    @Test
    fun test_SortMode_enum_values() {
        val modes = SortMode.entries
        assertEquals(3, modes.size)

        val labels = SortMode.entries.map { it.label }
        assertTrue(labels.contains("Recent"))
        assertTrue(labels.contains("Frequency"))
        assertTrue(labels.contains("Alphabetical"))
    }

    @Test
    fun test_Nutrients_creation() {
        val nutrients = Nutrients(
            calories = 250.0,
            proteinGrams = 20.0,
            carbohydrateGrams = 30.0,
            fatGrams = 10.0
        )

        assertEquals(250.0, nutrients.calories)
        assertEquals(20.0, nutrients.proteinGrams)
        assertEquals(30.0, nutrients.carbohydrateGrams)
        assertEquals(10.0, nutrients.fatGrams)
    }

    @Test
    fun test_Nutrients_defaults() {
        val nutrients = Nutrients(500.0)

        assertEquals(500.0, nutrients.calories)
        assertEquals(0.0, nutrients.proteinGrams)
        assertEquals(0.0, nutrients.carbohydrateGrams)
        assertEquals(0.0, nutrients.fatGrams)
    }

    @Test
    fun test_FoodItem_creation() {
        val food = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Apple",
            brand = "Organic Farms",
            barcode = "012345678901",
            servingQuantity = 150.0,
            servingUnit = "gram",
            nutrients = Nutrients(52.0, 0.3, 14.0, 0.3),
            frequency = 5,
            lastUsedDaysAgo = 2,
            isUserCreated = true
        )

        assertEquals("food-1", food.id)
        assertEquals(FoodKind.Ingredient, food.kind)
        assertEquals("Apple", food.name)
        assertEquals("Organic Farms", food.brand)
        assertEquals("012345678901", food.barcode)
        assertEquals(150.0, food.servingQuantity)
        assertEquals("gram", food.servingUnit)
        assertEquals(52.0, food.nutrients.calories)
        assertEquals(5, food.frequency)
        assertEquals(2, food.lastUsedDaysAgo)
        assertTrue(food.isUserCreated)
    }

    @Test
    fun test_FoodItem_empty_components() {
        val food = FoodItem(
            id = "food-2",
            kind = FoodKind.Ingredient,
            name = "Banana",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(89.0)
        )

        assertTrue(food.components.isEmpty())
    }

    @Test
    fun test_RecipeComponent_creation() {
        val component = RecipeComponent(
            item = FoodItem(
                id = "ingredient-1",
                kind = FoodKind.Ingredient,
                name = "Egg",
                servingQuantity = 50.0,
                servingUnit = "gram",
                nutrients = Nutrients(155.0, 13.0, 1.1, 11.0)
            ),
            amount = 2.0,
            unit = "large"
        )

        assertEquals("ingredient-1", component.item.id)
        assertEquals(2.0, component.amount)
        assertEquals("large", component.unit)
    }

    @Test
    fun test_DiaryEntry_creation() {
        val entry = DiaryEntry(
            id = "entry-1",
            food = FoodItem(
                id = "food-1",
                kind = FoodKind.Ingredient,
                name = "Oatmeal",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(150.0)
            ),
            date = java.time.LocalDate.now(),
            meal = Meal.Breakfast,
            servingMultiplier = 1.5
        )

        assertEquals("entry-1", entry.id)
        assertEquals("Oatmeal", entry.food.name)
        assertEquals(Meal.Breakfast, entry.meal)
        assertEquals(1.5, entry.servingMultiplier)
    }

    @Test
    fun test_WeightEntry_creation() {
        val entry = WeightEntry(
            id = "weight-1",
            date = java.time.LocalDate.now(),
            weightKg = 70.5
        )

        assertEquals("weight-1", entry.id)
        assertEquals(java.time.LocalDate.now(), entry.date)
        assertEquals(70.5, entry.weightKg)
    }

    @Test
    fun test_WeightChartRange_enum_values() {
        val ranges = WeightChartRange.entries
        assertEquals(3, ranges.size)

        val labels = WeightChartRange.entries.map { it.label }
        assertTrue(labels.contains("Daily"))
        assertTrue(labels.contains("Weekly"))
        assertTrue(labels.contains("Monthly"))
    }

    @Test
    fun test_HealthDashboardMetrics_creation_with_all_values() {
        val metrics = HealthDashboardMetrics(
            steps = 10000L,
            heartRateBpm = 72L,
            caloriesBurned = 500.0
        )

        assertEquals(10000L, metrics.steps)
        assertEquals(72L, metrics.heartRateBpm)
        assertEquals(500.0, metrics.caloriesBurned)
    }

    @Test
    fun test_HealthDashboardMetrics_partial_values() {
        val metrics = HealthDashboardMetrics(
            steps = 5000L,
            heartRateBpm = null
        )

        assertEquals(5000L, metrics.steps)
        assertNull(metrics.heartRateBpm)
        assertNull(metrics.caloriesBurned)
    }

    @Test
    fun test_HealthDashboardMetrics_all_nulls() {
        val metrics = HealthDashboardMetrics()

        assertNull(metrics.steps)
        assertNull(metrics.heartRateBpm)
        assertNull(metrics.caloriesBurned)
    }

    @Test
    fun test_SyncMetadata_creation_with_all_fields() {
        val metadata = SyncMetadata(
            recordId = "test-record",
            deviceId = "device-a",
            updatedAt = "2024-01-01T12:00:00Z",
            version = 10,
            deletedAt = "2024-01-02T00:00:00Z",
            lastSyncedAt = "2024-01-01T11:59:59Z",
            syncStatus = SyncStatus.Synced
        )

        assertEquals("test-record", metadata.recordId)
        assertEquals(10, metadata.version)
        assertEquals("2024-01-01T12:00:00Z", metadata.updatedAt)
        assertEquals("device-a", metadata.originDeviceId)
        assertEquals("2024-01-02T00:00:00Z", metadata.deletedAt)
        assertEquals("2024-01-01T11:59:59Z", metadata.lastSyncedAt)
        assertEquals(SyncStatus.Synced, metadata.syncStatus)
    }

    @Test
    fun test_DiaryEntryRecord_creation_with_all_fields() {
        val entry = DiaryEntry(
            id = "entry-test",
            food = FoodItem(
                id = "food-test",
                kind = FoodKind.Ingredient,
                name = "Test Food",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            date = java.time.LocalDate.now(),
            meal = Meal.Dinner,
            servingMultiplier = 1.0
        )

        val record = DiaryEntryRecord(
            entry = entry,
            sync = SyncMetadata(
                recordId = "entry-test",
                deviceId = "device-test",
                updatedAt = "2024-01-01T12:00:00Z"
            )
        )

        assertEquals("entry-test", record.entry.id)
        assertEquals("entry-test", record.sync.recordId)
        assertEquals("device-test", record.sync.deviceId)
    }
}
