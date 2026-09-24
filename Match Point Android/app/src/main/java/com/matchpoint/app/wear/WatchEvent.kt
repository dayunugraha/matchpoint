package com.matchpoint.app.wear

import com.matchpoint.app.domain.PointDisplay
import com.matchpoint.app.domain.Side

/**
 * Events Android sends to the watch (product spec, section 19). Wire format is
 * `NAME|field|field`, kept compact because Lite Wearable apps have very little memory. Field
 * values never contain `|`.
 */
sealed class WatchEvent {
    abstract fun encode(): String

    data class ScoreUpdate(
        val gamesA: Int,
        val gamesB: Int,
        val pointA: PointDisplay,
        val pointB: PointDisplay
    ) : WatchEvent() {
        override fun encode() = "SCORE_UPDATE|$gamesA|$gamesB|${pointA.wire()}|${pointB.wire()}"
    }

    data class SoundFxUpdate(val index: Int, val title: String) : WatchEvent() {
        override fun encode() = "SOUND_FX_UPDATE|$index|$title"
    }

    /** [live] false means there is no match on the phone to score. */
    data class MatchStatus(val live: Boolean) : WatchEvent() {
        override fun encode() = "MATCH_STATUS|${if (live) "LIVE" else "IDLE"}"
    }

    data class MatchComplete(val gamesA: Int, val gamesB: Int, val winner: Side) : WatchEvent() {
        override fun encode() = "MATCH_COMPLETE|$gamesA|$gamesB|${winner.name}"
    }

    data class ConnectionStatus(val connected: Boolean) : WatchEvent() {
        override fun encode() = "CONNECTION_STATUS|${if (connected) "CONNECTED" else "DISCONNECTED"}"
    }

    /** Debug only: reply to the watch's PING. Remove before release. */
    data object Pong : WatchEvent() {
        override fun encode() = "PONG"
    }

    /** [code] is a short machine code, for example FINISH_NOT_ALLOWED. The watch shows its own text. */
    data class Error(val code: String) : WatchEvent() {
        override fun encode() = "ERROR|$code"
    }
}

private fun PointDisplay.wire(): String = when (this) {
    PointDisplay.LOVE -> "0"
    PointDisplay.FIFTEEN -> "15"
    PointDisplay.THIRTY -> "30"
    PointDisplay.FORTY -> "40"
    PointDisplay.ADVANTAGE -> "AD"
}
