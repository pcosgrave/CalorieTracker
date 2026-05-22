package com.philipcosgrave.calorietracker.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.syncPreferencesDataStore by preferencesDataStore(name = "sync_preferences")

object SyncPreferencesKeys {
    val DeviceId: Preferences.Key<String> = stringPreferencesKey("device_id")
    val CurrentUserId: Preferences.Key<String> = stringPreferencesKey("current_user_id")
    val SyncEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("sync_enabled")
    val BackupMode: Preferences.Key<String> = stringPreferencesKey("backup_mode")
    val ApiBaseUrl: Preferences.Key<String> = stringPreferencesKey("api_base_url")
    val LastSuccessfulSyncAt: Preferences.Key<String> = stringPreferencesKey("last_successful_sync_at")
    val LastPulledAt: Preferences.Key<String> = stringPreferencesKey("last_pulled_at")
    val LastAcknowledgedChangeId: Preferences.Key<String> = stringPreferencesKey("last_acknowledged_change_id")
    val HiddenSeedIds: Preferences.Key<Set<String>> = stringSetPreferencesKey("hidden_seed_ids")
    val LegacyMigrationComplete: Preferences.Key<Boolean> = booleanPreferencesKey("legacy_migration_complete")
    val AuthAccessToken: Preferences.Key<String> = stringPreferencesKey("auth_access_token")
    val AuthIdToken: Preferences.Key<String> = stringPreferencesKey("auth_id_token")
    val AuthRefreshToken: Preferences.Key<String> = stringPreferencesKey("auth_refresh_token")
    val AuthUserSub: Preferences.Key<String> = stringPreferencesKey("auth_user_sub")
    val AuthUserEmail: Preferences.Key<String> = stringPreferencesKey("auth_user_email")
    val AuthUserName: Preferences.Key<String> = stringPreferencesKey("auth_user_name")
    val AuthAccessTokenExpiresAt: Preferences.Key<String> = stringPreferencesKey("auth_access_token_expires_at")
    val AuthPkceVerifier: Preferences.Key<String> = stringPreferencesKey("auth_pkce_verifier")
    val AuthExpectedState: Preferences.Key<String> = stringPreferencesKey("auth_expected_state")
}
