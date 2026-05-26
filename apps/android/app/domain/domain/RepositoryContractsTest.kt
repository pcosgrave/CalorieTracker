package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.test.MockDiaryRepository
import com.philipcosgrave.calorietracker.test.MockFoodRepository
import com.philipcosgrave.calorietracker.test.MockLocalRepositories
import com.philipcosgrave.calorietracker.test.MockWeightRepository
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking

class RepositoryContractsTest {

    @Test
    fun test_MockFoodRepository_operations() = runBlocking {
        val mockRepo = MockFoodRepository()
        val mockLocalRepo = MockLocalRepositories(mockFoodRepository = mockRepo)

        // Add food items
        val food1 = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Apple",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(52.0)
        )

        val food2 = FoodItem(
            id = "food-2",
            kind = FoodKind.Ingredient,
            name = "Banana",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(89.0)
        )

        mockRepo.addFoodItem(food1)
        mockRepo.addFoodItem(food2)

        // Get all food items
        val items = mockRepo.getFoodItems()
        assertEquals(2, items.size)
        assertTrue(items.any { it.id == "food-1" })
        assertTrue(items.any { it.id == "food-2" })

        // Get food by ID
        val apple = mockRepo.getFoodById("food-1")
        assertNotNull(apple)
        assertEquals("Apple", apple?.name)

        val banana = mockRepo.getFoodById("food-3")
        assertNull(banana)

        // Add food record
        val record = createFoodRecord(food1, "device-1")
        mockRepo.addFoodRecord(record)

        // Get all records
        val records = mockRepo.getFoodRecords()
        assertTrue(records.size >= 1)

