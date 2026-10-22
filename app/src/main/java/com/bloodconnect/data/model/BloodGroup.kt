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
        private val POSITIVE_RH_VARIANTS = setOf(
            "+", "POS", "POSITIVE", "PLUS", "+VE", "POS+"
        )
        private val NEGATIVE_RH_VARIANTS = setOf(
            "-", "NEG", "NEGATIVE", "MINUS", "-VE", "NEG-"
        )

        /**
         * Resolves a [BloodGroup] from any string representation:
         * - Display symbols: "A+", "a+", "O-", "ab+"
         * - Standard enum names: "A_POSITIVE", "o_negative"
         * - Common symbol variations: "o_pos", "a_neg", "opos", "ab_positive", "o positive"
         *
         * Robust, trimmed, and case-insensitive. Returns null rather than throwing
         * [IllegalArgumentException] when an unrecognized, null, or blank value is provided.
         */
        fun fromString(value: String?): BloodGroup? {
            if (value.isNullOrBlank()) return null
            val clean = value.trim()

            // 1. Direct match on display name ("A+", "O-") or enum name ("A_POSITIVE")
            entries.find {
                it.displayName.equals(clean, ignoreCase = true) ||
                it.name.equals(clean, ignoreCase = true)
            }?.let { return it }

            // 2. Parse ABO prefix and Rh factor variation
            val upper = clean.uppercase()
            val (abo, remainder) = when {
                upper.startsWith("AB") -> "AB" to upper.substring(2)
                upper.startsWith("A") -> "A" to upper.substring(1)
                upper.startsWith("B") -> "B" to upper.substring(1)
                upper.startsWith("O") -> "O" to upper.substring(1)
                else -> return null
            }

            val rhRaw = remainder.trim().replace(" ", "").replace("_", "")
            val cleanRh = rhRaw.removePrefix("-").removePrefix("+")

            val isPositive = rhRaw == "+" || rhRaw == "+VE" ||
                (!rhRaw.startsWith("-") && cleanRh in POSITIVE_RH_VARIANTS) ||
                (rhRaw.startsWith("-") && cleanRh in setOf("POS", "POSITIVE", "PLUS", "POS+"))

            val isNegative = rhRaw == "-" || rhRaw == "-VE" ||
                cleanRh in setOf("NEG", "NEGATIVE", "MINUS", "NEG-")

            return when {
                isPositive -> when (abo) {
                    "A" -> A_POSITIVE
                    "B" -> B_POSITIVE
                    "AB" -> AB_POSITIVE
                    "O" -> O_POSITIVE
                    else -> null
                }
                isNegative -> when (abo) {
                    "A" -> A_NEGATIVE
                    "B" -> B_NEGATIVE
                    "AB" -> AB_NEGATIVE
                    "O" -> O_NEGATIVE
                    else -> null
                }
                else -> null
            }
        }

        /**
         * Resolves a [BloodGroup] from string or safely returns [default] if null, blank, or unrecognized.
         */
        fun fromStringOrDefault(value: String?, default: BloodGroup = O_POSITIVE): BloodGroup {
            return fromString(value) ?: default
        }
    }
}
