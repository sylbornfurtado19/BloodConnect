package com.bloodconnect.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object IntentHelpers {
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
