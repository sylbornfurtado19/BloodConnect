package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Badge threshold evaluation ([Badge.earnedBadges]).
 */
class BadgeTest {

    @Test
    fun earnedBadges_count0_streak0_returnsEmpty() {
        val badges = Badge.earnedBadges(0, 0)
        assertTrue(badges.isEmpty())
    }

    @Test
    fun earnedBadges_count1_returnsFirstDrop() {
        val badges = Badge.earnedBadges(1, 0)
        assertEquals(1, badges.size)
        assertTrue(badges.contains(Badge.FIRST_DROP))
    }

    @Test
    fun earnedBadges_count3_returnsHelpingHand() {
        val badges = Badge.earnedBadges(3, 0)
        assertTrue(badges.contains(Badge.FIRST_DROP))
        assertTrue(badges.contains(Badge.HELPING_HAND))
        assertFalse(badges.contains(Badge.LIFESAVER))
    }

    @Test
    fun earnedBadges_count5_returnsLifesaver() {
        val badges = Badge.earnedBadges(5, 0)
        assertTrue(badges.contains(Badge.LIFESAVER))
    }

    @Test
    fun earnedBadges_count10_returnsHero() {
        val badges = Badge.earnedBadges(10, 0)
        assertTrue(badges.contains(Badge.HERO))
    }

    @Test
    fun earnedBadges_count25_returnsLegend() {
        val badges = Badge.earnedBadges(25, 0)
        assertTrue(badges.contains(Badge.LEGEND))
    }

    @Test
    fun earnedBadges_streak3_returnsOnARoll() {
        val badges = Badge.earnedBadges(0, 3)
        assertEquals(1, badges.size)
        assertTrue(badges.contains(Badge.ON_A_ROLL))
    }
}
