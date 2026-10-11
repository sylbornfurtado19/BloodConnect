package com.bloodconnect.data.model

/**
 * Immutable emergency blood request record.
 *
 * @property id Unique blood request identifier.
 * @property requesterId Identifier of the user or hospital staff who placed the request.
 * @property bloodGroup Target blood group required.
 * @property unitsNeeded Quantity of whole blood units requested.
 * @property hospital Name of the medical center or hospital facility.
 * @property city Locality or city where the blood is needed.
 * @property urgency Severity level ([Urgency.ROUTINE], [Urgency.URGENT], [Urgency.CRITICAL]).
 * @property status Current lifecycle state ([RequestStatus.PENDING], [RequestStatus.FULFILLED], [RequestStatus.CANCELLED]).
 * @property createdAt Epoch millisecond timestamp of request creation.
 */
data class BloodRequest(
    val id: String,
    val requesterId: String,
    val bloodGroup: BloodGroup,
    val unitsNeeded: Int,
    val hospital: String,
    val city: String,
    val urgency: Urgency,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
