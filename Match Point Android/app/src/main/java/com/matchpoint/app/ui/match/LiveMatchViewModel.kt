package com.matchpoint.app.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.audio.SoundEffects
import com.matchpoint.app.audio.SoundManager
import com.matchpoint.app.data.MatchEntity
import com.matchpoint.app.domain.AudioEventMapper
import com.matchpoint.app.domain.MatchConfig
import com.matchpoint.app.domain.MatchOutcome
import com.matchpoint.app.domain.MatchState
import com.matchpoint.app.domain.PlayerRef
import com.matchpoint.app.domain.Side
import com.matchpoint.app.domain.TennisScoringEngine
import com.matchpoint.app.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class LiveMatchUiState(
    val match: MatchEntity,
    val sideANames: List<String>,
    val sideBNames: List<String>,
    val sideAPlayerIds: List<UUID>,
    val sideBPlayerIds: List<UUID>,
    val score: MatchState,
    val hasHistory: Boolean,
    val allParticipants: List<Pair<UUID, String>>,
    val lastScoredSide: Side?
) {
    /** Which side is currently serving, for the announcer — [MatchState] only knows the
     * server's player id, not which side of the net they're on. */
    val currentServerSide: Side?
        get() = score.currentServerPlayerId?.let { id ->
            when {
                sideAPlayerIds.contains(id) -> Side.A
                sideBPlayerIds.contains(id) -> Side.B
                else -> null
            }
        }
}

class LiveMatchViewModel(
    private val repository: SessionRepository,
    private val matchId: UUID
) : ViewModel() {

    val uiState: StateFlow<LiveMatchUiState?> = combine(
        repository.observeMatch(matchId).filterNotNull(),
        repository.observeParticipants(matchId),
        repository.observeScoreEvents(matchId),
        repository.observeAllPlayers()
    ) { match, participants, events, players ->
        val nameOf = { id: UUID -> players.firstOrNull { it.id == id }?.name ?: "?" }
        val sideA = participants.filter { it.side == Side.A }.sortedBy { it.teamOrder }
        val sideB = participants.filter { it.side == Side.B }.sortedBy { it.teamOrder }

        val config = MatchConfig(
            type = match.type,
            sideAPlayers = sideA.map { PlayerRef(it.playerId, nameOf(it.playerId)) },
            sideBPlayers = sideB.map { PlayerRef(it.playerId, nameOf(it.playerId)) },
            firstToGames = match.firstToGames,
            deuceAdvantageEnabled = match.deuceAdvantageEnabled,
            startingServerPlayerId = match.startingServerPlayerId,
            manualServerOverridePlayerId = match.manualServerOverridePlayerId,
            manualServerOverrideGameIndex = match.manualServerOverrideGameIndex
        )
        val score = TennisScoringEngine.state(events.map { it.awardedSide }, config)

        LiveMatchUiState(
            match = match,
            sideANames = sideA.map { nameOf(it.playerId) },
            sideBNames = sideB.map { nameOf(it.playerId) },
            sideAPlayerIds = sideA.map { it.playerId },
            sideBPlayerIds = sideB.map { it.playerId },
            score = score,
            hasHistory = events.isNotEmpty(),
            allParticipants = (sideA + sideB).map { it.playerId to nameOf(it.playerId) },
            lastScoredSide = events.lastOrNull()?.awardedSide
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun recordPoint(side: Side) {
        val ui = uiState.value ?: return
        val before = ui.score
        viewModelScope.launch {
            repository.recordPoint(matchId, side)
            val after = repository.currentState(ui.match)
            val serverSideAfter = after.currentServerPlayerId?.let { id ->
                when {
                    ui.sideAPlayerIds.contains(id) -> Side.A
                    ui.sideBPlayerIds.contains(id) -> Side.B
                    else -> null
                }
            }
            if (serverSideAfter != null && before != after) {
                SoundManager.play(
                    AudioEventMapper.filenames(before, after, ui.match.firstToGames, ui.match.deuceAdvantageEnabled, serverSideAfter)
                )
            }
        }
    }

    fun playSoundEffect(filename: String) {
        SoundManager.playImmediately(listOf(filename))
    }

    // Sound FX picked from the Huawei Watch. Owned here (not on the watch) so the phone stays
    // the source of truth; the watch only steps through it and asks for playback.
    private val _selectedSoundFxIndex = MutableStateFlow(0)
    val selectedSoundFxIndex: StateFlow<Int> = _selectedSoundFxIndex.asStateFlow()

    fun selectNextSoundFx() {
        _selectedSoundFxIndex.value = (_selectedSoundFxIndex.value + 1) % SoundEffects.size
    }

    fun selectPreviousSoundFx() {
        _selectedSoundFxIndex.value = (_selectedSoundFxIndex.value - 1 + SoundEffects.size) % SoundEffects.size
    }

    fun playSelectedSoundFx() {
        playSoundEffect(SoundEffects[_selectedSoundFxIndex.value].filename)
    }

    fun undoLastPoint() {
        viewModelScope.launch { repository.undoLastPoint(matchId) }
    }

    fun finish(onFinished: () -> Unit) {
        val match = uiState.value?.match ?: return
        if (uiState.value?.score?.outcome !is MatchOutcome.Completed) return
        viewModelScope.launch {
            if (repository.finishMatch(match)) {
                SoundManager.play(AudioEventMapper.matchComplete())
                onFinished()
            }
        }
    }

    fun setServer(playerId: UUID) {
        val ui = uiState.value ?: return
        val gameIndex = ui.score.gamesA + ui.score.gamesB
        viewModelScope.launch { repository.setServer(ui.match, playerId, gameIndex) }
    }

    fun abandon(onAbandoned: () -> Unit) {
        val match = uiState.value?.match ?: return
        viewModelScope.launch {
            repository.abandonLiveMatch(match)
            onAbandoned()
        }
    }

    class Factory(private val repository: SessionRepository, private val matchId: UUID) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LiveMatchViewModel(repository, matchId) as T
    }
}
