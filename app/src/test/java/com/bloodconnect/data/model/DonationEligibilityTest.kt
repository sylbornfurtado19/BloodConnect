package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests verifying blood donation 90-day cooldown eligibility rules and boundaries.
 */
class DonationEligibilityTest {

    @Test
    fun isEligible_nullLastDonationAt_returnsTrue() {
        assertTrue(DonationEligibility.isEligible(null))
        assertNull(DonationEligibility.eligibleOn(null))
    }

    @Test
    fun isEligible_89DaysAgo_returnsFalse() {
        val now = 1700000000000L
        val eightyNineDaysAgo = now - TimeUnit.DAYS.toMillis(89)

        assertFalse(DonationEligibility.isEligible(eightyNineDaysAgo, now))
    }

    @Test
    fun isEligible_90DaysAgo_exactBoundary_returnsTrue() {
        val now = 1700000000000L
        val ninetyDaysAgo = now - TimeUnit.DAYS.toMillis(90)

        assertTrue(DonationEligibility.isEligible(ninetyDaysAgo, now))
    }

    @Test
    fun isEligible_91DaysAgo_returnsTrue() {
        val now = 1700000000000L
        val ninetyOneDaysAgo = now - TimeUnit.DAYS.toMillis(91)

        assertTrue(DonationEligibility.isEligible(ninetyOneDaysAgo, now))
    }

    @Test
    fun eligibleOn_returnsCorrectTimestampWhenInCooldown() {
        val lastDonation = 1000000000000L
        val expectedEligibleDate = lastDonation + TimeUnit.DAYS.toMillis(90)

        val actualEligibleDate = DonationEligibility.eligibleOn(lastDonation, now = lastDonation + 1000L)
        assertEquals(expectedEligibleDate, actualEligibleDate)
    }
}
