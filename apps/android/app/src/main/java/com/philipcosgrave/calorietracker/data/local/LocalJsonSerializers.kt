package com.philipcosgrave.calorietracker.data.local

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
import com.philipcosgrave.calorietracker.model.SyncEntityType
import com.philipcosgrave.calorietracker.model.SyncMetadata
import com.philipcosgrave.calorietracker.model.SyncOperation
import com.philipcosgrave.calorietracker.model.SyncStatus
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

fun FoodItem.toJsonString(): String = toJson().toString()

fun foodItemFromJsonString(value: String): FoodItem = foodItemFromJson(JSONObject(value))

fun DiaryEntry.toJsonString(): String =
    JSONObject()
        .put("id", id)
        .put("food", JSONObject(food.toJsonString()))
        .put("date", date.toString())
        .put("meal", meal.name)
        .put("servingMultiplier", servingMultiplier)
        .put("loggedAmount", loggedAmount)
        .put("loggedUnit", loggedUnit)
        .toString()

fun diaryEntryFromJsonString(value: String): DiaryEntry {
    val item = JSONObject(value)
    val food = foodItemFromJson(item.getJSONObject("food"))
    val servingMultiplier = item.getDouble("servingMultiplier")
    return DiaryEntry(
        id = item.getString("id"),
        food = food,
        date = LocalDate.parse(item.getString("date")),
        meal = Meal.valueOf(item.getString("meal")),
        servingMultiplier = servingMultiplier,
        loggedAmount = item.optDouble("loggedAmount").takeIf { !it.isNaN() && it > 0 } ?: (food.servingQuantity * servingMultiplier),
        loggedUnit = item.optString("loggedUnit").ifBlank { food.servingUnit },
    )
}

fun barcodeAliasRecordPayloadToJson(record: BarcodeAliasRecord): String =
    JSONObject()
        .put("barcode", record.barcode)
        .put("productId", record.productId)
        .put("ownerUserId", record.ownerUserId)
        .put("visibility", record.visibility)
        .put("createdAt", record.createdAt)
        .toString()

fun barcodeAliasRecordPayloadFromJson(value: String, sync: SyncMetadata): BarcodeAliasRecord {
    val json = JSONObject(value)
    return BarcodeAliasRecord(
        barcode = json.getString("barcode"),
        productId = json.getString("productId"),
        ownerUserId = json.getString("ownerUserId"),
        visibility = json.getString("visibility"),
        createdAt = json.getString("createdAt"),
        sync = sync,
    )
}

fun syncMetadataFromEntity(
    recordId: String,
    version: Long,
    updatedAt: String,
    deletedAt: String?,
    originDeviceId: String,
    lastSyncedAt: String?,
    syncStatus: String,
): SyncMetadata =
    SyncMetadata(
        recordId = recordId,
        version = version,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        originDeviceId = originDeviceId,
        lastSyncedAt = lastSyncedAt,
        syncStatus = SyncStatus.valueOf(syncStatus),
    )

fun SyncChangeEnvelope<*>.payloadToJsonString(): String? {
    val payloadValue = payload ?: return null
    return when (entityType) {
        SyncEntityType.FoodProduct -> JSONObject().put("food", JSONObject((payloadValue as FoodItemRecord).food.toJsonString())).put("sync", syncToJson(payloadValue.sync)).toString()
        SyncEntityType.BarcodeAlias -> JSONObject().put("alias", JSONObject(barcodeAliasRecordPayloadToJson(payloadValue as BarcodeAliasRecord))).put("sync", syncToJson(payloadValue.sync)).toString()
        SyncEntityType.DiaryEntry -> JSONObject().put("entry", JSONObject((payloadValue as DiaryEntryRecord).entry.toJsonString())).put("sync", syncToJson(payloadValue.sync)).toString()
    }
}

fun syncChangePayloadFromJson(
    entityType: SyncEntityType,
    value: String?,
): Any? {
    if (value == null) return null
    val json = JSONObject(value)
    val sync = syncFromJson(json.getJSONObject("sync"))
    return when (entityType) {
        SyncEntityType.FoodProduct -> FoodItemRecord(
            food = foodItemFromJson(json.getJSONObject("food")),
            sync = sync,
        )
        SyncEntityType.BarcodeAlias -> barcodeAliasRecordPayloadFromJson(json.getJSONObject("alias").toString(), sync)
        SyncEntityType.DiaryEntry -> DiaryEntryRecord(
            entry = diaryEntryFromJsonString(json.getJSONObject("entry").toString()),
            sync = sync,
        )
    }
}

