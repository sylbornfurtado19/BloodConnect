package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests verifying [DonationStreak.compute] logic across various donation timestamp sequences.
 */
class DonationStreakTest {

    @Test
    fun compute_emptyList_returnsZero() {
        assertEquals(0, DonationStreak.compute(emptyList()))
    }

    @Test
    fun compute_singleDonation_returnsOne() {
        assertEquals(1, DonationStreak.compute(listOf(1000L)))
    }

    @Test
    fun compute_gapWithin120Days_accumulatesStreak() {
        val t1 = 1000000000000L
        val t2 = t1 + TimeUnit.DAYS.toMillis(100)
        val t3 = t2 + TimeUnit.DAYS.toMillis(100)

        // 3 consecutive donations with 100-day gaps (<= 120 days)
        assertEquals(3, DonationStreak.compute(listOf(t1, t2, t3)))
    }

    @Test
    fun compute_gapExceeding120Days_breaksStreak() {
        val t1 = 1000000000000L
        val t2 = t1 + TimeUnit.DAYS.toMillis(130) // > 120 days gap
        val t3 = t2 + TimeUnit.DAYS.toMillis(10)

        // Only t2 and t3 are within 120 days of each other (streak of 2)
        assertEquals(2, DonationStreak.compute(listOf(t1, t2, t3)))
    }

    @Test
    fun compute_unsortedInput_sortsAndComputesCorrectly() {
        val t1 = 1000000000000L
        val t2 = t1 + TimeUnit.DAYS.toMillis(50)
        val t3 = t1 + TimeUnit.DAYS.toMillis(100)

        // Unsorted input list
        assertEquals(3, DonationStreak.compute(listOf(t3, t1, t2)))
    }
}
