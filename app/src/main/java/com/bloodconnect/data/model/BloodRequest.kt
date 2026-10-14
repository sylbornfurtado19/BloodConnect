package com.bloodconnect.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

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
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = RequestStatus.PENDING.displayName,
    val timestamp: Timestamp = Timestamp.now(),
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Resolves the strongly-typed [RequestStatus] enum for this request.
     * Legacy statuses like "Active" or all-caps "PENDING" are safely resolved to [RequestStatus.PENDING].
     */
    @get:Exclude
    val requestStatus: RequestStatus
        get() = RequestStatus.fromString(status) ?: RequestStatus.PENDING

    /**
     * Returns the standardized canonical string representation of [status].
     */
    @get:Exclude
    val canonicalStatus: String
        get() = requestStatus.displayName

    /**
     * Returns a copy with the status normalized to the canonical representation.
     */
    fun withCanonicalStatus(): BloodRequest = copy(status = canonicalStatus)
}
