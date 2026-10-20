package com.bloodconnect.data.model

/**
 * Immutable volunteer donor profile model used for discovery and search matching.
 *
 * @property id Unique donor identifier.
 * @property name Donor's full or volunteer display name.
 * @property bloodGroup ABO/Rh blood group.
 * @property locality City, neighborhood, or operational sector.
 * @property latitude Optional geographic latitude coordinate for radius calculation.
 * @property longitude Optional geographic longitude coordinate for radius calculation.
 * @property isAvailable Current donation availability status.
 * @property lastDonationAt Optional epoch millisecond timestamp of the donor's last blood donation.
 */
data class Donor(
    val id: String,
    val name: String,
    val bloodGroup: BloodGroup,
    val locality: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isAvailable: Boolean = true,
    val lastDonationAt: Long? = null
)
