package com.bloodconnect.ui.search

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Donor
import com.bloodconnect.utils.UserLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for donor search discovery, filtering, and proximity sorting behavior.
 * Verifies business requirements:
 * - Blood group filtering (matching vs all)
 * - Locality text query filtering (case-insensitive substring)
 * - Proximity radius threshold filtering
 * - Graceful handling of donors with missing geographic coordinates
 * - Nearest-first and farthest-first distance sorting
 */
class DonorFilterTest {

    // Reference device coordinates: Mumbai center (19.0760, 72.8777)
    private val testUserLocation = UserLocation(latitude = 19.0760, longitude = 72.8777)

    private val donorClose = Donor(
        id = "D1",
        name = "Donor Close (4km)",
        bloodGroup = BloodGroup.O_POSITIVE,
        locality = "Central City District",
        latitude = 19.0728,
        longitude = 72.8826,
        isAvailable = true
    )

    private val donorMedium = Donor(
        id = "D2",
        name = "Donor Medium (12km)",
        bloodGroup = BloodGroup.A_POSITIVE,
        locality = "Northside Civic Zone",
        latitude = 19.1663,
        longitude = 72.8526,
        isAvailable = true
    )

    private val donorFar = Donor(
        id = "D3",
        name = "Donor Far (54km)",
        bloodGroup = BloodGroup.O_POSITIVE,
        locality = "Highland Suburban Area",
        latitude = 19.4500,
        longitude = 73.2000,
        isAvailable = true
    )

    private val donorMissingCoords = Donor(
        id = "D4",
        name = "Donor No Coords",
        bloodGroup = BloodGroup.B_POSITIVE,
        locality = "University Campus Area",
        latitude = null,
        longitude = null,
        isAvailable = true
    )

    private val testDonors = listOf(donorClose, donorMedium, donorFar, donorMissingCoords)

    // -------------------------------------------------------------
    // Blood Group Filter Tests
    // -------------------------------------------------------------

