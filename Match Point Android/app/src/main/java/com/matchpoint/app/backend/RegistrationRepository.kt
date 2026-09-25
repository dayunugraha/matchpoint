package com.matchpoint.app.backend

import android.content.Context
import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Owns Match Point's one-time registration flow and the local flag that gates it: resume (or
 * create) an anonymous Supabase identity, write the `users` row from the name/optional email
 * the user enters, then hand off to [InstallationTracker] for the `installations` row.
 *
 * Registration status is cached locally so it's never re-shown once it succeeds — see
 * [isRegistered]. Match/session/player data never passes through here; this only ever writes
 * to `users`/`installations`, never Room.
 */
class RegistrationRepository(
    context: Context,
    private val backend: SupabaseBackend,
    private val installationTracker: InstallationTracker
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isRegistered = MutableStateFlow(prefs.getBoolean(KEY_COMPLETED, false))
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    /** Resumes/creates the anonymous session. Returns the Supabase user id, or null if
     * authentication itself failed (registration can't proceed without one). */
    suspend fun ensureAuthenticated(): String? = backend.ensureAuthenticated()

    /**
     * Writes the `users` row (upsert, so a retry after a partial failure is safe) and then
     * the `installations` row. Only flips [isRegistered] once both succeed — a failure leaves
     * local state untouched so the registration screen stays up and can be retried.
     */
    suspend fun register(userId: String, name: String, email: String?): Boolean {
        return try {
            backend.client.postgrest.from("users").upsert(UserUpsert(id = userId, name = name, email = email))
            val installed = installationTracker.recordInstallation(userId)
            if (installed) {
                prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
                _isRegistered.value = true
            }
            installed
        } catch (e: Exception) {
            Log.w(TAG, "Registration failed", e)
            false
        }
    }

    /** Called once per app start when already registered (see MainActivity) — confirms the
     * session is still valid and refreshes this installation's last_seen_at/app version. Not
     * meant to be called per-screen. */
    suspend fun ensureSessionAndTouchInstallation() {
        if (ensureAuthenticated() != null) installationTracker.touchInstallation()
    }

    companion object {
        private const val TAG = "RegistrationRepository"
        private const val PREFS_NAME = "registration_prefs"
        private const val KEY_COMPLETED = "registration_completed"
    }
}

@Serializable
private data class UserUpsert(
    val id: String,
    val name: String,
    val email: String? = null
)
