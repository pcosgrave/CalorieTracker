package com.philipcosgrave.calorietracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import com.philipcosgrave.calorietracker.data.readDiaryEntries
import com.philipcosgrave.calorietracker.data.readFoodItems
import com.philipcosgrave.calorietracker.data.readStringList
import com.philipcosgrave.calorietracker.data.local.BarcodeAliasDao
import com.philipcosgrave.calorietracker.data.local.BarcodeAliasEntity
import com.philipcosgrave.calorietracker.data.local.CalorieTrackerDatabase
import com.philipcosgrave.calorietracker.data.local.DiaryRecordDao
import com.philipcosgrave.calorietracker.data.local.DiaryRecordEntity
import com.philipcosgrave.calorietracker.data.local.FoodRecordDao
import com.philipcosgrave.calorietracker.data.local.FoodRecordEntity
import com.philipcosgrave.calorietracker.data.local.SyncOutboxDao
import com.philipcosgrave.calorietracker.data.local.SyncOutboxEntity
import com.philipcosgrave.calorietracker.data.local.SyncPreferencesKeys
import com.philipcosgrave.calorietracker.data.local.barcodeAliasRecordPayloadFromJson
import com.philipcosgrave.calorietracker.data.local.barcodeAliasRecordPayloadToJson
import com.philipcosgrave.calorietracker.data.local.diaryEntryFromJsonString as parseDiaryEntry
import com.philipcosgrave.calorietracker.data.local.foodItemFromJsonString
import com.philipcosgrave.calorietracker.data.local.payloadToJsonString
import com.philipcosgrave.calorietracker.data.local.syncChangePayloadFromJson
import com.philipcosgrave.calorietracker.data.local.syncMetadataFromEntity
import com.philipcosgrave.calorietracker.data.local.syncPreferencesDataStore
import com.philipcosgrave.calorietracker.data.local.toJsonString
import com.philipcosgrave.calorietracker.domain.createId
import com.philipcosgrave.calorietracker.model.AuthSession
import com.philipcosgrave.calorietracker.model.BarcodeAliasRecord
import com.philipcosgrave.calorietracker.model.DiaryEntryRecord
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodItemRecord
import com.philipcosgrave.calorietracker.model.SyncChangeEnvelope
import com.philipcosgrave.calorietracker.model.SyncCursor
import com.philipcosgrave.calorietracker.model.SyncEntityType
import com.philipcosgrave.calorietracker.model.SyncMetadata
import com.philipcosgrave.calorietracker.model.SyncOperation
import com.philipcosgrave.calorietracker.model.SyncSettings
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private const val DATABASE_NAME = "calorie-tracker.db"

object LocalRepositoryFactory {
    fun database(context: Context): CalorieTrackerDatabase =
        Room.databaseBuilder(context, CalorieTrackerDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
}

class RoomFoodRepository(private val dao: FoodRecordDao) : FoodRepository {
    override suspend fun list(): List<FoodItemRecord> =
        dao.listAll().map { entity ->
            FoodItemRecord(
                food = foodItemFromJsonString(entity.payloadJson),
                sync = entity.toSyncMetadata(),
            )
        }

    override suspend fun getById(recordId: String): FoodItemRecord? =
        dao.getById(recordId)?.let { entity ->
            FoodItemRecord(food = foodItemFromJsonString(entity.payloadJson), sync = entity.toSyncMetadata())
        }

    override suspend fun getByBarcode(barcode: String): FoodItemRecord? =
        dao.getByBarcode(barcode)?.let { entity ->
            FoodItemRecord(food = foodItemFromJsonString(entity.payloadJson), sync = entity.toSyncMetadata())
        }

    override suspend fun save(record: FoodItemRecord) {
        dao.upsert(
            FoodRecordEntity(
                recordId = record.sync.recordId,
                productId = record.food.id,
                barcode = record.food.barcode.ifBlank { null },
                payloadJson = record.food.toJsonString(),
                version = record.sync.version,
                updatedAt = record.sync.updatedAt,
                deletedAt = record.sync.deletedAt,
                originDeviceId = record.sync.originDeviceId,
                lastSyncedAt = record.sync.lastSyncedAt,
                syncStatus = record.sync.syncStatus.name,
            ),
        )
    }

