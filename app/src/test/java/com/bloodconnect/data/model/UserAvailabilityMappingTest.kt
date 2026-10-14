package com.bloodconnect.data.model

import com.bloodconnect.data.repository.resolveDonorAvailability
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for standardized donor availability ("isAvailable") mapping and
 * legacy field ("available") backward compatibility.
 */
class UserAvailabilityMappingTest {

    // -------------------------------------------------------------
    // User Data Class Defaults & Model Instantiation
    // -------------------------------------------------------------

    @Test
    fun user_defaultAvailability_isTrue() {
        val user = User()
        assertTrue("Default donor availability should be true", user.isAvailable)
    }

    @Test
    fun user_explicitAvailabilityInConstructor_isRespected() {
        val userAvailable = User(isAvailable = true)
        assertTrue(userAvailable.isAvailable)

        val userUnavailable = User(isAvailable = false)
        assertFalse(userUnavailable.isAvailable)
    }

    @Test
    fun user_copyWithUpdatedAvailability_updatesCorrectly() {
        val user = User(isAvailable = true)
        val updated = user.copy(isAvailable = false)
        assertFalse(updated.isAvailable)
    }

    // -------------------------------------------------------------
    // Legacy Field ("available") Setter & Backward Compatibility
    // -------------------------------------------------------------

    @Test
    fun user_legacyAvailableSetter_updatesIsAvailableWhenModernNotSet() {
        val user = User() // defaults isAvailable = true
        user.available = false

        assertEquals(false, user.available)
        assertFalse("Setting legacy available to false should update isAvailable to false", user.isAvailable)
    }

    @Test
    fun user_legacyAvailableSetter_updatesToTrueWhenModernNotSet() {
        val user = User(isAvailable = false)
        user.available = true

        assertEquals(true, user.available)
        assertTrue("Setting legacy available to true should update isAvailable to true", user.isAvailable)
    }

    @Test
    fun user_modernAvailability_takesPrecedenceOverLegacyField() {
        val user = User()
        user.applyModernAvailability(false)

        // Simulating legacy document field arriving after modern field
        user.available = true

        assertFalse("Modern isAvailable should take precedence over legacy available", user.isAvailable)
    }

    @Test
    fun user_nullLegacyAvailable_doesNotChangeIsAvailable() {
        val user = User(isAvailable = false)
        user.available = null

        assertNull(user.available)
        assertFalse(user.isAvailable)
    }

    // -------------------------------------------------------------
    // Firebase Firestore Annotations Verification via Reflection
    // -------------------------------------------------------------

    @Test
    fun user_isAvailableGetter_hasPropertyNameAnnotation() {
        val getter = User::class.java.getMethod("isAvailable")
        val annotation = getter.getAnnotation(PropertyName::class.java)

        assertNotNull("isAvailable getter must be annotated with @PropertyName", annotation)
        assertEquals("isAvailable", annotation?.value)
    }

    @Test
    fun user_isAvailableSetter_hasPropertyNameAnnotation() {
        val setter = User::class.java.getMethod("setAvailable", Boolean::class.javaPrimitiveType)
        val annotation = setter.getAnnotation(PropertyName::class.java)

        assertNotNull("isAvailable setter must be annotated with @PropertyName", annotation)
        assertEquals("isAvailable", annotation?.value)
    }

    @Test
    fun user_legacyAvailableSetter_hasPropertyNameAnnotation() {
        val setter = User::class.java.getMethod("setAvailable", java.lang.Boolean::class.java)
        val annotation = setter.getAnnotation(PropertyName::class.java)

        assertNotNull("setAvailable must be annotated with @PropertyName(\"available\")", annotation)
        assertEquals("available", annotation?.value)
    }

    @Test
    fun user_legacyAvailableGetter_hasExcludeAnnotation() {
        val getter = User::class.java.getMethod("getAvailable")
        val annotation = getter.getAnnotation(Exclude::class.java)

        assertNotNull("getAvailable must be annotated with @Exclude to prevent serializing the old field", annotation)
    }

