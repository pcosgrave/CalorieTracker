package com.philipcosgrave.calorietracker.data.repository

import android.net.Uri
import com.philipcosgrave.calorietracker.model.BarcodeAliasRecord
import com.philipcosgrave.calorietracker.model.AuthSession
import com.philipcosgrave.calorietracker.model.DiaryEntryRecord
import com.philipcosgrave.calorietracker.model.FoodItemRecord
import com.philipcosgrave.calorietracker.model.SyncChangeEnvelope
import com.philipcosgrave.calorietracker.model.SyncCursor
import com.philipcosgrave.calorietracker.model.SyncPullResponse
import com.philipcosgrave.calorietracker.model.SyncPushResponse
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.model.WeightEntry
import java.time.LocalDate

interface FoodRepository {
    suspend fun list(): List<FoodItemRecord>
    suspend fun getById(recordId: String): FoodItemRecord?
    suspend fun getByBarcode(barcode: String): FoodItemRecord?
    suspend fun save(record: FoodItemRecord)
    suspend fun softDelete(recordId: String, deletedAt: String)
}

interface BarcodeAliasRepository {
    suspend fun list(): List<BarcodeAliasRecord>
    suspend fun getByBarcode(barcode: String): BarcodeAliasRecord?
    suspend fun save(record: BarcodeAliasRecord)
    suspend fun softDelete(recordId: String, deletedAt: String)
}

interface DiaryRepository {
    suspend fun list(): List<DiaryEntryRecord>
    suspend fun listByDateRange(startDate: LocalDate, endDate: LocalDate): List<DiaryEntryRecord>
    suspend fun getById(recordId: String): DiaryEntryRecord?
    suspend fun save(record: DiaryEntryRecord)
    suspend fun softDelete(recordId: String, deletedAt: String)
}

interface WeightRepository {
    suspend fun list(ownerUserId: String): List<WeightEntry>
    suspend fun latest(ownerUserId: String): WeightEntry?
    suspend fun getById(ownerUserId: String, recordId: String): WeightEntry?
    suspend fun save(ownerUserId: String, entry: WeightEntry)
    suspend fun delete(ownerUserId: String, recordId: String)
}

interface SyncOutboxRepository {
    suspend fun listPendingChanges(): List<SyncChangeEnvelope<*>>
    suspend fun enqueue(change: SyncChangeEnvelope<*>)
    suspend fun acknowledge(changeIds: List<String>, syncedAt: String)
    suspend fun markRejected(changeId: String)
    suspend fun clear()
}

interface SyncStateRepository {
    suspend fun getCursor(): SyncCursor?
    suspend fun saveCursor(cursor: SyncCursor)
    suspend fun getSettings(): SyncSettings
    suspend fun saveSettings(settings: SyncSettings)
}

interface SyncTransport {
    suspend fun push(changes: List<SyncChangeEnvelope<*>>, cursor: SyncCursor?): SyncPushResponse
    suspend fun pull(cursor: SyncCursor?): SyncPullResponse
}

interface AuthRepository {
    suspend fun currentSession(): AuthSession?
    suspend fun currentOwnerUserId(): String
    suspend fun beginSignIn(returnToPath: String = "/settings", provider: String? = null): Uri
    suspend fun completeSignIn(callbackUri: Uri): AuthSession
    suspend fun signOut(): Uri
    suspend fun refreshSessionIfNeeded(): AuthSession?
}
