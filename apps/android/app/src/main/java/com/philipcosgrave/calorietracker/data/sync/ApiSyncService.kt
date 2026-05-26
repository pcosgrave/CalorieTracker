package com.philipcosgrave.calorietracker.data.sync

import com.philipcosgrave.calorietracker.data.local.barcodeAliasRecordPayloadFromJson
import com.philipcosgrave.calorietracker.data.local.barcodeAliasRecordPayloadToJson
import com.philipcosgrave.calorietracker.data.repository.AndroidLocalStore
import com.philipcosgrave.calorietracker.domain.nowIsoString
import com.philipcosgrave.calorietracker.model.BarcodeAliasRecord
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.DiaryEntryRecord
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodItemRecord
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.SyncChangeEnvelope
import com.philipcosgrave.calorietracker.model.SyncCursor
import com.philipcosgrave.calorietracker.model.SyncEntityType
import com.philipcosgrave.calorietracker.model.SyncOperation
import com.philipcosgrave.calorietracker.model.SyncPullResponse
import com.philipcosgrave.calorietracker.model.SyncPushResponse
import com.philipcosgrave.calorietracker.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

class ApiSyncService(private val localStore: AndroidLocalStore) {
    suspend fun syncNow(): SyncSummary = withContext(Dispatchers.IO) {
        val settings = localStore.syncStateRepository.getSettings()
        require(settings.syncEnabled && !settings.apiBaseUrl.isNullOrBlank()) {
            "Sync is disabled or API base URL is missing"
        }
        require(localStore.currentAuthSession() != null) {
            "You must sign in before syncing."
        }

        val cursor = localStore.syncStateRepository.getCursor() ?: SyncCursor(deviceId = localStore.deviceId())
        val pending = localStore.syncOutboxRepository.listPendingChanges()
        val pushResponse = postPush(settings.apiBaseUrl!!, cursor, pending)
        localStore.syncOutboxRepository.acknowledge(pushResponse.acceptedChangeIds, nowIsoString())

        val pullResponse = postPull(settings.apiBaseUrl, pushResponse.cursor)
        applyIncomingChanges(pullResponse)
        localStore.syncStateRepository.saveCursor(pullResponse.cursor)
        localStore.syncStateRepository.saveSettings(settings.copy(lastSuccessfulSyncAt = nowIsoString()))

        SyncSummary(pushResponse.acceptedChangeIds.size, pullResponse.changes.size)
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
        val ownerUserId = localStore.currentOwnerUserId()
        val requestBody = JSONObject()
            .put("deviceId", cursor.deviceId)
            .put("cursor", cursorToJson(cursor))
            .put("changes", JSONArray().apply { changes.forEach { put(syncChangeToJson(it, ownerUserId)) } })

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

    private fun syncChangeToJson(change: SyncChangeEnvelope<*>, ownerUserId: String): JSONObject =
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
            .put("payload", changePayloadToJson(change, ownerUserId))

    private fun changePayloadToJson(change: SyncChangeEnvelope<*>, ownerUserId: String): Any? =
        when (val payload = change.payload) {
            is FoodItemRecord -> JSONObject()
                .put("product", foodItemToWireJson(payload.food, payload.sync.updatedAt, ownerUserId))
                .put("sync", syncMetadataToJson(payload.sync))
            is BarcodeAliasRecord -> JSONObject()
                .put("alias", JSONObject(barcodeAliasRecordPayloadToJson(payload)))
                .put("sync", syncMetadataToJson(payload.sync))
            is DiaryEntryRecord -> JSONObject()
                .put("entry", diaryEntryToWireJson(payload, ownerUserId))
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
                val sync = syncMetadataFromJson(it.getJSONObject("sync"))
                val productJson = it.optJSONObject("product") ?: it.optJSONObject("food")
                productJson?.let { product ->
                    FoodItemRecord(
                        food = foodItemFromWireJson(product),
                        sync = sync,
                    )
                }
            }
            SyncEntityType.BarcodeAlias -> payloadJson?.let {
                barcodeAliasRecordPayloadFromJson(it.getJSONObject("alias").toString(), syncMetadataFromJson(it.getJSONObject("sync")))
            }
            SyncEntityType.DiaryEntry -> payloadJson?.let {
                val sync = syncMetadataFromJson(it.getJSONObject("sync"))
                val entryJson = it.optJSONObject("entry")
                entryJson?.let { entry ->
                    DiaryEntryRecord(
                        entry = diaryEntryFromWireJson(entry),
                        sync = sync,
                    )
                }
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

    private suspend fun postJson(url: String, body: JSONObject): JSONObject {
        val session = localStore.authRepository.refreshSessionIfNeeded()
            ?: error("You must sign in before syncing.")

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
        connection.doOutput = true
        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray())
        }
        val inputStream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = inputStream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            error("Sync request failed with status ${connection.responseCode}: $responseText")
        }
        return JSONObject(responseText)
    }
}

