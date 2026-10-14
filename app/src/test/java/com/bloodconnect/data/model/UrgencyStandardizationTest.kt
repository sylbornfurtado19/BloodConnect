package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit tests for urgency standardization across the BloodConnect platform.
 * Verifies:
 * - Urgency enum definitions and canonical values (ROUTINE, URGENT, CRITICAL).
 * - Safe handling and fallback for older Firestore records with legacy ("Normal") or missing values.
 * - Parsing with whitespace, case-insensitivity, and suffix stripping.
 * - [BloodRequest] model properties (urgencyLevel, canonicalUrgency, withCanonicalUrgency).
 * - In-memory and Firestore query-compatible urgency filtering behavior.
 * - No-argument constructor reflection validation for Firestore deserialization.
 */
class UrgencyStandardizationTest {

    // ------------------------------------------------------------------------
    // 1. Urgency Enum Canonical Definitions
    // ------------------------------------------------------------------------

    @Test
    fun urgencyEnum_hasExactlyThreeCanonicalLevels() {
        val entries = Urgency.entries
        assertEquals(3, entries.size)
        assertTrue(entries.contains(Urgency.ROUTINE))
        assertTrue(entries.contains(Urgency.URGENT))
        assertTrue(entries.contains(Urgency.CRITICAL))
    }

    @Test
    fun urgencyEnum_canonicalDisplayNames() {
        assertEquals("Routine", Urgency.ROUTINE.displayName)
        assertEquals("Urgent", Urgency.URGENT.displayName)
        assertEquals("Critical", Urgency.CRITICAL.displayName)
    }

    @Test
    fun urgencyEnum_canonicalValueMatchesDisplayName() {
        assertEquals("Routine", Urgency.ROUTINE.canonicalValue)
        assertEquals("Urgent", Urgency.URGENT.canonicalValue)
        assertEquals("Critical", Urgency.CRITICAL.canonicalValue)
    }

    // ------------------------------------------------------------------------
    // 2. Parsing Canonical Urgency Strings
    // ------------------------------------------------------------------------

    @Test
    fun fromString_canonicalDisplayStrings_resolvesCorrectly() {
        assertEquals(Urgency.ROUTINE, Urgency.fromString("Routine"))
        assertEquals(Urgency.URGENT, Urgency.fromString("Urgent"))
        assertEquals(Urgency.CRITICAL, Urgency.fromString("Critical"))
    }

    @Test
    fun fromString_enumNames_resolvesCorrectly() {
        assertEquals(Urgency.ROUTINE, Urgency.fromString("ROUTINE"))
        assertEquals(Urgency.URGENT, Urgency.fromString("URGENT"))
        assertEquals(Urgency.CRITICAL, Urgency.fromString("CRITICAL"))
    }

    @Test
    fun fromString_caseInsensitiveAndWhitespace_resolvesCorrectly() {
        assertEquals(Urgency.ROUTINE, Urgency.fromString("  routine  "))
        assertEquals(Urgency.URGENT, Urgency.fromString("\turgent\n"))
        assertEquals(Urgency.CRITICAL, Urgency.fromString("  CRITICAL  "))
    }

    // ------------------------------------------------------------------------
    // 3. Safe Resolution of Legacy and Alternative Values
    // ------------------------------------------------------------------------

    @Test
    fun fromString_legacyNormal_safelyResolvesToRoutine() {
        assertEquals(Urgency.ROUTINE, Urgency.fromString("Normal"))
        assertEquals(Urgency.ROUTINE, Urgency.fromString("normal"))
        assertEquals(Urgency.ROUTINE, Urgency.fromString("NORMAL"))
        assertEquals(Urgency.ROUTINE, Urgency.fromString("  Normal  "))
    }

    @Test
    fun fromString_legacyStandard_safelyResolvesToRoutine() {
        assertEquals(Urgency.ROUTINE, Urgency.fromString("Standard"))
        assertEquals(Urgency.ROUTINE, Urgency.fromString("standard"))
    }

    @Test
    fun fromString_legacyEmergency_safelyResolvesToCritical() {
        assertEquals(Urgency.CRITICAL, Urgency.fromString("Emergency"))
        assertEquals(Urgency.CRITICAL, Urgency.fromString("emergency"))
        assertEquals(Urgency.CRITICAL, Urgency.fromString("EMERGENCY"))
    }

    // ------------------------------------------------------------------------
    // 4. Formatted Display Suffix Stripping
    // ------------------------------------------------------------------------

