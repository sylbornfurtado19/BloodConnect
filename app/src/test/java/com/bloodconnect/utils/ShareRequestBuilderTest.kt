package com.bloodconnect.utils

import com.bloodconnect.data.model.BloodRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareRequestBuilderTest {

    @Test
    fun buildShareText_singleUnit_usesSingularUnit() {
        val request = BloodRequest(
            bloodGroup = "O+",
            hospital = "City Hospital",
            city = "Metropolis",
            unitsNeeded = 1,
            urgency = "Urgent",
            contactNumber = ""
        )

        val result = buildShareText(request)

        assertTrue("Expected '1 unit' in text, got: $result", result.contains("1 unit."))
        assertFalse("Did not expect '1 units' in text", result.contains("1 units"))
    }

    @Test
    fun buildShareText_multipleUnits_usesPluralUnits() {
        val request = BloodRequest(
            bloodGroup = "B+",
            hospital = "Memorial Hospital",
            city = "Gotham",
            unitsNeeded = 3,
            urgency = "Critical",
            contactNumber = ""
        )

        val result = buildShareText(request)

        assertTrue("Expected '3 units' in text, got: $result", result.contains("3 units."))
    }

    @Test
    fun buildShareText_withContactNumber_includesHospitalContact() {
        val request = BloodRequest(
            bloodGroup = "AB-",
            hospital = "General Hospital",
            city = "Central City",
            unitsNeeded = 2,
            urgency = "Routine",
            contactNumber = "+1-800-555-0199"
        )

        val result = buildShareText(request)

        assertTrue(
            "Expected hospital contact in text, got: $result",
            result.contains("Hospital contact: +1-800-555-0199.")
        )
    }

    @Test
    fun buildShareText_withoutContactNumber_omitsHospitalContact() {
        val requestEmptyContact = BloodRequest(
            bloodGroup = "A+",
            hospital = "St. Jude",
            city = "Star City",
            unitsNeeded = 1,
            urgency = "Urgent",
            contactNumber = ""
        )
        val resultEmpty = buildShareText(requestEmptyContact)
        assertFalse("Did not expect 'Hospital contact' for empty contact", resultEmpty.contains("Hospital contact"))

        val requestWhitespaceContact = BloodRequest(
            bloodGroup = "A+",
            hospital = "St. Jude",
            city = "Star City",
            unitsNeeded = 1,
            urgency = "Urgent",
            contactNumber = "   "
        )
        val resultWhitespace = buildShareText(requestWhitespaceContact)
        assertFalse("Did not expect 'Hospital contact' for whitespace contact", resultWhitespace.contains("Hospital contact"))
    }

    @Test
    fun buildShareText_doesNotIncludeRequesterDetailsOrCoordinates() {
        val request = BloodRequest(
            requesterId = "uid-12345",
            requesterName = "Bruce Wayne",
            bloodGroup = "O-",
            hospital = "Wayne Memorial Hospital",
            city = "Gotham",
            unitsNeeded = 1,
            urgency = "Critical",
            latitude = 40.7128,
            longitude = -74.0060,
            contactNumber = "+1987654321"
        )

        val result = buildShareText(request)

        assertFalse("Should not include requester name", result.contains("Bruce Wayne"))
        assertFalse("Should not include requester ID", result.contains("uid-12345"))
        assertFalse("Should not include latitude coordinate", result.contains("40.7128"))
        assertFalse("Should not include longitude coordinate", result.contains("-74.0060"))
    }

    @Test
    fun buildShareText_exactFormat_matchesExpectedTemplate() {
        val requestWithContact = BloodRequest(
            bloodGroup = "A+",
            hospital = "Apollo Hospital",
            city = "Mumbai",
            unitsNeeded = 1,
            urgency = "Urgent",
            contactNumber = "+919876543210"
        )
        val expectedWithContact = "URGENT: A+ blood needed at Apollo Hospital, Mumbai. 1 unit. Urgent. Hospital contact: +919876543210. Posted on BloodConnect."
        assertEquals(expectedWithContact, buildShareText(requestWithContact))

        val requestWithoutContact = BloodRequest(
            bloodGroup = "O-",
            hospital = "City Clinic",
            city = "Pune",
            unitsNeeded = 3,
            urgency = "Critical",
            contactNumber = ""
        )
        val expectedWithoutContact = "URGENT: O- blood needed at City Clinic, Pune. 3 units. Critical. Posted on BloodConnect."
        assertEquals(expectedWithoutContact, buildShareText(requestWithoutContact))
    }
}
