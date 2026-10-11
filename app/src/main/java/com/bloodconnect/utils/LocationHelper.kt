package com.bloodconnect.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Representation of geographic coordinate positions.
 *
 * @property latitude North/South geographic coordinate.
 * @property longitude East/West geographic coordinate.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double
)

/**
 * Helper for acquiring device geographic coordinates via Google Play Services Location.
 * Keeps runtime permission requesting in the UI layer and returns clear failure results
 * if permissions are missing or hardware location providers are disabled.
 */
@Singleton
class LocationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) {

    /**
     * Determines whether coarse or fine location permission is currently granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Suspends to retrieve the device's current latitude and longitude coordinates.
     *
     * @param highAccuracy If true, requests GPS priority; otherwise uses balanced network accuracy.
     * @return [Result] enclosing [UserLocation] on success or a descriptive failure error.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(highAccuracy: Boolean = true): Result<UserLocation> {
        if (!hasLocationPermission()) {
            return Result.failure(
                SecurityException("Location permission not granted. Please request location permission in the UI before requesting coordinates.")
            )
        }

        return try {
            val priority = if (highAccuracy) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }

            val cancellationTokenSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token).await()
                ?: fusedLocationClient.lastLocation.await()

            if (location != null) {
                Result.success(UserLocation(latitude = location.latitude, longitude = location.longitude))
            } else {
                Result.failure(
                    IllegalStateException("Location is unavailable. Ensure GPS and device location services are enabled.")
                )
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("Location provider failed to resolve position: ${e.localizedMessage ?: "Unknown provider error"}", e)
            )
        }
    }

    /**
     * Retrieves the last known cached location from the provider if available.
     *
     * @return [Result] enclosing cached [UserLocation] or failure if unavailable or unpermitted.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Result<UserLocation> {
        if (!hasLocationPermission()) {
            return Result.failure(
                SecurityException("Location permission not granted. Please request location permission in the UI.")
            )
        }

        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                Result.success(UserLocation(latitude = location.latitude, longitude = location.longitude))
            } else {
                Result.failure(
                    IllegalStateException("No cached location is currently available on this device.")
                )
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("Failed to retrieve cached location: ${e.localizedMessage ?: "Unknown error"}", e)
            )
        }
    }
}
