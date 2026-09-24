package com.matchpoint.app.domain

import kotlin.random.Random

/** Pure, fair-random matchup proposer. No side effects, no DB access. */
object MatchGenerator {

    fun generate(
        type: MatchType,
        eligible: List<EligiblePlayer>,
        history: PairingHistory,
        rng: Random = Random.Default
    ): GeneratedProposal? {
        val required = type.requiredPlayerCount
        if (eligible.size < required) return null

        // 1. Priority sort: fewer matchesPlayed first; ties by lastMatchEndedAt
        //    ascending, with "never played" (null) sorting first.
        val prioritized = eligible.sortedWith(
            compareBy(
                { it.matchesPlayed },
                { it.lastMatchEndedAt ?: Long.MIN_VALUE }
            )
        )

        // 2. Widen the pool slightly beyond the strict minimum and shuffle it,
        //    so near-equal-priority candidates get randomized.
        val poolSize = minOf(eligible.size, required + 2)
        val pool = prioritized.take(poolSize).shuffled(rng)

        // 3. Take the first `required` players from the shuffled pool.
        val chosen = pool.take(required).map { it.player }

        return when (type) {
            MatchType.SINGLES -> GeneratedProposal(
                type = type,
                sideA = listOf(chosen[0]),
                sideB = listOf(chosen[1])
            )
            MatchType.DOUBLES -> bestDoublesSplit(chosen, history)
        }
    }

    private fun bestDoublesSplit(chosen: List<PlayerRef>, history: PairingHistory): GeneratedProposal {
        val (p0, p1, p2, p3) = chosen

        val splits = listOf(
            listOf(p0, p1) to listOf(p2, p3),
            listOf(p0, p2) to listOf(p1, p3),
            listOf(p0, p3) to listOf(p1, p2)
        )

        val best = splits.minByOrNull { (sideA, sideB) -> pairingScore(sideA, sideB, history) }!!

        return GeneratedProposal(
            type = MatchType.DOUBLES,
            sideA = best.first,
            sideB = best.second
        )
    }

    private fun pairingScore(sideA: List<PlayerRef>, sideB: List<PlayerRef>, history: PairingHistory): Int {
        var score = 0
        if (setOf(sideA[0].id, sideA[1].id) in history.partnerPairs) score++
        if (setOf(sideB[0].id, sideB[1].id) in history.partnerPairs) score++
        for (a in sideA) {
            for (b in sideB) {
                if (setOf(a.id, b.id) in history.opponentPairs) score++
            }
        }
        return score
    }
}
