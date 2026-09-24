package com.matchpoint.app.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.PlayerEntity
import com.matchpoint.app.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PlayerCardUi(val player: PlayerEntity, val totalMatches: Int, val totalPoints: Int)

class PlayersListViewModel(private val repository: SessionRepository) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val players: StateFlow<List<PlayerCardUi>> = repository.observeAllPlayers()
        .flatMapLatest { players ->
            if (players.isEmpty()) flowOf(emptyList())
            else combine(players.map { p ->
                repository.observeCareerHistory(p.id).map { history ->
                    PlayerCardUi(p, history.sumOf { it.matchesPlayed }, history.sumOf { it.points })
                }
            }) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPlayer(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addPlayerToRoster(name.trim()) }
    }

    fun renamePlayer(player: PlayerEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.renamePlayer(player, newName.trim()) }
    }

    /** Returns false (via callback) if the player is currently in a live match and can't be deleted. */
    fun deletePlayer(player: PlayerEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.deletePlayer(player)) }
    }

    class Factory(private val repository: SessionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlayersListViewModel(repository) as T
    }
}
