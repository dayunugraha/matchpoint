package com.matchpoint.app.repository

import com.matchpoint.app.data.AppDatabase
import com.matchpoint.app.data.MatchEntity
import com.matchpoint.app.data.MatchParticipantEntity
import com.matchpoint.app.data.PlayerEntity
import com.matchpoint.app.data.SessionEntity
import com.matchpoint.app.data.SessionPlayerEntity
import com.matchpoint.app.domain.EligiblePlayer
import com.matchpoint.app.domain.MatchConfig
import com.matchpoint.app.domain.MatchGenerator
import com.matchpoint.app.domain.MatchOutcome
import com.matchpoint.app.domain.MatchState
import com.matchpoint.app.domain.MatchStatus
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.domain.PairingHistory
import com.matchpoint.app.domain.PlayerRef
import com.matchpoint.app.domain.SessionPlayerState
import com.matchpoint.app.domain.SessionStatus
import com.matchpoint.app.domain.Side
import com.matchpoint.app.domain.TennisScoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * The only place allowed to mutate persisted state — UI/ViewModels never
 * write to Room directly, mirroring the iOS `SessionService`.
 */
class SessionRepository(private val db: AppDatabase) {

    private val playerDao = db.playerDao()
    private val sessionDao = db.sessionDao()
    private val sessionPlayerDao = db.sessionPlayerDao()
    private val matchDao = db.matchDao()
    private val participantDao = db.matchParticipantDao()
    private val scoreEventDao = db.scoreEventDao()

    // ---- Session ----------------------------------------------------

    fun observeActiveSession(): Flow<SessionEntity?> = sessionDao.observeActive()
    fun observeAllSessions(): Flow<List<SessionEntity>> = sessionDao.observeAll()
    fun observeSession(id: UUID): Flow<SessionEntity?> = sessionDao.observeById(id)

    /** Creates a draft session immediately so players can be registered while the creation UI is open. */
    suspend fun startSession(name: String = "Mabar"): SessionEntity {
        val session = SessionEntity(
            id = UUID.randomUUID(),
            name = name,
            date = dayTruncated(System.currentTimeMillis()),
            createdAt = System.currentTimeMillis(),
            status = SessionStatus.DRAFT,
            preferredMatchType = MatchType.DOUBLES,
            preferredFirstToGames = 4,
            startTime = null,
            endTime = null,
            customBadgeImageData = null
        )
        sessionDao.insert(session)
        return session
    }

    /** Discards a draft session (and anything registered to it) if the creation flow is cancelled. */
    suspend fun cancelDraftSession(sessionId: UUID) {
        val session = sessionDao.getById(sessionId) ?: return
        if (session.status == SessionStatus.DRAFT) sessionDao.delete(session)
    }

    suspend fun finalizeSession(
        sessionId: UUID,
        name: String,
        preferredMatchType: MatchType,
        preferredFirstToGames: Int,
        startTime: Long?,
        endTime: Long?
    ) {
        val session = sessionDao.getById(sessionId) ?: return
        sessionDao.update(
            session.copy(
                name = name,
                status = SessionStatus.ACTIVE,
                preferredMatchType = preferredMatchType,
                preferredFirstToGames = preferredFirstToGames,
                startTime = startTime,
                endTime = endTime
            )
        )
    }

    suspend fun renameSession(session: SessionEntity, name: String) = sessionDao.update(session.copy(name = name))

    suspend fun updateSessionSchedule(session: SessionEntity, startTime: Long?, endTime: Long?) =
        sessionDao.update(session.copy(startTime = startTime, endTime = endTime))

    /** Full "Edit Mabar" update — name, date, and planned time window in one go. */
    suspend fun updateSessionDetails(session: SessionEntity, name: String, date: Long, startTime: Long?, endTime: Long?) =
        sessionDao.update(session.copy(name = name, date = date, startTime = startTime, endTime = endTime))

    suspend fun updateSessionBadge(session: SessionEntity, imageData: ByteArray?) =
        sessionDao.update(session.copy(customBadgeImageData = imageData))

    suspend fun endSession(session: SessionEntity) = sessionDao.update(session.copy(status = SessionStatus.ENDED))

