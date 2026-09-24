package com.matchpoint.app.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.MatchEntity
import com.matchpoint.app.domain.SessionPlayerState
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SelectablePlayer(val id: UUID, val name: String, val side: Side?)

data class SelectPlayersUiState(
    val match: MatchEntity? = null,
    val requiredPerSide: Int = 2,
    val players: List<SelectablePlayer> = emptyList()
) {
    val sideACount get() = players.count { it.side == Side.A }
    val sideBCount get() = players.count { it.side == Side.B }
    val canSave get() = sideACount == requiredPerSide && sideBCount == requiredPerSide
}

class SelectPlayersViewModel(
    private val repository: SessionRepository,
    private val matchId: UUID,
    initialSide: String
) : ViewModel() {

    private val preferredSide = when (initialSide) {
        "A" -> Side.A
        "B" -> Side.B
        else -> null
    }

    /** playerId -> assigned side, local until Save. Null until seeded from the match's current participants. */
    private val selectionOverride = MutableStateFlow<Map<UUID, Side>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SelectPlayersUiState> = repository.observeMatch(matchId).filterNotNull()
        .flatMapLatest { match ->
            combine(
                repository.observeParticipants(matchId),
                repository.observeSessionPlayers(match.sessionId),
                repository.observeAllPlayers(),
                selectionOverride
            ) { participants, sessionPlayers, players, override ->
                val baseSelection = participants.associate { it.playerId to it.side }
                val selection = override ?: baseSelection

                val nameOf = { id: UUID -> players.firstOrNull { it.id == id }?.name ?: "?" }
                val registeredIds = sessionPlayers.map { it.playerId }.toSet()
                val availableIds = sessionPlayers.filter { it.state == SessionPlayerState.AVAILABLE }.map { it.playerId }.toSet()
                val neverRegisteredIds = players.map { it.id }.filter { it !in registeredIds }
                // Candidates: currently-available session players, anyone never registered to this
                // session at all (a brand new session's roster starts empty), plus anyone already
                // assigned locally (covers the match's own participants, whose row state is PLAYING).
                val candidateIds = (availableIds + neverRegisteredIds + selection.keys).distinct()

                val selectable = candidateIds.map { id -> SelectablePlayer(id, nameOf(id), selection[id]) }
                    .sortedBy { it.name.lowercase() }

                SelectPlayersUiState(match = match, requiredPerSide = match.type.requiredPlayerCount / 2, players = selectable)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SelectPlayersUiState())

    /** Cycles a player: unassigned -> preferred/first-open side -> other side -> unassigned. */
    fun togglePlayer(playerId: UUID) {
        val state = uiState.value
        val current = state.players.associate { it.id to it.side }.toMutableMap()
        val currentSide = current[playerId]

        val next: Side? = when (currentSide) {
            null -> {
                val target = preferredSide ?: if (state.sideACount < state.requiredPerSide) Side.A else Side.B
                if (state.players.count { it.side == target } < state.requiredPerSide) target else target.opposite
            }
            Side.A -> if (currentSide == Side.A && current.values.count { it == Side.B } < state.requiredPerSide) Side.B else null
            Side.B -> null
        }

        if (next == null) current.remove(playerId) else current[playerId] = next
        selectionOverride.value = current.filterValues { it != null }.mapValues { it.value!! }
    }

    fun addNewPlayer(name: String) {
        val sessionId = uiState.value.match?.sessionId ?: return
        if (name.isBlank()) return
        viewModelScope.launch { repository.addNewPlayerAsAvailable(name.trim(), sessionId) }
    }

    fun save(onSaved: () -> Unit) {
        val state = uiState.value
        val match = state.match ?: return
        if (!state.canSave) return
        val sideA = state.players.filter { it.side == Side.A }.map { it.id }
        val sideB = state.players.filter { it.side == Side.B }.map { it.id }
        viewModelScope.launch {
            repository.updateParticipants(match, sideA, sideB)
            onSaved()
        }
    }

    class Factory(private val repository: SessionRepository, private val matchId: UUID, private val initialSide: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SelectPlayersViewModel(repository, matchId, initialSide) as T
    }
}
