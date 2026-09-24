package com.matchpoint.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.AppPreferences
import com.matchpoint.app.data.MatchEntity
import com.matchpoint.app.data.MatchParticipantEntity
import com.matchpoint.app.data.SessionEntity
import com.matchpoint.app.data.SessionPlayerEntity
import com.matchpoint.app.domain.MatchStatus
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.repository.rankedByStanding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SessionPlayerUi(val sessionPlayer: SessionPlayerEntity, val name: String)

data class MatchUi(
    val match: MatchEntity,
    val sideANames: List<String>,
    val sideBNames: List<String>,
    val sideACount: Int,
    val sideBCount: Int
) {
    val isReadyToStart: Boolean
        get() {
            val required = match.type.requiredPlayerCount / 2
            return match.status == MatchStatus.DRAFT && sideACount == required && sideBCount == required
        }

    val hasAnyPlayers: Boolean get() = sideACount > 0 || sideBCount > 0
}

data class HomeUiState(
    val activeSession: SessionEntity? = null,
    val roster: List<SessionPlayerUi> = emptyList(),
    val matches: List<MatchUi> = emptyList(),
    val standings: List<SessionPlayerUi> = emptyList(),
    val liveMatch: MatchUi? = null,
    val draftMatch: MatchUi? = null,
    val availableCount: Int = 0
)

class HomeViewModel(
    private val repository: SessionRepository,
    val preferences: AppPreferences
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = repository.observeActiveSession()
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(HomeUiState())
            } else {
                combine(
                    repository.observeSessionPlayers(session.id),
                    repository.observeAllPlayers(),
                    repository.observeMatches(session.id),
                    matchParticipantsFlow(session.id)
                ) { sessionPlayers, players, matches, participantsByMatch ->
                    val nameOf = { id: UUID -> players.firstOrNull { it.id == id }?.name ?: "?" }
                    val roster = sessionPlayers.map { SessionPlayerUi(it, nameOf(it.playerId)) }
                    val matchesUi = matches.map { m ->
                        val participants = participantsByMatch[m.id].orEmpty()
                        val sideA = participants.filter { it.side == Side.A }.sortedBy { it.teamOrder }
                        val sideB = participants.filter { it.side == Side.B }.sortedBy { it.teamOrder }
                        MatchUi(m, sideA.map { nameOf(it.playerId) }, sideB.map { nameOf(it.playerId) }, sideA.size, sideB.size)
                    }
                    val standings = sessionPlayers.rankedByStanding(nameOf).map { SessionPlayerUi(it, nameOf(it.playerId)) }
                    HomeUiState(
                        activeSession = session,
                        roster = roster,
                        matches = matchesUi,
                        standings = standings,
                        liveMatch = matchesUi.firstOrNull { it.match.status == MatchStatus.LIVE },
                        draftMatch = matchesUi.firstOrNull { it.match.status == MatchStatus.DRAFT },
                        availableCount = sessionPlayers.count { it.state == com.matchpoint.app.domain.SessionPlayerState.AVAILABLE }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun matchParticipantsFlow(sessionId: UUID) =
        repository.observeMatches(sessionId).flatMapLatest { matches ->
            if (matches.isEmpty()) {
                flowOf(emptyMap<UUID, List<MatchParticipantEntity>>())
            } else {
                combine(matches.map { m -> repository.observeParticipants(m.id) }) { lists ->
                    matches.mapIndexed { i, m -> m.id to lists[i] }.toMap()
                }
            }
        }

    /** Draft-row-immediately pattern: creates the Session row right away so the sheet can register/finalize it. */
    fun startDraftSession(onCreated: (UUID) -> Unit) {
        viewModelScope.launch { onCreated(repository.startSession().id) }
    }

    fun cancelDraftSession(sessionId: UUID) {
        viewModelScope.launch { repository.cancelDraftSession(sessionId) }
    }

    fun finalizeSession(
        sessionId: UUID,
        name: String,
        matchType: MatchType,
        firstToGames: Int,
        deuceAdvantageEnabled: Boolean,
        startTime: Long?,
        endTime: Long?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.finalizeSession(sessionId, name, matchType, firstToGames, startTime, endTime)
            repository.createDraftMatch(sessionId, matchType, firstToGames, deuceAdvantageEnabled)
            com.matchpoint.app.audio.SoundManager.play(listOf("mabar_ready"))
            onDone()
        }
    }

    fun renameSession(newName: String) {
        val session = uiState.value.activeSession ?: return
        viewModelScope.launch { repository.renameSession(session, newName) }
    }

    fun updateSessionDetails(name: String, date: Long, startTime: Long?, endTime: Long?) {
        val session = uiState.value.activeSession ?: return
        viewModelScope.launch { repository.updateSessionDetails(session, name, date, startTime, endTime) }
    }

    fun endSession() {
        val session = uiState.value.activeSession ?: return
        viewModelScope.launch { repository.endSession(session) }
    }

    /** A finished match leaves the mabar with no next one; make sure an empty draft is ready. */
    fun ensureNextMatch() {
        val session = uiState.value.activeSession ?: return
        viewModelScope.launch {
            repository.ensureNextDraftMatch(session.id, preferences.defaultDeuceAdvantageEnabled.value)
        }
    }

    fun startMatch(match: MatchEntity, onStarted: (UUID) -> Unit) {
        viewModelScope.launch {
            repository.startMatch(match)
            com.matchpoint.app.audio.SoundManager.play(com.matchpoint.app.domain.AudioEventMapper.matchStart)
            onStarted(match.id)
        }
    }

    class Factory(private val repository: SessionRepository, private val preferences: AppPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository, preferences) as T
    }
}
