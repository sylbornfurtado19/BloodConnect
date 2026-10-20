package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for request lifecycle extensions (Part 1):
 * - RequestStatus parsing for Partially Fulfilled and Expired
 * - effectiveStatus time-based expiration logic
 * - FULFILLED immutability against expiration
 */
class RequestLifecycleTest {

    @Test
    fun requestStatus_fromString_parsesPartiallyFulfilledAndExpired() {
        assertEquals(RequestStatus.PARTIALLY_FULFILLED, RequestStatus.fromString("Partially Fulfilled"))
        assertEquals(RequestStatus.PARTIALLY_FULFILLED, RequestStatus.fromString("partially fulfilled"))
        assertEquals(RequestStatus.PARTIALLY_FULFILLED, RequestStatus.fromString("PARTIALLY_FULFILLED"))
        assertEquals(RequestStatus.PARTIALLY_FULFILLED, RequestStatus.fromString("Partial"))

        assertEquals(RequestStatus.EXPIRED, RequestStatus.fromString("Expired"))
        assertEquals(RequestStatus.EXPIRED, RequestStatus.fromString("expired"))
        assertEquals(RequestStatus.EXPIRED, RequestStatus.fromString("EXPIRED"))
    }

    @Test
    fun effectiveStatus_isExpired_whenPendingAndExpiresAtInThePast() {
        val pastTime = System.currentTimeMillis() - 10000L
        val request = BloodRequest(
            status = RequestStatus.PENDING.displayName,
            expiresAt = pastTime
        )

        assertEquals(RequestStatus.EXPIRED, request.effectiveStatus)
        assertFalse(request.isActive)
    }

    @Test
    fun effectiveStatus_staysPending_whenExpiresAtZero() {
        val request = BloodRequest(
            status = RequestStatus.PENDING.displayName,
            expiresAt = 0L
        )

        assertEquals(RequestStatus.PENDING, request.effectiveStatus)
        assertTrue(request.isActive)
    }

    @Test
    fun effectiveStatus_fulfilledNeverBecomesExpired() {
        val pastTime = System.currentTimeMillis() - 10000L
        val request = BloodRequest(
            status = RequestStatus.FULFILLED.displayName,
            expiresAt = pastTime
        )

        assertEquals(RequestStatus.FULFILLED, request.effectiveStatus)
        assertFalse(request.isActive)
    }
}
