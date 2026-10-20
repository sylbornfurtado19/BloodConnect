package com.bloodconnect.data.model

/**
 * Donor recognition tier based on confirmed blood donation count.
 */
enum class DonorTier(val displayName: String, val minDonations: Int) {
    BRONZE("Bronze", 0),
    SILVER("Silver", 3),
    GOLD("Gold", 6),
    PLATINUM("Platinum", 10);

    companion object {
        fun fromCount(count: Int): DonorTier {
            return when {
                count >= PLATINUM.minDonations -> PLATINUM
                count >= GOLD.minDonations -> GOLD
                count >= SILVER.minDonations -> SILVER
                else -> BRONZE
            }
        }

        fun nextThreshold(count: Int): Int? {
            return when {
                count < SILVER.minDonations -> SILVER.minDonations
                count < GOLD.minDonations -> GOLD.minDonations
                count < PLATINUM.minDonations -> PLATINUM.minDonations
                else -> null
            }
        }
    }
}
