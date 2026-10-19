package com.bloodconnect.utils

import com.bloodconnect.data.repository.UserRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for non-blocking background location coordinate synchronization.
 */
@Singleton
class LocationSyncManager @Inject constructor(
    private val userRepository: UserRepository,
    private val locationHelper: LocationHelper
) {
    /**
     * Reads the user document via [UserRepository.getUser]; returns unless latitude == 0.0 && longitude == 0.0;
     * then calls [LocationHelper.getCurrentLocation] with highAccuracy = false inside withTimeoutOrNull(5000)
     * and on success updates Firestore via [UserRepository.updateLocation]. Swallows all exceptions.
     */
    suspend fun captureLocationIfMissing(uid: String) {
        try {
            val userResult = userRepository.getUser(uid)
            val user = userResult.getOrNull() ?: return
            if (user.latitude != 0.0 || user.longitude != 0.0) return

            val locationResult = withTimeoutOrNull(5000L) {
                locationHelper.getCurrentLocation(highAccuracy = false)
            }
            val location = locationResult?.getOrNull() ?: return
            userRepository.updateLocation(uid, location.latitude, location.longitude)
        } catch (_: Exception) {
            // Swallow all exceptions to ensure location capture never blocks or disrupts UI
        }
    }
}