    suspend fun getSession(sessionId: UUID): SessionEntity? = sessionDao.getById(sessionId)

    suspend fun deleteSession(session: SessionEntity) = sessionDao.delete(session)

    suspend fun deleteSessions(sessions: List<SessionEntity>) = sessions.forEach { sessionDao.delete(it) }

    // ---- Players ------------------------------------------------------

    fun observeAllPlayers(): Flow<List<PlayerEntity>> = playerDao.observeAll()
    fun observePlayer(playerId: UUID): Flow<PlayerEntity?> = playerDao.observeById(playerId)
    fun observeSessionPlayers(sessionId: UUID): Flow<List<SessionPlayerEntity>> = sessionPlayerDao.observeForSession(sessionId)
    fun observeCareerHistory(playerId: UUID): Flow<List<SessionPlayerEntity>> = sessionPlayerDao.observeForPlayer(playerId)
    fun observeParticipationsForPlayer(playerId: UUID): Flow<List<MatchParticipantEntity>> = participantDao.observeForPlayer(playerId)
    suspend fun getMatch(matchId: UUID): MatchEntity? = matchDao.getById(matchId)
    suspend fun getPlayer(playerId: UUID): PlayerEntity? = playerDao.getById(playerId)

    /** Adds a player to the permanent cross-session roster without registering them to any session. */
    suspend fun addPlayerToRoster(name: String): PlayerEntity {
        val player = PlayerEntity(id = UUID.randomUUID(), name = name, createdAt = System.currentTimeMillis())
        playerDao.insert(player)
        return player
    }

    suspend fun addNewPlayer(
        name: String,
        sessionId: UUID,
        state: SessionPlayerState = SessionPlayerState.REGISTERED
    ): SessionPlayerEntity {
        val player = PlayerEntity(id = UUID.randomUUID(), name = name, createdAt = System.currentTimeMillis())
        playerDao.insert(player)
        return registerExistingPlayer(player.id, sessionId, state)
    }

    suspend fun addNewPlayerAsAvailable(name: String, sessionId: UUID) =
        addNewPlayer(name, sessionId, SessionPlayerState.AVAILABLE)

    suspend fun registerExistingPlayer(
        playerId: UUID,
        sessionId: UUID,
        state: SessionPlayerState = SessionPlayerState.REGISTERED
    ): SessionPlayerEntity {
        sessionPlayerDao.getForSessionAndPlayer(sessionId, playerId)?.let { return it }
        val sessionPlayer = SessionPlayerEntity(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            playerId = playerId,
            state = state,
            lastMatchEndedAt = null
        )
        sessionPlayerDao.insert(sessionPlayer)
        return sessionPlayer
    }

    suspend fun registerExistingPlayerAsAvailable(playerId: UUID, sessionId: UUID) =
        registerExistingPlayer(playerId, sessionId, SessionPlayerState.AVAILABLE)

    suspend fun unregisterPlayer(sessionPlayer: SessionPlayerEntity) = sessionPlayerDao.delete(sessionPlayer)

    /** Returns false without deleting if the player is currently in a live match. */
    suspend fun deletePlayer(player: PlayerEntity): Boolean {
        if (sessionPlayerDao.playingCountForPlayer(player.id) > 0) return false
        playerDao.delete(player)
        return true
    }

    suspend fun renamePlayer(player: PlayerEntity, newName: String) = playerDao.update(player.copy(name = newName))

    /** Toggles a SessionPlayer between registered and available. */
    suspend fun setAvailable(sessionPlayer: SessionPlayerEntity, available: Boolean) {
        val newState = if (available) SessionPlayerState.AVAILABLE else SessionPlayerState.REGISTERED
        sessionPlayerDao.update(sessionPlayer.copy(state = newState))
    }

    suspend fun eligiblePlayers(sessionId: UUID): List<EligiblePlayer> {
        val available = sessionPlayerDao.getForSession(sessionId).filter { it.state == SessionPlayerState.AVAILABLE }
        return available.mapNotNull { sp ->
            val player = playerDao.getById(sp.playerId) ?: return@mapNotNull null
            EligiblePlayer(PlayerRef(player.id, player.name), sp.matchesPlayed, sp.lastMatchEndedAt)
        }
    }