private fun syncToJson(sync: SyncMetadata): JSONObject =
    JSONObject()
        .put("recordId", sync.recordId)
        .put("version", sync.version)
        .put("updatedAt", sync.updatedAt)
        .put("deletedAt", sync.deletedAt)
        .put("originDeviceId", sync.originDeviceId)
        .put("lastSyncedAt", sync.lastSyncedAt)
        .put("syncStatus", sync.syncStatus.name)

private fun syncFromJson(json: JSONObject): SyncMetadata =
    SyncMetadata(
        recordId = json.getString("recordId"),
        version = json.getLong("version"),
        updatedAt = json.getString("updatedAt"),
        deletedAt = json.optString("deletedAt").takeIf { it.isNotBlank() },
        originDeviceId = json.getString("originDeviceId"),
        lastSyncedAt = json.optString("lastSyncedAt").takeIf { it.isNotBlank() },
        syncStatus = SyncStatus.valueOf(json.getString("syncStatus")),
    )

private fun FoodItem.toJson(): JSONObject {
    val componentsJson = JSONArray()
    components.forEach { componentsJson.put(it.toJson()) }
    return JSONObject()
        .put("id", id)
        .put("kind", kind.name)
        .put("name", name)
        .put("brand", brand)
        .put("barcode", barcode)
        .put("servingQuantity", servingQuantity)
        .put("servingUnit", servingUnit)
        .put("nutrients", nutrients.toJson())
        .put("components", componentsJson)
        .put("frequency", frequency)
        .put("breakfastFrequency", breakfastFrequency)
        .put("lunchFrequency", lunchFrequency)
        .put("dinnerFrequency", dinnerFrequency)
        .put("snackFrequency", snackFrequency)
        .put("lastUsedAt", lastUsedAt)
        .put("lastUsedDaysAgo", lastUsedDaysAgo)
        .put("isUserCreated", isUserCreated)
}

private fun RecipeComponent.toJson(): JSONObject =
    JSONObject()
        .put("item", item.toJson())
        .put("amount", amount)
        .put("unit", unit)

private fun Nutrients.toJson(): JSONObject =
    JSONObject()
        .put("calories", calories)
        .put("proteinGrams", proteinGrams)
        .put("carbohydrateGrams", carbohydrateGrams)
        .put("fatGrams", fatGrams)

private fun foodItemFromJson(json: JSONObject): FoodItem {
    val componentsJson = json.optJSONArray("components") ?: JSONArray()
    return FoodItem(
        id = json.getString("id"),
        kind = FoodKind.valueOf(json.getString("kind")),
        name = json.getString("name"),
        brand = json.optString("brand"),
        barcode = json.optString("barcode"),
        servingQuantity = json.getDouble("servingQuantity"),
        servingUnit = json.getString("servingUnit"),
        nutrients = nutrientsFromJson(json.getJSONObject("nutrients")),
        components = List(componentsJson.length()) { index -> recipeComponentFromJson(componentsJson.getJSONObject(index)) },
        frequency = json.optInt("frequency"),
        breakfastFrequency = json.optInt("breakfastFrequency"),
        lunchFrequency = json.optInt("lunchFrequency"),
        dinnerFrequency = json.optInt("dinnerFrequency"),
        snackFrequency = json.optInt("snackFrequency"),
        lastUsedAt = json.optString("lastUsedAt").takeIf { it.isNotBlank() },
        lastUsedDaysAgo = json.optInt("lastUsedDaysAgo"),
        isUserCreated = json.optBoolean("isUserCreated", true),
    )
}

private fun recipeComponentFromJson(json: JSONObject): RecipeComponent =
    RecipeComponent(
        item = foodItemFromJson(json.getJSONObject("item")),
        amount = json.getDouble("amount"),
        unit = json.getString("unit"),
    )

private fun nutrientsFromJson(json: JSONObject): Nutrients =
    Nutrients(
        calories = json.getDouble("calories"),
        proteinGrams = json.optDouble("proteinGrams"),
        carbohydrateGrams = json.optDouble("carbohydrateGrams"),
        fatGrams = json.optDouble("fatGrams"),
    )
