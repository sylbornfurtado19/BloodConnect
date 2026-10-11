package com.bloodconnect.data.model

/**
 * Immutable user account model representing a registered donor or recipient.
 *
 * @property id Unique user identifier (e.g. Firebase Auth UID).
 * @property email Registered email address.
 * @property displayName User's full or preferred display name.
 * @property bloodGroup ABO/Rh blood group of the user.
 * @property isAvailable Current donor readiness/availability status.
 * @property locality City, district, or neighborhood of the user.
 * @property fcmToken Optional Firebase Cloud Messaging device registration token for push alerts.
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val bloodGroup: BloodGroup,
    val isAvailable: Boolean = true,
    val locality: String,
    val fcmToken: String? = null
)
