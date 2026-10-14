package com.bloodconnect.data.model

/**
 * Lifecycle status of an emergency blood request.
 */
enum class RequestStatus(val displayName: String) {
    PENDING("Pending"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled");

    val canonicalValue: String get() = displayName

    companion object {
        /**
         * Resolves a [RequestStatus] from any representation:
         * - Canonical display strings ("Pending", "Fulfilled", "Cancelled")
         * - Standard enum names ("PENDING", "FULFILLED", "CANCELLED")
         * - Legacy or alternative values ("Active", "active", "Canceled", etc.)
         * - Demonstration formats (e.g. "Pending (Demo)")
         */
        fun fromString(value: String?): RequestStatus? {
            if (value.isNullOrBlank()) return null
            val clean = value.trim()
            val base = clean.substringBefore(" (").trim()
            return when {
                base.equals("Active", ignoreCase = true) -> PENDING
                base.equals("Pending", ignoreCase = true) || base.equals("PENDING", ignoreCase = true) -> PENDING
                base.equals("Fulfilled", ignoreCase = true) || base.equals("FULFILLED", ignoreCase = true) -> FULFILLED
                base.equals("Cancelled", ignoreCase = true) || base.equals("CANCELLED", ignoreCase = true) ||
                    base.equals("Canceled", ignoreCase = true) || base.equals("CANCELED", ignoreCase = true) -> CANCELLED
                else -> entries.find {
                    it.displayName.equals(base, ignoreCase = true) ||
                    it.name.equals(base, ignoreCase = true) ||
                    clean.startsWith(it.displayName, ignoreCase = true)
                }
            }
        }

        /**
         * Resolves a [RequestStatus] from string or returns [default] (PENDING) if unrecognized.
         */
        fun fromStringOrDefault(value: String?, default: RequestStatus = PENDING): RequestStatus {
            return fromString(value) ?: default
        }
    }
}
