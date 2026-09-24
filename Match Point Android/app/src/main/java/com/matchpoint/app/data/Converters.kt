package com.matchpoint.app.data

import androidx.room.TypeConverter
import com.matchpoint.app.domain.MatchStatus
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.domain.SessionPlayerState
import com.matchpoint.app.domain.SessionStatus
import com.matchpoint.app.domain.Side
import java.util.UUID

class Converters {

    @TypeConverter
    fun fromUuid(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun toUuid(value: String?): UUID? = value?.let { UUID.fromString(it) }

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter
    fun fromSessionPlayerState(value: SessionPlayerState): String = value.name

    @TypeConverter
    fun toSessionPlayerState(value: String): SessionPlayerState = SessionPlayerState.valueOf(value)

    @TypeConverter
    fun fromMatchType(value: MatchType): String = value.name

    @TypeConverter
    fun toMatchType(value: String): MatchType = MatchType.valueOf(value)

    @TypeConverter
    fun fromMatchStatus(value: MatchStatus): String = value.name

    @TypeConverter
    fun toMatchStatus(value: String): MatchStatus = MatchStatus.valueOf(value)

    @TypeConverter
    fun fromSide(value: Side?): String? = value?.name

    @TypeConverter
    fun toSide(value: String?): Side? = value?.let { Side.valueOf(it) }
}
