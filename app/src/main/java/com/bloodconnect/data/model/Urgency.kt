package com.bloodconnect.data.model

/**
 * Urgency classification for emergency blood requests.
 */
enum class Urgency(val displayName: String) {
    ROUTINE("Routine"),
    URGENT("Urgent"),
    CRITICAL("Critical");

    companion object {
        /**
         * Resolves an [Urgency] from a display string (e.g. "Critical") or enum name.
         */
        fun fromString(value: String): Urgency? {
            return entries.find {
                it.displayName.equals(value.trim(), ignoreCase = true) ||
                it.name.equals(value.trim(), ignoreCase = true)
            }
        }
    }
}
