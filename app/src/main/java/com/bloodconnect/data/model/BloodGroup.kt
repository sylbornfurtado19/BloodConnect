package com.bloodconnect.data.model

/**
 * Standard ABO and Rh factor blood groups.
 */
enum class BloodGroup(val displayName: String) {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    companion object {
        /**
         * Resolves a [BloodGroup] from a display string (e.g. "O+") or enum name.
         */
        fun fromString(value: String): BloodGroup? {
            return entries.find {
                it.displayName.equals(value.trim(), ignoreCase = true) ||
                it.name.equals(value.trim(), ignoreCase = true)
            }
        }
    }
}
