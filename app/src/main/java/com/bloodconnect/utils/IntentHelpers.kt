package com.bloodconnect.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.bloodconnect.data.model.BloodRequest

/**
 * Top-level helper function to open directions to a hospital/medical facility.
 *
 * Flow:
 * 1. If both [latitude] and [longitude] are non-zero, attempts to launch Google Maps navigation
 *    via `google.navigation:q=lat,lon` with package `com.google.android.apps.maps`.
 * 2. On [ActivityNotFoundException] or if coordinates are zero, falls back to a geo intent
 *    `geo:0,0?q=<Uri.encode("hospital, city")>`.
 * 3. If that also fails, opens `https://www.google.com/maps/dir/?api=1&destination=<encoded>` in the browser.
 * 4. As a last resort, displays a Toast "No maps app found". Never crashes.
 */
fun openDirections(
    context: Context,
    latitude: Double,
    longitude: Double,
    hospital: String,
    city: String
) {
    val nonZeroCoords = latitude != 0.0 && longitude != 0.0
    if (nonZeroCoords) {
        try {
            val navUri = Uri.parse("google.navigation:q=$latitude,$longitude")
            val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(navIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall back to geo intent below
        } catch (_: Exception) {
            // Fall back to geo intent below
        }
    }

    val query = listOf(hospital.trim(), city.trim())
        .filter { it.isNotEmpty() }
        .joinToString(", ")
        .ifEmpty {
            if (nonZeroCoords) "$latitude,$longitude" else ""
        }
    val encoded = Uri.encode(query)

    try {
        val geoUri = Uri.parse("geo:0,0?q=$encoded")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(geoIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // Fall back to browser below
    } catch (_: Exception) {
        // Fall back to browser below
    }

    try {
        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encoded")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(webIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // Fall back to toast below
    } catch (_: Exception) {
        // Fall back to toast below
    }

    try {
        Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        // Safe fail - never crash
    }
}

/**
 * Top-level helper function to dial a phone number via [Intent.ACTION_DIAL].
 * Pre-fills the system dialer with `tel:<Uri.encode(number)>` without requiring
 * CALL_PHONE runtime permission.
 */
fun dialNumber(
    context: Context,
    number: String
) {
    try {
        val uri = Uri.parse("tel:${Uri.encode(number)}")
        val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(dialIntent)
    } catch (_: Exception) {
        // Safe fail - never crash
    }
}

/**
 * Backward compatibility object for legacy callers.
 */
object IntentHelpers {
    fun buildShareText(request: BloodRequest): String {
        val units = if (request.unitsNeeded > 0) request.unitsNeeded else request.unitsRequired
        val unitsText = if (units == 1) "1 unit" else "$units units"
        val urgencyText = request.urgency.trim().ifBlank { request.urgencyLevel.displayName }
        val contactPart = if (request.contactNumber.isNotBlank()) " Hospital contact: ${request.contactNumber.trim()}." else ""
        return "URGENT: ${request.bloodGroup.trim()} blood needed at ${request.hospital.trim()}, ${request.city.trim()}. $unitsText. $urgencyText.$contactPart Posted on BloodConnect."
    }

    fun shareRequest(context: Context, request: BloodRequest) {
        val text = buildShareText(request)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Share request").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Swallowed if no application can handle share intent
        }
    }

    fun openDirections(
        context: Context,
        latitude: Double,
        longitude: Double,
        hospital: String,
        city: String
    ) {
        com.bloodconnect.utils.openDirections(context, latitude, longitude, hospital, city)
    }

    fun openDirections(
        context: Context,
        latitude: Double?,
        longitude: Double?,
        hospital: String?,
        city: String?
    ) {
        com.bloodconnect.utils.openDirections(
            context = context,
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            hospital = hospital.orEmpty(),
            city = city.orEmpty()
        )
    }

    fun dialNumber(context: Context, number: String) {
        com.bloodconnect.utils.dialNumber(context, number)
    }
}

fun buildShareText(request: BloodRequest): String = IntentHelpers.buildShareText(request)
fun shareRequest(context: Context, request: BloodRequest) = IntentHelpers.shareRequest(context, request)
