package com.bloodconnect.ui.search

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Donor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DonorScorer].
 *
 * Note: Response history is not available yet, so it is not part of the score.
 */
class DonorScorerTest {

    private val now = 1_700_000_000_000L // arbitrary timestamp
    private val ninetyOneDaysAgo = now - (91L * 24 * 60 * 60 * 1000)
    private val tenDaysAgo = now - (10L * 24 * 60 * 60 * 1000)

    private fun createTestDonor(
        id: String = "donor-1",
        name: String = "Test Donor",
        bloodGroup: BloodGroup = BloodGroup.O_POSITIVE,
        locality: String = "Central",
        latitude: Double? = 12.9716,
        longitude: Double? = 77.5946,
        isAvailable: Boolean = true,
        lastDonationAt: Long? = ninetyOneDaysAgo
    ): Donor {
        return Donor(
            id = id,
            name = name,
            bloodGroup = bloodGroup,
            locality = locality,
            latitude = latitude,
            longitude = longitude,
            isAvailable = isAvailable,
            lastDonationAt = lastDonationAt
        )
    }

    // -----------------------------------------------------------------
    // 1. Nearer beats farther
    // -----------------------------------------------------------------
    @Test
    fun nearerBeatsFarther() {
        val donorNear = createTestDonor(id = "near")
        val donorFar = createTestDonor(id = "far")

        val scoreNear = DonorScorer.score(
            donor = donorNear,
            requested = BloodGroup.O_POSITIVE,
            distanceKm = 5.0,
            maxDistanceKm = 50,
            now = now
        )

        val scoreFar = DonorScorer.score(
            donor = donorFar,
            requested = BloodGroup.O_POSITIVE,
            distanceKm = 45.0,
            maxDistanceKm = 50,
            now = now
        )

        assertTrue("Nearer donor ($scoreNear) should score higher than farther donor ($scoreFar)", scoreNear > scoreFar)
    }

    // -----------------------------------------------------------------
    // 2. Available beats unavailable
    // -----------------------------------------------------------------
    @Test
    fun availableBeatsUnavailable() {
        val availableDonor = createTestDonor(id = "avail", isAvailable = true)
        val unavailableDonor = createTestDonor(id = "unavail", isAvailable = false)

        val scoreAvailable = DonorScorer.score(
            donor = availableDonor,
            requested = BloodGroup.O_POSITIVE,
            distanceKm = 10.0,
            maxDistanceKm = 50,
            now = now
        )

        val scoreUnavailable = DonorScorer.score(
            donor = unavailableDonor,
            requested = BloodGroup.O_POSITIVE,
            distanceKm = 10.0,
            maxDistanceKm = 50,
            now = now
        )

        assertTrue(
            "Available donor ($scoreAvailable) should score higher than unavailable donor ($scoreUnavailable)",
            scoreAvailable > scoreUnavailable
        )
    }

    // -----------------------------------------------------------------
    // 3. Exact group beats compatible
    // -----------------------------------------------------------------
    @Test
    fun exactGroupBeatsCompatible() {
        // Requested group: A+.
        // Donor with A+ is exact match (bloodMatch = 1.0).
        // Donor with O+ can donate to A+, so is compatible (bloodMatch = 0.6).
        val exactDonor = createTestDonor(id = "exact", bloodGroup = BloodGroup.A_POSITIVE)
        val compatibleDonor = createTestDonor(id = "compatible", bloodGroup = BloodGroup.O_POSITIVE)

        val scoreExact = DonorScorer.score(
            donor = exactDonor,
            requested = BloodGroup.A_POSITIVE,
            distanceKm = 10.0,
            maxDistanceKm = 50,
            now = now
        )

        val scoreCompatible = DonorScorer.score(
            donor = compatibleDonor,
            requested = BloodGroup.A_POSITIVE,
            distanceKm = 10.0,
            maxDistanceKm = 50,
            now = now
        )

        assertTrue(
            "Exact blood group ($scoreExact) should score higher than merely compatible group ($scoreCompatible)",
            scoreExact > scoreCompatible
        )
    }

