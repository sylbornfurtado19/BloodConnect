package com.bloodconnect.data.model

/**
 * Represents a donor's pledge to donate blood for an emergency blood request.
 * Stored in subcollection `blood_requests/{requestId}/pledges/{donorUid}`.
 */
data class Pledge(
    val donorId: String = "",
    val donorName: String = "",
    val bloodGroup: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "Pledged",
    val donatedAt: Long? = null,
    val etaMinutes: Int? = null,
    val requestId: String = "",
    val hospital: String = "",
    val city: String = ""
)