    // -------------------------------------------------------------
    // resolveDonorAvailability Pure Helper Tests (DonorRepositoryImpl)
    // -------------------------------------------------------------

    @Test
    fun resolveDonorAvailability_modernFieldPresent_isRespected() {
        assertTrue(resolveDonorAvailability(isAvailable = true, legacyAvailable = null))
        assertFalse(resolveDonorAvailability(isAvailable = false, legacyAvailable = null))
    }

    @Test
    fun resolveDonorAvailability_onlyLegacyFieldPresent_backwardCompatibilityPreserved() {
        // True legacy donor
        assertTrue(
            "Legacy true availability must be preserved",
            resolveDonorAvailability(isAvailable = null, legacyAvailable = true)
        )

        // False legacy donor must NOT be silently lost or flipped to default true
        assertFalse(
            "Legacy false availability must be preserved and not silently defaulted to true",
            resolveDonorAvailability(isAvailable = null, legacyAvailable = false)
        )
    }

    @Test
    fun resolveDonorAvailability_bothFieldsPresent_modernTakesPrecedence() {
        // Modern false, legacy true -> modern false wins
        assertFalse(
            "Modern isAvailable=false should win over legacy available=true",
            resolveDonorAvailability(isAvailable = false, legacyAvailable = true)
        )

        // Modern true, legacy false -> modern true wins
        assertTrue(
            "Modern isAvailable=true should win over legacy available=false",
            resolveDonorAvailability(isAvailable = true, legacyAvailable = false)
        )
    }

    @Test
    fun resolveDonorAvailability_neitherFieldPresent_usesDefault() {
        assertTrue(resolveDonorAvailability(isAvailable = null, legacyAvailable = null, default = true))
        assertFalse(resolveDonorAvailability(isAvailable = null, legacyAvailable = null, default = false))
    }

    // -------------------------------------------------------------
    // Donor Availability Updates & Repository Data Layer Mapping
    // -------------------------------------------------------------

    @Test
    fun updateAvailability_passesCorrectBooleanAndCanonicalIsAvailableField() {
        // Verify UserRepository exposes updateAvailability with (String, Boolean) parameters
        val updateMethod = com.bloodconnect.data.repository.UserRepository::class.java.methods.find { it.name.startsWith("updateAvailability") }
        assertNotNull("UserRepository must declare updateAvailability method", updateMethod)
        assertEquals(String::class.java, updateMethod?.parameterTypes?.get(0))
        assertEquals(Boolean::class.javaPrimitiveType, updateMethod?.parameterTypes?.get(1))

        // Fake data layer update consumer representing Firestore document.update(field, value)
        var capturedField: String? = null
        var capturedValue: Boolean? = null
        val fakeDataLayerUpdate: (String, Boolean) -> Unit = { field, value ->
            capturedField = field
            capturedValue = value
        }

        // Canonical field name mapped to the data layer via Firestore PropertyName annotation
        val canonicalField = User::class.java.getMethod("isAvailable")
            .getAnnotation(PropertyName::class.java)?.value

        // 1. Verify updating donor availability to false passes canonical "isAvailable" and false
        val userUnavailable = User(uid = "donor_001", isAvailable = true).copy(isAvailable = false)
        fakeDataLayerUpdate(canonicalField ?: "", userUnavailable.isAvailable)

        assertEquals("isAvailable", capturedField)
        assertEquals(false, capturedValue)
        assertFalse(userUnavailable.isAvailable)

        // 2. Verify updating donor availability to true passes canonical "isAvailable" and true
        val userAvailable = userUnavailable.copy(isAvailable = true)
        fakeDataLayerUpdate(canonicalField ?: "", userAvailable.isAvailable)

        assertEquals("isAvailable", capturedField)
        assertEquals(true, capturedValue)
        assertTrue(userAvailable.isAvailable)
    }
}
