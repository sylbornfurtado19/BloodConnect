package com.bloodconnect.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val displayName: String = "",
    val bloodGroup: String = "",
    val phone: String = "",
    val locality: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,
    val livesSaved: Int = 0,
    val donorTier: String = "Bronze",
    val fcmToken: String? = null,
    val lastDonationAt: Long? = null,
    val contactable: Boolean = true,
    val role: String = "DONOR",
    val organizationName: String = ""
) {
    @get:Exclude
    val isHospitalAdmin: Boolean
        get() = role.equals("HOSPITAL_ADMIN", ignoreCase = true) || role.equals("ADMIN", ignoreCase = true)

    @get:Exclude
    val effectiveInitials: String
        get() {
            val name = (displayName.ifBlank { fullName }).trim()
            if (name.isBlank()) return "U"
            val parts = name.split("\\s+".toRegex()).filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "U"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
            }
        }
    /**
     * Backward-compatibility setter for legacy Firestore documents containing only the old "available" field.
     * Excluded from serialization so only the standardized "isAvailable" field is written to Firestore.
     */
    @get:Exclude
    @set:PropertyName("available")
    var available: Boolean? = null
        set(value) {
            field = value
            if (value != null && !hasModernIsAvailable) {
                isAvailable = value
            }
        }

    @get:Exclude
    var hasModernIsAvailable: Boolean = false
        private set

    /**
     * Helper to set modern availability with precedence over legacy field.
     */
    fun applyModernAvailability(value: Boolean) {
        this.isAvailable = value
        this.hasModernIsAvailable = true
    }
}
