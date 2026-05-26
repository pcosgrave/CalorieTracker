package com.philipcosgrave.calorietracker.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.philipcosgrave.calorietracker.data.local.CalorieTrackerDatabase
import com.philipcosgrave.calorietracker.data.local.FoodRecordDao
import com.philipcosgrave.calorietracker.data.local.FoodRecordEntity
import com.philipcosgrave.calorietracker.domain.createFoodRecord
import com.philipcosgrave.calorietracker.domain.nowIsoString
import com.philipcosgrave.calorietracker.model.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FoodRepositoryTest {

    private lateinit var database: CalorieTrackerDatabase
    private lateinit var dao: FoodRecordDao
    private lateinit var repository: RoomFoodRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, CalorieTrackerDatabase::class.java)
            .allowMainThreadOperations()
            .build()
        dao = database.foodRecordDao()
        repository = RoomFoodRepository(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun test_list_returns_all_food_items() = runBlocking {
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

        repository.save(createFoodRecord(food1, "device-1"))
        repository.save(createFoodRecord(food2, "device-1"))

        val list = repository.list()
        assertEquals(2, list.size)
        assertTrue(list.any { it.food.id == "food-1" })
        assertTrue(list.any { it.food.id == "food-2" })
    }

    @Test
    fun test_getById_returns_food_record() = runBlocking {
        val food = FoodItem(
            id = "test-food",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createFoodRecord(food, "device-1"))

        val record = repository.getById("test-food")
        assertNotNull(record)
        assertEquals("Test Food", record?.food.name)
        assertEquals("test-food", record?.sync.recordId)
    }

    @Test
    fun test_getById_returns_null_for_nonexistent() = runBlocking {
        val record = repository.getById("nonexistent")
        assertNull(record)
    }

    @Test
    fun test_getByBarcode_returns_food_record() = runBlocking {
        val food = FoodItem(
            id = "barcode-food",
            kind = FoodKind.Ingredient,
            name = "Barcoded Food",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createFoodRecord(food, "device-1"))

        val record = repository.getByBarcode("123456789012")
        assertNotNull(record)
        assertEquals("Barcoded Food", record?.food.name)
        assertEquals("123456789012", record?.food.barcode)
    }

    @Test
    fun test_getByBarcode_returns_null_for_nonexistent() = runBlocking {
        val record = repository.getByBarcode("nonexistent")
        assertNull(record)
    }

    @Test
    fun test_save_upserts_food_record() = runBlocking {
        val food1 = FoodItem(
            id = "upsert-food",
            kind = FoodKind.Ingredient,
            name = "Original Name",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createFoodRecord(food1, "device-1"))
        val list1 = repository.list()
        assertEquals(1, list1.size)
        assertEquals("Original Name", list1[0].food.name)

        // Upsert with updated name
        val food2 = FoodItem(
            id = "upsert-food",
            kind = FoodKind.Ingredient,
            name = "Updated Name",
            servingQuantity = 150.0,
            servingUnit = "gram",
            nutrients = Nutrients(150.0)
        )

        repository.save(createFoodRecord(food2, "device-1"))
        val list2 = repository.list()
        assertEquals(1, list2.size)
        assertEquals("Updated Name", list2[0].food.name)
        assertEquals(2, list2[0].sync.version)
    }

    @Test
    fun test_soft_delete_marks_record_as_deleted() = runBlocking {
        val food = FoodItem(
            id = "delete-food",
            kind = FoodKind.Ingredient,
            name = "To Be Deleted",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createFoodRecord(food, "device-1"))
        val listBefore = repository.list()
        assertEquals(1, listBefore.size)

        repository.softDelete("delete-food", "2024-01-02T00:00:00Z")
        val listAfter = repository.list()
        assertEquals(1, listAfter.size)
        assertEquals("To Be Deleted", listAfter[0].food.name)
        assertNotNull(listAfter[0].sync.deletedAt)
        assertEquals(SyncStatus.LocalOnly, listAfter[0].sync.syncStatus)
    }

    @Test
    fun test_soft_delete_no_op_for_nonexistent() = runBlocking {
        repository.softDelete("nonexistent", "2024-01-02T00:00:00Z")
        // No exception should be thrown
    }

    @Test
    fun test_save_preserves_sync_metadata_fields() = runBlocking {
        val food = FoodItem(
            id = "metadata-food",
            kind = FoodKind.Ingredient,
            name = "Metadata Test",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val metadata = SyncMetadata(
            recordId = "metadata-food",
            deviceId = "special-device",
            updatedAt = "2024-01-01T10:00:00Z",
            version = 10,
            deletedAt = "2024-01-02T00:00:00Z",
            lastSyncedAt = "2024-01-01T09:59:59Z",
            syncStatus = SyncStatus.SyncError
        )

        repository.save(FoodItemRecord(
            food = food,
            sync = metadata
        ))

        val record = repository.getById("metadata-food")
        assertNotNull(record)
        assertEquals("special-device", record?.sync.deviceId)
        assertEquals("2024-01-01T10:00:00Z", record?.sync.updatedAt)
        assertEquals(10, record?.sync.version)
        assertEquals("2024-01-02T00:00:00Z", record?.sync.deletedAt)
        assertEquals("2024-01-01T09:59:59Z", record?.sync.lastSyncedAt)
        assertEquals(SyncStatus.SyncError, record?.sync.syncStatus)
    }

    @Test
    fun test_save_with_empty_barcode() = runBlocking {
        val food = FoodItem(
            id = "empty-barcode-food",
            kind = FoodKind.Ingredient,
            name = "Empty Barcode",
            barcode = "",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createFoodRecord(food, "device-1"))

        val record = repository.getById("empty-barcode-food")
        assertNotNull(record)
        assertNull(record?.food.barcode)
    }

    @Test
    fun test_save_with_null_barcode() = runBlocking {
        val food = FoodItem(
            id = "null-barcode-food",
            kind = FoodKind.Ingredient,
            name = "Null Barcode",
            barcode = null,
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createFoodRecord(food, "device-1"))

        val record = repository.getById("null-barcode-food")
        assertNotNull(record)
        assertNull(record?.food.barcode)
    }

    @Test
    fun test_list_empty_database() = runBlocking {
        val list = repository.list()
        assertTrue(list.isEmpty())
    }

    @Test
    fun test_getById_empty_database() = runBlocking {
        val record = repository.getById("nonexistent")
        assertNull(record)
    }

    @Test
    fun test_save_with_different_devices() = runBacking {
        val food = FoodItem(
            id = "multi-device-food",
            kind = FoodKind.Ingredient,
            name = "Multi Device",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createFoodRecord(food, "device-1"))
        val record1 = repository.getById("multi-device-food")
        assertEquals("device-1", record1?.sync.deviceId)

        // Simulate update from different device
        val updatedFood = FoodItem(
            id = "multi-device-food",
            kind = FoodKind.Ingredient,
            name = "Multi Device Updated",
            servingQuantity = 120.0,
            servingUnit = "gram",
            nutrients = Nutrients(120.0)
        )

        repository.save(createFoodRecord(updatedFood, "device-2", record1))
        val record2 = repository.getById("multi-device-food")
        assertEquals("device-2", record2?.sync.deviceId)
    }

    @Test
    fun test_getByBarcode_with_null_barcode_food() = runBlocking {
        val food = FoodItem(
            id = "null-barcode",
            kind = FoodKind.Ingredient,
            name = "Null Barcode Food",
            barcode = null,
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createFoodRecord(food, "device-1"))

        // Should not find by barcode since food has null barcode
        val record = repository.getByBarcode("any-barcode")
        assertNull(record)
    }

    @Test
    fun test_soft_delete_preserves_version_number() = runBlocking {
        val food = FoodItem(
            id = "version-food",
            kind = FoodKind.Ingredient,
            name = "Version Test",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createFoodRecord(food, "device-1"))
        val record1 = repository.getById("version-food")
        assertEquals(1, record1?.sync.version)

        repository.softDelete("version-food", "2024-01-02T00:00:00Z")
        val record2 = repository.getById("version-food")
        assertEquals(2, record2?.sync.version)
    }
}
