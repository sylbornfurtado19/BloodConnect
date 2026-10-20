package com.bloodconnect.data.model

import java.util.concurrent.TimeUnit

/**
 * Utility object for calculating blood donation cooldown and eligibility.
 */
object DonationEligibility {
    const val COOLDOWN_DAYS = 90L
    private val COOLDOWN_MILLIS = TimeUnit.DAYS.toMillis(COOLDOWN_DAYS)

    /**
     * Determines whether a donor is eligible to donate based on their [lastDonationAt] timestamp and [now].
     * Returns true if [lastDonationAt] is null or if at least [COOLDOWN_DAYS] (90 days) have elapsed.
     */
    fun isEligible(lastDonationAt: Long?, now: Long = System.currentTimeMillis()): Boolean {
        if (lastDonationAt == null) return true
        val elapsed = now - lastDonationAt
        return elapsed >= COOLDOWN_MILLIS
    }

    /**
     * Returns the epoch millisecond timestamp when the donor will become eligible to donate again,
     * or null if the donor is already eligible or has no recorded last donation.
     */
    fun eligibleOn(lastDonationAt: Long?, now: Long = System.currentTimeMillis()): Long? {
        if (lastDonationAt == null) return null
        val nextEligible = lastDonationAt + COOLDOWN_MILLIS
        return if (now >= nextEligible) null else nextEligible
    }
}
