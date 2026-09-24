package com.matchpoint.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.matchpoint.app.R

/**
 * The two selectable court palettes, ported 1:1 from iOS's `AppTheme` enum
 * (`DesignSystem/Theme.swift`). Every themed drawable in `res/drawable-nodpi`
 * follows the Android-side convention of "base name = Roland Garros,
 * `_wimbledon` suffix = Wimbledon" regardless of which way iOS's own asset
 * catalog names things (iOS inverts this for a couple of assets).
 */
enum class CourtTheme(val displayName: String, val subtitle: String) {
    ROLAND_GARROS("Roland Garros", "burnt clay orange"),
    WIMBLEDON("Wimbledon", "deep court green");

    val backgroundTop: Color
        get() = when (this) {
            ROLAND_GARROS -> Color(0xFF852F21)
            WIMBLEDON -> Color(0xFF0B2116)
        }

    val backgroundBottom: Color
        get() = when (this) {
            ROLAND_GARROS -> Color(0xFF471D12)
            WIMBLEDON -> Color(0xFF061409)
        }

    val accent: Color
        get() = when (this) {
            ROLAND_GARROS -> Color(0xFFF9D436)
            WIMBLEDON -> Color(0xFFD4F63E)
        }

    val accentText: Color
        get() = when (this) {
            ROLAND_GARROS -> Color(0xFF35160E)
            WIMBLEDON -> Color(0xFF0B2116)
        }

    val cardFill: Color
        get() = when (this) {
            ROLAND_GARROS -> Color.White.copy(alpha = 0.08f)
            WIMBLEDON -> Color.White.copy(alpha = 0.07f)
        }

    val cardBorder: Color
        get() = when (this) {
            ROLAND_GARROS -> Color.White.copy(alpha = 0.16f)
            WIMBLEDON -> Color.White.copy(alpha = 0.14f)
        }

    val textPrimary: Color
        get() = when (this) {
            ROLAND_GARROS -> Color(0xFFFDF5EF)
            WIMBLEDON -> Color(0xFFF5FAF2)
        }

    val textSecondary: Color
        get() = when (this) {
            ROLAND_GARROS -> Color.White.copy(alpha = 0.66f)
            WIMBLEDON -> Color.White.copy(alpha = 0.62f)
        }

    val danger: Color
        get() = when (this) {
            ROLAND_GARROS -> Color(0xFFDE3D3D)
            WIMBLEDON -> Color(0xFFE3702E)
        }

    val fieldFill: Color get() = textPrimary
    val fieldText: Color get() = accentText

    private fun res(base: Int, wimbledon: Int): Int = if (this == ROLAND_GARROS) base else wimbledon

    val logoImage: Int get() = res(R.drawable.match_point_logo, R.drawable.match_point_logo_wimbledon)
    val homeMabarIllustration: Int get() = res(R.drawable.home_mabar_illustration, R.drawable.home_mabar_illustration_wimbledon)
    val matchesEmptyIllustration: Int get() = res(R.drawable.matches_empty_illustration, R.drawable.matches_empty_illustration_wimbledon)
    val playersEmptyIllustration: Int get() = res(R.drawable.players_empty_illustration, R.drawable.players_empty_illustration_wimbledon)
    val scoreboardEmptyIllustration: Int get() = res(R.drawable.scoreboard_empty_illustration, R.drawable.scoreboard_empty_illustration_wimbledon)
    val matchCompleteIllustration: Int get() = res(R.drawable.match_complete_illustration, R.drawable.match_complete_illustration_wimbledon)
    val spinningTennisBall: Int get() = res(R.drawable.tennis_ball_3d, R.drawable.tennis_ball_3d_wimbledon)
    val pastMabarBadge: Int get() = res(R.drawable.past_mabar_badge, R.drawable.past_mabar_badge_wimbledon)

    val tabHomeActive: Int get() = res(R.drawable.tab_home_active, R.drawable.tab_home_active_wimbledon)
    val tabHomeInactive: Int get() = res(R.drawable.tab_home_inactive, R.drawable.tab_home_inactive_wimbledon)
    val tabMatchesActive: Int get() = res(R.drawable.tab_matches_active, R.drawable.tab_matches_active_wimbledon)
    val tabMatchesInactive: Int get() = res(R.drawable.tab_matches_inactive, R.drawable.tab_matches_inactive_wimbledon)
    val tabPlayersActive: Int get() = res(R.drawable.tab_players_active, R.drawable.tab_players_active_wimbledon)
    val tabPlayersInactive: Int get() = res(R.drawable.tab_players_inactive, R.drawable.tab_players_inactive_wimbledon)
    val tabSettingsActive: Int get() = res(R.drawable.tab_settings_active, R.drawable.tab_settings_active_wimbledon)
    val tabSettingsInactive: Int get() = res(R.drawable.tab_settings_inactive, R.drawable.tab_settings_inactive_wimbledon)
}
