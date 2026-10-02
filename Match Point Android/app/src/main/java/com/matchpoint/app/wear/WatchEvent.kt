package com.matchpoint.app.wear

import com.matchpoint.app.domain.PointDisplay
import com.matchpoint.app.domain.Side

/**
 * Events Android sends to the watch. Wire format is `NAME|field|field`, kept compact because
 * Lite Wearable apps have very little memory. Text fields are cleaned with [clean] so they
 * never contain `|`.
 */
sealed class WatchEvent {
    abstract fun encode(): String

    /** [winner] is set once a side has won by the scoring rules; the watch then offers Finish. */
    data class ScoreUpdate(
        val gamesA: Int,
        val gamesB: Int,
        val pointA: PointDisplay,
        val pointB: PointDisplay,
        val winner: Side?
    ) : WatchEvent() {
        override fun encode() =
            "SCORE_UPDATE|$gamesA|$gamesB|${pointA.wire()}|${pointB.wire()}|${winner?.name ?: "-"}"
    }

    /** Up to two names per side; the second is empty for singles. */
    data class Players(val sideA: List<String>, val sideB: List<String>) : WatchEvent() {
        override fun encode() =
            "PLAYERS|${sideA.name(0)}|${sideA.name(1)}|${sideB.name(0)}|${sideB.name(1)}"
    }

    /** The Sound FX the watch shows as buttons, in order. The watch sends back the index. */
    data class SoundFxList(val titles: List<String>) : WatchEvent() {
        override fun encode() = "SOUND_FX_LIST|" + titles.joinToString("|") { it.clean() }
    }

    /** [live] false means there is no match on the phone to score. */
    data class MatchStatus(val live: Boolean) : WatchEvent() {
        override fun encode() = "MATCH_STATUS|${if (live) "LIVE" else "IDLE"}"
    }

    data class MatchComplete(
        val gamesA: Int,
        val gamesB: Int,
        val winner: Side,
        val winnerNames: List<String>
    ) : WatchEvent() {
        override fun encode() =
            "MATCH_COMPLETE|$gamesA|$gamesB|${winner.name}|${winnerNames.name(0)}|${winnerNames.name(1)}"
    }

    /** Reply to the watch's PING heartbeat. */
    data object Pong : WatchEvent() {
        override fun encode() = "PONG"
    }

    /** [code] is a short machine code, for example FINISH_NOT_ALLOWED. The watch shows its own text. */
    data class Error(val code: String) : WatchEvent() {
        override fun encode() = "ERROR|$code"
    }
}

private fun String.clean(): String = replace("|", " ").trim()

private fun List<String>.name(index: Int): String = getOrNull(index)?.clean().orEmpty()

private fun PointDisplay.wire(): String = when (this) {
    PointDisplay.LOVE -> "0"
    PointDisplay.FIFTEEN -> "15"
    PointDisplay.THIRTY -> "30"
    PointDisplay.FORTY -> "40"
    PointDisplay.ADVANTAGE -> "AD"
}
