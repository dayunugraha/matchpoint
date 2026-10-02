package com.matchpoint.app.domain

import java.util.UUID

/**
 * Pure scoring engine. `state()` is the single source of truth: it takes the
 * entire ordered point-winner log plus config and re-derives the full match
 * state on every call — callers must never cache/store this, always recompute
 * from the event log so nothing can drift out of sync.
 */
object TennisScoringEngine {

    fun state(replaying: List<Side>, config: MatchConfig): MatchState {
        val serverOrder = serverOrder(config)

        var gamesA = 0
        var gamesB = 0
        var pointsA = 0
        var pointsB = 0
        var outcome: MatchOutcome = MatchOutcome.InProgress

        for (side in replaying) {
            if (outcome != MatchOutcome.InProgress) break

            if (side == Side.A) pointsA++ else pointsB++

            val gameWinner = gameWinner(pointsA, pointsB, config.deuceAdvantageEnabled)
            if (gameWinner != null) {
                if (gameWinner == Side.A) gamesA++ else gamesB++
                pointsA = 0
                pointsB = 0

                outcome = when {
                    gamesA >= config.firstToGames -> MatchOutcome.Completed(Side.A)
                    gamesB >= config.firstToGames -> MatchOutcome.Completed(Side.B)
                    else -> MatchOutcome.InProgress
                }
            }
        }

        val gamesCompleted = gamesA + gamesB
        val currentServer = currentServer(serverOrder, gamesCompleted, config)

        return MatchState(
            gamesA = gamesA,
            gamesB = gamesB,
            pointsA = pointsA,
            pointsB = pointsB,
            currentServerPlayerId = currentServer,
            gameDisplay = gameDisplay(pointsA, pointsB, config.deuceAdvantageEnabled),
            outcome = outcome
        )
    }

    /** Whether awarding the next point to a given score would win the game — used by
     * AudioEventMapper to detect "match point" without replaying the whole event log. */
    fun pointOutcome(pointsA: Int, pointsB: Int, deuceOn: Boolean): Side? = gameWinner(pointsA, pointsB, deuceOn)

    private fun gameWinner(pointsA: Int, pointsB: Int, deuceAdvantageEnabled: Boolean): Side? {
        return if (deuceAdvantageEnabled) {
            when {
                pointsA >= 4 && pointsA - pointsB >= 2 -> Side.A
                pointsB >= 4 && pointsB - pointsA >= 2 -> Side.B
                else -> null
            }
        } else {
            when {
                pointsA >= 4 -> Side.A
                pointsB >= 4 -> Side.B
                else -> null
            }
        }
    }

    fun gameDisplay(pointsA: Int, pointsB: Int, deuceAdvantageEnabled: Boolean): GameDisplay {
        if (pointsA < 3 || pointsB < 3) {
            return GameDisplay(
                pointsA = pointDisplayFor(pointsA),
                pointsB = pointDisplayFor(pointsB),
                isDeuce = false,
                advantageSide = null
            )
        }

        // Both sides have reached 3+ points ("deuce territory").
        if (!deuceAdvantageEnabled) {
            return GameDisplay(
                pointsA = PointDisplay.FORTY,
                pointsB = PointDisplay.FORTY,
                isDeuce = false,
                advantageSide = null
            )
        }

        return when {
            pointsA == pointsB -> GameDisplay(
                pointsA = PointDisplay.FORTY,
                pointsB = PointDisplay.FORTY,
                isDeuce = true,
                advantageSide = null
            )
            pointsA > pointsB -> GameDisplay(
                pointsA = PointDisplay.ADVANTAGE,
                pointsB = PointDisplay.FORTY,
                isDeuce = false,
                advantageSide = Side.A
            )
            else -> GameDisplay(
                pointsA = PointDisplay.FORTY,
                pointsB = PointDisplay.ADVANTAGE,
                isDeuce = false,
                advantageSide = Side.B
            )
        }
    }

    private fun pointDisplayFor(points: Int): PointDisplay = when (points) {
        0 -> PointDisplay.LOVE
        1 -> PointDisplay.FIFTEEN
        2 -> PointDisplay.THIRTY
        else -> PointDisplay.FORTY
    }

    /** Fixed cyclic serving order, computed once from the starting server + side compositions. */
    private fun serverOrder(config: MatchConfig): List<UUID> {
        val starter = config.startingServerPlayerId ?: return emptyList()
        val starterOnSideA = config.sideAPlayers.any { it.id == starter }
        val starterSide = if (starterOnSideA) config.sideAPlayers else config.sideBPlayers
        val otherSide = if (starterOnSideA) config.sideBPlayers else config.sideAPlayers

        // Defensive against malformed configs (e.g. a side momentarily short a player during an
        // edit) — state() is on the hot reactive path for every score/participant change, so it
        // must never throw; better to degrade the serve order than crash the whole app.
        return when (config.type) {
            MatchType.SINGLES -> listOfNotNull(starter, otherSide.firstOrNull()?.id)
            MatchType.DOUBLES -> listOfNotNull(
                starter,
                otherSide.getOrNull(0)?.id,
                starterSide.firstOrNull { it.id != starter }?.id,
                otherSide.getOrNull(1)?.id
            )
        }
    }

    private fun currentServer(order: List<UUID>, gamesCompleted: Int, config: MatchConfig): UUID? {
        if (config.manualServerOverridePlayerId != null &&
            config.manualServerOverrideGameIndex == gamesCompleted
        ) {
            return config.manualServerOverridePlayerId
        }
        if (order.isEmpty()) return null
        return order[gamesCompleted % order.size]
    }
}
