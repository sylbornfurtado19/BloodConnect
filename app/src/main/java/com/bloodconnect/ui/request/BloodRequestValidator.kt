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
            trimmed.isBlank() -> "Hospital or facility name is required"
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
            trimmed.isBlank() -> "City or locality is required"
            trimmed.length < 2 -> "City or locality must be at least 2 characters"
            else -> null
        }
    }
}
