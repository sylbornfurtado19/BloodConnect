package com.bloodconnect.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [NotifiedRequests] CSV tracking and de-duplication logic.
 */
class NotifiedRequestsTest {

    @Test
    fun contains_emptyString_returnsFalse() {
        assertFalse(NotifiedRequests.contains("", "req1"))
        assertFalse(NotifiedRequests.contains("   ", "req1"))
    }

    @Test
    fun contains_matchingId_returnsTrue() {
        val csv = "req1,req2,req3"
        assertTrue(NotifiedRequests.contains(csv, "req2"))
        assertFalse(NotifiedRequests.contains(csv, "req9"))
    }

    @Test
    fun add_appendsAndKeepsOrder() {
        var csv = ""
        csv = NotifiedRequests.add(csv, "req1")
        csv = NotifiedRequests.add(csv, "req2")
        assertEquals("req1,req2", csv)
    }

    @Test
    fun add_duplicateId_movesToTheEndWithoutGrowing() {
        var csv = "req1,req2,req3"
        csv = NotifiedRequests.add(csv, "req2")
        assertEquals("req1,req3,req2", csv)
    }

    @Test
    fun add_respectsCap_dropsOldest() {
        var csv = "req1,req2,req3"
        csv = NotifiedRequests.add(csv, "req4", cap = 3)
        assertEquals("req2,req3,req4", csv)
    }
}