    @Test
    fun fromString_withUnitOrNotesSuffix_stripsAndResolvesCorrectly() {
        assertEquals(Urgency.CRITICAL, Urgency.fromString("Critical · 2 Units needed"))
        assertEquals(Urgency.URGENT, Urgency.fromString("Urgent · 1 Unit needed"))
        assertEquals(Urgency.ROUTINE, Urgency.fromString("Routine (Scheduled procedure)"))
        assertEquals(Urgency.ROUTINE, Urgency.fromString("Normal · 1 Unit"))
    }

    // ------------------------------------------------------------------------
    // 5. Handling Missing, Null, Blank, and Invalid Values
    // ------------------------------------------------------------------------

    @Test
    fun fromString_nullOrBlankInput_returnsNull() {
        assertNull(Urgency.fromString(null))
        assertNull(Urgency.fromString(""))
        assertNull(Urgency.fromString("   "))
        assertNull(Urgency.fromString("\t\n"))
    }

    @Test
    fun fromString_unrecognizedValue_returnsNull() {
        assertNull(Urgency.fromString("Unknown"))
        assertNull(Urgency.fromString("High"))
        assertNull(Urgency.fromString("Low"))
        assertNull(Urgency.fromString("123"))
    }

    @Test
    fun fromStringOrDefault_nullOrBlank_returnsRoutineByDefault() {
        assertEquals(Urgency.ROUTINE, Urgency.fromStringOrDefault(null))
        assertEquals(Urgency.ROUTINE, Urgency.fromStringOrDefault(""))
        assertEquals(Urgency.ROUTINE, Urgency.fromStringOrDefault("   "))
    }

    @Test
    fun fromStringOrDefault_unrecognizedValue_returnsRoutineByDefault() {
        assertEquals(Urgency.ROUTINE, Urgency.fromStringOrDefault("Unknown"))
        assertEquals(Urgency.ROUTINE, Urgency.fromStringOrDefault("Fast"))
    }

    @Test
    fun fromStringOrDefault_customFallback_returnsCustomDefaultWhenUnresolved() {
        assertEquals(Urgency.URGENT, Urgency.fromStringOrDefault(null, default = Urgency.URGENT))
        assertEquals(Urgency.CRITICAL, Urgency.fromStringOrDefault("Unrecognized", default = Urgency.CRITICAL))
    }

    // ------------------------------------------------------------------------
    // 6. BloodRequest Model Default & Urgency Resolution
    // ------------------------------------------------------------------------

    @Test
    fun bloodRequest_defaultConstructor_usesCanonicalRoutine() {
        val request = BloodRequest()
        assertEquals("Routine", request.urgency)
        assertEquals(Urgency.ROUTINE, request.urgencyLevel)
        assertEquals("Routine", request.canonicalUrgency)
    }

    @Test
    fun bloodRequest_withCanonicalUrgencyLevels_resolvesCorrectly() {
        val routineReq = BloodRequest(urgency = "Routine")
        assertEquals(Urgency.ROUTINE, routineReq.urgencyLevel)
        assertEquals("Routine", routineReq.canonicalUrgency)

        val urgentReq = BloodRequest(urgency = "Urgent")
        assertEquals(Urgency.URGENT, urgentReq.urgencyLevel)
        assertEquals("Urgent", urgentReq.canonicalUrgency)

        val criticalReq = BloodRequest(urgency = "Critical")
        assertEquals(Urgency.CRITICAL, criticalReq.urgencyLevel)
        assertEquals("Critical", criticalReq.canonicalUrgency)
    }

    @Test
    fun bloodRequest_withLegacyNormalValue_safelyResolvesToRoutine() {
        val legacyReq = BloodRequest(
            id = "REQ-101",
            hospital = "City Hospital",
            urgency = "Normal"
        )

        assertEquals("Normal", legacyReq.urgency) // raw field preserved
        assertEquals(Urgency.ROUTINE, legacyReq.urgencyLevel)
        assertEquals("Routine", legacyReq.canonicalUrgency)

        val normalized = legacyReq.withCanonicalUrgency()
        assertEquals("Routine", normalized.urgency)
        assertEquals("REQ-101", normalized.id)
        assertEquals("City Hospital", normalized.hospital)
    }

    @Test
    fun bloodRequest_withMissingOrBlankUrgency_safelyResolvesToRoutine() {
        val emptyReq = BloodRequest(urgency = "")
        assertEquals(Urgency.ROUTINE, emptyReq.urgencyLevel)
        assertEquals("Routine", emptyReq.canonicalUrgency)

        val blankReq = BloodRequest(urgency = "   ")
        assertEquals(Urgency.ROUTINE, blankReq.urgencyLevel)
        assertEquals("Routine", blankReq.canonicalUrgency)
    }