    override suspend fun softDelete(recordId: String, deletedAt: String) {
        val existing = getById(recordId) ?: return
        save(existing.copy(sync = existing.sync.copy(updatedAt = deletedAt, deletedAt = deletedAt, version = existing.sync.version + 1)))
    }
}

class RoomBarcodeAliasRepository(private val dao: BarcodeAliasDao) : BarcodeAliasRepository {
    override suspend fun list(): List<BarcodeAliasRecord> =
        dao.listAll().map { entity ->
            barcodeAliasRecordPayloadFromJson(entity.payloadJson, entity.toSyncMetadata())
        }

    override suspend fun getByBarcode(barcode: String): BarcodeAliasRecord? =
        dao.getByBarcode(barcode)?.let { barcodeAliasRecordPayloadFromJson(it.payloadJson, it.toSyncMetadata()) }

    override suspend fun save(record: BarcodeAliasRecord) {
        dao.upsert(
            BarcodeAliasEntity(
                recordId = record.sync.recordId,
                barcode = record.barcode,
                productId = record.productId,
                payloadJson = barcodeAliasRecordPayloadToJson(record),
                version = record.sync.version,
                updatedAt = record.sync.updatedAt,
                deletedAt = record.sync.deletedAt,
                originDeviceId = record.sync.originDeviceId,
                lastSyncedAt = record.sync.lastSyncedAt,
                syncStatus = record.sync.syncStatus.name,
            ),
        )
    }

    override suspend fun softDelete(recordId: String, deletedAt: String) {
        val existing = list().firstOrNull { it.sync.recordId == recordId } ?: return
        save(existing.copy(sync = existing.sync.copy(updatedAt = deletedAt, deletedAt = deletedAt, version = existing.sync.version + 1)))
    }
}

class RoomDiaryRepository(private val dao: DiaryRecordDao) : DiaryRepository {
    override suspend fun list(): List<DiaryEntryRecord> =
        dao.listAll().map { entity ->
            DiaryEntryRecord(
                entry = parseDiaryEntry(entity.payloadJson),
                sync = entity.toSyncMetadata(),
            )
        }

    override suspend fun listByDateRange(startDate: LocalDate, endDate: LocalDate): List<DiaryEntryRecord> =
        dao.listByDateRange("${startDate}T00:00:00", "${endDate}T23:59:59").map { entity ->
            DiaryEntryRecord(entry = parseDiaryEntry(entity.payloadJson), sync = entity.toSyncMetadata())
        }

    override suspend fun getById(recordId: String): DiaryEntryRecord? =
        dao.getById(recordId)?.let { entity ->
            DiaryEntryRecord(entry = parseDiaryEntry(entity.payloadJson), sync = entity.toSyncMetadata())
        }

    override suspend fun save(record: DiaryEntryRecord) {
        dao.upsert(
            DiaryRecordEntity(
                recordId = record.sync.recordId,
                entryId = record.entry.id,
                loggedAt = record.entry.date.toString(),
                payloadJson = record.entry.toJsonString(),
                version = record.sync.version,
                updatedAt = record.sync.updatedAt,
                deletedAt = record.sync.deletedAt,
                originDeviceId = record.sync.originDeviceId,
                lastSyncedAt = record.sync.lastSyncedAt,
                syncStatus = record.sync.syncStatus.name,
            ),
        )
    }

    override suspend fun softDelete(recordId: String, deletedAt: String) {
        val existing = getById(recordId) ?: return
        save(existing.copy(sync = existing.sync.copy(updatedAt = deletedAt, deletedAt = deletedAt, version = existing.sync.version + 1)))
    }
}

class RoomSyncOutboxRepository(private val dao: SyncOutboxDao) : SyncOutboxRepository {
    override suspend fun listPendingChanges(): List<SyncChangeEnvelope<*>> =
        dao.listAll().map { entity ->
            SyncChangeEnvelope(
                changeId = entity.changeId,
                entityType = SyncEntityType.valueOf(entity.entityType),
                recordId = entity.recordId,
                operation = SyncOperation.valueOf(entity.operation),
                changedAt = entity.changedAt,
                deviceId = entity.deviceId,
                payload = syncChangePayloadFromJson(SyncEntityType.valueOf(entity.entityType), entity.payloadJson),
                baseVersion = entity.baseVersion,
            )
        }

