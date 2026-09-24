package com.matchpoint.app.backup

import com.matchpoint.app.data.AppDatabase
import com.matchpoint.app.data.MatchEntity
import com.matchpoint.app.data.MatchParticipantEntity
import com.matchpoint.app.data.PlayerEntity
import com.matchpoint.app.data.ScoreEventEntity
import com.matchpoint.app.data.SessionEntity
import com.matchpoint.app.data.SessionPlayerEntity
import com.matchpoint.app.domain.MatchStatus
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.domain.SessionPlayerState
import com.matchpoint.app.domain.SessionStatus
import com.matchpoint.app.domain.Side
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Manual JSON export/import — the whole reason this exists is that the app has no cloud
 * sync, so this is the only way data survives an app reinstall or device reset. Ported
 * from the iOS app's BackupService: same field names and full-replace restore semantics
 * (not a merge — simplest model for a personal single-device backup).
 */
class BackupThrows(message: String) : Exception(message)

class BackupService(private val db: AppDatabase) {

    companion object {
        const val CURRENT_VERSION = 1

        fun suggestedFilename(): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US)
            return "MatchPoint-Backup-${formatter.format(Date())}.json"
        }

        private fun isoFormat(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    suspend fun exportJson(): String {
        val playerDao = db.playerDao()
        val sessionDao = db.sessionDao()
        val sessionPlayerDao = db.sessionPlayerDao()
        val matchDao = db.matchDao()
        val participantDao = db.matchParticipantDao()
        val scoreEventDao = db.scoreEventDao()
        val iso = isoFormat()
        fun dateOrNull(millis: Long?) = millis?.let { iso.format(Date(it)) } ?: JSONObject.NULL

        val root = JSONObject()
        root.put("version", CURRENT_VERSION)
        root.put("exportedAt", iso.format(Date()))

        val playersArr = JSONArray()
        for (p in playerDao.getAll()) {
            playersArr.put(
                JSONObject()
                    .put("id", p.id.toString())
                    .put("name", p.name)
                    .put("createdAt", iso.format(Date(p.createdAt)))
            )
        }
        root.put("players", playersArr)

        val sessionsArr = JSONArray()
        for (s in sessionDao.getAll()) {
            val sessionObj = JSONObject()
                .put("id", s.id.toString())
                .put("name", s.name)
                .put("date", iso.format(Date(s.date)))
                .put("createdAt", iso.format(Date(s.createdAt)))
                .put("status", s.status.name)
                .put("preferredMatchType", s.preferredMatchType.name)
                .put("preferredFirstToGames", s.preferredFirstToGames)
                .put("startTime", dateOrNull(s.startTime))
                .put("endTime", dateOrNull(s.endTime))

            val spArr = JSONArray()
            for (sp in sessionPlayerDao.getForSession(s.id)) {
                spArr.put(
                    JSONObject()
                        .put("id", sp.id.toString())
                        .put("playerID", sp.playerId.toString())
                        .put("state", sp.state.name)
                        .put("matchesPlayed", sp.matchesPlayed)
                        .put("wins", sp.wins)
                        .put("losses", sp.losses)
                        .put("points", sp.points)
                        .put("gamesWon", sp.gamesWon)
                        .put("gamesLost", sp.gamesLost)
                        .put("lastMatchEndedAt", dateOrNull(sp.lastMatchEndedAt))
                )
            }
            sessionObj.put("sessionPlayers", spArr)

            val matchesArr = JSONArray()
            for (m in matchDao.getForSession(s.id)) {
                val matchObj = JSONObject()
                    .put("id", m.id.toString())
                    .put("type", m.type.name)
                    .put("status", m.status.name)
                    .put("startedAt", dateOrNull(m.startedAt))
                    .put("finishedAt", dateOrNull(m.finishedAt))
                    .put("deuceAdvantageEnabled", m.deuceAdvantageEnabled)
                    .put("firstToGames", m.firstToGames)
                    .put("startingServerPlayerID", m.startingServerPlayerId?.toString() ?: JSONObject.NULL)
                    .put("winnerSide", m.winnerSide?.name ?: JSONObject.NULL)
                    .put("finalScoreA", m.finalScoreA ?: JSONObject.NULL)
                    .put("finalScoreB", m.finalScoreB ?: JSONObject.NULL)
                    .put("regenerateCount", m.regenerateCount)
                    .put("displayOrder", m.displayOrder)
                    .put("manualServerOverridePlayerID", m.manualServerOverridePlayerId?.toString() ?: JSONObject.NULL)
                    .put("manualServerOverrideGameIndex", m.manualServerOverrideGameIndex ?: JSONObject.NULL)

                val partArr = JSONArray()
                for (part in participantDao.getForMatch(m.id)) {
                    partArr.put(
                        JSONObject()
                            .put("id", part.id.toString())
                            .put("playerID", part.playerId.toString())
                            .put("side", part.side.name)
                            .put("teamOrder", part.teamOrder)
                    )
                }
                matchObj.put("participants", partArr)

                val eventsArr = JSONArray()
                for (ev in scoreEventDao.getForMatch(m.id)) {
                    eventsArr.put(
                        JSONObject()
                            .put("id", ev.id.toString())
                            .put("sequence", ev.sequence)
                            .put("awardedSide", ev.awardedSide.name)
                            .put("timestamp", iso.format(Date(ev.timestamp)))
                    )
                }
                matchObj.put("scoreEvents", eventsArr)

                matchesArr.put(matchObj)
            }
            sessionObj.put("matches", matchesArr)

            sessionsArr.put(sessionObj)
        }
        root.put("sessions", sessionsArr)

        return root.toString(2)
    }

    /** Wipes every existing Player/Session (cascade-deleting their dependents) and rebuilds
     * the whole graph from [json], preserving original IDs so cross-references relink. */
    suspend fun restore(json: String) {
        val root = JSONObject(json)
        val version = root.optInt("version", 1)
        if (version > CURRENT_VERSION) {
            throw BackupThrows("This backup file (version $version) isn't supported by this version of the app.")
        }

        val playerDao = db.playerDao()
        val sessionDao = db.sessionDao()
        val sessionPlayerDao = db.sessionPlayerDao()
        val matchDao = db.matchDao()
        val participantDao = db.matchParticipantDao()
        val scoreEventDao = db.scoreEventDao()
        val iso = isoFormat()
        fun parseDate(s: String): Long = runCatching { iso.parse(s)?.time }.getOrNull() ?: System.currentTimeMillis()
        fun JSONObject.dateOrNull(key: String): Long? = if (isNull(key)) null else parseDate(getString(key))
        fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
        fun JSONObject.uuidOrNull(key: String): UUID? = stringOrNull(key)?.let { UUID.fromString(it) }
        fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)

        sessionDao.deleteAll()
        playerDao.deleteAll()

        val playersArr = root.getJSONArray("players")
        for (i in 0 until playersArr.length()) {
            val p = playersArr.getJSONObject(i)
            playerDao.insert(
                PlayerEntity(
                    id = UUID.fromString(p.getString("id")),
                    name = p.getString("name"),
                    createdAt = parseDate(p.getString("createdAt"))
                )
            )
        }

        val sessionsArr = root.getJSONArray("sessions")
        for (i in 0 until sessionsArr.length()) {
            val s = sessionsArr.getJSONObject(i)
            val sessionId = UUID.fromString(s.getString("id"))
            sessionDao.insert(
                SessionEntity(
                    id = sessionId,
                    name = s.getString("name"),
                    date = parseDate(s.getString("date")),
                    createdAt = parseDate(s.getString("createdAt")),
                    status = SessionStatus.valueOf(s.getString("status")),
                    preferredMatchType = MatchType.valueOf(s.getString("preferredMatchType")),
                    preferredFirstToGames = s.getInt("preferredFirstToGames"),
                    startTime = s.dateOrNull("startTime"),
                    endTime = s.dateOrNull("endTime"),
                    customBadgeImageData = null
                )
            )

            val spArr = s.getJSONArray("sessionPlayers")
            for (j in 0 until spArr.length()) {
                val sp = spArr.getJSONObject(j)
                sessionPlayerDao.insert(
                    SessionPlayerEntity(
                        id = UUID.fromString(sp.getString("id")),
                        sessionId = sessionId,
                        playerId = UUID.fromString(sp.getString("playerID")),
                        state = SessionPlayerState.valueOf(sp.getString("state")),
                        matchesPlayed = sp.getInt("matchesPlayed"),
                        wins = sp.getInt("wins"),
                        losses = sp.getInt("losses"),
                        points = sp.getInt("points"),
                        gamesWon = sp.getInt("gamesWon"),
                        gamesLost = sp.getInt("gamesLost"),
                        lastMatchEndedAt = sp.dateOrNull("lastMatchEndedAt")
                    )
                )
            }

            val matchesArr = s.getJSONArray("matches")
            for (j in 0 until matchesArr.length()) {
                val m = matchesArr.getJSONObject(j)
                val matchId = UUID.fromString(m.getString("id"))
                matchDao.insert(
                    MatchEntity(
                        id = matchId,
                        sessionId = sessionId,
                        type = MatchType.valueOf(m.getString("type")),
                        status = MatchStatus.valueOf(m.getString("status")),
                        startedAt = m.dateOrNull("startedAt"),
                        finishedAt = m.dateOrNull("finishedAt"),
                        deuceAdvantageEnabled = m.getBoolean("deuceAdvantageEnabled"),
                        firstToGames = m.getInt("firstToGames"),
                        startingServerPlayerId = m.uuidOrNull("startingServerPlayerID"),
                        winnerSide = m.stringOrNull("winnerSide")?.let { Side.valueOf(it) },
                        finalScoreA = m.intOrNull("finalScoreA"),
                        finalScoreB = m.intOrNull("finalScoreB"),
                        regenerateCount = m.optInt("regenerateCount", 0),
                        displayOrder = m.optInt("displayOrder", 0),
                        manualServerOverridePlayerId = m.uuidOrNull("manualServerOverridePlayerID"),
                        manualServerOverrideGameIndex = m.intOrNull("manualServerOverrideGameIndex")
                    )
                )

                val partArr = m.getJSONArray("participants")
                val participants = mutableListOf<MatchParticipantEntity>()
                for (k in 0 until partArr.length()) {
                    val part = partArr.getJSONObject(k)
                    participants.add(
                        MatchParticipantEntity(
                            id = UUID.fromString(part.getString("id")),
                            matchId = matchId,
                            playerId = UUID.fromString(part.getString("playerID")),
                            side = Side.valueOf(part.getString("side")),
                            teamOrder = part.getInt("teamOrder")
                        )
                    )
                }
                if (participants.isNotEmpty()) participantDao.insertAll(participants)

                val eventsArr = m.getJSONArray("scoreEvents")
                for (k in 0 until eventsArr.length()) {
                    val ev = eventsArr.getJSONObject(k)
                    scoreEventDao.insert(
                        ScoreEventEntity(
                            id = UUID.fromString(ev.getString("id")),
                            matchId = matchId,
                            sequence = ev.getInt("sequence"),
                            awardedSide = Side.valueOf(ev.getString("awardedSide")),
                            timestamp = parseDate(ev.getString("timestamp"))
                        )
                    )
                }
            }
        }
    }
}
