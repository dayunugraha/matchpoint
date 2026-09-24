package com.matchpoint.app.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.MatchEntity
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.domain.SessionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PlayerPoints(val name: String, val points: Int)

data class MatchCompleteUiState(
    val match: MatchEntity? = null,
    val winnerNames: List<String> = emptyList(),
    val pointsSummary: List<PlayerPoints> = emptyList(),
    val sessionEnded: Boolean = false
)

class MatchCompleteViewModel(
    private val repository: SessionRepository,
    private val matchId: UUID
) : ViewModel() {

    val uiState: StateFlow<MatchCompleteUiState> = combine(
        repository.observeMatch(matchId).filterNotNull(),
        repository.observeParticipants(matchId),
        repository.observeAllPlayers()
    ) { match, participants, players ->
        val nameOf = { id: UUID -> players.firstOrNull { it.id == id }?.name ?: "?" }
        val winner = match.winnerSide
        val winnerNames = participants.filter { it.side == winner }.sortedBy { it.teamOrder }.map { nameOf(it.playerId) }
        val summary = participants.map { p ->
            PlayerPoints(nameOf(p.playerId), if (p.side == winner) 3 else 0)
        }
        MatchCompleteUiState(match = match, winnerNames = winnerNames, pointsSummary = summary)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MatchCompleteUiState())

    /** "Continue to Next Match": creates a new draft with the same settings, then the caller clears the nav stack to Home. */
    fun continueToNextMatch(onDone: (sessionEnded: Boolean) -> Unit) {
        val match = uiState.value.match ?: return
        viewModelScope.launch {
            val session = repository.observeSession(match.sessionId).first()
            val ended = session == null || session.status == SessionStatus.ENDED
            if (!ended) {
                repository.ensureNextDraftMatch(match.sessionId, match.deuceAdvantageEnabled)
            }
            onDone(ended)
        }
    }

    class Factory(private val repository: SessionRepository, private val matchId: UUID) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MatchCompleteViewModel(repository, matchId) as T
    }
}
