package com.matchpoint.app.backend

import android.content.Context
import android.util.Log
import com.matchpoint.app.BuildConfig
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Owns this installation's local-only identity — a random UUID generated once and cached in
 * SharedPreferences, deliberately independent of any hardware identifier (ANDROID_ID, serial,
 * IMEI, MAC) so a reinstall always gets a fresh one — plus writing/refreshing its row in
 * Supabase's `installations` table. Never touches match/session/player data.
 */
class InstallationTracker(context: Context, private val backend: SupabaseBackend) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val installationId: String = prefs.getString(KEY_INSTALLATION_ID, null)
        ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_INSTALLATION_ID, it).apply() }

    /** Upsert (not insert) so a retry after a partial-registration failure never trips a
     * duplicate-key error on a row a previous attempt already wrote. */
    suspend fun recordInstallation(userId: String): Boolean {
        return try {
            backend.client.postgrest.from("installations").upsert(
                InstallationUpsert(
                    id = installationId,
                    userId = userId,
                    appVersion = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE
                )
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record installation", e)
            false
        }
    }

    /** Refreshes last_seen_at + current app version on this installation's existing row.
     * Called once per app start (see MainActivity), never per-screen. */
    suspend fun touchInstallation() {
        try {
            backend.client.postgrest.from("installations").update(
                InstallationTouch(
                    lastSeenAt = Instant.now().toString(),
                    appVersion = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE
                )
            ) {
                filter { eq("id", installationId) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update installation last_seen_at", e)
        }
    }

    companion object {
        private const val TAG = "InstallationTracker"
        private const val PREFS_NAME = "installation_tracker"
        private const val KEY_INSTALLATION_ID = "installation_id"
    }
}

@Serializable
private data class InstallationUpsert(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("version_code") val versionCode: Int
)

@Serializable
private data class InstallationTouch(
    @SerialName("last_seen_at") val lastSeenAt: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("version_code") val versionCode: Int
)
