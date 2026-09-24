package com.matchpoint.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun observeAll(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players")
    suspend fun getAll(): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: UUID): PlayerEntity?

    @Query("SELECT * FROM players WHERE id = :id")
    fun observeById(id: UUID): Flow<PlayerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity)

    @Update
    suspend fun update(player: PlayerEntity)

    @Delete
    suspend fun delete(player: PlayerEntity)

    @Query("DELETE FROM players")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions")
    suspend fun getAll(): List<SessionEntity>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActive(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActive(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeById(id: UUID): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: UUID): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)
}

@Dao
interface SessionPlayerDao {
    @Query("SELECT * FROM session_players WHERE sessionId = :sessionId")
    fun observeForSession(sessionId: UUID): Flow<List<SessionPlayerEntity>>

    @Query("SELECT * FROM session_players WHERE sessionId = :sessionId")
    suspend fun getForSession(sessionId: UUID): List<SessionPlayerEntity>

    @Query("SELECT * FROM session_players WHERE playerId = :playerId")
    fun observeForPlayer(playerId: UUID): Flow<List<SessionPlayerEntity>>

    @Query("SELECT * FROM session_players WHERE sessionId = :sessionId AND playerId = :playerId LIMIT 1")
    suspend fun getForSessionAndPlayer(sessionId: UUID, playerId: UUID): SessionPlayerEntity?

    @Query("SELECT COUNT(*) FROM session_players WHERE playerId = :playerId AND state = 'PLAYING'")
    suspend fun playingCountForPlayer(playerId: UUID): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sessionPlayer: SessionPlayerEntity)

    @Update
    suspend fun update(sessionPlayer: SessionPlayerEntity)

    @Delete
    suspend fun delete(sessionPlayer: SessionPlayerEntity)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE sessionId = :sessionId ORDER BY displayOrder DESC, finishedAt DESC")
    fun observeForSession(sessionId: UUID): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    fun observeById(id: UUID): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getById(id: UUID): MatchEntity?

    @Query("SELECT * FROM matches WHERE sessionId = :sessionId AND status IN ('LIVE', 'COMPLETED')")
    suspend fun getLiveOrCompletedForSession(sessionId: UUID): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE sessionId = :sessionId")
    suspend fun getForSession(sessionId: UUID): List<MatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(match: MatchEntity)

    @Update
    suspend fun update(match: MatchEntity)

    @Delete
    suspend fun delete(match: MatchEntity)
}

@Dao
interface MatchParticipantDao {
    @Query("SELECT * FROM match_participants WHERE matchId = :matchId ORDER BY side, teamOrder")
    fun observeForMatch(matchId: UUID): Flow<List<MatchParticipantEntity>>

    @Query("SELECT * FROM match_participants WHERE playerId = :playerId")
    fun observeForPlayer(playerId: UUID): Flow<List<MatchParticipantEntity>>

    @Query("SELECT * FROM match_participants WHERE matchId = :matchId ORDER BY side, teamOrder")
    suspend fun getForMatch(matchId: UUID): List<MatchParticipantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(participants: List<MatchParticipantEntity>)

    @Query("DELETE FROM match_participants WHERE matchId = :matchId")
    suspend fun deleteForMatch(matchId: UUID)
}

@Dao
interface ScoreEventDao {
    @Query("SELECT * FROM score_events WHERE matchId = :matchId ORDER BY sequence ASC")
    fun observeForMatch(matchId: UUID): Flow<List<ScoreEventEntity>>

    @Query("SELECT * FROM score_events WHERE matchId = :matchId ORDER BY sequence ASC")
    suspend fun getForMatch(matchId: UUID): List<ScoreEventEntity>

    @Query("SELECT MAX(sequence) FROM score_events WHERE matchId = :matchId")
    suspend fun maxSequence(matchId: UUID): Int?

    @Insert
    suspend fun insert(event: ScoreEventEntity)

    @Query("DELETE FROM score_events WHERE matchId = :matchId AND sequence = (SELECT MAX(sequence) FROM score_events WHERE matchId = :matchId)")
    suspend fun deleteHighestSequence(matchId: UUID)

    @Query("DELETE FROM score_events WHERE matchId = :matchId")
    suspend fun deleteAllForMatch(matchId: UUID)
}
