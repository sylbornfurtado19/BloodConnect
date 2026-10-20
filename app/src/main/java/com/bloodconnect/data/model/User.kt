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
    val lastDonationAt: Long? = null
) {
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
