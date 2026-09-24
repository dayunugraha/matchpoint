package com.matchpoint.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.SessionEntity
import com.matchpoint.app.domain.MatchStatus
import com.matchpoint.app.domain.SessionStatus
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.repository.rankedByStanding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PastSessionUi(val session: SessionEntity, val completedCount: Int, val topScorerText: String?)

class MatchesListViewModel(private val repository: SessionRepository) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val pastSessions: StateFlow<List<PastSessionUi>> = repository.observeAllSessions()
        .flatMapLatest { sessions ->
            val ended = sessions.filter { it.status == SessionStatus.ENDED }
            if (ended.isEmpty()) flowOf(emptyList())
            else combine(ended.map { perSessionFlow(it) }) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun perSessionFlow(session: SessionEntity): Flow<PastSessionUi> = combine(
        repository.observeMatches(session.id),
        repository.observeSessionPlayers(session.id),
        repository.observeAllPlayers()
    ) { matches, sessionPlayers, players ->
        val nameOf = { id: UUID -> players.firstOrNull { it.id == id }?.name ?: "?" }
        val completedCount = matches.count { it.status == MatchStatus.COMPLETED }
        val top = sessionPlayers.rankedByStanding(nameOf).firstOrNull()
        PastSessionUi(
            session = session,
            completedCount = completedCount,
            topScorerText = top?.let { "${nameOf(it.playerId)} · ${it.points} pts" }
        )
    }

    fun deleteSessions(sessions: List<SessionEntity>) {
        viewModelScope.launch { repository.deleteSessions(sessions) }
    }

    class Factory(private val repository: SessionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MatchesListViewModel(repository) as T
    }
}
