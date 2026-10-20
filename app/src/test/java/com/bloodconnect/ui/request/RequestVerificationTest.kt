package com.bloodconnect.ui.request

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying BloodRequestValidator functions for request verification:
 * - validateContactNumber(value: String): String?
 * - validatePatientReference(value: String): String?
 */
class RequestVerificationTest {

    // -------------------------------------------------------------
    // Contact Number Validator Tests
    // -------------------------------------------------------------

    @Test
    fun validateContactNumber_validInternationalFormat_returnsNull() {
        assertNull(BloodRequestValidator.validateContactNumber("+91 98765 43210"))
    }

    @Test
    fun validateContactNumber_validLandlineWithDashes_returnsNull() {
        assertNull(BloodRequestValidator.validateContactNumber("022-2345-6789"))
    }

    @Test
    fun validateContactNumber_validWithParenthesesAndSpaces_returnsNull() {
        assertNull(BloodRequestValidator.validateContactNumber("(022) 2345-6789"))
        assertNull(BloodRequestValidator.validateContactNumber("+1 (555) 123-4567"))
    }

    @Test
    fun validateContactNumber_tooShort_returnsErrorMessage() {
        // Less than 7 digits
        assertNotNull(BloodRequestValidator.validateContactNumber("123456"))
        assertNotNull(BloodRequestValidator.validateContactNumber("+12345"))
        assertNotNull(BloodRequestValidator.validateContactNumber("022-12"))
    }

    @Test
    fun validateContactNumber_containsLetters_returnsErrorMessage() {
        assertNotNull(BloodRequestValidator.validateContactNumber("+91 98765 ABCD0"))
        assertNotNull(BloodRequestValidator.validateContactNumber("call-hospital-now"))
        assertNotNull(BloodRequestValidator.validateContactNumber("022-2345-678x"))
    }

    @Test
    fun validateContactNumber_blankOrEmpty_returnsErrorMessage() {
        assertNotNull(BloodRequestValidator.validateContactNumber(""))
        assertNotNull(BloodRequestValidator.validateContactNumber("   "))
        assertNotNull(BloodRequestValidator.validateContactNumber("\t\n"))
    }

    // -------------------------------------------------------------
    // Patient Reference Validator Tests
    // -------------------------------------------------------------

    @Test
    fun validatePatientReference_blankOrEmpty_isAllowed_returnsNull() {
        assertNull(BloodRequestValidator.validatePatientReference(""))
        assertNull(BloodRequestValidator.validatePatientReference("   "))
    }

    @Test
    fun validatePatientReference_validDoctorNameOrCaseId_returnsNull() {
        assertNull(BloodRequestValidator.validatePatientReference("Dr. A. Sharma"))
        assertNull(BloodRequestValidator.validatePatientReference("Case #402 - ICU Bed 12"))
    }

    @Test
    fun validatePatientReference_exactMaxSixtyCharacters_returnsNull() {
        val sixtyChars = "A".repeat(60)
        assertNull(BloodRequestValidator.validatePatientReference(sixtyChars))
    }

    @Test
    fun validatePatientReference_sixtyOneCharacters_returnsErrorMessage() {
        val sixtyOneChars = "A".repeat(61)
        assertNotNull(BloodRequestValidator.validatePatientReference(sixtyOneChars))
    }
}
