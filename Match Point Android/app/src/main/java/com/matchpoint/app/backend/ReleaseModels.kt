package com.matchpoint.app.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of `public.releases` — only the columns the update checker needs. */
@Serializable
data class ReleaseInfo(
    @SerialName("version_name") val versionName: String,
    @SerialName("version_code") val versionCode: Int,
    @SerialName("apk_url") val apkUrl: String,
    @SerialName("release_notes") val releaseNotes: String? = null,
    @SerialName("is_mandatory") val isMandatory: Boolean = false
)

/** Immutable state for [ReleaseRepository.updateState]. */
sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object Error : UpdateState
    data class UpdateAvailable(
        val versionName: String,
        val versionCode: Int,
        val apkUrl: String,
        val releaseNotes: String?,
        val isMandatory: Boolean
    ) : UpdateState
}

/**
 * Pure version comparison — version_code only, never version_name. Split out from
 * [ReleaseRepository] so it's testable without a Supabase client or Android runtime.
 */
fun evaluateUpdateState(latest: ReleaseInfo?, currentVersionCode: Int): UpdateState {
    if (latest == null || latest.versionCode <= currentVersionCode) return UpdateState.UpToDate
    return UpdateState.UpdateAvailable(
        versionName = latest.versionName,
        versionCode = latest.versionCode,
        apkUrl = latest.apkUrl,
        releaseNotes = latest.releaseNotes,
        isMandatory = latest.isMandatory
    )
}
