package com.bloodconnect.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Immutable/deserializable emergency blood request record.
 *
 * Canonical Kotlin Properties:
 * - [id]: Primary unique request identifier.
 * - [unitsNeeded]: Quantity of blood units requested.
 * - [unitsReceived]: Quantity of blood units received/confirmed so far.
 * - [hospital]: Medical facility or hospital name.
 * - [createdAt]: Epoch millisecond creation timestamp.
 * - [expiresAt]: Epoch millisecond expiration timestamp (0L = never expires).
 * - [status]: Standardized status string (e.g. "Pending", "Partially Fulfilled", "Fulfilled", "Cancelled").
 * - [notes]: Optional additional details or instructions.
 *
 * Preserves bidirectional Firestore compatibility for legacy fields:
 * - "requestId" <-> "id"
 * - "unitsRequired" <-> "unitsNeeded"
 * - "hospitalName" <-> "hospital"
 * - "timestamp" (Firebase Timestamp) <-> "createdAt" (Long epoch millis)
 */
data class BloodRequest(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val bloodGroup: String = "",
    @get:PropertyName("unitsNeeded")
    @set:PropertyName("unitsNeeded")
    var unitsNeeded: Int = 1,
    val unitsReceived: Int = 0,
    @get:PropertyName("hospital")
    @set:PropertyName("hospital")
    var hospital: String = "",
    val city: String = "",
    val urgency: String = Urgency.ROUTINE.displayName,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = RequestStatus.PENDING.displayName,
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L,
    val notes: String = "",
    val contactNumber: String = "",
    val patientReference: String = "",
    /**
     * verified is set to true only by an admin in the Firebase Console (or Admin SDK), never by the app.
     */
    val verified: Boolean = false
) {
    // ------------------------------------------------------------------------
    // Backward-Compatible Property Aliases & Firestore Field Mappings
    // ------------------------------------------------------------------------

    /**
     * Backward-compatible alias for [id].
     */
    @get:Exclude
    val requestId: String
        get() = id

    /**
     * Firestore legacy field mapping for "requestId".
     * Reads legacy "requestId" if [id] is empty, and mirrors [id] on write.
     */
    @get:PropertyName("requestId")
    @set:PropertyName("requestId")
    var legacyRequestId: String?
        get() = id.ifBlank { null }
        set(value) {
            if (!value.isNullOrBlank() && id.isBlank()) {
                id = value
            }
        }

    /**
     * Backward-compatible alias for [unitsNeeded].
     */
    @get:Exclude
    val unitsRequired: Int
        get() = unitsNeeded

    /**
     * Firestore legacy field mapping for "unitsRequired".
     */
    @get:PropertyName("unitsRequired")
    @set:PropertyName("unitsRequired")
    var legacyUnitsRequired: Int?
        get() = unitsNeeded
        set(value) {
            if (value != null && value > 0 && unitsNeeded == 1) {
                unitsNeeded = value
            }
        }

    /**
     * Backward-compatible alias for [hospital].
     */
    @get:Exclude
    val hospitalName: String
        get() = hospital

    /**
     * Firestore legacy field mapping for "hospitalName".
     */
    @get:PropertyName("hospitalName")
    @set:PropertyName("hospitalName")
    var legacyHospitalName: String?
        get() = hospital.ifBlank { null }
        set(value) {
            if (!value.isNullOrBlank() && hospital.isBlank()) {
                hospital = value
            }
        }

    /**
     * Backward-compatible alias for [createdAt] represented as a Firebase [Timestamp].
     */
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Timestamp
        get() = Timestamp(Date(createdAt))
        set(value) {
            createdAt = value.toDate().time
        }

    // ------------------------------------------------------------------------
    // Status Resolution Helpers
    // ------------------------------------------------------------------------

    /**
     * Resolves the strongly-typed [RequestStatus] enum for this request.
     */
    @get:Exclude
    val requestStatus: RequestStatus
        get() = RequestStatus.fromString(status) ?: RequestStatus.PENDING

    /**
     * Computes the effective request status taking time-based expiration into account.
     * Derived dynamically so "Expired" status is never written to Firestore.
     */
    @get:Exclude
    val effectiveStatus: RequestStatus
        get() {
            val baseStatus = requestStatus
            val isPendingOrPartial = baseStatus == RequestStatus.PENDING || baseStatus == RequestStatus.PARTIALLY_FULFILLED
            return if (isPendingOrPartial && expiresAt > 0L && System.currentTimeMillis() > expiresAt) {
                RequestStatus.EXPIRED
            } else {
                baseStatus
            }
        }

    /**
     * Indicates whether the request is active and open for donor pledges.
     */
    @get:Exclude
    val isActive: Boolean
        get() = effectiveStatus == RequestStatus.PENDING || effectiveStatus == RequestStatus.PARTIALLY_FULFILLED

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

    // ------------------------------------------------------------------------
    // Urgency Resolution Helpers
    // ------------------------------------------------------------------------

    /**
     * Resolves the strongly-typed [Urgency] enum for this request.
     */
    @get:Exclude
    val urgencyLevel: Urgency
        get() = Urgency.fromStringOrDefault(urgency, Urgency.ROUTINE)

    /**
     * Returns the standardized canonical urgency string ("Routine", "Urgent", "Critical").
     */
    @get:Exclude
    val canonicalUrgency: String
        get() = urgencyLevel.displayName

    /**
     * Returns a copy with the urgency normalized to the canonical representation.
     */
    fun withCanonicalUrgency(): BloodRequest = copy(urgency = canonicalUrgency)

    companion object {
        /**
         * Factory allowing instantiation with any combination of modern or legacy parameters.
         */
        fun create(
            id: String = "",
            requestId: String = "",
            requesterId: String = "",
            requesterName: String = "",
            bloodGroup: String = "",
            unitsNeeded: Int = 1,
            unitsRequired: Int = 1,
            unitsReceived: Int = 0,
            hospital: String = "",
            hospitalName: String = "",
            city: String = "",
            urgency: String = Urgency.ROUTINE.displayName,
            latitude: Double = 0.0,
            longitude: Double = 0.0,
            status: String = RequestStatus.PENDING.displayName,
            createdAt: Long = System.currentTimeMillis(),
            expiresAt: Long = 0L,
            timestamp: Timestamp? = null,
            notes: String = "",
            contactNumber: String = "",
            patientReference: String = "",
            verified: Boolean = false
        ): BloodRequest {
            return BloodRequest(
                id = id.ifBlank { requestId },
                requesterId = requesterId,
                requesterName = requesterName,
                bloodGroup = bloodGroup,
                unitsNeeded = if (unitsNeeded != 1) unitsNeeded else unitsRequired,
                unitsReceived = unitsReceived,
                hospital = hospital.ifBlank { hospitalName },
                city = city,
                urgency = Urgency.fromStringOrDefault(urgency).displayName,
                latitude = latitude,
                longitude = longitude,
                status = status,
                createdAt = timestamp?.toDate()?.time ?: createdAt,
                expiresAt = expiresAt,
                notes = notes,
                contactNumber = contactNumber,
                patientReference = patientReference,
                verified = verified
            )
        }
    }
}