    // ---- Match generation ----------------------------------------------

    suspend fun pairingHistory(sessionId: UUID): PairingHistory {
        val matches = matchDao.getLiveOrCompletedForSession(sessionId)
        val partnerPairs = mutableSetOf<Set<UUID>>()
        val opponentPairs = mutableSetOf<Set<UUID>>()
        for (match in matches) {
            val participants = participantDao.getForMatch(match.id)
            val sideA = participants.filter { it.side == Side.A }
            val sideB = participants.filter { it.side == Side.B }
            if (sideA.size == 2) partnerPairs += setOf(sideA[0].playerId, sideA[1].playerId)
            if (sideB.size == 2) partnerPairs += setOf(sideB[0].playerId, sideB[1].playerId)
            for (a in sideA) for (b in sideB) opponentPairs += setOf(a.playerId, b.playerId)
        }
        return PairingHistory(partnerPairs, opponentPairs)
    }

    private val nextDraftMutex = Mutex()

    /**
     * Keeps an ACTIVE mabar from ever sitting without a next match: if it has no draft or live
     * match, creates an empty draft (no players selected) that copies the last finished
     * match's settings. Idempotent and serialized, so callers (Home, Match Complete) can call
     * it freely without risking two drafts.
     */
    suspend fun ensureNextDraftMatch(sessionId: UUID, fallbackDeuceAdvantageEnabled: Boolean): MatchEntity? =
        nextDraftMutex.withLock {
            val session = sessionDao.getById(sessionId) ?: return@withLock null
            if (session.status != SessionStatus.ACTIVE) return@withLock null
            val matches = matchDao.getForSession(sessionId)
            if (matches.any { it.status == MatchStatus.DRAFT || it.status == MatchStatus.LIVE }) return@withLock null
            val last = matches.filter { it.status == MatchStatus.COMPLETED }.maxByOrNull { it.finishedAt ?: 0L }
            createDraftMatch(
                sessionId,
                last?.type ?: session.preferredMatchType,
                last?.firstToGames ?: session.preferredFirstToGames,
                last?.deuceAdvantageEnabled ?: fallbackDeuceAdvantageEnabled
            )
        }

    suspend fun createDraftMatch(
        sessionId: UUID,
        type: MatchType,
        firstToGames: Int,
        deuceAdvantageEnabled: Boolean
    ): MatchEntity {
        val match = MatchEntity(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            type = type,
            status = MatchStatus.DRAFT,
            startedAt = null,
            finishedAt = null,
            deuceAdvantageEnabled = deuceAdvantageEnabled,
            firstToGames = firstToGames,
            startingServerPlayerId = null,
            winnerSide = null,
            finalScoreA = null,
            finalScoreB = null,
            manualServerOverridePlayerId = null,
            manualServerOverrideGameIndex = null
        )
        matchDao.insert(match)
        return match
    }

    /** Generates a fair matchup, creates participants, and flips those SessionPlayers to PLAYING. */
    suspend fun generateMatch(
        sessionId: UUID,
        type: MatchType,
        firstToGames: Int,
        deuceAdvantageEnabled: Boolean
    ): MatchEntity? {
        val eligible = eligiblePlayers(sessionId)
        val history = pairingHistory(sessionId)
        val proposal = MatchGenerator.generate(type, eligible, history) ?: return null

        val match = createDraftMatch(sessionId, type, firstToGames, deuceAdvantageEnabled)
        applyParticipants(match.id, proposal.sideA, proposal.sideB)
        markPlaying(sessionId, proposal.sideA + proposal.sideB)
        return match
    }

    /** Re-rolls a draft's matchup: releases current participants, then proposes a new one. */
    suspend fun regenerate(match: MatchEntity): MatchEntity? {
        val oldParticipants = participantDao.getForMatch(match.id)
        releaseToAvailable(match.sessionId, oldParticipants.map { it.playerId })

        val eligible = eligiblePlayers(match.sessionId)
        val history = pairingHistory(match.sessionId)
        val proposal = MatchGenerator.generate(match.type, eligible, history)
        if (proposal == null) {
            // Not enough eligible players for a re-roll — restore the previous matchup.
            markPlaying(match.sessionId, oldParticipants.map { PlayerRef(it.playerId, "") })
            return null
        }

        applyParticipants(match.id, proposal.sideA, proposal.sideB)
        markPlaying(match.sessionId, proposal.sideA + proposal.sideB)

        val updated = match.copy(regenerateCount = match.regenerateCount + 1)
        matchDao.update(updated)
        return updated
    }

