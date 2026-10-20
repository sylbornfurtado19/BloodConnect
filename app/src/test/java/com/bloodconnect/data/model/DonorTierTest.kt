package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying [DonorTier] threshold resolution and next-tier progress boundaries.
 */
class DonorTierTest {

    @Test
    fun fromCount_boundary0_returnsBronze() {
        assertEquals(DonorTier.BRONZE, DonorTier.fromCount(0))
        assertEquals(3, DonorTier.nextThreshold(0))
    }

    @Test
    fun fromCount_boundary2_returnsBronze() {
        assertEquals(DonorTier.BRONZE, DonorTier.fromCount(2))
        assertEquals(3, DonorTier.nextThreshold(2))
    }

    @Test
    fun fromCount_boundary3_returnsSilver() {
        assertEquals(DonorTier.SILVER, DonorTier.fromCount(3))
        assertEquals(6, DonorTier.nextThreshold(3))
    }

    @Test
    fun fromCount_boundary5_returnsSilver() {
        assertEquals(DonorTier.SILVER, DonorTier.fromCount(5))
        assertEquals(6, DonorTier.nextThreshold(5))
    }

    @Test
    fun fromCount_boundary6_returnsGold() {
        assertEquals(DonorTier.GOLD, DonorTier.fromCount(6))
        assertEquals(10, DonorTier.nextThreshold(6))
    }

    @Test
    fun fromCount_boundary9_returnsGold() {
        assertEquals(DonorTier.GOLD, DonorTier.fromCount(9))
        assertEquals(10, DonorTier.nextThreshold(9))
    }

    @Test
    fun fromCount_boundary10_returnsPlatinum() {
        assertEquals(DonorTier.PLATINUM, DonorTier.fromCount(10))
        assertNull(DonorTier.nextThreshold(10))
    }

    @Test
    fun fromCount_exceedingPlatinum_returnsPlatinum() {
        assertEquals(DonorTier.PLATINUM, DonorTier.fromCount(25))
        assertNull(DonorTier.nextThreshold(25))
    }
}
