package com.bloodconnect.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests for BloodRequest canonical model refactoring:
 * - Canonical properties: id, unitsNeeded, hospital, createdAt
 * - Backward-compatible property aliases: requestId, unitsRequired, hospitalName, timestamp
 * - Bidirectional Firestore serialization annotations & legacy fallback mapping
 */
class BloodRequestModelCanonicalTest {

    // -------------------------------------------------------------
    // Canonical Property Defaults
    // -------------------------------------------------------------

    @Test
    fun bloodRequest_canonicalDefaults_areCorrect() {
        val request = BloodRequest()

        assertEquals("", request.id)
        assertEquals(1, request.unitsNeeded)
        assertEquals("", request.hospital)
        assertTrue("createdAt should be a valid positive epoch timestamp", request.createdAt > 0L)
        assertEquals("Pending", request.status)
    }

    // -------------------------------------------------------------
    // Canonical Construction & Legacy Getter Aliases
    // -------------------------------------------------------------

    @Test
    fun bloodRequest_canonicalConstructor_populatesCanonicalAndAliases() {
        val testTime = 1728900000000L
        val request = BloodRequest(
            id = "REQ-101",
            unitsNeeded = 4,
            hospital = "City General Hospital",
            city = "Central Sector",
            createdAt = testTime
        )

        // Canonical properties
        assertEquals("REQ-101", request.id)
        assertEquals(4, request.unitsNeeded)
        assertEquals("City General Hospital", request.hospital)
        assertEquals(testTime, request.createdAt)

        // Legacy getter aliases
        assertEquals("REQ-101", request.requestId)
        assertEquals(4, request.unitsRequired)
        assertEquals("City General Hospital", request.hospitalName)
        assertEquals(testTime, request.timestamp.toDate().time)
    }

    // -------------------------------------------------------------
    // Legacy Deserialization Setters (Firestore Backward Compatibility)
    // -------------------------------------------------------------

    @Test
    fun bloodRequest_legacyRequestIdSetter_populatesIdWhenEmpty() {
        val request = BloodRequest()
        assertEquals("", request.id)

        // Simulate Firestore parsing a legacy document containing only "requestId"
        request.legacyRequestId = "LEGACY-REQ-99"

        assertEquals("LEGACY-REQ-99", request.id)
        assertEquals("LEGACY-REQ-99", request.requestId)
    }

    @Test
    fun bloodRequest_modernId_takesPrecedenceOverLegacyRequestId() {
        val request = BloodRequest(id = "MODERN-ID-1")

        // Simulate legacy "requestId" field arriving after modern "id"
        request.legacyRequestId = "OLD-REQUEST-ID"

        assertEquals("MODERN-ID-1", request.id)
        assertEquals("MODERN-ID-1", request.requestId)
    }

    @Test
    fun bloodRequest_legacyUnitsRequiredSetter_populatesUnitsNeededWhenDefault() {
        val request = BloodRequest() // unitsNeeded defaults to 1

        // Simulate legacy document with "unitsRequired" = 5
        request.legacyUnitsRequired = 5

        assertEquals(5, request.unitsNeeded)
        assertEquals(5, request.unitsRequired)
    }

    @Test
    fun bloodRequest_explicitUnitsNeeded_takesPrecedenceOverLegacyUnitsRequired() {
        val request = BloodRequest(unitsNeeded = 3)

        // Legacy unitsRequired arriving when modern unitsNeeded was already set
        request.legacyUnitsRequired = 5

        assertEquals(3, request.unitsNeeded)
        assertEquals(3, request.unitsRequired)
    }

    @Test
    fun bloodRequest_legacyHospitalNameSetter_populatesHospitalWhenEmpty() {
        val request = BloodRequest()
        assertEquals("", request.hospital)

        // Simulate legacy document with "hospitalName"
        request.legacyHospitalName = "Metro Care Center"

        assertEquals("Metro Care Center", request.hospital)
        assertEquals("Metro Care Center", request.hospitalName)
    }

    @Test
    fun bloodRequest_modernHospital_takesPrecedenceOverLegacyHospitalName() {
        val request = BloodRequest(hospital = "St. Jude Regional")

        request.legacyHospitalName = "Old Hospital Name"

        assertEquals("St. Jude Regional", request.hospital)
        assertEquals("St. Jude Regional", request.hospitalName)
    }

    @Test
    fun bloodRequest_timestampSetter_synchronizesCreatedAt() {
        val request = BloodRequest()
        val expectedEpoch = 1729000000000L
        val timestamp = Timestamp(Date(expectedEpoch))

        // Simulate Firestore setting Timestamp on legacy document
        request.timestamp = timestamp

        assertEquals(expectedEpoch, request.createdAt)
        assertEquals(timestamp, request.timestamp)
    }

    // -------------------------------------------------------------
    // Factory Method Compatibility (BloodRequest.create)
    // -------------------------------------------------------------

