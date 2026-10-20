package com.bloodconnect.data.model

import java.util.concurrent.TimeUnit

/**
 * Utility object for computing active donation streaks.
 */
object DonationStreak {
    private val MAX_GAP_MILLIS = TimeUnit.DAYS.toMillis(120)

    /**
     * Computes the donation streak from a list of donation timestamps ([donatedAtMillis]).
     * Sorts timestamps ascending; calculates consecutive donations counted back from the most recent one
     * where each gap to the previous donation is at most 120 days.
     * Returns 0 for an empty list, 1 for a single donation.
     */
    fun compute(donatedAtMillis: List<Long>): Int {
        if (donatedAtMillis.isEmpty()) return 0
        val sorted = donatedAtMillis.sorted()
        if (sorted.size == 1) return 1

        var streak = 1
        // Count backwards from the most recent donation
        for (i in sorted.size - 1 downTo 1) {
            val current = sorted[i]
            val previous = sorted[i - 1]
            val gap = current - previous
            if (gap <= MAX_GAP_MILLIS) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
}