    @Test
    fun filter_byBloodGroup_returnsOnlyMatchingDonors() {
        val (filtered, _) = filterAndSortDonors(
            donors = testDonors,
            bloodGroup = BloodGroup.O_POSITIVE
        )

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.bloodGroup == BloodGroup.O_POSITIVE })
        assertTrue(filtered.any { it.id == "D1" })
        assertTrue(filtered.any { it.id == "D3" })
        assertFalse(filtered.any { it.id == "D2" })
    }

    @Test
    fun filter_nullBloodGroup_returnsAllBloodGroups() {
        val (filtered, _) = filterAndSortDonors(
            donors = testDonors,
            bloodGroup = null
        )

        assertEquals(4, filtered.size)
    }

    // -------------------------------------------------------------
    // Locality Filter Tests
    // -------------------------------------------------------------

    @Test
    fun filter_byLocalityQuery_caseInsensitiveSubstringMatch() {
        val (filtered, _) = filterAndSortDonors(
            donors = testDonors,
            locationQuery = "central"
        )

        assertEquals(1, filtered.size)
        assertEquals("D1", filtered.first().id)
    }

    @Test
    fun filter_byLocalityQuery_trimsWhitespace() {
        val (filtered, _) = filterAndSortDonors(
            donors = testDonors,
            locationQuery = "   Northside   "
        )

        assertEquals(1, filtered.size)
        assertEquals("D2", filtered.first().id)
    }

    @Test
    fun filter_blankLocalityQuery_matchesAllLocalities() {
        val (filtered, _) = filterAndSortDonors(
            donors = testDonors,
            locationQuery = ""
        )

        assertEquals(4, filtered.size)
    }

    @Test
    fun filter_nonMatchingLocalityQuery_returnsEmptyList() {
        val (filtered, _) = filterAndSortDonors(
            donors = testDonors,
            locationQuery = "NonExistentPlace"
        )

        assertTrue(filtered.isEmpty())
    }

    // -------------------------------------------------------------
    // Radius / Distance Filter Tests
    // -------------------------------------------------------------

    @Test
    fun filter_byRadius_excludesDonorsBeyondThreshold() {
        // Radius of 10 km should include donorClose (~4 km), but exclude donorMedium (~12 km) and donorFar (~35 km)
        val (filtered, distances) = filterAndSortDonors(
            donors = testDonors,
            maxDistanceKm = 10,
            userLocation = testUserLocation
        )

        // Missing coordinate donor is retained gracefully, donorClose is retained
        assertTrue(filtered.any { it.id == "D1" })
        assertFalse(filtered.any { it.id == "D2" })
        assertFalse(filtered.any { it.id == "D3" })

        // Donor with coordinates within 10 km has a computed distance <= 10
        val d1Dist = distances["D1"]
        assertTrue(d1Dist != null && d1Dist <= 10.0)
    }

    @Test
    fun filter_byRadius_expandsWhenThresholdIncreases() {
        // Radius of 25 km should include donorClose and donorMedium, but exclude donorFar (~35 km)
        val (filtered, distances) = filterAndSortDonors(
            donors = testDonors,
            maxDistanceKm = 25,
            userLocation = testUserLocation
        )

        assertTrue(filtered.any { it.id == "D1" })
        assertTrue(filtered.any { it.id == "D2" })
        assertFalse(filtered.any { it.id == "D3" })

        assertTrue(distances.containsKey("D1"))
        assertTrue(distances.containsKey("D2"))
    }

    @Test
    fun filter_whenUserLocationIsNull_distanceFilterIsDisabled() {
        // When location is unavailable (permission denied or no GPS fix),
        // all donors matching blood group/locality pass through without distance exclusion
        val (filtered, distances) = filterAndSortDonors(
            donors = testDonors,
            maxDistanceKm = 5,
            userLocation = null
        )

        assertEquals(4, filtered.size)
        assertTrue(distances.isEmpty())
    }

    @Test
    fun filter_missingDonorCoordinates_handledGracefullyWithoutCrashing() {
        val (filtered, distances) = filterAndSortDonors(
            donors = listOf(donorMissingCoords),
            userLocation = testUserLocation
        )

        // Must retain donor without throwing NullPointerException
        assertEquals(1, filtered.size)
        assertEquals("D4", filtered.first().id)

        // Distance map must NOT claim a fake or fabricated distance for missing coordinates
        assertNull(distances["D4"])
    }

    // -------------------------------------------------------------
    // Nearest-First Sorting Tests
    // -------------------------------------------------------------

    @Test
    fun sort_nearestFirst_ordersAscendingByDistance_nullsAtEnd() {
        val (sorted, distances) = filterAndSortDonors(
            donors = testDonors,
            sortOrder = DistanceSortOrder.NEAREST,
            userLocation = testUserLocation,
            maxDistanceKm = 100
        )

        // Donors with coordinates should be in ascending order: D1 (< 10km) -> D2 (~12km) -> D3 (~54km)
        val ids = sorted.map { it.id }
        assertEquals("D1", ids[0])
        assertEquals("D2", ids[1])
        assertEquals("D3", ids[2])

        // Donor with missing coordinates (null distance) must be placed at the end
        assertEquals("D4", ids[3])

        val d1Dist = distances["D1"]!!
        val d2Dist = distances["D2"]!!
        val d3Dist = distances["D3"]!!
        assertTrue(d1Dist < d2Dist)
        assertTrue(d2Dist < d3Dist)
    }

    @Test
    fun sort_nearestFirst_ordersDonorsByAscendingDistance() {
        val unsortedDonors = listOf(donorFar, donorClose, donorMedium)
        val (sorted, distances) = filterAndSortDonors(
            donors = unsortedDonors,
            sortOrder = DistanceSortOrder.NEAREST,
            userLocation = testUserLocation,
            maxDistanceKm = 100
        )

        // Verifies nearest-first sorting orders donors strictly by ascending distance
        assertEquals(listOf("D1", "D2", "D3"), sorted.map { it.id })

        val d1Dist = distances["D1"]!!
        val d2Dist = distances["D2"]!!
        val d3Dist = distances["D3"]!!
        assertTrue(d1Dist <= d2Dist)
        assertTrue(d2Dist <= d3Dist)
    }

    // -------------------------------------------------------------
    // Farthest-First Sorting Tests
    // -------------------------------------------------------------

    @Test
    fun sort_farthestFirst_ordersDescendingByDistance_nullsAtEnd() {
        val (sorted, distances) = filterAndSortDonors(
            donors = testDonors,
            sortOrder = DistanceSortOrder.FARTHEST,
            userLocation = testUserLocation,
            maxDistanceKm = 100
        )

        // Donors with coordinates should be in descending order: D3 (~35km) -> D2 (~12km) -> D1 (< 10km)
        val ids = sorted.map { it.id }
        assertEquals("D3", ids[0])
        assertEquals("D2", ids[1])
        assertEquals("D1", ids[2])

        // Donor with missing coordinates (null distance) must be placed at the end
        assertEquals("D4", ids[3])

        val d1Dist = distances["D1"]!!
        val d2Dist = distances["D2"]!!
        val d3Dist = distances["D3"]!!
        assertTrue(d3Dist > d2Dist)
        assertTrue(d2Dist > d1Dist)
    }

    // -------------------------------------------------------------
    // Combined Filter & Sort Tests
    // -------------------------------------------------------------

    @Test
    fun filter_combinedBloodGroupLocalityAndRadius() {
        val (filtered, _) = filterAndSortDonors(
            donors = testDonors,
            bloodGroup = BloodGroup.O_POSITIVE,
            locationQuery = "Central",
            maxDistanceKm = 10,
            userLocation = testUserLocation
        )

        assertEquals(1, filtered.size)
        assertEquals("D1", filtered.first().id)
    }
}