    /** Manual edit of a draft's matchup. */
    suspend fun updateParticipants(match: MatchEntity, sideA: List<UUID>, sideB: List<UUID>) {
        val old = participantDao.getForMatch(match.id)
        releaseToAvailable(match.sessionId, old.map { it.playerId })

        participantDao.deleteForMatch(match.id)
        val entities = sideA.mapIndexed { i, id -> MatchParticipantEntity(UUID.randomUUID(), match.id, id, Side.A, i) } +
            sideB.mapIndexed { i, id -> MatchParticipantEntity(UUID.randomUUID(), match.id, id, Side.B, i) }
        participantDao.insertAll(entities)

        for (id in sideA + sideB) {
            ensureRegistered(match.sessionId, id, SessionPlayerState.PLAYING)
        }
    }

    /**
     * Gets-or-creates the SessionPlayer row for a roster player in a session, setting its state.
     * Needed because a player can be picked for a match without ever having been explicitly
     * registered/made-available in that session first (e.g. a brand new session's roster is empty).
     */
    private suspend fun ensureRegistered(sessionId: UUID, playerId: UUID, state: SessionPlayerState): SessionPlayerEntity {
        val existing = sessionPlayerDao.getForSessionAndPlayer(sessionId, playerId)
        if (existing != null) {
            if (existing.state != state) sessionPlayerDao.update(existing.copy(state = state))
            return existing
        }
        val sp = SessionPlayerEntity(id = UUID.randomUUID(), sessionId = sessionId, playerId = playerId, state = state, lastMatchEndedAt = null)
        sessionPlayerDao.insert(sp)
        return sp
    }

    private suspend fun applyParticipants(matchId: UUID, sideA: List<PlayerRef>, sideB: List<PlayerRef>) {
        participantDao.deleteForMatch(matchId)
        val entities = sideA.mapIndexed { i, p -> MatchParticipantEntity(UUID.randomUUID(), matchId, p.id, Side.A, i) } +
            sideB.mapIndexed { i, p -> MatchParticipantEntity(UUID.randomUUID(), matchId, p.id, Side.B, i) }
        participantDao.insertAll(entities)
    }

    private suspend fun markPlaying(sessionId: UUID, players: List<PlayerRef>) {
        for (p in players) {
            sessionPlayerDao.getForSessionAndPlayer(sessionId, p.id)?.let {
                sessionPlayerDao.update(it.copy(state = SessionPlayerState.PLAYING))
            }
        }
    }

    private suspend fun releaseToAvailable(sessionId: UUID, playerIds: List<UUID>) {
        for (id in playerIds) {
            sessionPlayerDao.getForSessionAndPlayer(sessionId, id)?.let {
                sessionPlayerDao.update(it.copy(state = SessionPlayerState.AVAILABLE))
            }
        }
    }

    // ---- Match lifecycle -------------------------------------------------

    suspend fun startMatch(match: MatchEntity) {
        // If Match Settings never explicitly picked a server, default to the first Side A
        // participant — mirrors iOS's SessionService.startMatch. Without this the announcer
        // (and the "Serving" pill) have no side to call the score from.
        val startingServerPlayerId = match.startingServerPlayerId
            ?: participantDao.getForMatch(match.id).filter { it.side == Side.A }.minByOrNull { it.teamOrder }?.playerId
        matchDao.update(
            match.copy(
                status = MatchStatus.LIVE,
                startedAt = System.currentTimeMillis(),
                startingServerPlayerId = startingServerPlayerId
            )
        )
    }

    suspend fun updateMatchSettings(match: MatchEntity, deuceAdvantageEnabled: Boolean, startingServerPlayerId: UUID?) =
        matchDao.update(match.copy(deuceAdvantageEnabled = deuceAdvantageEnabled, startingServerPlayerId = startingServerPlayerId))

