package com.smallkang.ankirsi

import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * FSRS-6 scheduler (github.com/open-spaced-repetition/py-fsrs) with the default parameters.
 * Grades: 1 again, 2 hard, 3 good, 4 easy.
 */
object Fsrs {
    private val w = doubleArrayOf(
        0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
        1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
    )
    private const val RETENTION = 0.9 // desired probability of recall when a card comes due
    private val decay = -w[20]
    private val factor = 0.9.pow(1 / decay) - 1

    fun review(card: Card, grade: Int, now: Long = System.currentTimeMillis()): Card {
        val today = studyDay(now)
        val elapsed = today - studyDay(card.lastReview)
        val stability = when {
            card.isNew -> w[grade - 1]
            elapsed <= 0 -> shortTermStability(card.stability, grade)
            else -> nextStability(card, grade, retrievability(elapsed, card.stability))
        }.coerceAtLeast(0.001)
        val difficulty = if (card.isNew) initialDifficulty(grade) else nextDifficulty(card.difficulty, grade)
        // A failed card stays due today and comes back after the rest of the session.
        val days = if (grade == 1) 0L else intervalDays(stability)
        return card.copy(
            due = today + days,
            firstReview = if (card.isNew) now else card.firstReview,
            lastReview = now,
            stability = stability,
            difficulty = difficulty.coerceIn(1.0, 10.0),
        )
    }

    private fun intervalDays(s: Double) =
        (s / factor * (RETENTION.pow(1 / decay) - 1)).roundToLong().coerceIn(1, 36500)

    private fun retrievability(days: Long, s: Double) = (1 + factor * days / s).pow(decay)

    private fun initialDifficulty(g: Int) = w[4] - exp(w[5] * (g - 1)) + 1

    private fun nextDifficulty(d: Double, g: Int): Double {
        val damped = d - w[6] * (g - 3) * (10 - d) / 9
        return w[7] * initialDifficulty(4) + (1 - w[7]) * damped
    }

    private fun shortTermStability(s: Double, g: Int): Double {
        val growth = exp(w[17] * (g - 3 + w[18])) * s.pow(-w[19])
        return s * if (g > 1) growth.coerceAtLeast(1.0) else growth
    }

    private fun nextStability(card: Card, g: Int, r: Double): Double = with(card) {
        if (g == 1) {
            min(
                w[11] * difficulty.pow(-w[12]) * ((stability + 1).pow(w[13]) - 1) * exp((1 - r) * w[14]),
                stability / exp(w[17] * w[18]),
            )
        } else {
            val bonus = when (g) { 2 -> w[15]; 4 -> w[16]; else -> 1.0 }
            stability * (1 + exp(w[8]) * (11 - difficulty) * stability.pow(-w[9]) * (exp((1 - r) * w[10]) - 1) * bonus)
        }
    }
}
