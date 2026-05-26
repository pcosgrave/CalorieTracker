package com.philipcosgrave.calorietracker.test

import com.philipcosgrave.calorietracker.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Mock repository for testing purposes.
 * Provides a flexible, configurable repository implementation for unit tests.
 */
class MockFoodRepository {
    private val _foodItems = MutableStateFlow(emptyList<FoodItem>())
    val foodItems: StateFlow<List<FoodItem>> = _foodItems

    private val _foodRecords = MutableStateFlow(emptyList<FoodItemRecord>())
    val foodRecords: StateFlow<List<FoodItemRecord>> = _foodRecords

    suspend fun addFoodItem(food: FoodItem) {
        val list = _foodItems.value + food
        _foodItems.value = list.sortedBy { it.name }
    }

    suspend fun addFoodRecord(record: FoodItemRecord) {
        val list = _foodRecords.value + record
        _foodRecords.value = list.sortedBy { it.food.id }
    }

    suspend fun getFoodItems(): List<FoodItem> = _foodItems.value

    suspend fun getFoodRecords(): List<FoodItemRecord> = _foodRecords.value

    suspend fun getFoodById(id: String): FoodItem? = _foodItems.value.find { it.id == id }

    suspend fun updateFoodRecord(record: FoodItemRecord): FoodItemRecord? {
        val index = _foodRecords.indexOfFirst { it.recordId == record.recordId }
        return if (index >= 0) {
            val list = _foodRecords.toMutableList()
            list[index] = record
            _foodRecords.value = list
            record
        } else {
            null
        }
    }

    suspend fun deleteFoodRecord(recordId: String): Boolean {
        val newList = _foodRecords.filter { it.recordId != recordId }
        _foodRecords.value = newList
        return newList.size < _foodRecords.value.size
    }
}

class MockDiaryRepository {
    private val _diaryEntries = MutableStateFlow(emptyList<DiaryEntry>())
    val diaryEntries: StateFlow<List<DiaryEntry>> = _diaryEntries

    private val _diaryRecords = MutableStateFlow(emptyList<DiaryEntryRecord>())
    val diaryRecords: StateFlow<List<DiaryEntryRecord>> = _diaryRecords

    suspend fun addDiaryEntry(entry: DiaryEntry) {
        val list = _diaryEntries.value + entry
        _diaryEntries.value = list.sortedBy { it.date }
    }

    suspend fun addDiaryRecord(record: DiaryEntryRecord) {
        val list = _diaryRecords.value + record
        _diaryRecords.value = list.sortedBy { it.entry.date }
    }

    suspend fun getDiaryEntries(): List<DiaryEntry> = _diaryEntries.value

    suspend fun getDiaryRecords(): List<DiaryEntryRecord> = _diaryRecords.value

    suspend fun getDiaryEntryById(id: String): DiaryEntry? = _diaryEntries.value.find { it.id == id }

    suspend fun getDiaryEntriesForDate(date: LocalDate): List<DiaryEntry> {
        return _diaryEntries.value.filter { it.date == date }
    }

    suspend fun getDiaryEntryForDateAndMeal(date: LocalDate, meal: Meal): DiaryEntry? {
        return _diaryEntries.value.firstOrNull { it.date == date && it.meal == meal }
    }

    suspend fun updateDiaryRecord(record: DiaryEntryRecord): DiaryEntryRecord? {
        val index = _diaryRecords.indexOfFirst { it.recordId == record.recordId }
        return if (index >= 0) {
            val list = _diaryRecords.toMutableList()
            list[index] = record
            _diaryRecords.value = list
            record
        } else {
            null
        }
    }

    suspend fun deleteDiaryRecord(recordId: String): Boolean {
        val newList = _diaryRecords.filter { it.recordId != recordId }
        _diaryRecords.value = newList
        return newList.size < _diaryRecords.value.size
    }
}

class MockBarcodeAliasRepository {
    private val _barcodeAliases = MutableStateFlow(emptyList<BarcodeAliasRecord>())
    val barcodeAliases: StateFlow<List<BarcodeAliasRecord>> = _barcodeAliases

    suspend fun addBarcodeAlias(alias: BarcodeAliasRecord) {
        val list = _barcodeAliases.value + alias
        _barcodeAliases.value = list.sortedBy { it.barcode }
    }

    suspend fun getBarcodeAliases(): List<BarcodeAliasRecord> = _barcodeAliases.value

    suspend fun getBarcodeAliasesByOwner(userId: String): List<BarcodeAliasRecord> {
        return _barcodeAliases.value.filter { it.ownerUserId == userId }
    }

