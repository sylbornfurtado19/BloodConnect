package com.bloodconnect.data.model

/**
 * Urgency classification for emergency blood requests.
 */
enum class Urgency(val displayName: String) {
    ROUTINE("Routine"),
    URGENT("Urgent"),
    CRITICAL("Critical");

    val canonicalValue: String get() = displayName

    companion object {
        /**
         * Resolves an [Urgency] from any string representation:
         * - Canonical display strings: "Routine", "Urgent", "Critical"
         * - Standard enum names: "ROUTINE", "URGENT", "CRITICAL"
         * - Legacy or alternative values:
         *   - "Normal", "NORMAL", "normal", "Standard" -> [ROUTINE]
         *   - "Emergency", "EMERGENCY" -> [CRITICAL]
         * - Case-insensitive and trimmed.
         */
        fun fromString(value: String?): Urgency? {
            if (value.isNullOrBlank()) return null
            val clean = value.trim()
            val base = clean.substringBefore(" ·").substringBefore(" (").trim()
            return when {
                base.equals("Routine", ignoreCase = true) || base.equals("ROUTINE", ignoreCase = true) -> ROUTINE
                base.equals("Normal", ignoreCase = true) || base.equals("NORMAL", ignoreCase = true) -> ROUTINE
                base.equals("Standard", ignoreCase = true) -> ROUTINE
                base.equals("Urgent", ignoreCase = true) || base.equals("URGENT", ignoreCase = true) -> URGENT
                base.equals("Critical", ignoreCase = true) || base.equals("CRITICAL", ignoreCase = true) -> CRITICAL
                base.equals("Emergency", ignoreCase = true) || base.equals("EMERGENCY", ignoreCase = true) -> CRITICAL
                else -> entries.find {
                    it.displayName.equals(base, ignoreCase = true) ||
                    it.name.equals(base, ignoreCase = true) ||
                    clean.startsWith(it.displayName, ignoreCase = true)
                }
            }
        }

        /**
         * Resolves an [Urgency] from string or safely returns [default] (ROUTINE) if null, blank, or unrecognized.
         * Ensures older Firestore records with missing, blank, or legacy urgency values default safely.
         */
        fun fromStringOrDefault(value: String?, default: Urgency = ROUTINE): Urgency {
            return fromString(value) ?: default
        }
    }
}
