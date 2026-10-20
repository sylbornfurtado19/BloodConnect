package com.bloodconnect.utils

import com.bloodconnect.data.repository.BloodRequestRepository
import com.bloodconnect.data.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager that synchronizes a donor's profile (lastDonationAt and availability)
 * with confirmed donated pledges recorded by requesters.
 */
@Singleton
class DonationSyncManager @Inject constructor(
    private val requestRepository: BloodRequestRepository,
    private val userRepository: UserRepository
) {
    /**
     * Loads confirmed donated pledges for [uid], updates user's lastDonationAt and sets isAvailable = false
     * if a newer confirmed donation timestamp exists, and returns the total confirmed donation count.
     * Swallows all exceptions.
     */
    suspend fun syncFromPledges(uid: String): Int {
        return try {
            val pledgesResult = requestRepository.getMyDonatedPledges(uid)
            val pledges = pledgesResult.getOrNull() ?: return 0
            if (pledges.isEmpty()) return 0

            val latestTimestamp = pledges.maxOfOrNull { it.donatedAt ?: it.createdAt } ?: 0L
            val userResult = userRepository.getUser(uid)
            val user = userResult.getOrNull()

            val currentLastDonation = user?.lastDonationAt ?: 0L
            if (latestTimestamp > currentLastDonation) {
                userRepository.recordDonation(uid, latestTimestamp)
            }

            pledges.size
        } catch (_: Exception) {
            0
        }
    }
}
