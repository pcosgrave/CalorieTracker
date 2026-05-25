package com.philipcosgrave.calorietracker.data.auth

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.datastore.preferences.core.edit
import com.philipcosgrave.calorietracker.BuildConfig
import com.philipcosgrave.calorietracker.data.local.SyncPreferencesKeys
import com.philipcosgrave.calorietracker.data.local.syncPreferencesDataStore
import com.philipcosgrave.calorietracker.data.repository.AuthRepository
import com.philipcosgrave.calorietracker.model.AuthSession
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant

private const val GUEST_USER_ID = "guest"

class CognitoAuthRepository(private val context: Context) : AuthRepository {
    override suspend fun currentSession(): AuthSession? {
        val prefs = context.syncPreferencesDataStore.data.first()
        val userSub = prefs[SyncPreferencesKeys.AuthUserSub] ?: return null
        val accessToken = prefs[SyncPreferencesKeys.AuthAccessToken] ?: return null
        val idToken = prefs[SyncPreferencesKeys.AuthIdToken] ?: return null
        val expiresAt = prefs[SyncPreferencesKeys.AuthAccessTokenExpiresAt]?.toLongOrNull() ?: 0L
        return AuthSession(
            userSub = userSub,
            accessToken = accessToken,
            idToken = idToken,
            refreshToken = prefs[SyncPreferencesKeys.AuthRefreshToken],
            email = prefs[SyncPreferencesKeys.AuthUserEmail],
            name = prefs[SyncPreferencesKeys.AuthUserName],
            expiresAtEpochMs = expiresAt,
        )
    }

    override suspend fun currentOwnerUserId(): String = currentSession()?.userSub ?: GUEST_USER_ID

    override suspend fun beginSignIn(returnToPath: String, provider: String?): Uri {
        val verifier = randomBase64Url(32)
        val state = randomBase64Url(24)
        context.syncPreferencesDataStore.edit { prefs ->
            prefs[SyncPreferencesKeys.AuthPkceVerifier] = verifier
            prefs[SyncPreferencesKeys.AuthExpectedState] = state
        }

        return Uri.parse("https://${BuildConfig.COGNITO_DOMAIN}.auth.${BuildConfig.AWS_REGION}.amazoncognito.com/oauth2/authorize")
            .buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", BuildConfig.COGNITO_ANDROID_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.COGNITO_ANDROID_REDIRECT_URI)
            .appendQueryParameter("scope", "openid email profile")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", sha256Base64Url(verifier))
            .appendQueryParameter("state", state)
            .appendQueryParameter("returnTo", returnToPath)
            .apply {
                if (!provider.isNullOrBlank()) {
                    appendQueryParameter("identity_provider", provider)
                }
            }
            .build()
    }

    override suspend fun completeSignIn(callbackUri: Uri): AuthSession {
        val code = callbackUri.getQueryParameter("code") ?: error("Missing auth code")
        val state = callbackUri.getQueryParameter("state") ?: error("Missing auth state")
        val prefs = context.syncPreferencesDataStore.data.first()
        val verifier = prefs[SyncPreferencesKeys.AuthPkceVerifier] ?: error("Missing PKCE verifier")
        val expectedState = prefs[SyncPreferencesKeys.AuthExpectedState] ?: error("Missing expected auth state")
        require(state == expectedState) { "Auth state mismatch" }

        val response = postForm(
            "https://${BuildConfig.COGNITO_DOMAIN}.auth.${BuildConfig.AWS_REGION}.amazoncognito.com/oauth2/token",
            mapOf(
                "grant_type" to "authorization_code",
                "client_id" to BuildConfig.COGNITO_ANDROID_CLIENT_ID,
                "code" to code,
                "code_verifier" to verifier,
                "redirect_uri" to BuildConfig.COGNITO_ANDROID_REDIRECT_URI,
            ),
        )

        val accessToken = response.getString("access_token")
        val idToken = response.getString("id_token")
        val refreshToken = response.optString("refresh_token").takeIf { it.isNotBlank() }
        val expiresAt = System.currentTimeMillis() + response.getLong("expires_in") * 1000
        val idPayload = decodeJwtPayload(idToken)
        val session = AuthSession(
            userSub = idPayload.getString("sub"),
            accessToken = accessToken,
            idToken = idToken,
            refreshToken = refreshToken,
            email = idPayload.optString("email").takeIf { it.isNotBlank() },
            name = idPayload.optString("name").takeIf { it.isNotBlank() },
            expiresAtEpochMs = expiresAt,
        )
        saveSession(session)
        return session
    }

