package com.philipcosgrave.calorietracker.data.sync

import com.philipcosgrave.calorietracker.data.repository.AndroidLocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RemoteHousehold(val householdId: String, val name: String, val role: String)
data class RemoteHouseholdMember(val memberId: String, val name: String, val role: String)

class HouseholdApiService(private val localStore: AndroidLocalStore) {
    suspend fun list(): List<RemoteHousehold> = request("GET", "/v1/households/").let(::households)
    suspend fun create(name: String): RemoteHousehold = household(request("POST", "/v1/households/", JSONObject().put("name", name)))
    suspend fun members(id: String): List<RemoteHouseholdMember> = request("GET", "/v1/households/$id/members").let(::membersFromJson)
    suspend fun leave(id: String) { request("POST", "/v1/households/$id/leave", JSONObject()) }
    suspend fun delete(id: String) { request("DELETE", "/v1/households/$id") }

    private suspend fun request(method: String, path: String, body: JSONObject? = null): String = withContext(Dispatchers.IO) {
        val settings = localStore.syncStateRepository.getSettings()
        val baseUrl = requireNotNull(settings.apiBaseUrl?.trim()?.trimEnd('/')) { "API base URL is missing" }
        val session = localStore.authRepository.refreshSessionIfNeeded() ?: error("Sign in to manage households")
        val tokens = listOf(session.idToken, session.accessToken).distinct()
        var failure = ""
        for (token in tokens) {
            val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
            connection.requestMethod = method; connection.setRequestProperty("Authorization", token)
            if (body != null) { connection.setRequestProperty("Content-Type", "application/json"); connection.doOutput = true; connection.outputStream.use { it.write(body.toString().toByteArray()) } }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode in 200..299) return@withContext text
            failure = "Household request $method $path failed (${connection.responseCode}): ${text.take(500)}"
            if (connection.responseCode !in listOf(401, 403)) break
        }
        error(failure)
    }
    private fun households(text: String): List<RemoteHousehold> { val json = JSONArray(text); return List(json.length()) { household(json.getJSONObject(it).toString()) } }
    private fun household(text: String): RemoteHousehold { val json = JSONObject(text); return RemoteHousehold(json.getString("householdId"), json.getString("name"), json.getString("role")) }
    private fun membersFromJson(text: String): List<RemoteHouseholdMember> { val json = JSONArray(text); return List(json.length()) { val item = json.getJSONObject(it); RemoteHouseholdMember(item.getString("memberId"), item.getString("name"), item.getString("role")) } }
}
