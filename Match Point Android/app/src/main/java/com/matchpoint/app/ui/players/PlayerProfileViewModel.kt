package com.matchpoint.app.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.data.PlayerEntity
import com.matchpoint.app.data.SessionPlayerEntity
import com.matchpoint.app.domain.MatchStatus
import com.matchpoint.app.domain.Side
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.repository.careerTotals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

data class MatchHistoryRow(
    val sessionName: String,
    val matchupText: String,
    val finishedAt: Long?,
    val myScore: Int,
    val opponentScore: Int,
    val won: Boolean
)

data class PlayerProfileUiState(
    val player: PlayerEntity? = null,
    val totals: SessionPlayerEntity? = null,
    val history: List<MatchHistoryRow> = emptyList()
)

class PlayerProfileViewModel(
    private val repository: SessionRepository,
    private val playerId: UUID
) : ViewModel() {

    val uiState: StateFlow<PlayerProfileUiState> = combine(
        repository.observePlayer(playerId),
        repository.observeCareerHistory(playerId),
        repository.observeParticipationsForPlayer(playerId)
    ) { player, career, participations -> Triple(player, career, participations) }
        .map { (player, career, participations) ->
            val totals = career.careerTotals()
            val rows = participations.mapNotNull { p ->
                val match = repository.getMatch(p.matchId) ?: return@mapNotNull null
                if (match.status != MatchStatus.COMPLETED) return@mapNotNull null
                val session = repository.getSession(match.sessionId)
                val allParticipants = repository.observeParticipants(match.id).first()

                val myScore = if (p.side == Side.A) match.finalScoreA ?: 0 else match.finalScoreB ?: 0
                val oppScore = if (p.side == Side.A) match.finalScoreB ?: 0 else match.finalScoreA ?: 0

                val partnerNames = allParticipants.filter { it.side == p.side && it.playerId != playerId }
                    .mapNotNull { repository.getPlayer(it.playerId)?.name }
                val opponentNames = allParticipants.filter { it.side == p.side.opposite }
                    .mapNotNull { repository.getPlayer(it.playerId)?.name }

                val matchupText = buildString {
                    if (partnerNames.isNotEmpty()) append("with ${partnerNames.joinToString(" & ")} ")
                    append("vs ${opponentNames.joinToString(" & ")}")
                }

                MatchHistoryRow(
                    sessionName = session?.name ?: "Mabar",
                    matchupText = matchupText,
                    finishedAt = match.finishedAt,
                    myScore = myScore,
                    opponentScore = oppScore,
                    won = match.winnerSide == p.side
                )
            }.sortedByDescending { it.finishedAt ?: 0 }
            PlayerProfileUiState(player, totals, rows)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProfileUiState())

    class Factory(private val repository: SessionRepository, private val playerId: UUID) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlayerProfileViewModel(repository, playerId) as T
    }
}
