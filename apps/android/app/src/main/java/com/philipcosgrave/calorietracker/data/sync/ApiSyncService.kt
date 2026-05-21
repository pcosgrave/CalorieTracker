package com.philipcosgrave.calorietracker.data.sync

import com.philipcosgrave.calorietracker.data.local.barcodeAliasRecordPayloadFromJson
import com.philipcosgrave.calorietracker.data.local.barcodeAliasRecordPayloadToJson
import com.philipcosgrave.calorietracker.data.local.diaryEntryFromJsonString
import com.philipcosgrave.calorietracker.data.local.toJsonString
import com.philipcosgrave.calorietracker.data.repository.AndroidLocalStore
import com.philipcosgrave.calorietracker.domain.nowIsoString
import com.philipcosgrave.calorietracker.model.BarcodeAliasRecord
import com.philipcosgrave.calorietracker.model.DiaryEntryRecord
import com.philipcosgrave.calorietracker.model.FoodItemRecord
import com.philipcosgrave.calorietracker.model.SyncChangeEnvelope
import com.philipcosgrave.calorietracker.model.SyncCursor
import com.philipcosgrave.calorietracker.model.SyncEntityType
import com.philipcosgrave.calorietracker.model.SyncOperation
import com.philipcosgrave.calorietracker.model.SyncPullResponse
import com.philipcosgrave.calorietracker.model.SyncPushResponse
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.model.SyncStatus
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiSyncService(private val localStore: AndroidLocalStore) {
    suspend fun syncNow(): SyncSummary {
        val settings = localStore.syncStateRepository.getSettings()
        require(settings.syncEnabled && !settings.apiBaseUrl.isNullOrBlank()) {
            "Sync is disabled or API base URL is missing"
        }

        val cursor = localStore.syncStateRepository.getCursor() ?: SyncCursor(deviceId = localStore.deviceId())
        val pending = localStore.syncOutboxRepository.listPendingChanges()
        val pushResponse = postPush(settings.apiBaseUrl!!, cursor, pending)
        localStore.syncOutboxRepository.acknowledge(pushResponse.acceptedChangeIds, nowIsoString())

        val pullResponse = postPull(settings.apiBaseUrl, pushResponse.cursor)
        applyIncomingChanges(pullResponse)
        localStore.syncStateRepository.saveCursor(pullResponse.cursor)
        localStore.syncStateRepository.saveSettings(settings.copy(lastSuccessfulSyncAt = nowIsoString()))

        return SyncSummary(pushResponse.acceptedChangeIds.size, pullResponse.changes.size)
    }

    private suspend fun applyIncomingChanges(response: SyncPullResponse) {
        for (change in response.changes) {
            when (change.entityType) {
                SyncEntityType.FoodProduct -> {
                    val payload = change.payload as? FoodItemRecord ?: continue
                    localStore.foodRepository.save(payload)
                }
                SyncEntityType.BarcodeAlias -> {
                    val payload = change.payload as? BarcodeAliasRecord ?: continue
                    localStore.barcodeAliasRepository.save(payload)
                }
                SyncEntityType.DiaryEntry -> {
                    val payload = change.payload as? DiaryEntryRecord ?: continue
                    localStore.diaryRepository.save(payload)
                }
            }
        }
    }

    private suspend fun postPush(baseUrl: String, cursor: SyncCursor, changes: List<SyncChangeEnvelope<*>>): SyncPushResponse {
        val requestBody = JSONObject()
            .put("userId", "local")
            .put("deviceId", cursor.deviceId)
            .put("cursor", cursorToJson(cursor))
            .put("changes", JSONArray().apply { changes.forEach { put(syncChangeToJson(it)) } })

        val response = postJson("${baseUrl.trimEnd('/')}/sync/push", requestBody)
        val cursorJson = response.getJSONObject("cursor")
        val accepted = response.getJSONArray("acceptedChangeIds")
        return SyncPushResponse(
            cursor = SyncCursor(
                deviceId = cursorJson.getString("deviceId"),
                lastPulledAt = cursorJson.optString("lastPulledAt").takeIf { it.isNotBlank() },
                lastAcknowledgedChangeId = cursorJson.optString("lastAcknowledgedChangeId").takeIf { it.isNotBlank() },
            ),
            acceptedChangeIds = List(accepted.length()) { accepted.getString(it) },
            rejectedChanges = emptyList(),
        )
    }

    private suspend fun postPull(baseUrl: String, cursor: SyncCursor): SyncPullResponse {
        val requestBody = JSONObject()
            .put("userId", "local")
            .put("deviceId", cursor.deviceId)
            .put("cursor", cursorToJson(cursor))

        val response = postJson("${baseUrl.trimEnd('/')}/sync/pull", requestBody)
        val cursorJson = response.getJSONObject("cursor")
        val changesJson = response.getJSONArray("changes")
        return SyncPullResponse(
            cursor = SyncCursor(
                deviceId = cursorJson.getString("deviceId"),
                lastPulledAt = cursorJson.optString("lastPulledAt").takeIf { it.isNotBlank() },
                lastAcknowledgedChangeId = cursorJson.optString("lastAcknowledgedChangeId").takeIf { it.isNotBlank() },
            ),
            changes = List(changesJson.length()) { index ->
                syncChangeFromJson(changesJson.getJSONObject(index))
            },
        )
    }

    private fun cursorToJson(cursor: SyncCursor): JSONObject =
        JSONObject()
            .put("deviceId", cursor.deviceId)
            .put("lastPulledAt", cursor.lastPulledAt)
            .put("lastAcknowledgedChangeId", cursor.lastAcknowledgedChangeId)

    private fun syncChangeToJson(change: SyncChangeEnvelope<*>): JSONObject =
        JSONObject()
            .put("changeId", change.changeId)
            .put("entityType", when (change.entityType) {
                SyncEntityType.FoodProduct -> "food_product"
                SyncEntityType.BarcodeAlias -> "barcode_alias"
                SyncEntityType.DiaryEntry -> "diary_entry"
            })
            .put("recordId", change.recordId)
            .put("operation", if (change.operation == SyncOperation.Upsert) "upsert" else "delete")
            .put("changedAt", change.changedAt)
            .put("deviceId", change.deviceId)
            .put("baseVersion", change.baseVersion)
            .put("payload", changePayloadToJson(change))

    private fun changePayloadToJson(change: SyncChangeEnvelope<*>): Any? =
        when (val payload = change.payload) {
            is FoodItemRecord -> JSONObject()
                .put("product", JSONObject(payload.food.toJsonString()))
                .put("sync", syncMetadataToJson(payload.sync))
            is BarcodeAliasRecord -> JSONObject()
                .put("alias", JSONObject(barcodeAliasRecordPayloadToJson(payload)))
                .put("sync", syncMetadataToJson(payload.sync))
            is DiaryEntryRecord -> JSONObject()
                .put("entry", JSONObject(payload.entry.toJsonString()))
                .put("sync", syncMetadataToJson(payload.sync))
            else -> null
        }

    private fun syncChangeFromJson(json: JSONObject): SyncChangeEnvelope<*> {
        val entityType = when (json.getString("entityType")) {
            "food_product" -> SyncEntityType.FoodProduct
            "barcode_alias" -> SyncEntityType.BarcodeAlias
            else -> SyncEntityType.DiaryEntry
        }
        val payloadJson = json.optJSONObject("payload")
        val payload = when (entityType) {
            SyncEntityType.FoodProduct -> payloadJson?.let {
                FoodItemRecord(
                    food = com.philipcosgrave.calorietracker.data.local.foodItemFromJsonString(it.getJSONObject("product").toString()),
                    sync = syncMetadataFromJson(it.getJSONObject("sync")),
                )
            }
            SyncEntityType.BarcodeAlias -> payloadJson?.let {
                barcodeAliasRecordPayloadFromJson(it.getJSONObject("alias").toString(), syncMetadataFromJson(it.getJSONObject("sync")))
            }
            SyncEntityType.DiaryEntry -> payloadJson?.let {
                DiaryEntryRecord(
                    entry = diaryEntryFromJsonString(it.getJSONObject("entry").toString()),
                    sync = syncMetadataFromJson(it.getJSONObject("sync")),
                )
            }
        }
        return SyncChangeEnvelope(
            changeId = json.getString("changeId"),
            entityType = entityType,
            recordId = json.getString("recordId"),
            operation = if (json.getString("operation") == "upsert") SyncOperation.Upsert else SyncOperation.Delete,
            changedAt = json.getString("changedAt"),
            deviceId = json.getString("deviceId"),
            payload = payload,
            baseVersion = json.optLong("baseVersion").takeIf { json.has("baseVersion") },
        )
    }

    private fun syncMetadataToJson(sync: com.philipcosgrave.calorietracker.model.SyncMetadata): JSONObject =
        JSONObject()
            .put("recordId", sync.recordId)
            .put("version", sync.version)
            .put("updatedAt", sync.updatedAt)
            .put("deletedAt", sync.deletedAt)
            .put("originDeviceId", sync.originDeviceId)
            .put("lastSyncedAt", sync.lastSyncedAt)
            .put("syncStatus", syncStatusToWire(sync.syncStatus))

    private fun syncMetadataFromJson(json: JSONObject): com.philipcosgrave.calorietracker.model.SyncMetadata =
        com.philipcosgrave.calorietracker.model.SyncMetadata(
            recordId = json.getString("recordId"),
            version = json.getLong("version"),
            updatedAt = json.getString("updatedAt"),
            deletedAt = json.optString("deletedAt").takeIf { it.isNotBlank() },
            originDeviceId = json.getString("originDeviceId"),
            lastSyncedAt = json.optString("lastSyncedAt").takeIf { it.isNotBlank() },
            syncStatus = syncStatusFromWire(json.getString("syncStatus")),
        )

    private fun syncStatusToWire(value: SyncStatus): String =
        when (value) {
            SyncStatus.LocalOnly -> "local_only"
            SyncStatus.PendingPush -> "pending_push"
            SyncStatus.Synced -> "synced"
            SyncStatus.SyncError -> "sync_error"
        }

    private fun syncStatusFromWire(value: String): SyncStatus =
        when (value) {
            "local_only" -> SyncStatus.LocalOnly
            "pending_push" -> SyncStatus.PendingPush
            "synced" -> SyncStatus.Synced
            "sync_error" -> SyncStatus.SyncError
            else -> SyncStatus.LocalOnly
        }

    private fun postJson(url: String, body: JSONObject): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-debug-user-id", "local")
        connection.doOutput = true
        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray())
        }
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(responseText)
    }
}

data class SyncSummary(
    val pushed: Int,
    val pulled: Int,
)
