package com.matchpoint.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PlayerEntity::class,
        SessionEntity::class,
        SessionPlayerEntity::class,
        MatchEntity::class,
        MatchParticipantEntity::class,
        ScoreEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionPlayerDao(): SessionPlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun matchParticipantDao(): MatchParticipantDao
    abstract fun scoreEventDao(): ScoreEventDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "match_point.db"
                ).build().also { instance = it }
            }
    }
}
