package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit tests verifying standard ABO and Rh compatibility rules
 * across all 64 donor/recipient blood group combinations.
 */
class BloodGroupCompatibilityTest {

    @Test
    fun verifyAll64CombinationsTruthTable() {
        val expectedCompatibilities = mapOf(
            // O- (Universal donor: can donate to everyone)
            (BloodGroup.O_NEGATIVE to BloodGroup.O_NEGATIVE) to true,
            (BloodGroup.O_NEGATIVE to BloodGroup.O_POSITIVE) to true,
            (BloodGroup.O_NEGATIVE to BloodGroup.A_NEGATIVE) to true,
            (BloodGroup.O_NEGATIVE to BloodGroup.A_POSITIVE) to true,
            (BloodGroup.O_NEGATIVE to BloodGroup.B_NEGATIVE) to true,
            (BloodGroup.O_NEGATIVE to BloodGroup.B_POSITIVE) to true,
            (BloodGroup.O_NEGATIVE to BloodGroup.AB_NEGATIVE) to true,
            (BloodGroup.O_NEGATIVE to BloodGroup.AB_POSITIVE) to true,

            // O+ (Can donate to O+, A+, B+, AB+)
            (BloodGroup.O_POSITIVE to BloodGroup.O_NEGATIVE) to false,
            (BloodGroup.O_POSITIVE to BloodGroup.O_POSITIVE) to true,
            (BloodGroup.O_POSITIVE to BloodGroup.A_NEGATIVE) to false,
            (BloodGroup.O_POSITIVE to BloodGroup.A_POSITIVE) to true,
            (BloodGroup.O_POSITIVE to BloodGroup.B_NEGATIVE) to false,
            (BloodGroup.O_POSITIVE to BloodGroup.B_POSITIVE) to true,
            (BloodGroup.O_POSITIVE to BloodGroup.AB_NEGATIVE) to false,
            (BloodGroup.O_POSITIVE to BloodGroup.AB_POSITIVE) to true,

            // A- (Can donate to A-, A+, AB-, AB+)
            (BloodGroup.A_NEGATIVE to BloodGroup.O_NEGATIVE) to false,
            (BloodGroup.A_NEGATIVE to BloodGroup.O_POSITIVE) to false,
            (BloodGroup.A_NEGATIVE to BloodGroup.A_NEGATIVE) to true,
            (BloodGroup.A_NEGATIVE to BloodGroup.A_POSITIVE) to true,
            (BloodGroup.A_NEGATIVE to BloodGroup.B_NEGATIVE) to false,
            (BloodGroup.A_NEGATIVE to BloodGroup.B_POSITIVE) to false,
            (BloodGroup.A_NEGATIVE to BloodGroup.AB_NEGATIVE) to true,
            (BloodGroup.A_NEGATIVE to BloodGroup.AB_POSITIVE) to true,

            // A+ (Can donate to A+, AB+)
            (BloodGroup.A_POSITIVE to BloodGroup.O_NEGATIVE) to false,
            (BloodGroup.A_POSITIVE to BloodGroup.O_POSITIVE) to false,
            (BloodGroup.A_POSITIVE to BloodGroup.A_NEGATIVE) to false,
            (BloodGroup.A_POSITIVE to BloodGroup.A_POSITIVE) to true,
            (BloodGroup.A_POSITIVE to BloodGroup.B_NEGATIVE) to false,
            (BloodGroup.A_POSITIVE to BloodGroup.B_POSITIVE) to false,
            (BloodGroup.A_POSITIVE to BloodGroup.AB_NEGATIVE) to false,
            (BloodGroup.A_POSITIVE to BloodGroup.AB_POSITIVE) to true,

            // B- (Can donate to B-, B+, AB-, AB+)
            (BloodGroup.B_NEGATIVE to BloodGroup.O_NEGATIVE) to false,
            (BloodGroup.B_NEGATIVE to BloodGroup.O_POSITIVE) to false,
            (BloodGroup.B_NEGATIVE to BloodGroup.A_NEGATIVE) to false,
            (BloodGroup.B_NEGATIVE to BloodGroup.A_POSITIVE) to false,
            (BloodGroup.B_NEGATIVE to BloodGroup.B_NEGATIVE) to true,
            (BloodGroup.B_NEGATIVE to BloodGroup.B_POSITIVE) to true,
            (BloodGroup.B_NEGATIVE to BloodGroup.AB_NEGATIVE) to true,
            (BloodGroup.B_NEGATIVE to BloodGroup.AB_POSITIVE) to true,

            // B+ (Can donate to B+, AB+)
            (BloodGroup.B_POSITIVE to BloodGroup.O_NEGATIVE) to false,
            (BloodGroup.B_POSITIVE to BloodGroup.O_POSITIVE) to false,
            (BloodGroup.B_POSITIVE to BloodGroup.A_NEGATIVE) to false,
            (BloodGroup.B_POSITIVE to BloodGroup.A_POSITIVE) to false,
            (BloodGroup.B_POSITIVE to BloodGroup.B_NEGATIVE) to false,
            (BloodGroup.B_POSITIVE to BloodGroup.B_POSITIVE) to true,
            (BloodGroup.B_POSITIVE to BloodGroup.AB_NEGATIVE) to false,
            (BloodGroup.B_POSITIVE to BloodGroup.AB_POSITIVE) to true,

            // AB- (Can donate to AB-, AB+)
            (BloodGroup.AB_NEGATIVE to BloodGroup.O_NEGATIVE) to false,
            (BloodGroup.AB_NEGATIVE to BloodGroup.O_POSITIVE) to false,
            (BloodGroup.AB_NEGATIVE to BloodGroup.A_NEGATIVE) to false,
            (BloodGroup.AB_NEGATIVE to BloodGroup.A_POSITIVE) to false,
            (BloodGroup.AB_NEGATIVE to BloodGroup.B_NEGATIVE) to false,
            (BloodGroup.AB_NEGATIVE to BloodGroup.B_POSITIVE) to false,
            (BloodGroup.AB_NEGATIVE to BloodGroup.AB_NEGATIVE) to true,
            (BloodGroup.AB_NEGATIVE to BloodGroup.AB_POSITIVE) to true,

            // AB+ (Can donate ONLY to AB+)
            (BloodGroup.AB_POSITIVE to BloodGroup.O_NEGATIVE) to false,
            (BloodGroup.AB_POSITIVE to BloodGroup.O_POSITIVE) to false,
            (BloodGroup.AB_POSITIVE to BloodGroup.A_NEGATIVE) to false,
            (BloodGroup.AB_POSITIVE to BloodGroup.A_POSITIVE) to false,
            (BloodGroup.AB_POSITIVE to BloodGroup.B_NEGATIVE) to false,
            (BloodGroup.AB_POSITIVE to BloodGroup.B_POSITIVE) to false,
            (BloodGroup.AB_POSITIVE to BloodGroup.AB_NEGATIVE) to false,
            (BloodGroup.AB_POSITIVE to BloodGroup.AB_POSITIVE) to true
        )

        var evaluatedCount = 0
        for (donor in BloodGroup.entries) {
            for (recipient in BloodGroup.entries) {
                val expected = expectedCompatibilities[donor to recipient] ?: false
                val actual = donor.canDonateTo(recipient)
                assertEquals("Compatibility failure for donor $donor to recipient $recipient", expected, actual)
                evaluatedCount++
            }
        }

        assertEquals(64, evaluatedCount)
    }

    @Test
    fun verifyCompatibleDonorGroups_returnsCorrectDonorsForRecipient() {
        // Universal recipient AB+ accepts all 8 donor groups
        val abPosDonors = BloodGroup.AB_POSITIVE.compatibleDonorGroups()
        assertEquals(8, abPosDonors.size)

        // O- recipient accepts ONLY O- donors
        val oNegDonors = BloodGroup.O_NEGATIVE.compatibleDonorGroups()
        assertEquals(listOf(BloodGroup.O_NEGATIVE), oNegDonors)

        // A+ recipient accepts O-, O+, A-, A+
        val aPosDonors = BloodGroup.A_POSITIVE.compatibleDonorGroups()
        assertEquals(
            listOf(BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE).sortedBy { it.name },
            aPosDonors.sortedBy { it.name }
        )
    }
}