    suspend fun getBarcodeAlias(barcode: String): BarcodeAliasRecord? {
        return _barcodeAliases.value.find { it.barcode == barcode }
    }

    suspend fun deleteBarcodeAlias(barcode: String): Boolean {
        val newList = _barcodeAliases.filter { it.barcode != barcode }
        _barcodeAliases.value = newList
        return newList.size < _barcodeAliases.value.size
    }
}

class MockWeightRepository {
    private val _weightEntries = MutableStateFlow(emptyList<WeightEntry>())
    val weightEntries: StateFlow<List<WeightEntry>> = _weightEntries

    suspend fun addWeightEntry(entry: WeightEntry) {
        val list = _weightEntries.value + entry
        _weightEntries.value = list.sortedBy { it.date }
    }

    suspend fun getWeightEntries(): List<WeightEntry> = _weightEntries.value

    suspend fun getWeightEntriesForDate(date: LocalDate): List<WeightEntry> {
        return _weightEntries.value.filter { it.date == date }
    }

    suspend fun getLatestWeightEntry(): WeightEntry? = _weightEntries.value.lastOrNull()

    suspend fun getWeightEntryForDate(date: LocalDate): WeightEntry? {
        return _weightEntries.value.firstOrNull { it.date == date }
    }

    suspend fun updateWeightEntry(entry: WeightEntry): WeightEntry? {
        val index = _weightEntries.indexOfFirst { it.id == entry.id }
        return if (index >= 0) {
            val list = _weightEntries.toMutableList()
            list[index] = entry
            _weightEntries.value = list
            entry
        } else {
            null
        }
    }

    suspend fun deleteWeightEntry(entryId: String): Boolean {
        val newList = _weightEntries.filter { it.id != entryId }
        _weightEntries.value = newList
        return newList.size < _weightEntries.value.size
    }
}

class MockSyncRepository {
    private val _syncMetadata = MutableStateFlow(emptyMap<String, SyncMetadata>())
    val syncMetadata: StateFlow<Map<String, SyncMetadata>> = _syncMetadata

    suspend fun addSyncMetadata(recordId: String, metadata: SyncMetadata) {
        val current = _syncMetadata.value
        _syncMetadata.value = current + mapOf(recordId to metadata)
    }

    suspend fun getSyncMetadata(recordId: String): SyncMetadata? = _syncMetadata.value[recordId]

    suspend fun getSyncMetadataByDevice(deviceId: String): Map<String, SyncMetadata> {
        return _syncMetadata.value.filter { it.value.originDeviceId == deviceId }
    }

    suspend fun updateSyncMetadata(recordId: String, metadata: SyncMetadata): SyncMetadata? {
        return _syncMetadata[recordId]?.let {
            _syncMetadata.value[recordId] = metadata
            metadata
        }
    }

    suspend fun markAsSynced(recordId: String): SyncMetadata? {
        return _syncMetadata.value[recordId]?.let { metadata ->
            val updated = metadata.copy(
                updatedAt = metadata.updatedAt,
                syncStatus = SyncStatus.Synced,
                lastSyncedAt = java.time.Instant.now().toString()
            )
            _syncMetadata.value[recordId] = updated
            updated
        }
    }

    suspend fun markAsError(recordId: String): SyncMetadata? {
        return _syncMetadata.value[recordId]?.let { metadata ->
            val updated = metadata.copy(
                syncStatus = SyncStatus.SyncError,
                updatedAt = java.time.Instant.now().toString()
            )
            _syncMetadata.value[recordId] = updated
            updated
        }
    }

    suspend fun getPendingRecords(): List<Pair<String, SyncMetadata>> {
        return _syncMetadata.value.filter { it.value.syncStatus == SyncStatus.PendingPush }
            .map { it.value.recordId to it.value }
    }
}

class MockLocalRepositories(
    private val deviceId: String = "test-device",
    private val mockFoodRepository: MockFoodRepository = MockFoodRepository(),
    private val mockDiaryRepository: MockDiaryRepository = MockDiaryRepository(),
    private val mockBarcodeAliasRepository: MockBarcodeAliasRepository = MockBarcodeAliasRepository(),
    private val mockWeightRepository: MockWeightRepository = MockWeightRepository(),
    private val mockSyncRepository: MockSyncRepository = MockSyncRepository()
) {
    val foodRepository = mockFoodRepository
    val diaryRepository = mockDiaryRepository
    val barcodeAliasRepository = mockBarcodeAliasRepository
    val weightRepository = mockWeightRepository
    val syncRepository = mockSyncRepository
}
