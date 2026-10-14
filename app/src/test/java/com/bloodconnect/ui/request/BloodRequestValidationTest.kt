package com.bloodconnect.ui.request

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for blood request form validation rules.
 * Verifies business requirements:
 * - Units needed within allowed range (1 to 20)
 * - Hospital name length (minimum 3 characters)
 * - City / locality length (minimum 2 characters)
 * - Blood group presence
 */
class BloodRequestValidationTest {

    // -------------------------------------------------------------
    // Units Needed Validation Tests (1 - 20)
    // -------------------------------------------------------------

    @Test
    fun validateUnits_validUnitsWithinRange_returnsNull() {
        assertNull(BloodRequestValidator.validateUnits("1"))
        assertNull(BloodRequestValidator.validateUnits("5"))
        assertNull(BloodRequestValidator.validateUnits("10"))
        assertNull(BloodRequestValidator.validateUnits("19"))
        assertNull(BloodRequestValidator.validateUnits("20"))
    }

    @Test
    fun validateUnits_validUnitsWithSurroundingWhitespace_returnsNull() {
        assertNull(BloodRequestValidator.validateUnits("  3  "))
        assertNull(BloodRequestValidator.validateUnits("\t15\n"))
    }

    @Test
    fun validateUnits_emptyOrBlankInput_returnsEmptyErrorMessage() {
        assertEquals("Units needed cannot be empty", BloodRequestValidator.validateUnits(""))
        assertEquals("Units needed cannot be empty", BloodRequestValidator.validateUnits("   "))
    }

    @Test
    fun validateUnits_zeroUnits_returnsPositiveNumberErrorMessage() {
        assertEquals(
            "Please enter a valid positive number of units (1 to 20)",
            BloodRequestValidator.validateUnits("0")
        )
    }

    @Test
    fun validateUnits_negativeUnits_returnsPositiveNumberErrorMessage() {
        assertEquals(
            "Please enter a valid positive number of units (1 to 20)",
            BloodRequestValidator.validateUnits("-1")
        )
        assertEquals(
            "Please enter a valid positive number of units (1 to 20)",
            BloodRequestValidator.validateUnits("-10")
        )
    }

    @Test
    fun validateUnits_nonNumericCharacters_returnsPositiveNumberErrorMessage() {
        assertEquals(
            "Please enter a valid positive number of units (1 to 20)",
            BloodRequestValidator.validateUnits("abc")
        )
        assertEquals(
            "Please enter a valid positive number of units (1 to 20)",
            BloodRequestValidator.validateUnits("two")
        )
        assertEquals(
            "Please enter a valid positive number of units (1 to 20)",
            BloodRequestValidator.validateUnits("1.5")
        )
    }

    @Test
    fun validateUnits_unitsExceedingTwenty_returnsExceedErrorMessage() {
        assertEquals(
            "Units needed cannot exceed 20 units",
            BloodRequestValidator.validateUnits("21")
        )
        assertEquals(
            "Units needed cannot exceed 20 units",
            BloodRequestValidator.validateUnits("50")
        )
        assertEquals(
            "Units needed cannot exceed 20 units",
            BloodRequestValidator.validateUnits("100")
        )
    }

    // -------------------------------------------------------------
    // Units Needed Boundary Tests (0, 1, 20, 21)
    // -------------------------------------------------------------

    @Test
    fun validateUnits_boundaryZero_returnsPositiveNumberErrorMessage() {
        val result = BloodRequestValidator.validateUnits("0")
        assertEquals(
            "Please enter a valid positive number of units (1 to 20)",
            result
        )
    }

    @Test
    fun validateUnits_boundaryOne_lowerValidBound_returnsNull() {
        val result = BloodRequestValidator.validateUnits("1")
        assertNull(result)
    }

    @Test
    fun validateUnits_boundaryTwenty_upperValidBound_returnsNull() {
        val result = BloodRequestValidator.validateUnits("20")
        assertNull(result)
    }

    @Test
    fun validateUnits_boundaryTwentyOne_firstExceedingValue_returnsExceedErrorMessage() {
        val result = BloodRequestValidator.validateUnits("21")
        assertEquals(
            "Units needed cannot exceed 20 units",
            result
        )
    }

    @Test
    fun validateUnits_boundaryValuesWithWhitespace_handlesBoundariesCorrectly() {
        assertEquals(
            "Please enter a valid positive number of units (1 to 20)",
            BloodRequestValidator.validateUnits("  0  ")
        )
        assertNull(BloodRequestValidator.validateUnits("  1  "))
        assertNull(BloodRequestValidator.validateUnits("  20  "))
        assertEquals(
            "Units needed cannot exceed 20 units",
            BloodRequestValidator.validateUnits("  21  ")
        )
    }

    // -------------------------------------------------------------
    // Hospital Name Validation Tests (minimum 3 characters)
    // -------------------------------------------------------------

    @Test
    fun validateHospital_validNames_returnsNull() {
        assertNull(BloodRequestValidator.validateHospital("St. Jude"))
        assertNull(BloodRequestValidator.validateHospital("City General Hospital"))
        assertNull(BloodRequestValidator.validateHospital("Clinic #1"))
    }

    @Test
    fun validateHospital_exactThreeCharacters_returnsNull() {
        assertNull(BloodRequestValidator.validateHospital("AIH"))
        assertNull(BloodRequestValidator.validateHospital("CMC"))
    }