    // -----------------------------------------------------------------
    // 4. Cooldown lowers the score
    // -----------------------------------------------------------------
    @Test
    fun cooldownLowersScore() {
        val eligibleDonor = createTestDonor(id = "eligible", lastDonationAt = ninetyOneDaysAgo)
        val cooldownDonor = createTestDonor(id = "cooldown", lastDonationAt = tenDaysAgo)

        val scoreEligible = DonorScorer.score(
            donor = eligibleDonor,
            requested = BloodGroup.O_POSITIVE,
            distanceKm = 10.0,
            maxDistanceKm = 50,
            now = now
        )

        val scoreCooldown = DonorScorer.score(
            donor = cooldownDonor,
            requested = BloodGroup.O_POSITIVE,
            distanceKm = 10.0,
            maxDistanceKm = 50,
            now = now
        )

        assertTrue(
            "Eligible donor ($scoreEligible) should score higher than cooldown donor ($scoreCooldown)",
            scoreEligible > scoreCooldown
        )
    }

    // -----------------------------------------------------------------
    // 5. Unknown distance does not crash
    // -----------------------------------------------------------------
    @Test
    fun unknownDistanceDoesNotCrash() {
        val donorUnknownDist = createTestDonor(id = "unknown-dist")

        val score = DonorScorer.score(
            donor = donorUnknownDist,
            requested = BloodGroup.O_POSITIVE,
            distanceKm = null,
            maxDistanceKm = 50,
            now = now
        )

        assertTrue("Score for unknown distance should be between 0 and 1", score in 0.0..1.0)
        // Check calculation:
        // distanceScore = 0.3 * 0.45 = 0.135
        // availability = 1.0 * 0.25 = 0.25
        // bloodMatch = 1.0 * 0.20 = 0.20
        // eligibility = 1.0 * 0.10 = 0.10
        // total = 0.685
        assertEquals(0.685, score, 0.001)
    }

    // -----------------------------------------------------------------
    // 6. All scores are between 0 and 1
    // -----------------------------------------------------------------
    @Test
    fun allScoresBetweenZeroAndOne() {
        val testCases = listOf(
            // Perfect donor: distance 0km, available, exact match, eligible -> 1.0
            createTestDonor(bloodGroup = BloodGroup.O_POSITIVE, isAvailable = true, lastDonationAt = null) to 0.0,
            // Worst donor: distance beyond max, unavailable, incompatible, in cooldown -> 0.0
            createTestDonor(bloodGroup = BloodGroup.B_POSITIVE, isAvailable = false, lastDonationAt = tenDaysAgo) to 100.0,
            // Mid donors: unknown distance, compatible
            createTestDonor(bloodGroup = BloodGroup.O_NEGATIVE, isAvailable = true, lastDonationAt = tenDaysAgo) to null,
            createTestDonor(bloodGroup = BloodGroup.AB_POSITIVE, isAvailable = false, lastDonationAt = ninetyOneDaysAgo) to 25.0
        )

        for ((donor, dist) in testCases) {
            val scoreWithReq = DonorScorer.score(
                donor = donor,
                requested = BloodGroup.A_POSITIVE,
                distanceKm = dist,
                maxDistanceKm = 50,
                now = now
            )
            assertTrue("Score ($scoreWithReq) should be within [0.0, 1.0]", scoreWithReq in 0.0..1.0)

            val scoreNoReq = DonorScorer.score(
                donor = donor,
                requested = null,
                distanceKm = dist,
                maxDistanceKm = 50,
                now = now
            )
            assertTrue("Score with no requested group ($scoreNoReq) should be within [0.0, 1.0]", scoreNoReq in 0.0..1.0)
        }
    }
}