    @Test
    fun bloodRequest_factoryCreate_supportsLegacyParameters() {
        val expectedEpoch = 1728000000000L
        val legacyTimestamp = Timestamp(Date(expectedEpoch))

        val request = BloodRequest.create(
            requestId = "LEGACY-FACTORY-1",
            unitsRequired = 3,
            hospitalName = "Apollo Memorial",
            timestamp = legacyTimestamp
        )

        assertEquals("LEGACY-FACTORY-1", request.id)
        assertEquals("LEGACY-FACTORY-1", request.requestId)
        assertEquals(3, request.unitsNeeded)
        assertEquals(3, request.unitsRequired)
        assertEquals("Apollo Memorial", request.hospital)
        assertEquals("Apollo Memorial", request.hospitalName)
        assertEquals(expectedEpoch, request.createdAt)
        assertEquals(legacyTimestamp, request.timestamp)
    }

    @Test
    fun bloodRequest_factoryCreate_prefersModernParametersOverLegacy() {
        val request = BloodRequest.create(
            id = "MODERN-ID",
            requestId = "LEGACY-ID",
            unitsNeeded = 4,
            unitsRequired = 2,
            hospital = "Modern Clinic",
            hospitalName = "Legacy Hospital"
        )

        assertEquals("MODERN-ID", request.id)
        assertEquals(4, request.unitsNeeded)
        assertEquals("Modern Clinic", request.hospital)
    }

    // -------------------------------------------------------------
    // Reflection Inspection for Firestore Annotations
    // -------------------------------------------------------------

    @Test
    fun bloodRequest_canonicalGetters_havePropertyNameAnnotations() {
        val idGetter = BloodRequest::class.java.getMethod("getId")
        val idAnnotation = idGetter.getAnnotation(PropertyName::class.java)
        assertNotNull("getId must have @PropertyName", idAnnotation)
        assertEquals("id", idAnnotation?.value)

        val unitsNeededGetter = BloodRequest::class.java.getMethod("getUnitsNeeded")
        val unitsAnnotation = unitsNeededGetter.getAnnotation(PropertyName::class.java)
        assertNotNull("getUnitsNeeded must have @PropertyName", unitsAnnotation)
        assertEquals("unitsNeeded", unitsAnnotation?.value)

        val hospitalGetter = BloodRequest::class.java.getMethod("getHospital")
        val hospitalAnnotation = hospitalGetter.getAnnotation(PropertyName::class.java)
        assertNotNull("getHospital must have @PropertyName", hospitalAnnotation)
        assertEquals("hospital", hospitalAnnotation?.value)

        val createdAtGetter = BloodRequest::class.java.getMethod("getCreatedAt")
        val createdAtAnnotation = createdAtGetter.getAnnotation(PropertyName::class.java)
        assertNotNull("getCreatedAt must have @PropertyName", createdAtAnnotation)
        assertEquals("createdAt", createdAtAnnotation?.value)
    }

    @Test
    fun bloodRequest_legacyGetters_havePropertyNameOrExclude() {
        // requestId alias getter is excluded; legacyRequestId has @PropertyName("requestId")
        val requestIdGetter = BloodRequest::class.java.getMethod("getRequestId")
        assertNotNull("getRequestId must have @Exclude", requestIdGetter.getAnnotation(Exclude::class.java))

        val legacyRequestIdGetter = BloodRequest::class.java.getMethod("getLegacyRequestId")
        val reqIdAnnotation = legacyRequestIdGetter.getAnnotation(PropertyName::class.java)
        assertNotNull("getLegacyRequestId must have @PropertyName", reqIdAnnotation)
        assertEquals("requestId", reqIdAnnotation?.value)

        // unitsRequired alias getter is excluded; legacyUnitsRequired has @PropertyName("unitsRequired")
        val unitsRequiredGetter = BloodRequest::class.java.getMethod("getUnitsRequired")
        assertNotNull("getUnitsRequired must have @Exclude", unitsRequiredGetter.getAnnotation(Exclude::class.java))

        val legacyUnitsRequiredGetter = BloodRequest::class.java.getMethod("getLegacyUnitsRequired")
        val unitsReqAnnotation = legacyUnitsRequiredGetter.getAnnotation(PropertyName::class.java)
        assertNotNull("getLegacyUnitsRequired must have @PropertyName", unitsReqAnnotation)
        assertEquals("unitsRequired", unitsReqAnnotation?.value)

        // hospitalName alias getter is excluded; legacyHospitalName has @PropertyName("hospitalName")
        val hospitalNameGetter = BloodRequest::class.java.getMethod("getHospitalName")
        assertNotNull("getHospitalName must have @Exclude", hospitalNameGetter.getAnnotation(Exclude::class.java))

        val legacyHospitalNameGetter = BloodRequest::class.java.getMethod("getLegacyHospitalName")
        val hospNameAnnotation = legacyHospitalNameGetter.getAnnotation(PropertyName::class.java)
        assertNotNull("getLegacyHospitalName must have @PropertyName", hospNameAnnotation)
        assertEquals("hospitalName", hospNameAnnotation?.value)

        // timestamp getter has @PropertyName("timestamp")
        val timestampGetter = BloodRequest::class.java.getMethod("getTimestamp")
        val tsAnnotation = timestampGetter.getAnnotation(PropertyName::class.java)
        assertNotNull("getTimestamp must have @PropertyName", tsAnnotation)
        assertEquals("timestamp", tsAnnotation?.value)
    }
}
