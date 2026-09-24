package com.matchpoint.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class RecordablePlayer(val id: UUID, val name: String, val side: Side?)

data class RecordMatchUiState(
    val isEditing: Boolean = false,
    val matchType: MatchType = MatchType.DOUBLES,
    val players: List<RecordablePlayer> = emptyList(),
    val scoreA: String = "",
    val scoreB: String = ""
) {
    val requiredPerSide get() = matchType.requiredPlayerCount / 2
    val canSave get() =
        players.count { it.side == Side.A } == requiredPerSide &&
            players.count { it.side == Side.B } == requiredPerSide &&
            scoreA.toIntOrNull() != null && scoreB.toIntOrNull() != null &&
            scoreA.toIntOrNull() != scoreB.toIntOrNull()
}

class RecordMatchViewModel(
    private val repository: SessionRepository,
    private val sessionId: UUID,
    private val existingMatchId: UUID?
) : ViewModel() {

    // A one-time load (not a reactive Flow) — simplest fit for a create/edit form screen.
    private val state = MutableStateFlow(RecordMatchUiState())
    val ui: StateFlow<RecordMatchUiState> = state

    init {
        viewModelScope.launch {
            val allPlayers = repository.observeAllPlayers().first()
            val sessionPlayers = repository.observeSessionPlayers(sessionId).first()
            val nameOf = { id: UUID -> allPlayers.firstOrNull { it.id == id }?.name ?: "?" }

            var type = MatchType.DOUBLES
            var sel = emptyMap<UUID, Side>()
            var sA = ""
            var sB = ""
            var editing = false

            if (existingMatchId != null) {
                val match = repository.getMatch(existingMatchId)
                if (match != null) {
                    editing = true
                    type = match.type
                    sA = match.finalScoreA?.toString() ?: ""
                    sB = match.finalScoreB?.toString() ?: ""
                    val participants = repository.observeParticipants(existingMatchId).first()
                    sel = participants.associate { it.playerId to it.side }
                }
            }

            // Manual after-the-fact entry — only players already registered to this session are
            // selectable here (matches iOS RecordPastMatchSheet, which lists session.sessionPlayers).
            val candidateIds = (sessionPlayers.map { it.playerId } + sel.keys).distinct()
            val playersUi = candidateIds.map { id -> RecordablePlayer(id, nameOf(id), sel[id]) }.sortedBy { it.name.lowercase() }

            state.value = RecordMatchUiState(editing, type, playersUi, sA, sB)
        }
    }

    fun setMatchType(type: MatchType) {
        if (state.value.isEditing) return
        state.value = state.value.copy(matchType = type, players = state.value.players.map { it.copy(side = null) })
    }

    fun togglePlayer(playerId: UUID) {
        val s = state.value
        val current = s.players.associate { it.id to it.side }.toMutableMap()
        val currentSide = current[playerId]
        val required = s.requiredPerSide
        val next: Side? = when (currentSide) {
            null -> if (current.values.count { it == Side.A } < required) Side.A else if (current.values.count { it == Side.B } < required) Side.B else null
            Side.A -> if (current.values.count { it == Side.B } < required) Side.B else null
            Side.B -> null
        }
        if (next == null) current.remove(playerId) else current[playerId] = next
        state.value = s.copy(players = s.players.map { it.copy(side = current[it.id]) })
    }

    fun setScoreA(value: String) { state.value = state.value.copy(scoreA = value.filter(Char::isDigit)) }
    fun setScoreB(value: String) { state.value = state.value.copy(scoreB = value.filter(Char::isDigit)) }

    fun save(onSaved: () -> Unit) {
        val s = state.value
        if (!s.canSave) return
        val sideA = s.players.filter { it.side == Side.A }.map { it.id }
        val sideB = s.players.filter { it.side == Side.B }.map { it.id }
        val scoreAInt = s.scoreA.toInt()
        val scoreBInt = s.scoreB.toInt()

        viewModelScope.launch {
            if (s.isEditing && existingMatchId != null) {
                repository.getMatch(existingMatchId)?.let {
                    repository.updateCompletedMatch(it, scoreAInt, scoreBInt, newSideA = sideA, newSideB = sideB)
                }
            } else {
                repository.recordCompletedMatch(
                    sessionId = sessionId,
                    type = s.matchType,
                    sideAPlayerIds = sideA,
                    sideBPlayerIds = sideB,
                    scoreA = scoreAInt,
                    scoreB = scoreBInt,
                    deuceAdvantageEnabled = true,
                    firstToGames = maxOf(scoreAInt, scoreBInt)
                )
            }
            onSaved()
        }
    }

    class Factory(
        private val repository: SessionRepository,
        private val sessionId: UUID,
        private val existingMatchId: UUID?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecordMatchViewModel(repository, sessionId, existingMatchId) as T
    }
}