    override suspend fun signOut(): Uri {
        context.syncPreferencesDataStore.edit { prefs ->
            prefs.remove(SyncPreferencesKeys.AuthAccessToken)
            prefs.remove(SyncPreferencesKeys.AuthIdToken)
            prefs.remove(SyncPreferencesKeys.AuthRefreshToken)
            prefs.remove(SyncPreferencesKeys.AuthUserSub)
            prefs.remove(SyncPreferencesKeys.AuthUserEmail)
            prefs.remove(SyncPreferencesKeys.AuthUserName)
            prefs.remove(SyncPreferencesKeys.AuthAccessTokenExpiresAt)
            prefs.remove(SyncPreferencesKeys.AuthPkceVerifier)
            prefs.remove(SyncPreferencesKeys.AuthExpectedState)
            prefs[SyncPreferencesKeys.CurrentUserId] = GUEST_USER_ID
        }

        return Uri.parse("https://${BuildConfig.COGNITO_DOMAIN}.auth.${BuildConfig.AWS_REGION}.amazoncognito.com/logout")
            .buildUpon()
            .appendQueryParameter("client_id", BuildConfig.COGNITO_ANDROID_CLIENT_ID)
            .appendQueryParameter("logout_uri", BuildConfig.COGNITO_ANDROID_LOGOUT_URI)
            .build()
    }

    override suspend fun refreshSessionIfNeeded(): AuthSession? {
        val current = currentSession() ?: return null
        if (current.expiresAtEpochMs > System.currentTimeMillis() + 30_000) {
            return current
        }

        val refreshToken = current.refreshToken ?: return null
        val response = postForm(
            "https://${BuildConfig.COGNITO_DOMAIN}.auth.${BuildConfig.AWS_REGION}.amazoncognito.com/oauth2/token",
            mapOf(
                "grant_type" to "refresh_token",
                "client_id" to BuildConfig.COGNITO_ANDROID_CLIENT_ID,
                "refresh_token" to refreshToken,
            ),
        )
        val accessToken = response.getString("access_token")
        val nextIdToken = response.optString("id_token").takeIf { it.isNotBlank() } ?: current.idToken
        val idPayload = decodeJwtPayload(nextIdToken)
        val refreshed = current.copy(
            userSub = idPayload.getString("sub"),
            accessToken = accessToken,
            idToken = nextIdToken,
            email = idPayload.optString("email").takeIf { it.isNotBlank() } ?: current.email,
            name = idPayload.optString("name").takeIf { it.isNotBlank() } ?: current.name,
            expiresAtEpochMs = System.currentTimeMillis() + response.getLong("expires_in") * 1000,
        )
        saveSession(refreshed)
        return refreshed
    }

    private suspend fun saveSession(session: AuthSession) {
        context.syncPreferencesDataStore.edit { prefs ->
            prefs[SyncPreferencesKeys.AuthAccessToken] = session.accessToken
            prefs[SyncPreferencesKeys.AuthIdToken] = session.idToken
            session.refreshToken?.let { prefs[SyncPreferencesKeys.AuthRefreshToken] = it }
            prefs[SyncPreferencesKeys.AuthUserSub] = session.userSub
            prefs[SyncPreferencesKeys.CurrentUserId] = session.userSub
            session.email?.let { prefs[SyncPreferencesKeys.AuthUserEmail] = it }
            session.name?.let { prefs[SyncPreferencesKeys.AuthUserName] = it }
            prefs[SyncPreferencesKeys.AuthAccessTokenExpiresAt] = session.expiresAtEpochMs.toString()
            prefs.remove(SyncPreferencesKeys.AuthPkceVerifier)
            prefs.remove(SyncPreferencesKeys.AuthExpectedState)
        }
    }

    private fun postForm(url: String, params: Map<String, String>): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.doOutput = true
        val body = params.entries.joinToString("&") { "${Uri.encode(it.key)}=${Uri.encode(it.value)}" }
        connection.outputStream.use { output ->
            output.write(body.toByteArray())
        }
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(responseText)
    }
}

private fun randomBase64Url(byteCount: Int): String {
    val bytes = ByteArray(byteCount)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
}

private fun sha256Base64Url(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
}

private fun decodeJwtPayload(token: String): JSONObject {
    val parts = token.split(".")
    val payload = parts.getOrNull(1) ?: error("Invalid JWT")
    val normalized = payload
        .replace('-', '+')
        .replace('_', '/')
        .let { candidate ->
            val remainder = candidate.length % 4
            if (remainder == 0) candidate else candidate + "=".repeat(4 - remainder)
        }
    val json = String(Base64.decode(normalized, Base64.DEFAULT))
    return JSONObject(json)
}
