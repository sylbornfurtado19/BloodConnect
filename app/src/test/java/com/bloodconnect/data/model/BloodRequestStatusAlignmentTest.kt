package com.bloodconnect.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for standardized request status values ("Pending", "Fulfilled", "Cancelled"),
 * legacy compatibility ("Active", "PENDING", "Canceled"), model serialization, and filtering.
 */
class BloodRequestStatusAlignmentTest {

    // -------------------------------------------------------------
    // RequestStatus Enum & Canonical Values
    // -------------------------------------------------------------

    @Test
    fun requestStatus_canonicalValues_areStandardized() {
        assertEquals("Pending", RequestStatus.PENDING.displayName)
        assertEquals("Fulfilled", RequestStatus.FULFILLED.displayName)
        assertEquals("Cancelled", RequestStatus.CANCELLED.displayName)

        assertEquals("Pending", RequestStatus.PENDING.canonicalValue)
        assertEquals("Fulfilled", RequestStatus.FULFILLED.canonicalValue)
        assertEquals("Cancelled", RequestStatus.CANCELLED.canonicalValue)
    }

    @Test
    fun requestStatus_fromString_parsesCanonicalDisplayNames() {
        assertEquals(RequestStatus.PENDING, RequestStatus.fromString("Pending"))
        assertEquals(RequestStatus.FULFILLED, RequestStatus.fromString("Fulfilled"))
        assertEquals(RequestStatus.CANCELLED, RequestStatus.fromString("Cancelled"))

        // Case-insensitive
        assertEquals(RequestStatus.PENDING, RequestStatus.fromString("pending"))
        assertEquals(RequestStatus.FULFILLED, RequestStatus.fromString("fulfilled"))
        assertEquals(RequestStatus.CANCELLED, RequestStatus.fromString("cancelled"))
    }

    @Test
    fun requestStatus_fromString_parsesLegacyEnumNames() {
        assertEquals(RequestStatus.PENDING, RequestStatus.fromString("PENDING"))
        assertEquals(RequestStatus.FULFILLED, RequestStatus.fromString("FULFILLED"))
        assertEquals(RequestStatus.CANCELLED, RequestStatus.fromString("CANCELLED"))
    }

    @Test
    fun requestStatus_fromString_parsesLegacyActiveAsPending() {
        // "Active" was previously used inconsistently in BloodRequest.kt and BloodRequestRepository.kt
        assertEquals(RequestStatus.PENDING, RequestStatus.fromString("Active"))
        assertEquals(RequestStatus.PENDING, RequestStatus.fromString("active"))
        assertEquals(RequestStatus.PENDING, RequestStatus.fromString("ACTIVE"))
    }

    @Test
    fun requestStatus_fromString_parsesAlternativeSpelling() {
        // Single 'l' spelling compatibility
        assertEquals(RequestStatus.CANCELLED, RequestStatus.fromString("Canceled"))
        assertEquals(RequestStatus.CANCELLED, RequestStatus.fromString("canceled"))
        assertEquals(RequestStatus.CANCELLED, RequestStatus.fromString("CANCELED"))
    }

    @Test
    fun requestStatus_fromString_parsesDemoFormats() {
        // UI demo dataset formats with suffixes
        assertEquals(RequestStatus.PENDING, RequestStatus.fromString("Pending (Demo)"))
        assertEquals(RequestStatus.FULFILLED, RequestStatus.fromString("Fulfilled (Demo)"))
        assertEquals(RequestStatus.CANCELLED, RequestStatus.fromString("Cancelled (Demo)"))
    }

    @Test
    fun requestStatus_fromString_handlesWhitespaceAndTrimming() {
        assertEquals(RequestStatus.PENDING, RequestStatus.fromString("   Pending  "))
        assertEquals(RequestStatus.PENDING, RequestStatus.fromString("  Active \n"))
        assertEquals(RequestStatus.FULFILLED, RequestStatus.fromString("\tFulfilled\t"))
    }

    @Test
    fun requestStatus_fromString_invalidOrBlank_returnsNull() {
        assertNull(RequestStatus.fromString(null))
        assertNull(RequestStatus.fromString(""))
        assertNull(RequestStatus.fromString("   "))
        assertNull(RequestStatus.fromString("UnknownStatus"))
        assertNull(RequestStatus.fromString("In_Review"))
    }

    @Test
    fun requestStatus_fromStringOrDefault_returnsDefaultOnUnrecognized() {
        assertEquals(RequestStatus.PENDING, RequestStatus.fromStringOrDefault("Active"))
        assertEquals(RequestStatus.PENDING, RequestStatus.fromStringOrDefault(null))
        assertEquals(RequestStatus.CANCELLED, RequestStatus.fromStringOrDefault("Unknown", RequestStatus.CANCELLED))
    }

    // -------------------------------------------------------------
    // BloodRequest Data Class Model & Deserialization Compatibility
    // -------------------------------------------------------------

    @Test
    fun bloodRequest_defaultStatus_isCanonicalPending() {
        val request = BloodRequest()
        assertEquals("Pending", request.status)
        assertEquals(RequestStatus.PENDING, request.requestStatus)
        assertEquals("Pending", request.canonicalStatus)
    }

    @Test
    fun bloodRequest_legacyActiveDocument_deserializesAndResolvesSafely() {
        // Simulating existing Firestore document with status = "Active"
        val request = BloodRequest(status = "Active")
        assertEquals("Active", request.status)
        assertEquals(RequestStatus.PENDING, request.requestStatus)
        assertEquals("Pending", request.canonicalStatus)

        val normalized = request.withCanonicalStatus()
        assertEquals("Pending", normalized.status)
    }

