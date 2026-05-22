package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.BarcodeAliasRecord
import com.philipcosgrave.calorietracker.model.DiaryEntryRecord
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodItemRecord
import com.philipcosgrave.calorietracker.model.SyncChangeEnvelope
import com.philipcosgrave.calorietracker.model.SyncEntityType
import com.philipcosgrave.calorietracker.model.SyncMetadata
import com.philipcosgrave.calorietracker.model.SyncOperation
import com.philipcosgrave.calorietracker.model.SyncStatus
import java.time.Instant

fun nowIsoString(): String = Instant.now().toString()

fun createSyncMetadata(
    recordId: String,
    deviceId: String,
    updatedAt: String = nowIsoString(),
    version: Long = 1,
    deletedAt: String? = null,
    lastSyncedAt: String? = null,
    syncStatus: SyncStatus = SyncStatus.LocalOnly,
): SyncMetadata =
    SyncMetadata(
        recordId = recordId,
        version = version,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        originDeviceId = deviceId,
        lastSyncedAt = lastSyncedAt,
        syncStatus = syncStatus,
    )

fun FoodItemRecord.markPendingSync(updatedAt: String, deletedAt: String? = null): FoodItemRecord =
    copy(
        sync = sync.copy(
            version = sync.version + 1,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            syncStatus = SyncStatus.PendingPush,
        ),
    )

fun DiaryEntryRecord.markPendingSync(updatedAt: String, deletedAt: String? = null): DiaryEntryRecord =
    copy(
        sync = sync.copy(
            version = sync.version + 1,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            syncStatus = SyncStatus.PendingPush,
        ),
    )

fun BarcodeAliasRecord.markPendingSync(updatedAt: String, deletedAt: String? = null): BarcodeAliasRecord =
    copy(
        sync = sync.copy(
            version = sync.version + 1,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            syncStatus = SyncStatus.PendingPush,
        ),
    )

fun createFoodRecord(food: FoodItem, deviceId: String, existing: FoodItemRecord? = null): FoodItemRecord {
    val updatedAt = nowIsoString()
    return existing?.copy(food = food)?.markPendingSync(updatedAt) ?: FoodItemRecord(
        food = food,
        sync = createSyncMetadata(
            recordId = food.id,
            deviceId = deviceId,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.PendingPush,
        ),
    )
}

fun createBarcodeAliasRecord(food: FoodItem, deviceId: String, ownerUserId: String): BarcodeAliasRecord? {
    if (food.barcode.isBlank()) return null
    val updatedAt = nowIsoString()
    return BarcodeAliasRecord(
        barcode = food.barcode,
        productId = food.id,
        ownerUserId = ownerUserId,
        visibility = "private",
        createdAt = updatedAt,
        sync = createSyncMetadata(
            recordId = "${ownerUserId}:${food.barcode}",
            deviceId = deviceId,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.PendingPush,
        ),
    )
}

fun createChangeEnvelope(
    entityType: SyncEntityType,
    operation: SyncOperation,
    deviceId: String,
    recordId: String,
    payload: Any?,
    baseVersion: Long? = null,
): SyncChangeEnvelope<Any> =
    SyncChangeEnvelope(
        changeId = createId("change"),
        entityType = entityType,
        recordId = recordId,
        operation = operation,
        changedAt = nowIsoString(),
        deviceId = deviceId,
        payload = payload,
        baseVersion = baseVersion,
    )
