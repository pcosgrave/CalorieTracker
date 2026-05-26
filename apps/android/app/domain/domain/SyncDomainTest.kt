package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SyncDomainTest {

    @Test
    fun test_nowIsoString_returns_current_timestamp() {
        val timestamp = nowIsoString()
        assertTrue(timestamp.startsWith("20")) // Year 20XX
        assertEquals("T", timestamp[10]) // ISO format has T separator
        assertTrue(timestamp.length >= 20) // At least YYYY-MM-DDTHH:MM:SS
    }

    @Test
    fun test_nowIsoString_consecutive_calls_different_timestamp() {
        val t1 = nowIsoString()
        Thread.sleep(100) // 100ms delay
        val t2 = nowIsoString()
        assertTrue(t1 != t2 || (t1.contains(".") && t1.split('.').last().toInt() < t2.split('.').last().toInt()))
    }

    @Test
    fun test_createSyncMetadata_basic() {
        val metadata = createSyncMetadata(
            recordId = "food-1",
            deviceId = "device-1",
            updatedAt = "2024-01-01T12:00:00Z",
            version = 1
        )

        assertEquals("food-1", metadata.recordId)
        assertEquals(1, metadata.version)
        assertEquals("2024-01-01T12:00:00Z", metadata.updatedAt)
        assertEquals(SyncStatus.PendingPush, metadata.syncStatus)
        assertEquals("device-1", metadata.originDeviceId)
        assertNull(metadata.deletedAt)
        assertNull(metadata.lastSyncedAt)
    }

    @Test
    fun test_createSyncMetadata_with_all_fields() {
        val metadata = createSyncMetadata(
            recordId = "diary-1",
            deviceId = "device-2",
            updatedAt = "2024-01-01T13:00:00Z",
            version = 5,
            deletedAt = "2024-01-02T00:00:00Z",
            lastSyncedAt = "2024-01-01T23:59:59Z",
            syncStatus = SyncStatus.Synced
        )

        assertEquals("diary-1", metadata.recordId)
        assertEquals(5, metadata.version)
        assertEquals("2024-01-01T13:00:00Z", metadata.updatedAt)
        assertEquals("2024-01-02T00:00:00Z", metadata.deletedAt)
        assertEquals("2024-01-01T23:59:59Z", metadata.lastSyncedAt)
        assertEquals(SyncStatus.Synced, metadata.syncStatus)
    }

    @Test
    fun test_markPendingSync_updates_food_record() {
        val metadata = createSyncMetadata(
            recordId = "food-1",
            deviceId = "device-1",
            version = 1,
            syncStatus = SyncStatus.Synced
        )

        val pending = metadata.markPendingSync(
            updatedAt = "2024-01-01T14:00:00Z"
        )

        assertEquals(2, pending.version)
        assertEquals(SyncStatus.PendingPush, pending.syncStatus)
        assertEquals("2024-01-01T14:00:00Z", pending.updatedAt)
    }

    @Test
    fun test_markPendingSync_with_deletion() {
        val metadata = createSyncMetadata("food-2", "device-1")
        val deleted = metadata.markPendingSync(
            updatedAt = "2024-01-01T14:00:00Z",
            deletedAt = "2024-01-01T15:00:00Z"
        )

        assertNotNull(deleted.deletedAt)
        assertEquals(SyncStatus.PendingPush, deleted.syncStatus)
    }

    @Test
    fun test_createFoodRecord_basic() {
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
        assertEquals("food-1", record.sync.recordId)
        assertEquals(SyncStatus.PendingPush, record.sync.syncStatus)
        assertEquals("device-1", record.sync.deviceId)
        assertNotNull(record.sync.updatedAt)
    }

    @Test
    fun test_createFoodRecord_with_existing_record() {
        val existing = createFoodRecord(
            FoodItem(id = "food-1", kind = FoodKind.Ingredient, name = "Old Name", nutrients = Nutrients(50.0)),
            "device-1"
        )

        val updatedFood = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "New Name",
            servingQuantity = 150.0,
            servingUnit = "gram",
            nutrients = Nutrients(150.0)
        )

        val newRecord = createFoodRecord(updatedFood, "device-1", existing)

        assertEquals("New Name", newRecord.food.name)
        assertEquals(2, newRecord.sync.version)
        assertEquals(SyncStatus.PendingPush, newRecord.sync.syncStatus)
    }

    @Test
    fun test_createBarcodeAliasRecord_with_barcode() {
        val food = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-1")

        assertNotNull(alias)
        assertEquals("123456789012", alias.barcode)
        assertEquals("food-1", alias.productId)
        assertEquals("user-1", alias.ownerUserId)
        assertEquals("private", alias.visibility)
        assertEquals("user-1:123456789012", alias.sync.recordId)
    }

    @Test
    fun test_createBarcodeAliasRecord_without_barcode() {
        val food = FoodItem(
            id = "food-2",
            kind = FoodKind.Ingredient,
            name = "No Barcode Food",
            barcode = "",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-2")
        assertNull(alias)
    }

    @Test
    fun test_createBarcodeAliasRecord_null_barcode() {
        val food = FoodItem(
            id = "food-3",
            kind = FoodKind.Ingredient,
            name = "Null Barcode Food",
            barcode = null,
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "user-3")
        assertNull(alias)
    }

    @Test
    fun test_createDiaryRecord_with_entry() {
        val entry = DiaryEntry(
            id = "entry-1",
            food = FoodItem(
                id = "food-1",
                kind = FoodKind.Ingredient,
                name = "Apple",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(52.0)
            ),
            date = java.time.LocalDate.now(),
            meal = Meal.Breakfast,
            servingMultiplier = 1.0
        )

        val record = DiaryEntryRecord(
            entry = entry,
            sync = SyncMetadata(
                recordId = "entry-1",
                deviceId = "device-1"
            )
        )

        assertEquals("entry-1", record.entry.id)
        assertEquals("Apple", record.entry.food.name)
    }

    @Test
    fun test_createChangeEnvelope_food_product() {
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-1",
            payload = mapOf("name" to "Test Food")
        )

        assertEquals(SyncEntityType.FoodProduct, envelope.entityType)
        assertEquals("food-1", envelope.recordId)
        assertEquals(SyncOperation.Upsert, envelope.operation)
        assertNotNull(envelope.payload)
    }

    @Test
    fun test_createChangeEnvelope_diary_entry() {
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.DiaryEntry,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "entry-1",
            payload = mapOf("meal" to "Lunch")
        )

        assertEquals(SyncEntityType.DiaryEntry, envelope.entityType)
        assertEquals("entry-1", envelope.recordId)
        assertNotNull(envelope.payload)
    }

    @Test
    fun test_createChangeEnvelope_delete_operation() {
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Delete,
            deviceId = "device-1",
            recordId = "food-delete",
            payload = null,
            baseVersion = 5L
        )

        assertEquals(SyncOperation.Delete, envelope.operation)
        assertNull(envelope.payload)
        assertEquals(5L, envelope.baseVersion)
    }

    @Test
    fun test_createChangeEnvelope_null_payload() {
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-empty"
        )

        assertNull(envelope.payload)
        assertNull(envelope.baseVersion)
    }

    @Test
    fun test_createChangeEnvelope_changeId_format() {
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-1"
        )

        assertTrue(envelope.changeId.startsWith("change-"))
    }

    @Test
    fun test_createChangeEnvelope_changedAt_timestamp() {
        val timestamp = nowIsoString()
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-test"
        )

        assertEquals(timestamp, envelope.changedAt)
    }

    @Test
    fun test_createFoodRecord_all_fields() {
        val food = FoodItem(
            id = "food-detailed",
            kind = FoodKind.Ingredient,
            name = "Detailed Food",
            brand = "Brand Name",
            barcode = "123456789012",
            servingQuantity = 200.0,
            servingUnit = "gram",
            nutrients = Nutrients(200.0, 20.0, 30.0, 10.0),
            frequency = 5,
            lastUsedDaysAgo = 2,
            isUserCreated = true
        )

        val record = createFoodRecord(food, "device-full")

        assertEquals("food-detailed", record.food.id)
        assertEquals("Brand Name", record.food.brand)
        assertEquals("123456789012", record.food.barcode)
        assertEquals(200.0, record.food.servingQuantity)
        assertEquals("gram", record.food.servingUnit)
        assertEquals(200.0, record.food.nutrients.calories)
        assertEquals(5, record.food.frequency)
        assertEquals(2, record.food.lastUsedDaysAgo)
        assertTrue(record.food.isUserCreated)
    }

    @Test
    fun test_createBarcodeAliasRecord_all_fields() {
        val food = FoodItem(
            id = "food-alias",
            kind = FoodKind.Ingredient,
            name = "Alias Food",
            barcode = "987654321098",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-alias", "user-alias")

        assertEquals("987654321098", alias.barcode)
        assertEquals("food-alias", alias.productId)
        assertEquals("user-alias", alias.ownerUserId)
        assertEquals("private", alias.visibility)
        assertEquals("user-alias:987654321098", alias.sync.recordId)
        assertNotNull(alias.sync.createdAt)
    }

    @Test
    fun test_createChangeEnvelope_multiple_devices() {
        val envelope1 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-a",
            recordId = "food-shared"
        )

        val envelope2 = createChangeEnvelope(
            entityType = SyncEntityType.DiaryEntry,
            operation = SyncOperation.Upsert,
            deviceId = "device-b",
            recordId = "entry-shared"
        )

        assertEquals("device-a", envelope1.deviceId)
        assertEquals("device-b", envelope2.deviceId)
        assertEquals(SyncEntityType.FoodProduct, envelope1.entityType)
        assertEquals(SyncEntityType.DiaryEntry, envelope2.entityType)
    }

    @Test
    fun test_createSyncMetadata_version_incrementation() {
        var metadata = createSyncMetadata("food-v1", "device-v1", version = 0)
        assertEquals(0, metadata.version)

        metadata = metadata.markPendingSync("t1").markPendingSync("t2").markPendingSync("t3")
        assertEquals(3, metadata.version)
    }

    @Test
    fun test_createSyncMetadata_defaults_on_null_fields() {
        val metadata = createSyncMetadata(
            recordId = "food-null",
            deviceId = "device-null",
            updatedAt = null,
            version = null,
            deletedAt = null,
            lastSyncedAt = null,
            syncStatus = null
        )

        assertEquals("food-null", metadata.recordId)
        assertEquals("device-null", metadata.originDeviceId)
        assertNotNull(metadata.updatedAt)
        assertEquals(1, metadata.version)
        assertNull(metadata.deletedAt)
        assertNull(metadata.lastSyncedAt)
        assertEquals(SyncStatus.PendingPush, metadata.syncStatus)
    }

    @Test
    fun test_createFoodRecord_with_null_deletedAt() {
        val food = FoodItem(
            id = "food-deleted",
            kind = FoodKind.Ingredient,
            name = "To Be Deleted",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val record = createFoodRecord(food, "device-delete")
        // Record should not be deleted yet
        assertNull(record.sync.deletedAt)
    }

    @Test
    fun test_createBarcodeAliasRecord_with_null_ownership() {
        val food = FoodItem(
            id = "food-null-owner",
            kind = FoodKind.Ingredient,
            name = "No Owner",
            barcode = "111222333444",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val alias = createBarcodeAliasRecord(food, "device-1", "")

        assertNotNull(alias)
        assertEquals("", alias.ownerUserId)
        assertEquals(":111222333444", alias.sync.recordId)
    }

    @Test
    fun test_createChangeEnvelope_with_empty_recordId() {
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-empty",
            recordId = ""
        )

        assertEquals("", envelope.recordId)
        assertNotNull(envelope.changeId)
    }

    @Test
    fun test_createSyncMetadata_consistent_timestamps() {
        val metadata1 = createSyncMetadata("food-ts1", "device-ts1")
        val metadata2 = createSyncMetadata("food-ts2", "device-ts2")

        // Both should have timestamps in the current year
        assertTrue(metadata1.updatedAt.startsWith("20"))
        assertTrue(metadata2.updatedAt.startsWith("20"))
    }

    @Test
    fun test_createSyncMetadata_with_empty_recordId() {
        val metadata = createSyncMetadata("", "device-empty-id")

        assertEquals("", metadata.recordId)
        assertEquals("device-empty-id", metadata.originDeviceId)
        assertNotNull(metadata.updatedAt)
    }

    @Test
    fun test_createFoodRecord_preserves_existing_record() {
        val existing = FoodItemRecord(
            food = FoodItem(
                id = "food-existing",
                kind = FoodKind.Ingredient,
                name = "Existing Food",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            sync = SyncMetadata(
                recordId = "food-existing",
                deviceId = "device-existing",
                updatedAt = "2024-01-01T10:00:00Z",
                version = 10,
                syncStatus = SyncStatus.Synced
            )
        )

        val updatedFood = FoodItem(
            id = "food-existing",
            kind = FoodKind.Ingredient,
            name = "Updated Food",
            servingQuantity = 150.0,
            servingUnit = "gram",
            nutrients = Nutrients(150.0)
        )

        val newRecord = createFoodRecord(updatedFood, "device-updating", existing)

        assertEquals("Updated Food", newRecord.food.name)
        assertEquals(2, newRecord.sync.version)
        // Should preserve original deviceId
        assertEquals("device-existing", newRecord.sync.deviceId)
    }

    @Test
    fun test_createFoodRecord_existing_with_deleted() {
        val existing = FoodItemRecord(
            food = FoodItem(
                id = "food-deleted-existing",
                kind = FoodKind.Ingredient,
                name = "Deleted Food",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            sync = SyncMetadata(
                recordId = "food-deleted-existing",
                deviceId = "device-deleted",
                updatedAt = "2024-01-01T10:00:00Z",
                deletedAt = "2024-01-02T00:00:00Z",
                version = 5,
                syncStatus = SyncStatus.LocalOnly
            )
        )

        val updatedFood = FoodItem(
            id = "food-deleted-existing",
            kind = FoodKind.Ingredient,
            name = "Restored Food",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val newRecord = createFoodRecord(updatedFood, "device-restoring", existing)

        assertEquals("Restored Food", newRecord.food.name)
        assertEquals(6, newRecord.sync.version)
        // Deletion should be reset
        assertNull(newRecord.sync.deletedAt)
        assertEquals(SyncStatus.PendingPush, newRecord.sync.syncStatus)
    }
}
