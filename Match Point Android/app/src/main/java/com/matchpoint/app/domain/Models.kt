package com.matchpoint.app.domain

import java.util.UUID

/** Pure, DB-agnostic domain types. No Room/Android imports in this package. */

data class PlayerRef(
    val id: UUID,
    val name: String
)

data class MatchConfig(
    val type: MatchType,
    val sideAPlayers: List<PlayerRef>,
    val sideBPlayers: List<PlayerRef>,
    val firstToGames: Int,
    val deuceAdvantageEnabled: Boolean,
    val startingServerPlayerId: UUID?,
    val manualServerOverridePlayerId: UUID? = null,
    val manualServerOverrideGameIndex: Int? = null
)

sealed class MatchOutcome {
    data object InProgress : MatchOutcome()
    data class Completed(val winner: Side) : MatchOutcome()
}

data class GameDisplay(
    val pointsA: PointDisplay,
    val pointsB: PointDisplay,
    val isDeuce: Boolean,
    val advantageSide: Side?
)

data class MatchState(
    val gamesA: Int,
    val gamesB: Int,
    val pointsA: Int,
    val pointsB: Int,
    val currentServerPlayerId: UUID?,
    val gameDisplay: GameDisplay,
    val outcome: MatchOutcome
) {
    val finalScoreA: Int get() = gamesA
    val finalScoreB: Int get() = gamesB
}

data class EligiblePlayer(
    val player: PlayerRef,
    val matchesPlayed: Int,
    val lastMatchEndedAt: Long?
)

/** Session-scoped history of who has already partnered/opposed whom, for fair matchmaking. */
data class PairingHistory(
    val partnerPairs: Set<Set<UUID>>,
    val opponentPairs: Set<Set<UUID>>
)

data class GeneratedProposal(
    val type: MatchType,
    val sideA: List<PlayerRef>,
    val sideB: List<PlayerRef>
)
