package com.bloodconnect.data.model

/**
 * Lifecycle status of an emergency blood request.
 */
enum class RequestStatus(val displayName: String) {
    PENDING("Pending"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled");

    companion object {
        /**
         * Resolves a [RequestStatus] from a display string (e.g. "Pending") or enum name.
         */
        fun fromString(value: String): RequestStatus? {
            return entries.find {
                it.displayName.equals(value.trim(), ignoreCase = true) ||
                it.name.equals(value.trim(), ignoreCase = true)
            }
        }
    }
}