    override suspend fun enqueue(change: SyncChangeEnvelope<*>) {
        dao.upsert(
            SyncOutboxEntity(
                changeId = change.changeId,
                entityType = change.entityType.name,
                recordId = change.recordId,
                operation = change.operation.name,
                changedAt = change.changedAt,
                deviceId = change.deviceId,
                payloadJson = change.payloadToJsonString(),
                baseVersion = change.baseVersion,
            ),
        )
    }

    override suspend fun acknowledge(changeIds: List<String>, syncedAt: String) {
        dao.deleteByIds(changeIds)
    }

    override suspend fun markRejected(changeId: String) {
        dao.deleteByIds(listOf(changeId))
    }

    override suspend fun clear() {
        dao.clear()
    }
}

class DataStoreSyncStateRepository(private val context: Context) : SyncStateRepository {
    private suspend fun currentUserScope(): String =
        context.syncPreferencesDataStore.data.first()[SyncPreferencesKeys.CurrentUserId] ?: "guest"

    private fun syncEnabledKey(userId: String) = androidx.datastore.preferences.core.booleanPreferencesKey("sync_enabled.$userId")
    private fun backupModeKey(userId: String) = stringPreferencesKey("backup_mode.$userId")
    private fun apiBaseUrlKey(userId: String) = stringPreferencesKey("api_base_url.$userId")
    private fun lastSuccessfulSyncAtKey(userId: String) = stringPreferencesKey("last_successful_sync_at.$userId")
    private fun lastPulledAtKey(userId: String) = stringPreferencesKey("last_pulled_at.$userId")
    private fun lastAcknowledgedChangeIdKey(userId: String) = stringPreferencesKey("last_acknowledged_change_id.$userId")

    override suspend fun getCursor(): SyncCursor? {
        val prefs = context.syncPreferencesDataStore.data.first()
        val userId = currentUserScope()
        val deviceId = prefs[SyncPreferencesKeys.DeviceId] ?: return null
        return SyncCursor(
            deviceId = deviceId,
            lastPulledAt = prefs[lastPulledAtKey(userId)],
            lastAcknowledgedChangeId = prefs[lastAcknowledgedChangeIdKey(userId)],
        )
    }

    override suspend fun saveCursor(cursor: SyncCursor) {
        val userId = currentUserScope()
        context.syncPreferencesDataStore.edit { prefs ->
            prefs[SyncPreferencesKeys.DeviceId] = cursor.deviceId
            cursor.lastPulledAt?.let { prefs[lastPulledAtKey(userId)] = it }
            cursor.lastAcknowledgedChangeId?.let { prefs[lastAcknowledgedChangeIdKey(userId)] = it }
        }
    }

    override suspend fun getSettings(): SyncSettings {
        val prefs = context.syncPreferencesDataStore.data.first()
        val userId = currentUserScope()
        return SyncSettings(
            syncEnabled = prefs[syncEnabledKey(userId)] ?: false,
            backupMode = prefs[backupModeKey(userId)]?.let { SyncSettings.BackupMode.valueOf(it) }
                ?: SyncSettings.BackupMode.Disabled,
            apiBaseUrl = prefs[apiBaseUrlKey(userId)] ?: com.philipcosgrave.calorietracker.BuildConfig.SYNC_API_BASE_URL,
            lastSuccessfulSyncAt = prefs[lastSuccessfulSyncAtKey(userId)],
        )
    }

