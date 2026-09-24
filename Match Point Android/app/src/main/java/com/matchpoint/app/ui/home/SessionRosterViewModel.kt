package com.matchpoint.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.PlayerEntity
import com.matchpoint.app.data.SessionPlayerEntity
import com.matchpoint.app.domain.SessionPlayerState
import com.matchpoint.app.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class RosterPlayerUi(val sessionPlayer: SessionPlayerEntity, val name: String)

data class SessionRosterUiState(
    val roster: List<RosterPlayerUi> = emptyList(),
    val unregisteredRosterPlayers: List<PlayerEntity> = emptyList()
)

class SessionRosterViewModel(
    private val repository: SessionRepository,
    private val sessionId: UUID
) : ViewModel() {

    val uiState: StateFlow<SessionRosterUiState> = combine(
        repository.observeSessionPlayers(sessionId),
        repository.observeAllPlayers()
    ) { sessionPlayers, players ->
        val nameOf = { id: UUID -> players.firstOrNull { it.id == id }?.name ?: "?" }
        val registeredIds = sessionPlayers.map { it.playerId }.toSet()
        SessionRosterUiState(
            roster = sessionPlayers.map { RosterPlayerUi(it, nameOf(it.playerId)) }.sortedBy { it.name.lowercase() },
            unregisteredRosterPlayers = players.filter { it.id !in registeredIds }.sortedBy { it.name.lowercase() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionRosterUiState())

    fun addNewPlayer(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addNewPlayerAsAvailable(name.trim(), sessionId) }
    }

    fun registerExisting(playerId: UUID) {
        viewModelScope.launch { repository.registerExistingPlayerAsAvailable(playerId, sessionId) }
    }

    fun toggleAvailable(sessionPlayer: SessionPlayerEntity) {
        viewModelScope.launch {
            repository.setAvailable(sessionPlayer, available = sessionPlayer.state != SessionPlayerState.AVAILABLE)
        }
    }

    fun unregister(sessionPlayer: SessionPlayerEntity) {
        viewModelScope.launch { repository.unregisterPlayer(sessionPlayer) }
    }

    class Factory(private val repository: SessionRepository, private val sessionId: UUID) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionRosterViewModel(repository, sessionId) as T
    }
}
