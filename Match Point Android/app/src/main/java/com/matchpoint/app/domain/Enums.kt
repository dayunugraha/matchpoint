package com.matchpoint.app.domain

enum class SessionStatus { DRAFT, ACTIVE, ENDED }

enum class SessionPlayerState { REGISTERED, AVAILABLE, PLAYING, WAITING }

enum class MatchType(val requiredPlayerCount: Int) {
    SINGLES(2),
    DOUBLES(4)
}

enum class MatchStatus { DRAFT, LIVE, COMPLETED, ABANDONED }

enum class Side {
    A, B;

    val opposite: Side
        get() = if (this == A) B else A
}

enum class PointDisplay { LOVE, FIFTEEN, THIRTY, FORTY, ADVANTAGE }