        // Update record
        val updatedFood = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Apple (Updated)",
            servingQuantity = 120.0,
            servingUnit = "gram",
            nutrients = Nutrients(62.4)
        )
        val updatedRecord = createFoodRecord(updatedFood, "device-1", records.firstOrNull { it.recordId == "food-1" })
        val result = mockRepo.updateFoodRecord(updatedRecord)
        assertNotNull(result)
        assertEquals("Apple (Updated)", result?.food?.name)

        // Delete record
        val deleted = mockRepo.deleteFoodRecord("food-1")
        assertTrue(deleted)

        val remaining = mockRepo.getFoodRecords()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun test_MockDiaryRepository_operations() = runBlocking {
        val mockRepo = MockDiaryRepository()
        val mockLocalRepo = MockLocalRepositories(mockDiaryRepository = mockRepo)

        // Add diary entries
        val entry1 = DiaryEntry(
            id = "entry-1",
            food = FoodItem(
                id = "food-1",
                kind = FoodKind.Ingredient,
                name = "Oatmeal",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(150.0)
            ),
            date = java.time.LocalDate.now().minusDays(1),
            meal = Meal.Breakfast,
            servingMultiplier = 1.5
        )

        val entry2 = DiaryEntry(
            id = "entry-2",
            food = FoodItem(
                id = "food-2",
                kind = FoodKind.Ingredient,
                name = "Chicken Salad",
                servingQuantity = 200.0,
                servingUnit = "gram",
                nutrients = Nutrients(350.0)
            ),
            date = java.time.LocalDate.now(),
            meal = Meal.Lunch,
            servingMultiplier = 1.0
        )

        mockRepo.addDiaryEntry(entry1)
        mockRepo.addDiaryEntry(entry2)

        // Get all diary entries
        val entries = mockRepo.getDiaryEntries()
        assertEquals(2, entries.size)

        // Get entries by date
        val todayEntries = mockRepo.getDiaryEntriesForDate(java.time.LocalDate.now())
        assertEquals(1, todayEntries.size)
        assertEquals(Meal.Lunch, todayEntries[0].meal)

        // Get entry for date and meal
        val lunchEntry = mockRepo.getDiaryEntryForDateAndMeal(
            java.time.LocalDate.now(),
            Meal.Lunch
        )
        assertNotNull(lunchEntry)
        assertEquals("Chicken Salad", lunchEntry.food.name)

        // Add another entry for today
        val dinnerEntry = DiaryEntry(
            id = "entry-3",
            food = FoodItem(
                id = "food-3",
                kind = FoodKind.Ingredient,
                name = "Fish and Chips",
                servingQuantity = 300.0,
                servingUnit = "gram",
                nutrients = Nutrients(450.0)
            ),
            date = java.time.LocalDate.now(),
            meal = Meal.Dinner,
            servingMultiplier = 1.0
        )
        mockRepo.addDiaryEntry(dinnerEntry)

        // Verify we now have 2 entries for today
        val todayEntriesAfter = mockRepo.getDiaryEntriesForDate(java.time.LocalDate.now())
        assertEquals(2, todayEntriesAfter.size)

        // Update record
        val updatedEntry = DiaryEntry(
            id = "entry-2",
            food = FoodItem(
                id = "food-2",
                kind = FoodKind.Ingredient,
                name = "Chicken Salad (Updated)",
                servingQuantity = 180.0,
                servingUnit = "gram",
                nutrients = Nutrients(306.0)
            ),
            date = java.time.LocalDate.now(),
            meal = Meal.Lunch,
            servingMultiplier = 1.0
        )
        val updatedRecord = DiaryEntryRecord(
            entry = updatedEntry,
            sync = createSyncMetadata("entry-2", "device-test")
        )
        val result = mockRepo.updateDiaryRecord(updatedRecord)
        assertNotNull(result)
        assertEquals("Chicken Salad (Updated)", result?.entry?.food?.name)

        // Delete entry
        val deleted = mockRepo.deleteDiaryRecord("entry-2")
        assertTrue(deleted)

        val remaining = mockRepo.getDiaryEntries()
        assertEquals(2, remaining.size)
    }

    @Test
    fun test_MockBarcodeAliasRepository_operations() = runBlocking {
        val mockRepo = MockBarcodeAliasRepository()
        val mockLocalRepo = MockLocalRepositories(mockBarcodeAliasRepository = mockRepo)

        // Add barcode aliases
        val alias1 = BarcodeAliasRecord(
            barcode = "123456789012",
            productId = "product-1",
            ownerUserId = "user-1",
            visibility = "private",
            createdAt = "2024-01-01T00:00:00Z",
            sync = SyncMetadata(
                recordId = "user-1:123456789012",
                deviceId = "device-1"
            )
        )

        val alias2 = BarcodeAliasRecord(
            barcode = "987654321098",
            productId = "product-2",
            ownerUserId = "user-2",
            visibility = "private",
            createdAt = "2024-01-02T00:00:00Z",
            sync = SyncMetadata(
                recordId = "user-2:987654321098",
                deviceId = "device-2"
            )
        )

        mockRepo.addBarcodeAlias(alias1)
        mockRepo.addBarcodeAlias(alias2)

        // Get all aliases
        val aliases = mockRepo.getBarcodeAliases()
        assertEquals(2, aliases.size)

        // Get aliases by owner
        val user1Aliases = mockRepo.getBarcodeAliasesByOwner("user-1")
        assertEquals(1, user1Aliases.size)
        assertEquals("123456789012", user1Aliases[0].barcode)

        // Get alias by barcode
        val foundAlias = mockRepo.getBarcodeAlias("123456789012")
        assertNotNull(foundAlias)
        assertEquals("product-1", foundAlias.productId)

        // Get non-existent alias
        val nullAlias = mockRepo.getBarcodeAlias("non-existent")
        assertNull(nullAlias)

        // Delete alias
        val deleted = mockRepo.deleteBarcodeAlias("123456789012")
        assertTrue(deleted)

        val remaining = mockRepo.getBarcodeAliases()
        assertEquals(1, remaining.size)
        assertEquals("987654321098", remaining[0].barcode)
    }

    @Test
    fun test_MockWeightRepository_operations() = runBlocking {
        val mockRepo = MockWeightRepository()
        val mockLocalRepo = MockLocalRepositories(mockWeightRepository = mockRepo)

        // Add weight entries
        val entry1 = WeightEntry(
            id = "weight-1",
            date = java.time.LocalDate.now().minusDays(1),
            weightKg = 70.5
        )

        val entry2 = WeightEntry(
            id = "weight-2",
            date = java.time.LocalDate.now(),
            weightKg = 70.2
        )

        val entry3 = WeightEntry(
            id = "weight-3",
            date = java.time.LocalDate.now().minusDays(3),
            weightKg = 71.0
        )

        mockRepo.addWeightEntry(entry1)
        mockRepo.addWeightEntry(entry2)
        mockRepo.addWeightEntry(entry3)

        // Get all weight entries
        val entries = mockRepo.getWeightEntries()
        assertEquals(3, entries.size)

        // Get entries for specific date
        val todayEntries = mockRepo.getWeightEntriesForDate(java.time.LocalDate.now())
        assertEquals(1, todayEntries.size)
        assertEquals(70.2, todayEntries[0].weightKg)

        // Get latest weight entry
        val latest = mockRepo.getLatestWeightEntry()
        assertNotNull(latest)
        assertEquals(70.2, latest.weightKg)

        // Get weight entry for specific date
        val dateEntry = mockRepo.getWeightEntryForDate(java.time.LocalDate.now().minusDays(1))
        assertNotNull(dateEntry)
        assertEquals(70.5, dateEntry.weightKg)

        // Update weight entry
        val updatedEntry = WeightEntry(
            id = "weight-1",
            date = java.time.LocalDate.now().minusDays(1),
            weightKg = 70.3
        )
        val result = mockRepo.updateWeightEntry(updatedEntry)
        assertNotNull(result)
        assertEquals(70.3, result.weightKg)

        // Verify update reflected
        val entriesAfter = mockRepo.getWeightEntries()
        val updatedEntryAfter = entriesAfter.find { it.id == "weight-1" }
        assertEquals(70.3, updatedEntryAfter?.weightKg)

        // Delete weight entry
        val deleted = mockRepo.deleteWeightEntry("weight-2")
        assertTrue(deleted)

        val remaining = mockRepo.getWeightEntries()
        assertEquals(2, remaining.size)
    }

    @Test
    fun test_MockSyncRepository_operations() = runBlocking {
        val mockRepo = MockSyncRepository()
        val mockLocalRepo = MockLocalRepositories(mockSyncRepository = mockRepo)

        // Add sync metadata
        val metadata1 = SyncMetadata(
            recordId = "food-1",
            deviceId = "device-1",
            updatedAt = "2024-01-01T12:00:00Z",
            syncStatus = SyncStatus.LocalOnly
        )

        val metadata2 = SyncMetadata(
            recordId = "food-2",
            deviceId = "device-1",
            updatedAt = "2024-01-01T12:00:00Z",
            syncStatus = SyncStatus.PendingPush
        )

        mockRepo.addSyncMetadata("food-1", metadata1)
        mockRepo.addSyncMetadata("food-2", metadata2)

        // Get sync metadata by record ID
        val meta1 = mockRepo.getSyncMetadata("food-1")
        assertNotNull(meta1)
        assertEquals("food-1", meta1.recordId)

        val meta2 = mockRepo.getSyncMetadata("food-2")
        assertNotNull(meta2)
        assertEquals(SyncStatus.PendingPush, meta2.syncStatus)

        // Get sync metadata by device
        val deviceMetadata = mockRepo.getSyncMetadataByDevice("device-1")
        assertTrue(deviceMetadata.containsKey("food-1"))
        assertTrue(deviceMetadata.containsKey("food-2"))

        // Update sync metadata
        val updated = SyncMetadata(
            recordId = "food-1",
            deviceId = "device-1",
            updatedAt = "2024-01-02T12:00:00Z",
            version = 2,
            syncStatus = SyncStatus.Synced
        )
        val result = mockRepo.updateSyncMetadata("food-1", updated)
        assertNotNull(result)
        assertEquals("2024-01-02T12:00:00Z", result.updatedAt)
        assertEquals(2, result.version)

        // Mark as synced
        val synced = mockRepo.markAsSynced("food-2")
        assertNotNull(synced)
        assertEquals(SyncStatus.Synced, synced.syncStatus)
        assertNotNull(synced.lastSyncedAt)

        // Mark as error
        val error = mockRepo.markAsError("food-1")
        assertNotNull(error)
        assertEquals(SyncStatus.SyncError, error.syncStatus)

        // Get pending records
        val pending = mockRepo.getPendingRecords()
        assertEquals(0, pending.size) // food-2 was marked synced
    }

    @Test
    fun test_MockLocalRepositories_integration() = runBlocking {
        val mockFoodRepository = MockFoodRepository()
        val mockDiaryRepository = MockDiaryRepository()
        val mockBarcodeAliasRepository = MockBarcodeAliasRepository()
        val mockWeightRepository = MockWeightRepository()
        val mockSyncRepository = MockSyncRepository()

        val mockLocalRepo = MockLocalRepositories(
            deviceId = "integration-device",
            mockFoodRepository = mockFoodRepository,
            mockDiaryRepository = mockDiaryRepository,
            mockBarcodeAliasRepository = mockBarcodeAliasRepository,
            mockWeightRepository = mockWeightRepository,
            mockSyncRepository = mockSyncRepository
        )

        // Setup test data
        val food1 = FoodItem(
            id = "food-integration-1",
            kind = FoodKind.Ingredient,
            name = "Integrated Food",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        mockFoodRepository.addFoodItem(food1)

        // Add barcode alias
        val alias = createBarcodeAliasRecord(food1, "integration-device", "integration-user")
        mockBarcodeAliasRepository.addBarcodeAlias(alias)

        // Add diary entry
        val diaryEntry = DiaryEntry(
            id = "diary-integration-1",
            food = food1,
            date = java.time.LocalDate.now(),
            meal = Meal.Lunch,
            servingMultiplier = 1.0
        )
        mockDiaryRepository.addDiaryEntry(diaryEntry)

        // Add weight entry
        val weightEntry = WeightEntry(
            id = "weight-integration-1",
            date = java.time.LocalDate.now(),
            weightKg = 70.0
        )
        mockWeightRepository.addWeightEntry(weightEntry)

        // Add sync metadata
        val syncMetadata = SyncMetadata(
            recordId = "food-integration-1",
            deviceId = "integration-device",
            syncStatus = SyncStatus.Synced
        )
        mockSyncRepository.addSyncMetadata("food-integration-1", syncMetadata)

        // Verify all data
        val foods = mockFoodRepository.getFoodItems()
        assertEquals(1, foods.size)
        assertEquals("Integrated Food", foods[0].name)

        val aliases = mockBarcodeAliasRepository.getBarcodeAliases()
        assertEquals(1, aliases.size)
        assertEquals("123456789012", aliases[0].barcode)

        val entries = mockDiaryRepository.getDiaryEntries()
        assertEquals(1, entries.size)
        assertEquals(Meal.Lunch, entries[0].meal)

        val weights = mockWeightRepository.getWeightEntries()
        assertEquals(1, weights.size)
        assertEquals(70.0, weights[0].weightKg)

        val sync = mockSyncRepository.getSyncMetadata("food-integration-1")
        assertNotNull(sync)
        assertEquals(SyncStatus.Synced, sync.syncStatus)
    }
}
