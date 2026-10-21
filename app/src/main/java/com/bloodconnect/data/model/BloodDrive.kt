package com.bloodconnect.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents a community blood donation camp or drive organized by an institution.
 * Entered/managed by admins; read by app users with RSVP capability.
 */
data class BloodDrive(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    val title: String = "",
    val organizer: String = "",
    val address: String = "",
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val contactNumber: String = "",
    val description: String = ""
)
