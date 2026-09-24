package com.matchpoint.app.data

import android.content.Context
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.ui.theme.CourtTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small key-value store for match defaults — ported from the iOS app's @AppStorage settings. */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("match_point_prefs", Context.MODE_PRIVATE)

    private val _defaultMatchType = MutableStateFlow(readMatchType())
    val defaultMatchType: StateFlow<MatchType> = _defaultMatchType

    private val _defaultDeuceAdvantageEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEUCE, false))
    val defaultDeuceAdvantageEnabled: StateFlow<Boolean> = _defaultDeuceAdvantageEnabled

    private val _announcerEnabled = MutableStateFlow(prefs.getBoolean(KEY_ANNOUNCER, true))
    val announcerEnabled: StateFlow<Boolean> = _announcerEnabled

    private val _courtTheme = MutableStateFlow(readCourtTheme())
    val courtTheme: StateFlow<CourtTheme> = _courtTheme

    fun setCourtTheme(theme: CourtTheme) {
        prefs.edit().putString(KEY_COURT_THEME, theme.name).apply()
        _courtTheme.value = theme
    }

    private fun readCourtTheme(): CourtTheme =
        prefs.getString(KEY_COURT_THEME, null)?.let { runCatching { CourtTheme.valueOf(it) }.getOrNull() }
            ?: CourtTheme.ROLAND_GARROS

    fun setDefaultMatchType(type: MatchType) {
        prefs.edit().putString(KEY_MATCH_TYPE, type.name).apply()
        _defaultMatchType.value = type
    }

    fun setDefaultDeuceAdvantageEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEUCE, enabled).apply()
        _defaultDeuceAdvantageEnabled.value = enabled
    }

    fun setAnnouncerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANNOUNCER, enabled).apply()
        _announcerEnabled.value = enabled
    }

    private fun readMatchType(): MatchType =
        prefs.getString(KEY_MATCH_TYPE, null)?.let { runCatching { MatchType.valueOf(it) }.getOrNull() }
            ?: MatchType.DOUBLES

    companion object {
        private const val KEY_MATCH_TYPE = "default_match_type"
        private const val KEY_DEUCE = "default_deuce_advantage_enabled"
        private const val KEY_ANNOUNCER = "announcer_enabled"
        private const val KEY_COURT_THEME = "selected_theme"
    }
}