private fun foodItemToWireJson(food: FoodItem, updatedAt: String, ownerUserId: String): JSONObject =
    JSONObject()
        .put("productId", food.id)
        .put("ownerUserId", ownerUserId)
        .put("visibility", "private")
        .put("barcode", food.barcode.ifBlank { null })
        .put("name", food.name)
        .put("brand", food.brand.ifBlank { null })
        .put(
            "serving",
            JSONObject()
                .put("label", food.servingUnit)
                .put("quantity", food.servingQuantity)
                .put("unit", food.servingUnit),
        )
        .put(
            "nutrients",
            JSONObject()
                .put("calories", food.nutrients.calories)
                .put("proteinGrams", food.nutrients.proteinGrams)
                .put("carbohydrateGrams", food.nutrients.carbohydrateGrams)
                .put("fatGrams", food.nutrients.fatGrams),
        )
        .put("recipeComponents", JSONArray().apply {
            food.components.forEach { component ->
                put(
                    JSONObject()
                        .put("item", foodItemToWireJson(component.item, updatedAt, ownerUserId))
                        .put("amount", component.amount)
                        .put("unit", component.unit),
                )
            }
        })
        .put("createdAt", updatedAt)
        .put("updatedAt", updatedAt)

private fun diaryEntryToWireJson(record: DiaryEntryRecord, ownerUserId: String): JSONObject =
    JSONObject()
        .put("entryId", record.entry.id)
        .put("ownerUserId", ownerUserId)
        .put("productId", record.entry.food.id)
        .put("loggedAt", "${record.entry.date}T12:00:00.000Z")
        .put("meal", when (record.entry.meal) {
            Meal.Breakfast -> "breakfast"
            Meal.Lunch -> "lunch"
            Meal.Dinner -> "dinner"
            Meal.Snack -> "snack"
        })
        .put("servingMultiplier", record.entry.servingMultiplier)
        .put("loggedAmount", record.entry.loggedAmount)
        .put("loggedUnit", record.entry.loggedUnit)
        .put("productSnapshot", foodItemToWireJson(record.entry.food, record.sync.updatedAt, ownerUserId))
        .put("createdAt", record.sync.updatedAt)
        .put("updatedAt", record.sync.updatedAt)

private fun foodItemFromWireJson(json: JSONObject): FoodItem {
    val componentsJson = json.optJSONArray("recipeComponents") ?: JSONArray()
    return FoodItem(
        id = json.optString("productId", json.optString("id")),
        kind = if (componentsJson.length() > 0) FoodKind.Recipe else FoodKind.Ingredient,
        name = json.getString("name"),
        brand = json.optString("brand"),
        barcode = json.optString("barcode"),
        servingQuantity = json.optJSONObject("serving")?.optDouble("quantity") ?: json.optDouble("servingQuantity", 1.0),
        servingUnit = json.optJSONObject("serving")?.optString("unit") ?: json.optString("servingUnit", "serving"),
        nutrients = nutrientsFromWireJson(json.getJSONObject("nutrients")),
        components = List(componentsJson.length()) { index ->
            val component = componentsJson.getJSONObject(index)
            RecipeComponent(
                item = foodItemFromWireJson(component.getJSONObject("item")),
                amount = component.optDouble("amount"),
                unit = component.optString("unit", "serving"),
            )
        },
    )
}

private fun diaryEntryFromWireJson(json: JSONObject): DiaryEntry {
    val food = foodItemFromWireJson(json.getJSONObject("productSnapshot"))
    val servingMultiplier = json.getDouble("servingMultiplier")
    return DiaryEntry(
        id = json.optString("entryId", json.optString("id")),
        food = food,
        date = LocalDate.parse(json.optString("loggedAt").take(10)),
        meal = when (json.getString("meal")) {
            "breakfast" -> Meal.Breakfast
            "lunch" -> Meal.Lunch
            "dinner" -> Meal.Dinner
            else -> Meal.Snack
        },
        servingMultiplier = servingMultiplier,
        loggedAmount = json.optDouble("loggedAmount").takeIf { !it.isNaN() && it > 0 } ?: (food.servingQuantity * servingMultiplier),
        loggedUnit = json.optString("loggedUnit").ifBlank { food.servingUnit },
    )
}

private fun nutrientsFromWireJson(json: JSONObject): Nutrients =
    Nutrients(
        calories = json.getDouble("calories"),
        proteinGrams = json.optDouble("proteinGrams"),
        carbohydrateGrams = json.optDouble("carbohydrateGrams"),
        fatGrams = json.optDouble("fatGrams"),
    )

data class SyncSummary(
    val pushed: Int,
    val pulled: Int,
)
