package com.matchpoint.app.ui.navigation

import java.util.UUID

/** Route constants + arg builders. Mirrors the iOS app's MatchRoute enum + 4-tab structure. */
object Routes {
    const val HOME = "home"
    const val MATCHES = "matches"
    const val PLAYERS = "players"
    const val SETTINGS = "settings"

    const val LIVE_MATCH = "live/{matchId}"
    const val MATCH_COMPLETE = "complete/{matchId}"
    const val SELECT_PLAYERS = "selectPlayers/{matchId}/{side}"
    const val SESSION_HISTORY = "sessionHistory/{sessionId}"
    const val MATCH_DETAIL = "matchDetail/{matchId}"
    const val RECORD_MATCH = "recordMatch/{sessionId}/{matchId}"
    const val PLAYER_PROFILE = "playerProfile/{playerId}"
    const val MATCH_SETTINGS = "matchSettings/{matchId}"
    const val SESSION_ROSTER = "sessionRoster/{sessionId}"

    val bottomTabs = listOf(HOME, MATCHES, PLAYERS, SETTINGS)

    const val RECORD_MATCH_NEW = "new"

    fun liveMatch(matchId: UUID) = "live/$matchId"
    fun matchComplete(matchId: UUID) = "complete/$matchId"
    fun selectPlayers(matchId: UUID, side: String = "ANY") = "selectPlayers/$matchId/$side"
    fun sessionHistory(sessionId: UUID) = "sessionHistory/$sessionId"
    fun matchDetail(matchId: UUID) = "matchDetail/$matchId"
    fun recordMatch(sessionId: UUID, matchId: UUID? = null) = "recordMatch/$sessionId/${matchId ?: RECORD_MATCH_NEW}"
    fun playerProfile(playerId: UUID) = "playerProfile/$playerId"
    fun matchSettings(matchId: UUID) = "matchSettings/$matchId"
    fun sessionRoster(sessionId: UUID) = "sessionRoster/$sessionId"
}
