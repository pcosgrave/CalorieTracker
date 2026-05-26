package com.philipcosgrave.calorietracker.model

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SyncModelsTest {

    @Test
    fun test_SyncStatus_enum_values() {
        val statuses = listOf(SyncStatus.LocalOnly, SyncStatus.PendingPush, SyncStatus.Synced, SyncStatus.SyncError)
        assertEquals(4, statuses.size)

        val strings = SyncStatus.entries.map { it.name }
        assertTrue(strings.contains("LOCAL_ONLY"))
        assertTrue(strings.contains("PENDING_PUSH"))
        assertTrue(strings.contains("SYNCED"))
        assertTrue(strings.contains("SYNC_ERROR"))
    }

    @Test
    fun test_SyncEntityType_enum_values() {
        val types = SyncEntityType.entries
        assertEquals(4, types.size)

        val strings = SyncEntityType.entries.map { it.name }
        assertTrue(strings.contains("FOOD_PRODUCT"))
        assertTrue(strings.contains("BARCODE_ALIAS"))
        assertTrue(strings.contains("DIARY_ENTRY"))
    }

    @Test
    fun test_SyncOperation_enum_values() {
        val operations = SyncOperation.entries
        assertEquals(2, operations.size)

        val strings = SyncOperation.entries.map { it.name }
        assertTrue(strings.contains("UPSERT"))
        assertTrue(strings.contains("DELETE"))
    }

    @Test
    fun test_SyncMetadata_creation() {
        val metadata = SyncMetadata(
            recordId = "test-123",
            version = 5,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = "2024-01-02T00:00:00Z",
            lastSyncedAt = "2024-01-01T23:59:59Z",
            syncStatus = SyncStatus.Synced,
            originDeviceId = "device-a"
        )

        assertEquals("test-123", metadata.recordId)
        assertEquals(5, metadata.version)
        assertEquals("2024-01-01T12:00:00Z", metadata.updatedAt)
        assertEquals("2024-01-02T00:00:00Z", metadata.deletedAt)
        assertNotNull(metadata.lastSyncedAt)
        assertEquals(SyncStatus.Synced, metadata.syncStatus)
        assertEquals("device-a", metadata.originDeviceId)
    }

    @Test
    fun test_SyncMetadata_defaults() {
        val metadata = SyncMetadata(recordId = "test", deviceId = "device-1")

        assertEquals("test", metadata.recordId)
        assertEquals(1, metadata.version)
        assertNotNull(metadata.updatedAt)
        assertTrue(metadata.updatedAt.startsWith("20")) // Year
        assertNull(metadata.deletedAt)
        assertNull(metadata.lastSyncedAt)
        assertEquals(SyncStatus.LocalOnly, metadata.syncStatus)
        assertEquals("device-1", metadata.originDeviceId)
    }

    @Test
    fun test_FoodItemRecord_creation() {
        val food = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0, 10.0, 20.0, 5.0)
        )

        val sync = SyncMetadata(
            recordId = "food-1",
            deviceId = "device-1",
            updatedAt = "2024-01-01T12:00:00Z"
        )

        val record = FoodItemRecord(food = food, sync = sync)

        assertEquals("food-1", record.food.id)
        assertEquals("food-1", record.sync.recordId)
        assertEquals("device-1", record.sync.deviceId)
    }

    @Test
    fun test_BarcodeAliasRecord_creation() {
        val food = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val record = BarcodeAliasRecord(
            barcode = "123456789012",
            productId = "food-1",
            ownerUserId = "user-1",
            visibility = "private",
            createdAt = "2024-01-01T12:00:00Z",
            sync = SyncMetadata(
                recordId = "user-1:123456789012",
                deviceId = "device-1"
            )
        )

        assertEquals("123456789012", record.barcode)
        assertEquals("food-1", record.productId)
        assertEquals("user-1", record.ownerUserId)
        assertEquals("private", record.visibility)
        assertEquals("user-1:123456789012", record.sync.recordId)
    }

    @Test
    fun test_DiaryEntryRecord_creation() {
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
            date = LocalDate.now(),
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
        assertEquals("entry-1", record.sync.recordId)
    }

    @Test
    fun test_SyncCursor_creation() {
        val cursor = SyncCursor(
            deviceId = "device-1",
            lastPulledAt = "2024-01-01T12:00:00Z",
            lastAcknowledgedChangeId = "change-abc123"
        )

        assertEquals("device-1", cursor.deviceId)
        assertNotNull(cursor.lastPulledAt)
        assertNotNull(cursor.lastAcknowledgedChangeId)
    }

    @Test
    fun test_SyncCursor_defaults() {
        val cursor = SyncCursor(deviceId = "device-1")

        assertEquals("device-1", cursor.deviceId)
        assertNull(cursor.lastPulledAt)
        assertNull(cursor.lastAcknowledgedChangeId)
    }

    @Test
    fun test_SyncSettings_defaults() {
        val settings = SyncSettings()

        assertEquals(true, settings.syncEnabled)
        assertEquals(SyncSettings.BackupMode.Disabled, settings.backupMode)
        assertEquals(null, settings.apiBaseUrl)
        assertEquals(1800, settings.calorieTargetMin)
        assertEquals(2200, settings.calorieTargetMax)
        assertEquals(SyncSettings.WeightUnit.Kilograms, settings.weightUnit)
        assertNull(settings.goalWeightKg)
    }

    @Test
    fun test_SyncSettings_custom_values() {
        val settings = SyncSettings(
            syncEnabled = false,
            backupMode = SyncSettings.BackupMode.AutomaticBackup,
            apiBaseUrl = "https://api.example.com",
            calorieTargetMin = 1500,
            calorieTargetMax = 2500,
            weightUnit = SyncSettings.WeightUnit.Pounds,
            goalWeightKg = 70.5
        )

        assertEquals(false, settings.syncEnabled)
        assertEquals(SyncSettings.BackupMode.AutomaticBackup, settings.backupMode)
        assertEquals("https://api.example.com", settings.apiBaseUrl)
        assertEquals(1500, settings.calorieTargetMin)
        assertEquals(2500, settings.calorieTargetMax)
        assertEquals(SyncSettings.WeightUnit.Pounds, settings.weightUnit)
        assertEquals(70.5, settings.goalWeightKg)
    }

    @Test
    fun test_SyncChangeEnvelope_creation() {
        val envelope = SyncChangeEnvelope(
            changeId = "change-123",
            entityType = SyncEntityType.FoodProduct,
            recordId = "food-1",
            operation = SyncOperation.Upsert,
            changedAt = "2024-01-01T12:00:00Z",
            deviceId = "device-1",
            payload = mapOf("name" to "Test"),
            baseVersion = 4L
        )

        assertEquals("change-123", envelope.changeId)
        assertEquals(SyncEntityType.FoodProduct, envelope.entityType)
        assertEquals("food-1", envelope.recordId)
        assertEquals(SyncOperation.Upsert, envelope.operation)
        assertEquals("2024-01-01T12:00:00Z", envelope.changedAt)
        assertEquals("device-1", envelope.deviceId)
        assertNotNull(envelope.payload)
        assertEquals(4L, envelope.baseVersion)
    }

    @Test
    fun test_SyncChangeEnvelope_null_payload() {
        val envelope = SyncChangeEnvelope(
            entityType = SyncEntityType.DiaryEntry,
            operation = SyncOperation.Delete,
            deviceId = "device-1",
            recordId = "entry-1"
        )

        assertNull(envelope.payload)
        assertNull(envelope.baseVersion)
    }

    @Test
    fun test_SyncChangeRejection_creation() {
        val rejection = SyncChangeRejection(
            changeId = "change-123",
            code = SyncChangeRejection.Code.Conflict,
            message = "Conflict: existing record has different version"
        )

        assertEquals("change-123", rejection.changeId)
        assertEquals(SyncChangeRejection.Code.Conflict, rejection.code)
        assertEquals("Conflict: existing record has different version", rejection.message)
    }

    @Test
    fun test_SyncChangeRejection_all_codes() {
        val codes = SyncChangeRejection.Code.entries
        assertEquals(4, codes.size)

        val strings = SyncChangeRejection.Code.entries.map { it.name }
        assertTrue(strings.contains("CONFLICT"))
        assertTrue(strings.contains("VALIDATION_ERROR"))
        assertTrue(strings.contains("NOT_FOUND"))
        assertTrue(strings.contains("UNKNOWN"))
    }

    @Test
    fun test_SyncPushResponse_creation() {
        val response = SyncPushResponse(
            cursor = SyncCursor(deviceId = "device-1"),
            acceptedChangeIds = listOf("change-1", "change-2"),
            rejectedChanges = listOf(
                SyncChangeRejection(
                    changeId = "change-3",
                    code = SyncChangeRejection.Code.NotFound,
                    message = "Food not found"
                )
            )
        )

        assertEquals(1, response.acceptedChangeIds.size)
        assertTrue(response.acceptedChangeIds.contains("change-1"))
        assertEquals(1, response.rejectedChanges.size)
        assertEquals("NOT_FOUND", response.rejectedChanges[0].code.name)
    }

    @Test
    fun test_SyncPushResponse_empty_lists() {
        val response = SyncPushResponse(cursor = SyncCursor(deviceId = "device-1"))

        assertTrue(response.acceptedChangeIds.isEmpty())
        assertTrue(response.rejectedChanges.isEmpty())
    }

    @Test
    fun test_SyncPullResponse_creation() {
        val changes = listOf(
            createChangeEnvelope(
                entityType = SyncEntityType.FoodProduct,
                operation = SyncOperation.Upsert,
                deviceId = "device-2",
                recordId = "food-new"
            ),
            createChangeEnvelope(
                entityType = SyncEntityType.DiaryEntry,
                operation = SyncOperation.Upsert,
                deviceId = "device-2",
                recordId = "entry-new"
            )
        )

        val response = SyncPullResponse(
            cursor = SyncCursor(deviceId = "device-1"),
            changes = changes
        )

        assertEquals(2, response.changes.size)
        assertEquals(SyncEntityType.FoodProduct, response.changes[0].entityType)
        assertEquals(SyncEntityType.DiaryEntry, response.changes[1].entityType)
    }

    @Test
    fun test_BackupMode_enum_values() {
        val modes = SyncSettings.BackupMode.entries
        assertEquals(3, modes.size)

        val strings = SyncSettings.BackupMode.entries.map { it.name }
        assertTrue(strings.contains("DISABLED"))
        assertTrue(strings.contains("MANUAL_BACKUP"))
        assertTrue(strings.contains("AUTOMATIC_BACKUP"))
    }

    @Test
    fun test_WeightUnit_enum_values() {
        val units = SyncSettings.WeightUnit.entries
        assertEquals(2, units.size)

        val strings = SyncSettings.WeightUnit.entries.map { it.name }
        assertTrue(strings.contains("KILOGRAMS"))
        assertTrue(strings.contains("POUNDS"))
    }

    private fun createChangeEnvelope(
        entityType: SyncEntityType,
        operation: SyncOperation,
        deviceId: String,
        recordId: String,
        payload: Any? = null,
        baseVersion: Long? = null
    ): SyncChangeEnvelope<Any> {
        return SyncChangeEnvelope(
            changeId = "change-${recordId}",
            entityType = entityType,
            recordId = recordId,
            operation = operation,
            changedAt = "2024-01-01T12:00:00Z",
            deviceId = deviceId,
            payload = payload,
            baseVersion = baseVersion
        )
    }
}