    @Test
    fun bloodRequest_createFactory_canonicalizesLegacyUrgency() {
        val reqFromNormal = BloodRequest.create(
            hospital = "City Clinic",
            urgency = "Normal"
        )
        assertEquals("Routine", reqFromNormal.urgency)
        assertEquals(Urgency.ROUTINE, reqFromNormal.urgencyLevel)

        val reqFromUrgent = BloodRequest.create(
            hospital = "Metro Hospital",
            urgency = "Urgent"
        )
        assertEquals("Urgent", reqFromUrgent.urgency)
        assertEquals(Urgency.URGENT, reqFromUrgent.urgencyLevel)

        val reqFromEmergency = BloodRequest.create(
            hospital = "Trauma Care",
            urgency = "Emergency"
        )
        assertEquals("Critical", reqFromEmergency.urgency)
        assertEquals(Urgency.CRITICAL, reqFromEmergency.urgencyLevel)
    }

    // ------------------------------------------------------------------------
    // 7. Urgency Filtering Logic Validation
    // ------------------------------------------------------------------------

    private fun filterRequestsByUrgency(
        requests: List<BloodRequest>,
        filter: String
    ): List<BloodRequest> {
        if (filter.equals("All", ignoreCase = true)) return requests
        val targetUrgency = Urgency.fromString(filter) ?: return emptyList()
        return requests.filter { it.urgencyLevel == targetUrgency }
    }

    @Test
    fun urgencyFiltering_routineFilter_matchesCanonicalRoutineAndLegacyNormal() {
        val dataset = listOf(
            BloodRequest(id = "1", urgency = "Routine"),
            BloodRequest(id = "2", urgency = "Normal"), // legacy
            BloodRequest(id = "3", urgency = "Urgent"),
            BloodRequest(id = "4", urgency = "Critical"),
            BloodRequest(id = "5", urgency = "") // missing -> Routine
        )

        val routineMatches = filterRequestsByUrgency(dataset, "Routine")
        assertEquals(3, routineMatches.size)
        assertTrue(routineMatches.any { it.id == "1" })
        assertTrue(routineMatches.any { it.id == "2" })
        assertTrue(routineMatches.any { it.id == "5" })
    }

    @Test
    fun urgencyFiltering_urgentFilter_matchesOnlyUrgent() {
        val dataset = listOf(
            BloodRequest(id = "1", urgency = "Routine"),
            BloodRequest(id = "2", urgency = "Normal"),
            BloodRequest(id = "3", urgency = "Urgent"),
            BloodRequest(id = "4", urgency = "Critical")
        )

        val urgentMatches = filterRequestsByUrgency(dataset, "Urgent")
        assertEquals(1, urgentMatches.size)
        assertEquals("3", urgentMatches.first().id)
    }

    @Test
    fun urgencyFiltering_criticalFilter_matchesCriticalAndEmergency() {
        val dataset = listOf(
            BloodRequest(id = "1", urgency = "Routine"),
            BloodRequest(id = "2", urgency = "Normal"),
            BloodRequest(id = "3", urgency = "Critical"),
            BloodRequest(id = "4", urgency = "Emergency") // legacy alias
        )

        val criticalMatches = filterRequestsByUrgency(dataset, "Critical")
        assertEquals(2, criticalMatches.size)
        assertTrue(criticalMatches.any { it.id == "3" })
        assertTrue(criticalMatches.any { it.id == "4" })
    }

    @Test
    fun urgencyFiltering_allFilter_returnsEntireDataset() {
        val dataset = listOf(
            BloodRequest(id = "1", urgency = "Routine"),
            BloodRequest(id = "2", urgency = "Normal"),
            BloodRequest(id = "3", urgency = "Urgent"),
            BloodRequest(id = "4", urgency = "Critical")
        )

        val allMatches = filterRequestsByUrgency(dataset, "All")
        assertEquals(4, allMatches.size)
    }

    // ------------------------------------------------------------------------
    // 8. Firestore Deserialization & Reflection Integrity
    // ------------------------------------------------------------------------

    @Test
    fun bloodRequest_hasNoArgumentConstructorForFirestore() {
        val constructor = BloodRequest::class.java.getDeclaredConstructor()
        assertNotNull(constructor)
        val instance = constructor.newInstance()
        assertNotNull(instance)
        assertEquals("Routine", instance.urgency)
        assertEquals(Urgency.ROUTINE, instance.urgencyLevel)
    }
}
