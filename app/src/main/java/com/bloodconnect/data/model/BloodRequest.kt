package com.bloodconnect.data.model

import com.google.firebase.Timestamp

data class BloodRequest(
    val requestId: String = "",
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val bloodGroup: String = "",
    val unitsNeeded: Int = 1,
    val unitsRequired: Int = 1,
    val hospital: String = "",
    val hospitalName: String = "",
    val city: String = "",
    val urgency: String = "Normal",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "Active",
    val timestamp: Timestamp = Timestamp.now(),
    val createdAt: Long = System.currentTimeMillis()
)
