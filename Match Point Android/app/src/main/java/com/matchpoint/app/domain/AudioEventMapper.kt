package com.matchpoint.app.domain

/**
 * Pure mapping from a scoring transition to the announcer clip(s) to play, in order.
 * No Android/MediaPlayer imports — ported 1:1 from the iOS app's AudioEventMapper.swift.
 * Doesn't know about priority, queueing, or actual playback; that's SoundManager's job.
 */
object AudioEventMapper {

    /** Filenames (without extension) to play, in order, for the point just recorded. */
    fun filenames(
        before: MatchState,
        after: MatchState,
        firstToGames: Int,
        deuceAdvantageEnabled: Boolean,
        currentServerSide: Side
    ): List<String> {
        // A game was just won. "game" plays immediately; the game tally reads out after a
        // beat, real-umpire style — but only once we have a clip for that exact combo
        // (0–4, so Quick Match only). "pause_3s" is a silent gap, not a clip.
        if (after.gamesA + after.gamesB > before.gamesA + before.gamesB) {
            val sequence = mutableListOf("game")
            gameScoreFilename(after.gamesA, after.gamesB, currentServerSide)?.let {
                sequence.add("pause_3s")
                sequence.add(it)
            }
            return sequence
        }

        // Tied at 40+. "Deuce" specifically implies win-by-2 advantage scoring, so it's
        // only accurate when Deuce & Advantage is on — otherwise the tie is "forty all".
        if (after.pointsA == after.pointsB && after.pointsA >= 3) {
            if (!deuceAdvantageEnabled) return listOf("forty_all")
            val cameFromAdvantage = before.pointsA != before.pointsB && before.pointsA >= 3 && before.pointsB >= 3
            return listOf(if (cameFromAdvantage) "back_to_deuce" else "deuce")
        }

        if (matchPointSide(after, firstToGames, deuceAdvantageEnabled) != null) return listOf("match_point")

        if (deuceAdvantageEnabled && after.gameDisplay.advantageSide != null) return listOf("advantage")

        return normalScoreFilenames(after.pointsA, after.pointsB, currentServerSide)
    }

    /** Fires once when a draft match transitions to live. */
    val matchStart: List<String> = listOf("match_start", "play")

    /** Fires once the organizer confirms Finish Match. */
    fun matchComplete(): List<String> = listOf("match_complete", matchWonVariants.random())

    private val matchWonVariants = listOf("congratulations", "what_a_match", "ggwp")

    /** The side for whom the resulting state means "win the next point and the match is over". */
    private fun matchPointSide(after: MatchState, firstToGames: Int, deuceAdvantageEnabled: Boolean): Side? {
        val display = after.gameDisplay
        display.advantageSide?.let { advantageSide ->
            val games = if (advantageSide == Side.A) after.gamesA else after.gamesB
            return if (games == firstToGames - 1) advantageSide else null
        }
        if (after.gamesA == firstToGames - 1 &&
            TennisScoringEngine.pointOutcome(after.pointsA + 1, after.pointsB, deuceAdvantageEnabled) == Side.A
        ) return Side.A
        if (after.gamesB == firstToGames - 1 &&
            TennisScoringEngine.pointOutcome(after.pointsA, after.pointsB + 1, deuceAdvantageEnabled) == Side.B
        ) return Side.B
        return null
    }

    /** "Love all" through "thirty forty" — the server's score is always called first. */
    private fun normalScoreFilenames(pointsA: Int, pointsB: Int, serverSide: Side): List<String> {
        if (pointsA == pointsB) return listOf(if (pointsA == 0) "love_all" else "${word(pointsA)}_all")
        val (serverPoints, receiverPoints) = if (serverSide == Side.A) pointsA to pointsB else pointsB to pointsA
        return listOf("${word(serverPoints)}_${word(receiverPoints)}")
    }

    private fun word(points: Int): String = when (points) {
        0 -> "love"
        1 -> "fifteen"
        2 -> "thirty"
        else -> "forty"
    }

    /** Only 0–4 are defined — enough for Quick Match (first to 4). */
    private val gameCountWords = mapOf(0 to "love", 1 to "one", 2 to "two", 3 to "three", 4 to "four")

    /** One filename per exact game-score combo — the side about to serve the next game is
     * read first, "games_X_all" when tied. `null` if either count is outside [gameCountWords]. */
    private fun gameScoreFilename(gamesA: Int, gamesB: Int, serverSide: Side): String? {
        val (serverGames, otherGames) = if (serverSide == Side.A) gamesA to gamesB else gamesB to gamesA
        val serverWord = gameCountWords[serverGames] ?: return null
        val otherWord = gameCountWords[otherGames] ?: return null
        if (gamesA == gamesB) return "games_${serverWord}_all"
        return "games_${serverWord}_$otherWord"
    }
}
