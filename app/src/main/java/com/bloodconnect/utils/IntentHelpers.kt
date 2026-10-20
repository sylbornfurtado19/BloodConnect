package com.bloodconnect.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

import com.bloodconnect.data.model.BloodRequest

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
        val query = listOf(hospital, city).filter { it.isNotBlank() }.joinToString(", ")
        val uri = if (latitude != 0.0 || longitude != 0.0) {
            if (query.isNotBlank()) {
                Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(query)})")
            } else {
                Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            }
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val webUri = if (latitude != 0.0 || longitude != 0.0) {
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
            } else {
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(query)}")
            }
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {
                // Swallowed if no browser/map app is available
            }
        }
    }

    fun openDirections(
        context: Context,
        latitude: Double?,
        longitude: Double?,
        hospital: String?,
        city: String?
    ) {
        openDirections(
            context = context,
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            hospital = hospital.orEmpty(),
            city = city.orEmpty()
        )
    }
}

fun buildShareText(request: BloodRequest): String = IntentHelpers.buildShareText(request)
