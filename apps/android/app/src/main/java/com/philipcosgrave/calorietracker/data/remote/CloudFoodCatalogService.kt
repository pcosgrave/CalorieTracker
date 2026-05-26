package com.philipcosgrave.calorietracker.data.remote

import com.philipcosgrave.calorietracker.BuildConfig
import com.philipcosgrave.calorietracker.data.repository.AndroidLocalStore
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CloudFoodCatalogService(
    private val localStore: AndroidLocalStore,
) {
    suspend fun lookupPersonalBarcode(barcode: String): FoodItem? =
        lookup("${baseUrl()}/foods/lookup/$barcode")

    suspend fun lookupCommunityBarcode(barcode: String): FoodItem? =
        lookup("${baseUrl()}/foods/community/lookup/$barcode")

    suspend fun searchPersonalFoods(query: String): List<FoodItem> =
        search("${baseUrl()}/foods/search?query=${encode(query)}")

    suspend fun searchCommunityFoods(query: String): List<FoodItem> =
        search("${baseUrl()}/foods/community/search?query=${encode(query)}")

    suspend fun publishCommunityFood(item: FoodItem): Boolean {
        val body = JSONObject()
            .put("productId", item.id)
            .put("barcode", item.barcode.ifBlank { null })
            .put("name", item.name)
            .put("brand", item.brand.ifBlank { null })
            .put(
                "serving",
                JSONObject()
                    .put("label", item.servingUnit)
                    .put("quantity", item.servingQuantity)
                    .put("unit", item.servingUnit),
            )
            .put(
                "nutrients",
                JSONObject()
                    .put("calories", item.nutrients.calories)
                    .put("proteinGrams", item.nutrients.proteinGrams)
                    .put("carbohydrateGrams", item.nutrients.carbohydrateGrams)
                    .put("fatGrams", item.nutrients.fatGrams),
            )

        val response = authorizedRequest("${baseUrl()}/foods/community", "POST", body)
        return response.optBoolean("existed", false)
    }

    private suspend fun lookup(url: String): FoodItem? {
        val response = authorizedRequest(url, "GET")
        val found = response.optBoolean("found", false)
        return if (found) response.optJSONObject("product")?.let(::foodItemFromJson) else null
    }

    private suspend fun search(url: String): List<FoodItem> {
        val response = authorizedRequest(url, "GET")
        val products = response.optJSONArray("products") ?: JSONArray()
        return List(products.length()) { index ->
            products.optJSONObject(index)
        }.filterNotNull().map(::foodItemFromJson)
    }

    private suspend fun authorizedRequest(url: String, method: String, body: JSONObject? = null): JSONObject {
        val session = localStore.authRepository.refreshSessionIfNeeded()
            ?: error("You must sign in to search saved and community foods.")

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${session.idToken}")

        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray())
            }
        }

        val inputStream =
            if (connection.responseCode in 200..299) connection.inputStream
            else connection.errorStream
        val responseText = inputStream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            error("Cloud catalog request failed with status ${connection.responseCode}: $responseText")
        }

        return JSONObject(responseText)
    }

    private fun foodItemFromJson(json: JSONObject): FoodItem {
        val serving = json.optJSONObject("serving") ?: JSONObject()
        val nutrients = json.optJSONObject("nutrients") ?: JSONObject()
        return FoodItem(
            id = json.optString("productId", json.optString("id")),
            kind = FoodKind.Ingredient,
            name = json.getString("name"),
            brand = json.optString("brand"),
            barcode = json.optString("barcode"),
            servingQuantity = serving.optDouble("quantity").takeUnless { it.isNaN() || it <= 0.0 } ?: 1.0,
            servingUnit = serving.optString("unit").ifBlank { serving.optString("label", "serving") },
            nutrients = Nutrients(
                calories = nutrients.optDouble("calories").takeUnless { it.isNaN() } ?: 0.0,
                proteinGrams = nutrients.optDouble("proteinGrams").takeUnless { it.isNaN() } ?: 0.0,
                carbohydrateGrams = nutrients.optDouble("carbohydrateGrams").takeUnless { it.isNaN() } ?: 0.0,
                fatGrams = nutrients.optDouble("fatGrams").takeUnless { it.isNaN() } ?: 0.0,
            ),
            isUserCreated = false,
        )
    }

    private fun baseUrl(): String = BuildConfig.SYNC_API_BASE_URL.trimEnd('/')

    private fun encode(value: String): String = java.net.URLEncoder.encode(value.trim(), Charsets.UTF_8.name())
}
