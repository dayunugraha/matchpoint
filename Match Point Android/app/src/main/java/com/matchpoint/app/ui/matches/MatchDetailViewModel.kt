package com.matchpoint.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.MatchEntity
import com.matchpoint.app.domain.SessionStatus
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

data class MatchDetailUiState(
    val match: MatchEntity? = null,
    val sideANames: List<String> = emptyList(),
    val sideBNames: List<String> = emptyList(),
    val serverName: String? = null,
    val editable: Boolean = false
)

class MatchDetailViewModel(
    private val repository: SessionRepository,
    private val matchId: UUID
) : ViewModel() {

    val uiState: StateFlow<MatchDetailUiState> = combine(
        repository.observeMatch(matchId).filterNotNull(),
        repository.observeParticipants(matchId),
        repository.observeAllPlayers()
    ) { match, participants, players ->
        val nameOf = { id: UUID -> players.firstOrNull { it.id == id }?.name ?: "?" }
        val session = repository.getSession(match.sessionId)
        MatchDetailUiState(
            match = match,
            sideANames = participants.filter { it.side == Side.A }.sortedBy { it.teamOrder }.map { nameOf(it.playerId) },
            sideBNames = participants.filter { it.side == Side.B }.sortedBy { it.teamOrder }.map { nameOf(it.playerId) },
            serverName = match.startingServerPlayerId?.let(nameOf),
            editable = session?.status == SessionStatus.ENDED
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MatchDetailUiState())

    class Factory(private val repository: SessionRepository, private val matchId: UUID) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MatchDetailViewModel(repository, matchId) as T
    }
}
