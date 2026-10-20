package com.bloodconnect.ui.request

/**
 * Pure validation utility functions for blood request submissions.
 * Enforces business rules for required units (1 to 20), hospital name length, and city length.
 */
object BloodRequestValidator {

    /**
     * Validates blood group requirement.
     *
     * @param bloodGroup Selected blood group token or null if unselected.
     * @return User-readable error message, or null if valid.
     */
    fun validateBloodGroup(bloodGroup: String?): String? {
        return if (bloodGroup.isNullOrBlank()) {
            "Please select a required blood group"
        } else {
            null
        }
    }

    /**
     * Validates blood units needed (allowed range: 1 to 20).
     *
     * @param units Input string representation of units.
     * @return User-readable error message, or null if valid.
     */
    fun validateUnits(units: String): String? {
        val trimmed = units.trim()
        val parsed = trimmed.toIntOrNull()
        return when {
            trimmed.isBlank() -> "Units needed cannot be empty"
            parsed == null || parsed <= 0 -> "Please enter a valid positive number of units (1 to 20)"
            parsed > 20 -> "Units needed cannot exceed 20 units"
            else -> null
        }
    }

    /**
     * Validates hospital or facility name (required, min 3 characters).
     *
     * @param hospital Input hospital or medical facility name.
     * @return User-readable error message, or null if valid.
     */
    fun validateHospital(hospital: String): String? {
        val trimmed = hospital.trim()
        return when {
            trimmed.isBlank() -> "Hospital name is required"
            trimmed.length < 3 -> "Hospital name must be at least 3 characters"
            else -> null
        }
    }

    /**
     * Validates city or locality name (required, min 2 characters).
     *
     * @param city Input city or locality name.
     * @return User-readable error message, or null if valid.
     */
    fun validateCity(city: String): String? {
        val trimmed = city.trim()
        return when {
            trimmed.isBlank() -> "City is required"
            trimmed.length < 2 -> "City must be at least 2 characters"
            else -> null
        }
    }

    /**
     * Validates blood request urgency level.
     * Enforces valid urgency levels (Routine, Urgent, Critical) or legacy equivalents (Normal).
     *
     * @param urgency Selected urgency token or null if unselected.
     * @return User-readable error message, or null if valid.
     */
    fun validateUrgency(urgency: String?): String? {
        if (urgency.isNullOrBlank()) {
            return "Urgency level is required"
        }
        return if (com.bloodconnect.data.model.Urgency.fromString(urgency) != null) {
            null
        } else {
            "Please select a valid urgency level (Routine, Urgent, or Critical)"
        }
    }

    /**
     * Validates hospital or ward contact number.
     * Required; after removing spaces, "-", "(" and ")" it must be 7 to 15 digits with an optional leading "+".
     *
     * @param value Input contact number.
     * @return User-readable error message, or null if valid.
     */
    fun validateContactNumber(value: String): String? {
        if (value.isBlank()) {
            return "Contact number is required"
        }
        val cleaned = value.filterNot { it == ' ' || it == '-' || it == '(' || it == ')' }
        val phoneRegex = Regex("^\\+?[0-9]{7,15}$")
        return if (phoneRegex.matches(cleaned)) {
            null
        } else {
            "Please enter a valid phone number (7 to 15 digits)"
        }
    }

    /**
     * Validates optional doctor name or patient reference ID (max 60 characters).
     *
     * @param value Input patient reference or doctor name.
     * @return User-readable error message, or null if valid.
     */
    fun validatePatientReference(value: String): String? {
        return if (value.trim().length > 60 || value.length > 60) {
            "Patient reference cannot exceed 60 characters"
        } else {
            null
        }
    }
}
