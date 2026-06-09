package com.philipcosgrave.calorietracker.data.repository

import com.philipcosgrave.calorietracker.BuildConfig
import com.philipcosgrave.calorietracker.data.local.aiFoodLogResponseFromJsonString
import com.philipcosgrave.calorietracker.model.AiFoodLogResponse
import com.philipcosgrave.calorietracker.model.Meal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

class ApiAiFoodLogRepository(
    private val localStore: AndroidLocalStore,
) : AiFoodLogRepository {
    override suspend fun parseFoodLog(
        transcript: String,
        date: LocalDate,
        fallbackMeal: Meal,
    ): AiFoodLogResponse = withContext(Dispatchers.IO) {
        val session = localStore.authRepository.refreshSessionIfNeeded()
            ?: error("You must sign in before using AI Log.")

        val body = JSONObject()
            .put("transcript", transcript)
            .put("date", date.toString())
            .put("fallbackMeal", fallbackMeal.name)
            .toString()
            .toByteArray()

        val url = "${BuildConfig.SYNC_API_BASE_URL.trimEnd('/')}/ai/parse-food-log"
        val attempts = listOf(session.idToken, session.accessToken).distinct()
        var lastFailure: String? = null

        for (token in attempts) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", token)
            connection.doOutput = true
            connection.outputStream.use { output ->
                output.write(body)
            }

            val responseText = readResponseText(connection)
            if (connection.responseCode in 200..299) {
                return@withContext aiFoodLogResponseFromJsonString(responseText)
            }

            lastFailure = "status ${connection.responseCode}: $responseText"
            if (connection.responseCode != 401 && connection.responseCode != 403) {
                break
            }
        }

        error("AI Log request failed with ${lastFailure ?: "an unknown authorization error"}")
    }
}

private fun readResponseText(connection: HttpURLConnection): String {
    val stream =
        if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

    if (stream == null) {
        return ""
    }

    return stream.bufferedReader().use { it.readText() }
}
