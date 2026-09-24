package com.matchpoint.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.MatchEntity
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class MatchSettingsUiState(
    val match: MatchEntity? = null,
    val participants: List<Pair<UUID, String>> = emptyList(),
    val selectedServerId: UUID? = null,
    val deuceAdvantageEnabled: Boolean = true
)

/** Local, un-committed edits — matches the iOS "Done"-to-save pattern rather than autosaving per tap. */
private data class LocalEdits(val deuce: Boolean, val serverId: UUID?)

class MatchSettingsViewModel(
    private val repository: SessionRepository,
    private val matchId: UUID
) : ViewModel() {

    private val localEdits = MutableStateFlow<LocalEdits?>(null)

    val uiState: StateFlow<MatchSettingsUiState> = combine(
        repository.observeMatch(matchId).filterNotNull(),
        repository.observeParticipants(matchId),
        repository.observeAllPlayers(),
        localEdits
    ) { match, participants, players, edits ->
        val nameOf = { id: UUID -> players.firstOrNull { it.id == id }?.name ?: "?" }
        val ordered = participants.sortedBy { it.teamOrder }
        val defaultServer = match.startingServerPlayerId ?: ordered.firstOrNull { it.side == Side.A }?.playerId
        val current = edits ?: LocalEdits(match.deuceAdvantageEnabled, defaultServer)
        if (edits == null) localEdits.value = current

        MatchSettingsUiState(
            match = match,
            participants = ordered.map { it.playerId to nameOf(it.playerId) },
            selectedServerId = current.serverId,
            deuceAdvantageEnabled = current.deuce
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MatchSettingsUiState())

    fun setStartingServer(playerId: UUID) {
        val edits = localEdits.value ?: LocalEdits(uiState.value.deuceAdvantageEnabled, null)
        localEdits.value = edits.copy(serverId = playerId)
    }

    fun setDeuceAdvantageEnabled(enabled: Boolean) {
        val edits = localEdits.value ?: LocalEdits(true, uiState.value.selectedServerId)
        localEdits.value = edits.copy(deuce = enabled)
    }

    fun save(onDone: () -> Unit) {
        val match = uiState.value.match ?: return
        val edits = localEdits.value ?: return
        viewModelScope.launch {
            repository.updateMatchSettings(match, edits.deuce, edits.serverId)
            onDone()
        }
    }

    class Factory(private val repository: SessionRepository, private val matchId: UUID) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MatchSettingsViewModel(repository, matchId) as T
    }
}