    override suspend fun saveSettings(settings: SyncSettings) {
        val userId = currentUserScope()
        context.syncPreferencesDataStore.edit { prefs ->
            prefs[syncEnabledKey(userId)] = settings.syncEnabled
            prefs[backupModeKey(userId)] = settings.backupMode.name
            if (settings.apiBaseUrl.isNullOrBlank()) {
                prefs.remove(apiBaseUrlKey(userId))
            } else {
                prefs[apiBaseUrlKey(userId)] = settings.apiBaseUrl
            }
            settings.lastSuccessfulSyncAt?.let { prefs[lastSuccessfulSyncAtKey(userId)] = it }
        }
    }
}

class AndroidLocalStore(
    private val context: Context,
    val foodRepository: FoodRepository,
    val barcodeAliasRepository: BarcodeAliasRepository,
    val diaryRepository: DiaryRepository,
    val syncOutboxRepository: SyncOutboxRepository,
    val syncStateRepository: SyncStateRepository,
    val authRepository: AuthRepository,
) {
    suspend fun currentOwnerUserId(): String = authRepository.currentOwnerUserId()

    suspend fun currentAuthSession(): AuthSession? = authRepository.currentSession()

    suspend fun deviceId(): String {
        val existing = syncStateRepository.getCursor()?.deviceId
        if (existing != null) return existing
        val next = createId("device")
        syncStateRepository.saveCursor(SyncCursor(deviceId = next))
        return next
    }

    suspend fun hiddenSeedIds(): Set<String> =
        context.syncPreferencesDataStore.data.first()[SyncPreferencesKeys.HiddenSeedIds] ?: emptySet()

    suspend fun saveHiddenSeedIds(ids: Set<String>) {
        context.syncPreferencesDataStore.edit { prefs ->
            prefs[SyncPreferencesKeys.HiddenSeedIds] = ids
        }
    }

    suspend fun migrateLegacyIfNeeded(
        readLegacyFoods: (Context, String) -> List<FoodItem>,
        readLegacyDiary: (Context) -> List<com.philipcosgrave.calorietracker.model.DiaryEntry>,
        readLegacyStrings: (Context, String) -> List<String>,
    ) {
        val prefs = context.syncPreferencesDataStore.data.first()
        if (prefs[SyncPreferencesKeys.LegacyMigrationComplete] == true) return
        if (foodRepository.list().isNotEmpty() || diaryRepository.list().isNotEmpty()) {
            context.syncPreferencesDataStore.edit { it[SyncPreferencesKeys.LegacyMigrationComplete] = true }
            return
        }

        val deviceId = deviceId()
        val customFoods = readLegacyFoods(context, "customFoods")
        val recipes = readLegacyFoods(context, "recipes")
        val diaryEntries = readLegacyDiary(context)
        val hiddenSeeds = readLegacyStrings(context, "hiddenSeedIds").toSet()

        for (food in customFoods + recipes) {
            foodRepository.save(
                FoodItemRecord(
                    food = food,
                    sync = SyncMetadata(
                        recordId = food.id,
                        version = 1,
                        updatedAt = java.time.Instant.now().toString(),
                        originDeviceId = deviceId,
                    ),
                ),
            )
        }

        for (entry in diaryEntries) {
            diaryRepository.save(
                DiaryEntryRecord(
                    entry = entry,
                    sync = SyncMetadata(
                        recordId = entry.id,
                        version = 1,
                        updatedAt = java.time.Instant.now().toString(),
                        originDeviceId = deviceId,
                    ),
                ),
            )
        }

        saveHiddenSeedIds(hiddenSeeds)
        context.syncPreferencesDataStore.edit { it[SyncPreferencesKeys.LegacyMigrationComplete] = true }
    }
}

private fun FoodRecordEntity.toSyncMetadata(): SyncMetadata =
    syncMetadataFromEntity(recordId, version, updatedAt, deletedAt, originDeviceId, lastSyncedAt, syncStatus)

private fun BarcodeAliasEntity.toSyncMetadata(): SyncMetadata =
    syncMetadataFromEntity(recordId, version, updatedAt, deletedAt, originDeviceId, lastSyncedAt, syncStatus)

private fun DiaryRecordEntity.toSyncMetadata(): SyncMetadata =
    syncMetadataFromEntity(recordId, version, updatedAt, deletedAt, originDeviceId, lastSyncedAt, syncStatus)