    @Test
    fun validateHospital_blankOrEmpty_returnsRequiredErrorMessage() {
        assertEquals(
            "Hospital or facility name is required",
            BloodRequestValidator.validateHospital("")
        )
        assertEquals(
            "Hospital or facility name is required",
            BloodRequestValidator.validateHospital("   ")
        )
    }

    @Test
    fun validateHospital_tooShortFewerThanThreeChars_returnsMinLengthErrorMessage() {
        assertEquals(
            "Hospital name must be at least 3 characters",
            BloodRequestValidator.validateHospital("A")
        )
        assertEquals(
            "Hospital name must be at least 3 characters",
            BloodRequestValidator.validateHospital("AB")
        )
    }

    @Test
    fun validateHospital_spacesAroundShortName_trimsBeforeValidation() {
        assertEquals(
            "Hospital name must be at least 3 characters",
            BloodRequestValidator.validateHospital("  AB  ")
        )
    }

    // -------------------------------------------------------------
    // City / Locality Validation Tests (minimum 2 characters)
    // -------------------------------------------------------------

    @Test
    fun validateCity_validCities_returnsNull() {
        assertNull(BloodRequestValidator.validateCity("NY"))
        assertNull(BloodRequestValidator.validateCity("LA"))
        assertNull(BloodRequestValidator.validateCity("Mumbai"))
        assertNull(BloodRequestValidator.validateCity("Central District"))
    }

    @Test
    fun validateCity_exactTwoCharacters_returnsNull() {
        assertNull(BloodRequestValidator.validateCity("SF"))
        assertNull(BloodRequestValidator.validateCity("Goa"))
    }

    @Test
    fun validateCity_blankOrEmpty_returnsRequiredErrorMessage() {
        assertEquals(
            "City or locality is required",
            BloodRequestValidator.validateCity("")
        )
        assertEquals(
            "City or locality is required",
            BloodRequestValidator.validateCity("   ")
        )
    }

    @Test
    fun validateCity_singleCharacter_returnsMinLengthErrorMessage() {
        assertEquals(
            "City or locality must be at least 2 characters",
            BloodRequestValidator.validateCity("A")
        )
        assertEquals(
            "City or locality must be at least 2 characters",
            BloodRequestValidator.validateCity(" 7 ")
        )
    }

    // -------------------------------------------------------------
    // Blood Group Validation Tests
    // -------------------------------------------------------------

    @Test
    fun validateBloodGroup_validSelection_returnsNull() {
        assertNull(BloodRequestValidator.validateBloodGroup("O+"))
        assertNull(BloodRequestValidator.validateBloodGroup("A-"))
        assertNull(BloodRequestValidator.validateBloodGroup("AB+"))
    }

    @Test
    fun validateBloodGroup_nullOrEmpty_returnsSelectErrorMessage() {
        assertNotNull(BloodRequestValidator.validateBloodGroup(null))
        assertEquals(
            "Please select a required blood group",
            BloodRequestValidator.validateBloodGroup(null)
        )
        assertEquals(
            "Please select a required blood group",
            BloodRequestValidator.validateBloodGroup("")
        )
    }

    // -------------------------------------------------------------
    // Urgency Validation Tests (Routine, Urgent, Critical)
    // -------------------------------------------------------------

    @Test
    fun validateUrgency_canonicalLevels_returnsNull() {
        assertNull(BloodRequestValidator.validateUrgency("Routine"))
        assertNull(BloodRequestValidator.validateUrgency("Urgent"))
        assertNull(BloodRequestValidator.validateUrgency("Critical"))
    }

    @Test
    fun validateUrgency_legacyAndAlternativeEquivalents_returnsNull() {
        assertNull(BloodRequestValidator.validateUrgency("Normal"))
        assertNull(BloodRequestValidator.validateUrgency("normal"))
        assertNull(BloodRequestValidator.validateUrgency("NORMAL"))
        assertNull(BloodRequestValidator.validateUrgency("Standard"))
        assertNull(BloodRequestValidator.validateUrgency("Emergency"))
        assertNull(BloodRequestValidator.validateUrgency("EMERGENCY"))
    }

    @Test
    fun validateUrgency_caseInsensitiveAndSurroundingWhitespace_returnsNull() {
        assertNull(BloodRequestValidator.validateUrgency("  routine  "))
        assertNull(BloodRequestValidator.validateUrgency("\tUrgent\n"))
        assertNull(BloodRequestValidator.validateUrgency("CRITICAL"))
        assertNull(BloodRequestValidator.validateUrgency("  Normal  "))
    }

    @Test
    fun validateUrgency_nullOrBlank_returnsRequiredErrorMessage() {
        assertEquals(
            "Urgency level is required",
            BloodRequestValidator.validateUrgency(null)
        )
        assertEquals(
            "Urgency level is required",
            BloodRequestValidator.validateUrgency("")
        )
        assertEquals(
            "Urgency level is required",
            BloodRequestValidator.validateUrgency("   ")
        )
    }

    @Test
    fun validateUrgency_invalidValues_returnsInvalidErrorMessage() {
        val expectedMessage = "Please select a valid urgency level (Routine, Urgent, or Critical)"
        assertEquals(expectedMessage, BloodRequestValidator.validateUrgency("Immediate"))
        assertEquals(expectedMessage, BloodRequestValidator.validateUrgency("Fast"))
        assertEquals(expectedMessage, BloodRequestValidator.validateUrgency("Low"))
        assertEquals(expectedMessage, BloodRequestValidator.validateUrgency("123"))
        assertEquals(expectedMessage, BloodRequestValidator.validateUrgency("Moderate"))
    }
}