    @Test
    fun bloodRequest_legacyUppercasePending_resolvesSafely() {
        // Simulating document with status = "PENDING"
        val request = BloodRequest(status = "PENDING")
        assertEquals("PENDING", request.status)
        assertEquals(RequestStatus.PENDING, request.requestStatus)
        assertEquals("Pending", request.canonicalStatus)

        val normalized = request.withCanonicalStatus()
        assertEquals("Pending", normalized.status)
    }

    @Test
    fun bloodRequest_fulfilledAndCancelledStatuses_resolveCorrectly() {
        val fulfilledReq = BloodRequest(status = "Fulfilled")
        assertEquals(RequestStatus.FULFILLED, fulfilledReq.requestStatus)
        assertEquals("Fulfilled", fulfilledReq.canonicalStatus)

        val cancelledReq = BloodRequest(status = "Cancelled")
        assertEquals(RequestStatus.CANCELLED, cancelledReq.requestStatus)
        assertEquals("Cancelled", cancelledReq.canonicalStatus)
    }

    @Test
    fun bloodRequest_withCanonicalStatus_normalizesAllVariants() {
        val activeReq = BloodRequest(status = "Active").withCanonicalStatus()
        assertEquals("Pending", activeReq.status)

        val upperReq = BloodRequest(status = "PENDING").withCanonicalStatus()
        assertEquals("Pending", upperReq.status)

        val canceledReq = BloodRequest(status = "Canceled").withCanonicalStatus()
        assertEquals("Cancelled", canceledReq.status)
    }

    @Test
    fun bloodRequest_computedProperties_haveExcludeAnnotation() {
        val requestStatusGetter = BloodRequest::class.java.getMethod("getRequestStatus")
        assertNotNull(
            "requestStatus getter must be annotated with @Exclude",
            requestStatusGetter.getAnnotation(Exclude::class.java)
        )

        val canonicalStatusGetter = BloodRequest::class.java.getMethod("getCanonicalStatus")
        assertNotNull(
            "canonicalStatus getter must be annotated with @Exclude",
            canonicalStatusGetter.getAnnotation(Exclude::class.java)
        )
    }

    @Test
    fun bloodRequest_statusProperty_hasPropertyNameAnnotation() {
        val statusGetter = BloodRequest::class.java.getMethod("getStatus")
        val annotation = statusGetter.getAnnotation(PropertyName::class.java)
        assertNotNull("status getter should have @PropertyName", annotation)
        assertEquals("status", annotation?.value)
    }

    // -------------------------------------------------------------
    // Status Filtering Logic (UI & Repository Query Validation)
    // -------------------------------------------------------------

    private fun filterRequests(
        requests: List<BloodRequest>,
        selectedFilter: String
    ): List<BloodRequest> {
        if (selectedFilter == "All") return requests
        val filterStatus = RequestStatus.fromString(selectedFilter)
        return requests.filter {
            val reqStatus = RequestStatus.fromString(it.status)
            reqStatus == filterStatus || it.status.startsWith(selectedFilter, ignoreCase = true)
        }
    }

    @Test
    fun statusFiltering_allFilter_returnsEntireDataset() {
        val dataset = listOf(
            BloodRequest(id = "1", status = "Pending"),
            BloodRequest(id = "2", status = "Active"),
            BloodRequest(id = "3", status = "PENDING"),
            BloodRequest(id = "4", status = "Fulfilled"),
            BloodRequest(id = "5", status = "Cancelled")
        )

        val result = filterRequests(dataset, "All")
        assertEquals(5, result.size)
    }

    @Test
    fun statusFiltering_pendingFilter_matchesCanonicalAndLegacyRepresentations() {
        val dataset = listOf(
            BloodRequest(id = "1", status = "Pending"),
            BloodRequest(id = "2", status = "Active"),     // legacy from earlier model
            BloodRequest(id = "3", status = "PENDING"),    // legacy uppercase enum
            BloodRequest(id = "4", status = "pending"),    // lowercase
            BloodRequest(id = "5", status = "Pending (Demo)"), // UI demo format
            BloodRequest(id = "6", status = "Fulfilled"),
            BloodRequest(id = "7", status = "Cancelled")
        )

        val result = filterRequests(dataset, "Pending")
        assertEquals(5, result.size)
        assertTrue(result.all { it.requestStatus == RequestStatus.PENDING })
    }

    @Test
    fun statusFiltering_fulfilledFilter_matchesOnlyFulfilled() {
        val dataset = listOf(
            BloodRequest(id = "1", status = "Pending"),
            BloodRequest(id = "2", status = "Active"),
            BloodRequest(id = "3", status = "Fulfilled"),
            BloodRequest(id = "4", status = "FULFILLED"),
            BloodRequest(id = "5", status = "Fulfilled (Demo)"),
            BloodRequest(id = "6", status = "Cancelled")
        )

        val result = filterRequests(dataset, "Fulfilled")
        assertEquals(3, result.size)
        assertTrue(result.all { it.requestStatus == RequestStatus.FULFILLED })
    }

    @Test
    fun statusFiltering_cancelledFilter_matchesOnlyCancelled() {
        val dataset = listOf(
            BloodRequest(id = "1", status = "Pending"),
            BloodRequest(id = "2", status = "Active"),
            BloodRequest(id = "3", status = "Cancelled"),
            BloodRequest(id = "4", status = "CANCELLED"),
            BloodRequest(id = "5", status = "Canceled"), // single 'l'
            BloodRequest(id = "6", status = "Cancelled (Demo)"),
            BloodRequest(id = "7", status = "Fulfilled")
        )

        val result = filterRequests(dataset, "Cancelled")
        assertEquals(4, result.size)
        assertTrue(result.all { it.requestStatus == RequestStatus.CANCELLED })
    }
}
