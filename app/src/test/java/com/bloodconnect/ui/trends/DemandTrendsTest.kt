package com.bloodconnect.ui.trends

import com.bloodconnect.data.model.BloodRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemandTrendsTest {

    private val now = 1_700_000_000_000L
    private val dayMillis = 24L * 60L * 60L * 1000L

    @Test
    fun computeTrends_emptyList_returnsEightGroupsWithZeros() {
        val trends = computeTrends(emptyList(), now)
        assertEquals(8, trends.size)
        assertEquals(STANDARD_BLOOD_GROUPS, trends.map { it.group })
        trends.forEach { trend ->
            assertEquals(0, trend.last7)
            assertEquals(0, trend.previous7)
            assertEquals(Direction.STABLE, trend.direction)
        }
    }

    @Test
    fun computeTrends_rising_identifiedCorrectly() {
        // Last 7 days: 10 units of A+. Previous 7 days: 5 units of A+.
        // 10 >= 5 * 1.25 (6.25) and (10 - 5) >= 2 => RISING
        val reqs = listOf(
            BloodRequest(bloodGroup = "A+", unitsNeeded = 10, createdAt = now - 2 * dayMillis),
            BloodRequest(bloodGroup = "A+", unitsNeeded = 5, createdAt = now - 10 * dayMillis)
        )
        val trends = computeTrends(reqs, now)
        val aPlus = trends.first { it.group == "A+" }
        assertEquals(10, aPlus.last7)
        assertEquals(5, aPlus.previous7)
        assertEquals(Direction.RISING, aPlus.direction)
    }

    @Test
    fun computeTrends_falling_identifiedCorrectly() {
        // Last 7 days: 2 units of O-. Previous 7 days: 8 units of O-.
        // 2 <= 8 * 0.75 (6.0) and (8 - 2) >= 2 => FALLING
        val reqs = listOf(
            BloodRequest(bloodGroup = "O-", unitsNeeded = 2, createdAt = now - 3 * dayMillis),
            BloodRequest(bloodGroup = "O-", unitsNeeded = 8, createdAt = now - 9 * dayMillis)
        )
        val trends = computeTrends(reqs, now)
        val oMinus = trends.first { it.group == "O-" }
        assertEquals(2, oMinus.last7)
        assertEquals(8, oMinus.previous7)
        assertEquals(Direction.FALLING, oMinus.direction)
    }

    @Test
    fun computeTrends_stable_whenWithinThreshold() {
        // Last 7 days: 10 units. Previous 7 days: 10 units. => STABLE
        val reqs = listOf(
            BloodRequest(bloodGroup = "B+", unitsNeeded = 10, createdAt = now - 1 * dayMillis),
            BloodRequest(bloodGroup = "B+", unitsNeeded = 10, createdAt = now - 8 * dayMillis)
        )
        val trends = computeTrends(reqs, now)
        val bPlus = trends.first { it.group == "B+" }
        assertEquals(10, bPlus.last7)
        assertEquals(10, bPlus.previous7)
        assertEquals(Direction.STABLE, bPlus.direction)
    }

    @Test
    fun computeTrends_tinyChanges_stayStable() {
        // Change of +1 (e.g. 0 to 1, or 5 to 6) should stay STABLE because diff < 2
        val reqs = listOf(
            BloodRequest(bloodGroup = "AB+", unitsNeeded = 1, createdAt = now - 2 * dayMillis)
        )
        val trends = computeTrends(reqs, now)
        val abPlus = trends.first { it.group == "AB+" }
        assertEquals(1, abPlus.last7)
        assertEquals(0, abPlus.previous7)
        assertEquals(Direction.STABLE, abPlus.direction)

        // Previous 7: 3 units. Last 7: 4 units. Diff is 1 (< 2) => STABLE
        val reqs2 = listOf(
            BloodRequest(bloodGroup = "AB-", unitsNeeded = 4, createdAt = now - 3 * dayMillis),
            BloodRequest(bloodGroup = "AB-", unitsNeeded = 3, createdAt = now - 11 * dayMillis)
        )
        val trends2 = computeTrends(reqs2, now)
        val abMinus = trends2.first { it.group == "AB-" }
        assertEquals(4, abMinus.last7)
        assertEquals(3, abMinus.previous7)
        assertEquals(Direction.STABLE, abMinus.direction)
    }

    @Test
    fun computeTrends_requestsOlderThanFourteenDays_ignored() {
        // Request from 15 days ago must be completely ignored
        val reqs = listOf(
            BloodRequest(bloodGroup = "O+", unitsNeeded = 5, createdAt = now - 1 * dayMillis),
            BloodRequest(bloodGroup = "O+", unitsNeeded = 15, createdAt = now - 16 * dayMillis)
        )
        val trends = computeTrends(reqs, now)
        val oPlus = trends.first { it.group == "O+" }
        assertEquals(5, oPlus.last7)
        assertEquals(0, oPlus.previous7)
        assertEquals(Direction.RISING, oPlus.direction)
    }
}
