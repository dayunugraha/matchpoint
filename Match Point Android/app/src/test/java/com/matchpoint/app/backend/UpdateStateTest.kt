package com.matchpoint.app.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateStateTest {

    private fun release(versionCode: Int, isMandatory: Boolean = false) = ReleaseInfo(
        versionName = "irrelevant",
        versionCode = versionCode,
        apkUrl = "https://example.com/app.apk",
        releaseNotes = "notes",
        isMandatory = isMandatory
    )

    @Test
    fun `same version code is up to date`() {
        assertEquals(UpdateState.UpToDate, evaluateUpdateState(release(versionCode = 1), currentVersionCode = 1))
    }

    @Test
    fun `higher latest version code is an update`() {
        val state = evaluateUpdateState(release(versionCode = 2), currentVersionCode = 1)
        assertTrue(state is UpdateState.UpdateAvailable)
        assertEquals(2, (state as UpdateState.UpdateAvailable).versionCode)
    }

    @Test
    fun `lower latest version code is up to date`() {
        assertEquals(UpdateState.UpToDate, evaluateUpdateState(release(versionCode = 1), currentVersionCode = 2))
    }

    @Test
    fun `no release row is up to date`() {
        assertEquals(UpdateState.UpToDate, evaluateUpdateState(latest = null, currentVersionCode = 1))
    }

    @Test
    fun `mandatory release is carried through as UpdateAvailable`() {
        val state = evaluateUpdateState(release(versionCode = 2, isMandatory = true), currentVersionCode = 1)
        assertTrue(state is UpdateState.UpdateAvailable)
        assertTrue((state as UpdateState.UpdateAvailable).isMandatory)
    }

    @Test
    fun `version name never drives the comparison`() {
        // version_name deliberately "0.1" (lexically/semantically "lower") but version_code higher
        val higherCodeLowerName = ReleaseInfo(
            versionName = "0.1",
            versionCode = 2,
            apkUrl = "https://example.com/app.apk",
            releaseNotes = null,
            isMandatory = false
        )
        val state = evaluateUpdateState(higherCodeLowerName, currentVersionCode = 1)
        assertTrue(state is UpdateState.UpdateAvailable)
    }
}
