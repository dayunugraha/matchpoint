package com.matchpoint.app.backend

import android.util.Log
import com.matchpoint.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Owns the single Supabase client for the app — installation tracking, release checks, and
 * (later) user registration all go through this. This is the ONLY class that talks to
 * `io.github.jan.supabase.*`; no match/session/player data ever flows through here — that
 * stays local-first in Room via SessionRepository, same separation WearEngineManager keeps
 * for the watch remote.
 *
 * Configured from [BuildConfig.SUPABASE_URL] / [BuildConfig.SUPABASE_ANON_KEY], which Gradle
 * fills in from the gitignored supabase.properties (see supabase.properties.example) — the
 * anon/publishable key only, never a service-role key or the database password.
 */
class SupabaseBackend {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }

    /**
     * Resumes the persisted session if one exists (the Auth plugin loads/refreshes it from
     * local storage on its own — no re-sign-in needed), otherwise signs in anonymously. Used
     * both for first-launch registration and to confirm a session is still valid on later
     * launches. Never logs the key, token, or session — only the resulting user id.
     */
    suspend fun ensureAuthenticated(): String? {
        return try {
            client.auth.awaitInitialization()
            client.auth.currentUserOrNull()?.id?.let { return it }
            client.auth.signInAnonymously()
            client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.w(TAG, "Failed to ensure an authenticated session", e)
            null
        }
    }

    companion object {
        private const val TAG = "SupabaseBackend"
    }
}
