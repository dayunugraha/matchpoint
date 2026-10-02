package com.matchpoint.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class TennisScoringEngineTest {

    private fun player() = PlayerRef(UUID.randomUUID(), "p")

    @Test
    fun `doubles server order does not crash when starter's side is short a player`() {
        val starter = player()
        val config = MatchConfig(
            type = MatchType.DOUBLES,
            sideAPlayers = listOf(starter), // malformed: doubles side missing its partner
            sideBPlayers = listOf(player(), player()),
            firstToGames = 6,
            deuceAdvantageEnabled = true,
            startingServerPlayerId = starter.id
        )

        // Must not throw NoSuchElementException (regression: crashed live scoring/finish flow).
        val state = TennisScoringEngine.state(replaying = emptyList(), config = config)
        assertEquals(starter.id, state.currentServerPlayerId)
    }

    @Test
    fun `doubles server order does not crash when other side is short a player`() {
        val starter = player()
        val partner = player()
        val config = MatchConfig(
            type = MatchType.DOUBLES,
            sideAPlayers = listOf(starter, partner),
            sideBPlayers = listOf(player()), // malformed: only one opponent
            firstToGames = 6,
            deuceAdvantageEnabled = true,
            startingServerPlayerId = starter.id
        )

        val state = TennisScoringEngine.state(replaying = emptyList(), config = config)
        assertEquals(starter.id, state.currentServerPlayerId)
    }

    @Test
    fun `well-formed doubles config cycles through all four players`() {
        val a1 = player(); val a2 = player(); val b1 = player(); val b2 = player()
        val config = MatchConfig(
            type = MatchType.DOUBLES,
            sideAPlayers = listOf(a1, a2),
            sideBPlayers = listOf(b1, b2),
            firstToGames = 6,
            deuceAdvantageEnabled = true,
            startingServerPlayerId = a1.id
        )

        // Win 4 games in a row (4 points each replay burst) to advance through the server order.
        val winners = List(4 * 4) { Side.A }
        val expectedServers = listOf(a1.id, b1.id, a2.id, b2.id)
        for (game in 0 until 4) {
            val state = TennisScoringEngine.state(replaying = winners.take(game * 4), config = config)
            assertEquals(expectedServers[game], state.currentServerPlayerId)
        }
    }

    @Test
    fun `no starting server means no current server`() {
        val config = MatchConfig(
            type = MatchType.SINGLES,
            sideAPlayers = listOf(player()),
            sideBPlayers = listOf(player()),
            firstToGames = 6,
            deuceAdvantageEnabled = true,
            startingServerPlayerId = null
        )

        val state = TennisScoringEngine.state(replaying = emptyList(), config = config)
        assertNull(state.currentServerPlayerId)
    }
}
