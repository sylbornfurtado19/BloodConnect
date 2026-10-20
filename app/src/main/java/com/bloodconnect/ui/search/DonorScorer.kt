package com.bloodconnect.ui.search

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.DonationEligibility
import com.bloodconnect.data.model.Donor

/**
 * Intelligent donor ranking and scoring utility.
 *
 * Computes a weighted match score between 0.0 and 1.0 based on:
 * - Distance proximity (0.45 weight): 1 - distance/maxDistance clamped to 0..1 (0.3 when distance is unknown)
 * - Donor availability (0.25 weight): 1.0 if available, else 0.0
 * - Blood group match (0.20 weight): 1.0 for exact group, 0.6 for compatible group, 1.0 when no group was requested
 * - Donation cooldown eligibility (0.10 weight): 1.0 if eligible, else 0.0
 *
 * Note: Response history is not available yet, so it is not part of the score.
 */
object DonorScorer {

    /**
     * Calculates the smart ranking score for [donor] given the search criteria.
     *
     * @param donor The donor record to score.
     * @param requested The requested recipient blood group, or null if any blood group is acceptable.
     * @param distanceKm Proximity distance in kilometers from user location, or null if coordinates are unknown.
     * @param maxDistanceKm Maximum search radius in kilometers.
     * @param now Current timestamp in epoch milliseconds for cooldown evaluation.
     * @return Normalized score between 0.0 and 1.0.
     */
    fun score(
        donor: Donor,
        requested: BloodGroup?,
        distanceKm: Double?,
        maxDistanceKm: Int,
        now: Long
    ): Double {
        // Distance score (0.45 weight)
        val distanceScore = if (distanceKm == null) {
            0.3
        } else {
            val maxDist = if (maxDistanceKm <= 0) 50.0 else maxDistanceKm.toDouble()
            (1.0 - (distanceKm / maxDist)).coerceIn(0.0, 1.0)
        }

        // Availability score (0.25 weight)
        val availabilityScore = if (donor.isAvailable) 1.0 else 0.0

        // Blood match score (0.20 weight)
        val bloodMatchScore = when {
            requested == null -> 1.0
            donor.bloodGroup == requested -> 1.0
            donor.bloodGroup.canDonateTo(requested) -> 0.6
            else -> 0.0
        }

        // Eligibility score (0.10 weight)
        val eligibilityScore = if (DonationEligibility.isEligible(donor.lastDonationAt, now)) 1.0 else 0.0

        val totalScore = (0.45 * distanceScore) +
                (0.25 * availabilityScore) +
                (0.20 * bloodMatchScore) +
                (0.10 * eligibilityScore)

        return totalScore.coerceIn(0.0, 1.0)
    }
}
