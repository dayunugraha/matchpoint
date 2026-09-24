package com.matchpoint.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.MatchEntity
import com.matchpoint.app.data.MatchParticipantEntity
import com.matchpoint.app.data.SessionEntity
import com.matchpoint.app.data.SessionPlayerEntity
import com.matchpoint.app.domain.MatchStatus
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

data class HistoryMatchUi(val match: MatchEntity, val sideANames: List<String>, val sideBNames: List<String>)
data class HistoryPlayerUi(val sessionPlayer: SessionPlayerEntity, val name: String)

data class SessionHistoryUiState(
    val session: SessionEntity? = null,
    val standings: List<HistoryPlayerUi> = emptyList(),
    val completedMatches: List<HistoryMatchUi> = emptyList()
)

class SessionHistoryDetailViewModel(
    private val repository: SessionRepository,
    private val sessionId: UUID
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SessionHistoryUiState> = combine(
        repository.observeSession(sessionId),
        repository.observeSessionPlayers(sessionId),
        repository.observeAllPlayers(),
        repository.observeMatches(sessionId),
        matchParticipantsFlow()
    ) { session, sessionPlayers, players, matches, participantsByMatch ->
        val nameOf = { id: UUID -> players.firstOrNull { it.id == id }?.name ?: "?" }
        val standings = sessionPlayers.rankedByStanding(nameOf).map { HistoryPlayerUi(it, nameOf(it.playerId)) }
        val completed = matches.filter { it.status == MatchStatus.COMPLETED }.map { m ->
            val participants = participantsByMatch[m.id].orEmpty()
            HistoryMatchUi(
                m,
                participants.filter { it.side == Side.A }.sortedBy { it.teamOrder }.map { nameOf(it.playerId) },
                participants.filter { it.side == Side.B }.sortedBy { it.teamOrder }.map { nameOf(it.playerId) }
            )
        }
        SessionHistoryUiState(session, standings, completed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionHistoryUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun matchParticipantsFlow() =
        repository.observeMatches(sessionId).flatMapLatest { matches ->
            if (matches.isEmpty()) flowOf(emptyMap<UUID, List<MatchParticipantEntity>>())
            else combine(matches.map { m -> repository.observeParticipants(m.id) }) { lists ->
                matches.mapIndexed { i, m -> m.id to lists[i] }.toMap()
            }
        }

    /** Swaps a completed match with its neighbor (up = toward index 0) and persists the new order. */
    fun moveMatch(matchId: UUID, up: Boolean) {
        val ordered = uiState.value.completedMatches.map { it.match.id }.toMutableList()
        val index = ordered.indexOf(matchId)
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target < 0 || target >= ordered.size) return
        val tmp = ordered[index]
        ordered[index] = ordered[target]
        ordered[target] = tmp
        viewModelScope.launch { repository.reorderCompletedMatches(ordered) }
    }

    fun renameSession(newName: String) {
        val session = uiState.value.session ?: return
        viewModelScope.launch { repository.renameSession(session, newName) }
    }

    fun updateSessionDetails(name: String, date: Long, startTime: Long?, endTime: Long?) {
        val session = uiState.value.session ?: return
        viewModelScope.launch { repository.updateSessionDetails(session, name, date, startTime, endTime) }
    }

    fun deleteSession(onDeleted: () -> Unit) {
        val session = uiState.value.session ?: return
        viewModelScope.launch {
            repository.deleteSession(session)
            onDeleted()
        }
    }

    class Factory(private val repository: SessionRepository, private val sessionId: UUID) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionHistoryDetailViewModel(repository, sessionId) as T
    }
}
