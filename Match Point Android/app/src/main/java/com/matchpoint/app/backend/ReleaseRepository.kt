package com.matchpoint.app.backend

import android.util.Log
import com.matchpoint.app.BuildConfig
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Checks `public.releases` for a newer build than the one installed. Non-critical by design:
 * a failed check just leaves [updateState] at [UpdateState.Error] and never blocks the rest of
 * the app — Match Point is local-first and must keep working offline regardless of this result.
 *
 * [checkForUpdateOnce] is guarded to run at most once per process — callers can call it from
 * every app-foreground event without triggering duplicate requests.
 */
class ReleaseRepository(private val backend: SupabaseBackend) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Checking)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var hasChecked = false

    suspend fun checkForUpdateOnce() {
        if (hasChecked) return
        hasChecked = true
        _updateState.value = try {
            val latest = backend.client.postgrest.from("releases")
                .select(
                    columns = Columns.list(
                        "version_name", "version_code", "apk_url", "release_notes", "is_mandatory"
                    )
                ) {
                    order("version_code", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<ReleaseInfo>()
                .firstOrNull()
            evaluateUpdateState(latest, BuildConfig.VERSION_CODE)
        } catch (e: Exception) {
            Log.w(TAG, "Release check failed", e)
            UpdateState.Error
        }
    }

    companion object {
        private const val TAG = "ReleaseRepository"
    }
}