    suspend fun recordPoint(matchId: UUID, side: Side) {
        val next = (scoreEventDao.maxSequence(matchId) ?: -1) + 1
        scoreEventDao.insert(
            com.matchpoint.app.data.ScoreEventEntity(
                id = UUID.randomUUID(),
                matchId = matchId,
                sequence = next,
                awardedSide = side,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun undoLastPoint(matchId: UUID) = scoreEventDao.deleteHighestSequence(matchId)

    /** Manual server override, scoped to the current game index only. */
    suspend fun setServer(match: MatchEntity, playerId: UUID, currentGameIndex: Int) =
        matchDao.update(match.copy(manualServerOverridePlayerId = playerId, manualServerOverrideGameIndex = currentGameIndex))

    /** The only way live score is ever read — always recomputed from the event log, never cached. */
    suspend fun currentState(match: MatchEntity): MatchState {
        val participants = participantDao.getForMatch(match.id)
        val sideA = participants.filter { it.side == Side.A }.sortedBy { it.teamOrder }
        val sideB = participants.filter { it.side == Side.B }.sortedBy { it.teamOrder }

        val config = MatchConfig(
            type = match.type,
            sideAPlayers = sideA.map { toPlayerRef(it.playerId) },
            sideBPlayers = sideB.map { toPlayerRef(it.playerId) },
            firstToGames = match.firstToGames,
            deuceAdvantageEnabled = match.deuceAdvantageEnabled,
            startingServerPlayerId = match.startingServerPlayerId,
            manualServerOverridePlayerId = match.manualServerOverridePlayerId,
            manualServerOverrideGameIndex = match.manualServerOverrideGameIndex
        )
        val events = scoreEventDao.getForMatch(match.id).map { it.awardedSide }
        return TennisScoringEngine.state(events, config)
    }

    /** Live games score, reactive to every point tapped — used by the Home mabar card. */
    fun observeGamesScore(match: MatchEntity): Flow<Pair<Int, Int>> =
        combine(participantDao.observeForMatch(match.id), scoreEventDao.observeForMatch(match.id)) { participants, events ->
            val sideA = participants.filter { it.side == Side.A }.sortedBy { it.teamOrder }
            val sideB = participants.filter { it.side == Side.B }.sortedBy { it.teamOrder }
            val config = MatchConfig(
                type = match.type,
                sideAPlayers = sideA.map { PlayerRef(it.playerId, "") },
                sideBPlayers = sideB.map { PlayerRef(it.playerId, "") },
                firstToGames = match.firstToGames,
                deuceAdvantageEnabled = match.deuceAdvantageEnabled,
                startingServerPlayerId = match.startingServerPlayerId,
                manualServerOverridePlayerId = match.manualServerOverridePlayerId,
                manualServerOverrideGameIndex = match.manualServerOverrideGameIndex
            )
            val state = TennisScoringEngine.state(events.map { it.awardedSide }, config)
            state.gamesA to state.gamesB
        }

    private suspend fun toPlayerRef(playerId: UUID): PlayerRef {
        val player = playerDao.getById(playerId)
        return PlayerRef(playerId, player?.name ?: "")
    }

    suspend fun isFinishable(match: MatchEntity): Boolean = currentState(match).outcome is MatchOutcome.Completed

    /**
     * Completes the match: snapshots the final score, deletes the point-by-point
     * log (only draft/live matches keep one), folds the result into every
     * participant's running stats, and releases them back to available.
     */
    suspend fun finishMatch(match: MatchEntity): Boolean {
        val state = currentState(match)
        val outcome = state.outcome as? MatchOutcome.Completed ?: return false
        val finishedAt = System.currentTimeMillis()

        matchDao.update(
            match.copy(
                status = MatchStatus.COMPLETED,
                finalScoreA = state.finalScoreA,
                finalScoreB = state.finalScoreB,
                winnerSide = outcome.winner,
                finishedAt = finishedAt
            )
        )
        scoreEventDao.deleteAllForMatch(match.id)
        adjustStats(match.id, state.finalScoreA, state.finalScoreB, outcome.winner, sign = 1, endedAt = finishedAt)

        val participants = participantDao.getForMatch(match.id)
        releaseToAvailable(match.sessionId, participants.map { it.playerId })
        return true
    }

    /** sign = +1 to apply a result, -1 to subtract it (used when editing an already-completed match). */
    private suspend fun adjustStats(matchId: UUID, scoreA: Int, scoreB: Int, winner: Side, sign: Int, endedAt: Long?) {
        val match = matchDao.getById(matchId) ?: return
        val participants = participantDao.getForMatch(matchId)
        for (p in participants) {
            val sp = sessionPlayerDao.getForSessionAndPlayer(match.sessionId, p.playerId) ?: continue
            val won = p.side == winner
            val gamesWonDelta = if (p.side == Side.A) scoreA else scoreB
            val gamesLostDelta = if (p.side == Side.A) scoreB else scoreA
            sessionPlayerDao.update(
                sp.copy(
                    matchesPlayed = sp.matchesPlayed + sign,
                    wins = sp.wins + if (won) sign else 0,
                    losses = sp.losses + if (!won) sign else 0,
                    points = sp.points + if (won) sign * 3 else 0,
                    gamesWon = sp.gamesWon + sign * gamesWonDelta,
                    gamesLost = sp.gamesLost + sign * gamesLostDelta,
                    lastMatchEndedAt = if (sign > 0) endedAt else sp.lastMatchEndedAt
                )
            )
        }
    }

    /** Manual after-the-fact match entry (no live scoring), still folded into stats. */
    suspend fun recordCompletedMatch(
        sessionId: UUID,
        type: MatchType,
        sideAPlayerIds: List<UUID>,
        sideBPlayerIds: List<UUID>,
        scoreA: Int,
        scoreB: Int,
        deuceAdvantageEnabled: Boolean,
        firstToGames: Int
    ): MatchEntity {
        val winner = if (scoreA > scoreB) Side.A else Side.B
        val finishedAt = System.currentTimeMillis()
        val match = MatchEntity(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            type = type,
            status = MatchStatus.COMPLETED,
            startedAt = null,
            finishedAt = finishedAt,
            deuceAdvantageEnabled = deuceAdvantageEnabled,
            firstToGames = firstToGames,
            startingServerPlayerId = null,
            winnerSide = winner,
            finalScoreA = scoreA,
            finalScoreB = scoreB,
            manualServerOverridePlayerId = null,
            manualServerOverrideGameIndex = null
        )
        matchDao.insert(match)
        val entities = sideAPlayerIds.mapIndexed { i, id -> MatchParticipantEntity(UUID.randomUUID(), match.id, id, Side.A, i) } +
            sideBPlayerIds.mapIndexed { i, id -> MatchParticipantEntity(UUID.randomUUID(), match.id, id, Side.B, i) }
        participantDao.insertAll(entities)
        for (id in sideAPlayerIds + sideBPlayerIds) {
            ensureRegistered(sessionId, id, SessionPlayerState.AVAILABLE)
        }
        adjustStats(match.id, scoreA, scoreB, winner, sign = 1, endedAt = finishedAt)
        return match
    }

    /**
     * Edits an already-completed match's score and (optionally) its players: subtracts the old
     * result from the old participants' stats, swaps in the new roster if given, then re-applies.
     */
    suspend fun updateCompletedMatch(
        match: MatchEntity,
        newScoreA: Int,
        newScoreB: Int,
        newSideA: List<UUID>? = null,
        newSideB: List<UUID>? = null
    ) {
        val oldWinner = match.winnerSide ?: return
        adjustStats(match.id, match.finalScoreA ?: 0, match.finalScoreB ?: 0, oldWinner, sign = -1, endedAt = null)

        if (newSideA != null && newSideB != null) {
            participantDao.deleteForMatch(match.id)
            val entities = newSideA.mapIndexed { i, id -> MatchParticipantEntity(UUID.randomUUID(), match.id, id, Side.A, i) } +
                newSideB.mapIndexed { i, id -> MatchParticipantEntity(UUID.randomUUID(), match.id, id, Side.B, i) }
            participantDao.insertAll(entities)
            for (id in newSideA + newSideB) {
                ensureRegistered(match.sessionId, id, SessionPlayerState.AVAILABLE)
            }
        }

        val newWinner = if (newScoreA > newScoreB) Side.A else Side.B
        val updated = match.copy(finalScoreA = newScoreA, finalScoreB = newScoreB, winnerSide = newWinner)
        matchDao.update(updated)
        adjustStats(match.id, newScoreA, newScoreB, newWinner, sign = 1, endedAt = match.finishedAt)
    }

    suspend fun reorderCompletedMatches(orderedIds: List<UUID>) {
        val n = orderedIds.size
        orderedIds.forEachIndexed { index, id ->
            matchDao.getById(id)?.let { matchDao.update(it.copy(displayOrder = n - index)) }
        }
    }

    suspend fun abandonMatch(match: MatchEntity) {
        matchDao.update(match.copy(status = MatchStatus.ABANDONED))
        val participants = participantDao.getForMatch(match.id)
        releaseToAvailable(match.sessionId, participants.map { it.playerId })
        scoreEventDao.deleteAllForMatch(match.id)
    }

    /**
     * Abandoning from live scoring doesn't retire the match slot — it resets this same
     * match back to an empty draft (no participants, no score) so the mabar card keeps
     * showing it, ready to pick players again, instead of falling back to the "set up a
     * new match" prompt. Mirrors iOS's `SessionService.abandonLiveMatch`.
     */
    suspend fun abandonLiveMatch(match: MatchEntity) {
        if (match.status != MatchStatus.LIVE) return
        val participants = participantDao.getForMatch(match.id)
        releaseToAvailable(match.sessionId, participants.map { it.playerId })
        participantDao.deleteForMatch(match.id)
        scoreEventDao.deleteAllForMatch(match.id)
        matchDao.update(
            match.copy(
                status = MatchStatus.DRAFT,
                startedAt = null,
                startingServerPlayerId = null,
                manualServerOverridePlayerId = null,
                manualServerOverrideGameIndex = null
            )
        )
    }

    fun observeMatches(sessionId: UUID): Flow<List<MatchEntity>> = matchDao.observeForSession(sessionId)
    fun observeMatch(matchId: UUID): Flow<MatchEntity?> = matchDao.observeById(matchId)
    fun observeParticipants(matchId: UUID): Flow<List<MatchParticipantEntity>> = participantDao.observeForMatch(matchId)
    fun observeScoreEvents(matchId: UUID) = scoreEventDao.observeForMatch(matchId)

    private fun dayTruncated(millis: Long): Long {
        val dayMs = 24L * 60 * 60 * 1000
        return (millis / dayMs) * dayMs
    }
}

/** rankedByStanding: players with matchesPlayed > 0, sorted by points desc, winRate desc, gameDifference desc, name asc. */
fun List<SessionPlayerEntity>.rankedByStanding(nameFor: (UUID) -> String): List<SessionPlayerEntity> =
    filter { it.matchesPlayed > 0 }
        .sortedWith(
            compareByDescending<SessionPlayerEntity> { it.points }
                .thenByDescending { it.winRate }
                .thenByDescending { it.gameDifference }
                .thenBy { nameFor(it.playerId) }
        )

val SessionPlayerEntity.winRate: Double
    get() = if (wins + losses == 0) 0.0 else wins.toDouble() / (wins + losses)

val SessionPlayerEntity.gameDifference: Int
    get() = gamesWon - gamesLost

/** careerTotals: sum of stats across every SessionPlayer row a Player has ever had. */
fun List<SessionPlayerEntity>.careerTotals(): SessionPlayerEntity? {
    if (isEmpty()) return null
    val totalMatches = sumOf { it.matchesPlayed }
    if (totalMatches == 0) return null
    return SessionPlayerEntity(
        id = UUID.randomUUID(),
        sessionId = first().sessionId,
        playerId = first().playerId,
        state = SessionPlayerState.REGISTERED,
        matchesPlayed = totalMatches,
        wins = sumOf { it.wins },
        losses = sumOf { it.losses },
        points = sumOf { it.points },
        gamesWon = sumOf { it.gamesWon },
        gamesLost = sumOf { it.gamesLost },
        lastMatchEndedAt = mapNotNull { it.lastMatchEndedAt }.maxOrNull()
    )
}
