package com.bloodconnect.data.model

/**
 * Standard ABO and Rh factor blood groups with compatibility logic.
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

    /**
     * Determines whether this donor blood group can donate to the given [recipient] blood group
     * according to standard ABO and Rh compatibility rules.
     */
    fun canDonateTo(recipient: BloodGroup): Boolean {
        val donorRhPositive = displayName.endsWith("+")
        val recipientRhPositive = recipient.displayName.endsWith("+")

        // Rh factor rule: Rh+ donors can only donate to Rh+ recipients
        if (donorRhPositive && !recipientRhPositive) {
            return false
        }

        // ABO group compatibility rule
        val donorAbo = displayName.dropLast(1)
        val recipientAbo = recipient.displayName.dropLast(1)

        return when (donorAbo) {
            "O" -> true // Universal ABO donor
            "A" -> recipientAbo == "A" || recipientAbo == "AB"
            "B" -> recipientAbo == "B" || recipientAbo == "AB"
            "AB" -> recipientAbo == "AB"
            else -> false
        }
    }

    /**
     * Returns the list of all donor blood groups that can donate to this recipient blood group.
     */
    fun compatibleDonorGroups(): List<BloodGroup> {
        return entries.filter { it.canDonateTo(this) }
    }

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
