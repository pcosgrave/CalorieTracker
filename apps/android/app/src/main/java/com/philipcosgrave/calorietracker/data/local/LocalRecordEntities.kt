package com.philipcosgrave.calorietracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_records",
    indices = [Index(value = ["barcode"])],
)
data class FoodRecordEntity(
    @PrimaryKey val recordId: String,
    val productId: String,
    val barcode: String?,
    val payloadJson: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String?,
    val originDeviceId: String,
    val lastSyncedAt: String?,
    val syncStatus: String,
)

@Entity(
    tableName = "barcode_alias_records",
    indices = [Index(value = ["barcode"], unique = true)],
)
data class BarcodeAliasEntity(
    @PrimaryKey val recordId: String,
    val barcode: String,
    val productId: String,
    val payloadJson: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String?,
    val originDeviceId: String,
    val lastSyncedAt: String?,
    val syncStatus: String,
)

@Entity(tableName = "diary_records")
data class DiaryRecordEntity(
    @PrimaryKey val recordId: String,
    val entryId: String,
    val loggedAt: String,
    val payloadJson: String,
    val version: Long,
    val updatedAt: String,
    val deletedAt: String?,
    val originDeviceId: String,
    val lastSyncedAt: String?,
    val syncStatus: String,
)

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val changeId: String,
    val entityType: String,
    val recordId: String,
    val operation: String,
    val changedAt: String,
    val deviceId: String,
    val payloadJson: String?,
    val baseVersion: Long?,
)

@Entity(
    tableName = "weight_records",
    indices = [Index(value = ["ownerUserId", "loggedOn"])],
)
data class WeightRecordEntity(
    @PrimaryKey val recordId: String,
    val ownerUserId: String,
    val loggedOn: String,
    val weightKg: Double,
    val updatedAt: String,
)
