package com.philipcosgrave.calorietracker.model

enum class SyncStatus {
    LocalOnly,
    PendingPush,
    Synced,
    SyncError,
}

enum class SyncEntityType {
    FoodProduct,
    BarcodeAlias,
    DiaryEntry,
}

enum class SyncOperation {
    Upsert,
    Delete,
}

data class SyncMetadata(
    val recordId: String,
    val version: Long,
    val updatedAt: String,
    val originDeviceId: String,
    val deletedAt: String? = null,
    val lastSyncedAt: String? = null,
    val syncStatus: SyncStatus = SyncStatus.LocalOnly,
)

data class FoodItemRecord(
    val food: FoodItem,
    val sync: SyncMetadata,
)

data class BarcodeAliasRecord(
    val barcode: String,
    val productId: String,
    val ownerUserId: String,
    val visibility: String,
    val createdAt: String,
    val sync: SyncMetadata,
)

data class DiaryEntryRecord(
    val entry: DiaryEntry,
    val sync: SyncMetadata,
)

data class SyncCursor(
    val deviceId: String,
    val lastPulledAt: String? = null,
    val lastAcknowledgedChangeId: String? = null,
)

data class SyncSettings(
    val syncEnabled: Boolean,
    val backupMode: BackupMode,
    val apiBaseUrl: String? = null,
    val lastSuccessfulSyncAt: String? = null,
) {
    enum class BackupMode {
        Disabled,
        ManualBackup,
        AutomaticBackup,
    }
}

data class SyncChangeEnvelope<TPayload>(
    val changeId: String,
    val entityType: SyncEntityType,
    val recordId: String,
    val operation: SyncOperation,
    val changedAt: String,
    val deviceId: String,
    val payload: TPayload? = null,
    val baseVersion: Long? = null,
)

data class SyncChangeRejection(
    val changeId: String,
    val code: Code,
    val message: String,
) {
    enum class Code {
        Conflict,
        ValidationError,
        NotFound,
        Unknown,
    }
}

data class SyncPushResponse(
    val cursor: SyncCursor,
    val acceptedChangeIds: List<String>,
    val rejectedChanges: List<SyncChangeRejection>,
)

data class SyncPullResponse(
    val cursor: SyncCursor,
    val changes: List<SyncChangeEnvelope<*>>,
)
