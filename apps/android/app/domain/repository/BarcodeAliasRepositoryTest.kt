package com.philipcosgrave.calorietracker.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.philipcosgrave.calorietracker.data.local.CalorieTrackerDatabase
import com.philipcosgrave.calorietracker.data.local.BarcodeAliasDao
import com.philipcosgrave.calorietracker.domain.createBarcodeAliasRecord
import com.philipcosgrave.calorietracker.model.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class BarcodeAliasRepositoryTest {

    private lateinit var database: CalorieTrackerDatabase
    private lateinit var dao: BarcodeAliasDao
    private lateinit var repository: RoomBarcodeAliasRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, CalorieTrackerDatabase::class.java)
            .allowMainThreadOperations()
            .build()
        dao = database.barcodeAliasDao()
        repository = RoomBarcodeAliasRepository(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun test_list_returns_all_aliases() = runBlocking {
        val food1 = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Apple",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(52.0)
        )
        val food2 = FoodItem(
            id = "food-2",
            kind = FoodKind.Ingredient,
            name = "Banana",
            barcode = "987654321098",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(89.0)
        )

        val alias1 = createBarcodeAliasRecord(food1, "device-1", "user-1")
        val alias2 = createBarcodeAliasRecord(food2, "device-1", "user-2")

        repository.save(alias1!!)
        repository.save(alias2!!)

        val list = repository.list()
        assertEquals(2, list.size)
        assertTrue(list.any { it.barcode == "123456789012" })
        assertTrue(list.any { it.barcode == "987654321098" })
    }

    @Test
    fun test_getByBarcode_returns_alias() = runBlocking {
        val food = FoodItem(
            id = "test-food",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-1")!!

        repository.save(alias)

        val result = repository.getByBarcode("123456789012")
        assertNotNull(result)
        assertEquals("123456789012", result.barcode)
        assertEquals("test-food", result.productId)
        assertEquals("user-1", result.ownerUserId)
    }

    @Test
    fun test_getByBarcode_returns_null_for_nonexistent() = runBlocking {
        val result = repository.getByBarcode("nonexistent")
        assertNull(result)
    }

    @Test
    fun test_save_upserts_alias() = runBlocking {
        val food1 = FoodItem(
            id = "upsert-food",
            kind = FoodKind.Ingredient,
            name = "Original Name",
            barcode = "upsert-barcode",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias1 = createBarcodeAliasRecord(food1, "device-1", "user-1")!!
        repository.save(alias1)

        val list1 = repository.list()
        assertEquals(1, list1.size)

        // Upsert with updated user
        val food2 = FoodItem(
            id = "upsert-food",
            kind = FoodKind.Ingredient,
            name = "Original Name",
            barcode = "upsert-barcode",
            servingQuantity = 150.0,
            servingUnit = "gram",
            nutrients = Nutrients(150.0)
        )

        val alias2 = createBarcodeAliasRecord(food2, "device-1", "user-2")!!
        repository.save(alias2)

        val list2 = repository.list()
        assertEquals(1, list2.size)
        assertEquals("user-2", list2[0].ownerUserId)
        assertEquals(2, list2[0].sync.version)
    }

    @Test
    fun test_soft_delete_marks_alias_as_deleted() = runBlocking {
        val food = FoodItem(
            id = "delete-food",
            kind = FoodKind.Ingredient,
            name = "To Be Deleted",
            barcode = "delete-barcode",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-1")!!

        repository.save(alias)
        val listBefore = repository.list()
        assertEquals(1, listBefore.size)

        repository.softDelete("delete-barcode", "2024-01-02T00:00:00Z")
        val listAfter = repository.list()
        assertEquals(1, listAfter.size)
        assertEquals("To Be Deleted", listAfter[0].name)
        assertNotNull(listAfter[0].sync.deletedAt)
        assertEquals(SyncStatus.LocalOnly, listAfter[0].sync.syncStatus)
    }

    @Test
    fun test_soft_delete_no_op_for_nonexistent() = runBlocking {
        repository.softDelete("nonexistent", "2024-01-02T00:00:00Z")
        // No exception should be thrown
    }

    @Test
    fun test_save_preserves_sync_metadata() = runBlocking {
        val food = FoodItem(
            id = "metadata-food",
            kind = FoodKind.Ingredient,
            name = "Metadata Test",
            barcode = "metadata-barcode",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val metadata = SyncMetadata(
            recordId = "user-1:metadata-barcode",
            deviceId = "special-device",
            updatedAt = "2024-01-01T10:00:00Z",
            version = 10,
            deletedAt = "2024-01-02T00:00:00Z",
            lastSyncedAt = "2024-01-01T09:59:59Z",
            syncStatus = SyncStatus.SyncError
        )

        val alias = BarcodeAliasRecord(
            barcode = "metadata-barcode",
            productId = "metadata-food",
            ownerUserId = "user-1",
            visibility = "private",
            createdAt = "2024-01-01T09:00:00Z",
            sync = metadata
        )

        repository.save(alias)

        val result = repository.getByBarcode("metadata-barcode")
        assertNotNull(result)
        assertEquals("special-device", result.sync.deviceId)
        assertEquals("2024-01-01T10:00:00Z", result.sync.updatedAt)
        assertEquals(10, result.sync.version)
        assertEquals(SyncStatus.SyncError, result.sync.syncStatus)
    }

    @Test
    fun test_list_empty_database() = runBlocking {
        val list = repository.list()
        assertTrue(list.isEmpty())
    }

    @Test
    fun test_getByBarcode_empty_database() = runBlocking {
        val result = repository.getByBarcode("nonexistent")
        assertNull(result)
    }

    @Test
    fun test_save_with_different_owners() = runBacking {
        val food = FoodItem(
            id = "shared-food",
            kind = FoodKind.Ingredient,
            name = "Shared Food",
            barcode = "shared-barcode",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias1 = createBarcodeAliasRecord(food, "device-1", "user-1")!!
        val alias2 = createBarcodeAliasRecord(food, "device-2", "user-2")!!

        repository.save(alias1)
        repository.save(alias2)

        // Should only return one (the last saved)
        val list = repository.list()
        assertEquals(1, list.size)
        assertEquals("user-2", list[0].ownerUserId)
    }

    @Test
    fun test_save_with_null_barcode() = runBlocking {
        val food = FoodItem(
            id = "no-barcode",
            kind = FoodKind.Ingredient,
            name = "No Barcode Food",
            barcode = "",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-1")
        assertNull(alias) // Should be null due to no barcode
    }

    @Test
    fun test_save_with_null_barcode_in_food() = runBlocking {
        val food = FoodItem(
            id = "null-barcode",
            kind = FoodKind.Ingredient,
            name = "Null Barcode Food",
            barcode = null,
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-1")
        assertNull(alias) // Should be null due to null barcode
    }

    @Test
    fun test_save_with_blank_barcode() = runBlocking {
        val food = FoodItem(
            id = "blank-barcode",
            kind = FoodKind.Ingredient,
            name = "Blank Barcode Food",
            barcode = "   ",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-1")
        assertNull(alias) // Should be null due to whitespace-only barcode
    }

    @Test
    fun test_soft_delete_preserves_version() = runBlocking {
        val food = FoodItem(
            id = "version-food",
            kind = FoodKind.Ingredient,
            name = "Version Test",
            barcode = "version-barcode",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-1")!!

        repository.save(alias)
        val record1 = repository.getByBarcode("version-barcode")
        assertEquals(1, record1?.sync.version)

        repository.softDelete("version-barcode", "2024-01-02T00:00:00Z")
        val record2 = repository.getByBarcode("version-barcode")
        assertEquals(2, record2?.sync.version)
    }

    @Test
    fun test_getByBarcode_with_trailing_spaces() = runBlocking {
        val food = FoodItem(
            id = "trailing-food",
            kind = FoodKind.Ingredient,
            name = "Trailing Spaces",
            barcode = "123   ",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        repository.save(createBarcodeAliasRecord(food, "device-1", "user-1")!!)

        // Should not match with trailing spaces
        val result = repository.getByBarcode("123   ")
        assertNull(result)
    }

    @Test
    fun test_soft_delete_with_different_users() = runBlocking {
        val food1 = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Food 1",
            barcode = "barcode-1",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )
        val food2 = FoodItem(
            id = "food-2",
            kind = FoodKind.Ingredient,
            name = "Food 2",
            barcode = "barcode-2",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias1 = createBarcodeAliasRecord(food1, "device-1", "user-1")!!
        val alias2 = createBarcodeAliasRecord(food2, "device-1", "user-2")!!

        repository.save(alias1)
        repository.save(alias2)

        // Delete user-1's alias
        repository.softDelete("barcode-1", "2024-01-02T00:00:00Z")

        val remaining = repository.list()
        assertEquals(1, remaining.size)
        assertEquals("barcode-2", remaining[0].barcode)
    }
}
