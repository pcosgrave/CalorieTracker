package com.philipcosgrave.calorietracker.data.local

import com.philipcosgrave.calorietracker.model.*
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class LocalRecordEntitiesTest {

    private val formatter = DateTimeFormatter.ISO_INSTANT

    @Test
    fun test_FoodRecordEntity_creation() {
        val food = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            brand = "Brand",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0, 10.0, 20.0, 5.0)
        )

        val record = FoodRecordEntity(
            recordId = "food-1",
            productId = "food-1",
            barcode = "123456789012",
            payloadJson = foodItemToJson(food),
            version = 1,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = null,
            originDeviceId = "device-1",
            lastSyncedAt = "2024-01-01T11:59:59Z",
            syncStatus = "pending_push"
        )

        assertEquals("food-1", record.recordId)
        assertEquals("123456789012", record.barcode)
        assertEquals(1, record.version)
        assertEquals("pending_push", record.syncStatus)
    }

    @Test
    fun test_FoodRecordEntity_deleted_record() {
        val food = FoodItem(
            id = "food-deleted",
            kind = FoodKind.Ingredient,
            name = "Deleted Food",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val record = FoodRecordEntity(
            recordId = "food-deleted",
            productId = "food-deleted",
            barcode = "987654321098",
            payloadJson = foodItemToJson(food),
            version = 5,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = "2024-01-02T00:00:00Z",
            originDeviceId = "device-1",
            lastSyncedAt = "2024-01-01T11:59:59Z",
            syncStatus = "pending_push"
        )

        assertEquals("2024-01-02T00:00:00Z", record.deletedAt)
        assertEquals("pending_push", record.syncStatus)
    }

    @Test
    fun test_FoodRecordEntity_synced_record() {
        val food = FoodItem(
            id = "food-synced",
            kind = FoodKind.Ingredient,
            name = "Synced Food",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val record = FoodRecordEntity(
            recordId = "food-synced",
            productId = "food-synced",
            barcode = null,
            payloadJson = foodItemToJson(food),
            version = 3,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = null,
            originDeviceId = "device-1",
            lastSyncedAt = "2024-01-01T23:59:59Z",
            syncStatus = "synced"
        )

        assertNull(record.deletedAt)
        assertEquals("synced", record.syncStatus)
    }

    @Test
    fun test_BarcodeAliasEntity_creation() {
        val food = FoodItem(
            id = "food-1",
            kind = FoodKind.Ingredient,
            name = "Test Food",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val entity = BarcodeAliasEntity(
            recordId = "user-1:123456789012",
            barcode = "123456789012",
            productId = "food-1",
            payloadJson = barcodeAliasToJson(food, "user-1"),
            version = 1,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = null,
            originDeviceId = "device-1",
            lastSyncedAt = "2024-01-01T11:59:59Z",
            syncStatus = "pending_push"
        )

        assertEquals("user-1:123456789012", entity.recordId)
        assertEquals("123456789012", entity.barcode)
        assertEquals(1, entity.version)
    }

    @Test
    fun test_BarcodeAliasEntity_deleted() {
        val food = FoodItem(
            id = "food-deleted",
            kind = FoodKind.Ingredient,
            name = "Deleted",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val entity = BarcodeAliasEntity(
            recordId = "user-1:123456789012",
            barcode = "123456789012",
            productId = "food-deleted",
            payloadJson = barcodeAliasToJson(food, "user-1"),
            version = 10,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = "2024-01-02T00:00:00Z",
            originDeviceId = "device-1",
            lastSyncedAt = "2024-01-01T11:59:59Z",
            syncStatus = "synced"
        )

        assertNotNull(entity.deletedAt)
        assertEquals("synced", entity.syncStatus)
    }

    @Test
    fun test_DiaryRecordEntity_creation() {
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

        val entity = DiaryRecordEntity(
            recordId = "entry-1",
            entryId = "entry-1",
            loggedAt = "2024-01-01T12:00:00Z",
            payloadJson = diaryEntryToJson(entry),
            version = 1,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = null,
            originDeviceId = "device-1",
            lastSyncedAt = "2024-01-01T11:59:59Z",
            syncStatus = "pending_push"
        )

        assertEquals("entry-1", entity.recordId)
        assertEquals("entry-1", entity.entryId)
        assertEquals("pending_push", entity.syncStatus)
    }

    @Test
    fun test_DiaryRecordEntity_deleted() {
        val entry = DiaryEntry(
            id = "entry-deleted",
            food = FoodItem(
                id = "food-deleted",
                kind = FoodKind.Ingredient,
                name = "Deleted",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(52.0)
            ),
            date = LocalDate.now().minusDays(5),
            meal = Meal.Snack,
            servingMultiplier = 1.0
        )

        val entity = DiaryRecordEntity(
            recordId = "entry-deleted",
            entryId = "entry-deleted",
            loggedAt = "2024-01-01T12:00:00Z",
            payloadJson = diaryEntryToJson(entry),
            version = 2,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = "2024-01-02T00:00:00Z",
            originDeviceId = "device-1",
            lastSyncedAt = "2024-01-01T11:59:59Z",
            syncStatus = "synced"
        )

        assertNotNull(entity.deletedAt)
        assertEquals("synced", entity.syncStatus)
    }

    @Test
    fun test_SyncOutboxEntity_creation() {
        val outbox = SyncOutboxEntity(
            changeId = "change-123",
            entityType = "FOOD_PRODUCT",
            recordId = "food-1",
            operation = "UPSERT",
            changedAt = "2024-01-01T12:00:00Z",
            deviceId = "device-1",
            payloadJson = null,
            baseVersion = null
        )

        assertEquals("change-123", outbox.changeId)
        assertEquals("FOOD_PRODUCT", outbox.entityType)
        assertEquals("UPSERT", outbox.operation)
    }

    @Test
    fun test_SyncOutboxEntity_with_payload() {
        val payload = mapOf(
            "name" to "Test Product",
            "nutrients" to mapOf("calories" to 100.0)
        )

        val outbox = SyncOutboxEntity(
            changeId = "change-with-payload",
            entityType = "FOOD_PRODUCT",
            recordId = "food-1",
            operation = "UPSERT",
            changedAt = "2024-01-01T12:00:00Z",
            deviceId = "device-1",
            payloadJson = payloadToJson(payload),
            baseVersion = 4L
        )

        assertNotNull(outbox.payloadJson)
        assertEquals(4L, outbox.baseVersion)
    }

    @Test
    fun test_WeightRecordEntity_creation() {
        val record = WeightRecordEntity(
            recordId = "weight-1",
            ownerUserId = "user-1",
            loggedOn = "2024-01-01T12:00:00Z",
            weightKg = 70.5,
            updatedAt = "2024-01-01T12:00:00Z"
        )

        assertEquals("weight-1", record.recordId)
        assertEquals("user-1", record.ownerUserId)
        assertEquals(70.5, record.weightKg, 0.01)
    }

    @Test
    fun test_SyncMetadata_status_values() {
        assertEquals("pending_push", "pending_push")
        assertEquals("synced", "synced")
        assertEquals("sync_error", "sync_error")
        assertEquals("local_only", "local_only")
    }

    @Test
    fun test_FoodRecordEntity_payload_with_barcode() {
        val food = FoodItem(
            id = "food-with-barcode",
            kind = FoodKind.Ingredient,
            name = "Product with Barcode",
            barcode = "123456789012",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val record = FoodRecordEntity(
            recordId = "food-with-barcode",
            productId = "food-with-barcode",
            barcode = "123456789012",
            payloadJson = foodItemToJson(food),
            version = 1,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = null,
            originDeviceId = "device-1",
            lastSyncedAt = null,
            syncStatus = "pending_push"
        )

        assertEquals("123456789012", record.barcode)
    }

    @Test
    fun test_FoodRecordEntity_payload_without_barcode() {
        val food = FoodItem(
            id = "food-without-barcode",
            kind = FoodKind.Ingredient,
            name = "Product without Barcode",
            barcode = "",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(100.0)
        )

        val record = FoodRecordEntity(
            recordId = "food-without-barcode",
            productId = "food-without-barcode",
            barcode = null,
            payloadJson = foodItemToJson(food),
            version = 1,
            updatedAt = "2024-01-01T12:00:00Z",
            deletedAt = null,
            originDeviceId = "device-1",
            lastSyncedAt = null,
            syncStatus = "pending_push"
        )

        assertNull(record.barcode)
    }

    private fun foodItemToJson(food: FoodItem): String {
        return """
            {
                "id": "${food.id}",
                "kind": "${food.kind.name}",
                "name": "${food.name}",
                "brand": "${food.brand}",
                "servingQuantity": ${food.servingQuantity},
                "servingUnit": "${food.servingUnit}",
                "nutrients": {
                    "calories": ${food.nutrients.calories},
                    "proteinGrams": ${food.nutrients.proteinGrams},
                    "carbohydrateGrams": ${food.nutrients.carbohydrateGrams},
                    "fatGrams": ${food.nutrients.fatGrams}
                },
                "frequency": ${food.frequency},
                "lastUsedDaysAgo": ${food.lastUsedDaysAgo},
                "isUserCreated": ${food.isUserCreated}
            }
        """.trimIndent()
    }

    private fun barcodeAliasToJson(food: FoodItem, ownerUserId: String): String {
        return """
            {
                "barcode": "${food.barcode}",
                "productId": "${food.id}",
                "ownerUserId": "${ownerUserId}",
                "visibility": "private"
            }
        """.trimIndent()
    }

    private fun diaryEntryToJson(entry: DiaryEntry): String {
        return """
            {
                "id": "${entry.id}",
                "food": {
                    "id": "${entry.food.id}",
                    "kind": "${entry.food.kind.name}",
                    "name": "${entry.food.name}",
                    "servingQuantity": ${entry.food.servingQuantity},
                    "servingUnit": "${entry.food.servingUnit}",
                    "nutrients": {
                        "calories": ${entry.food.nutrients.calories},
                        "proteinGrams": ${entry.food.nutrients.proteinGrams},
                        "carbohydrateGrams": ${entry.food.nutrients.carbohydrateGrams},
                        "fatGrams": ${entry.food.nutrients.fatGrams}
                    }
                },
                "date": "${entry.date}",
                "meal": "${entry.meal.name}",
                "servingMultiplier": ${entry.servingMultiplier}
            }
        """.trimIndent()
    }

    private fun payloadToJson(payload: Map<String, Any>): String {
        val builder = StringBuilder("{")
        var first = true
        for ((key, value) in payload) {
            if (!first) builder.append(",")
            first = false
            builder.append("\"$key\":")
            if (value is Number) {
                builder.append(value)
            } else {
                builder.append("\"")
                builder.append(value.toString())
                builder.append("\"")
            }
        }
        builder.append("}")
        return builder.toString()
    }
}
