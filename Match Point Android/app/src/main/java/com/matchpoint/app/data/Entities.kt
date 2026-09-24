package com.matchpoint.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.matchpoint.app.domain.MatchStatus
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.domain.SessionPlayerState
import com.matchpoint.app.domain.SessionStatus
import com.matchpoint.app.domain.Side
import java.util.UUID

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val createdAt: Long
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val date: Long,
    val createdAt: Long,
    val status: SessionStatus,
    val preferredMatchType: MatchType,
    val preferredFirstToGames: Int,
    val startTime: Long?,
    val endTime: Long?,
    val customBadgeImageData: ByteArray?
)

@Entity(
    tableName = "session_players",
    foreignKeys = [
        ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlayerEntity::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("playerId")]
)
data class SessionPlayerEntity(
    @PrimaryKey val id: UUID,
    val sessionId: UUID,
    val playerId: UUID,
    val state: SessionPlayerState,
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val points: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val lastMatchEndedAt: Long?
)

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId")]
)
data class MatchEntity(
    @PrimaryKey val id: UUID,
    val sessionId: UUID,
    val type: MatchType,
    val status: MatchStatus,
    val startedAt: Long?,
    val finishedAt: Long?,
    val deuceAdvantageEnabled: Boolean,
    val firstToGames: Int,
    val startingServerPlayerId: UUID?,
    val winnerSide: Side?,
    val finalScoreA: Int?,
    val finalScoreB: Int?,
    val regenerateCount: Int = 0,
    val displayOrder: Int = 0,
    val manualServerOverridePlayerId: UUID?,
    val manualServerOverrideGameIndex: Int?
)

@Entity(
    tableName = "match_participants",
    foreignKeys = [
        ForeignKey(entity = MatchEntity::class, parentColumns = ["id"], childColumns = ["matchId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlayerEntity::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("matchId"), Index("playerId")]
)
data class MatchParticipantEntity(
    @PrimaryKey val id: UUID,
    val matchId: UUID,
    val playerId: UUID,
    val side: Side,
    val teamOrder: Int
)

@Entity(
    tableName = "score_events",
    foreignKeys = [
        ForeignKey(entity = MatchEntity::class, parentColumns = ["id"], childColumns = ["matchId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("matchId")]
)
data class ScoreEventEntity(
    @PrimaryKey val id: UUID,
    val matchId: UUID,
    val sequence: Int,
    val awardedSide: Side,
    val timestamp: Long
)
