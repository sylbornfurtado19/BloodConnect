package com.bloodconnect.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bloodconnect.MainActivity
import com.bloodconnect.R
import com.bloodconnect.ui.search.calculateDistanceKm
import com.bloodconnect.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class BloodConnectMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Topic-based notification system; onNewToken is a no-op as per requirements
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isEmpty()) return

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val requesterId = data["requesterId"]
        if (requesterId == currentUser.uid) return

        // Radius check (30 km threshold)
        val requestLat = data["latitude"]?.toDoubleOrNull() ?: 0.0
        val requestLon = data["longitude"]?.toDoubleOrNull() ?: 0.0

        if (requestLat != 0.0 && requestLon != 0.0) {
            val locationResult = try {
                runBlocking {
                    withTimeoutOrNull(2000L) {
                        LocationHelper(applicationContext).getLastKnownLocation()
                    }
                }
            } catch (_: Exception) {
                null
            }

            val lastKnownLocation = locationResult?.getOrNull()
            if (lastKnownLocation != null) {
                val distance = calculateDistanceKm(
                    userLat = lastKnownLocation.latitude,
                    userLon = lastKnownLocation.longitude,
                    donorLat = requestLat,
                    donorLon = requestLon
                )
                if (distance > 30.0) {
                    return // Ignore requests beyond 30 km radius
                }
            }
        }

        // Notification permission check for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val title = data["title"] ?: "Emergency Blood Needed"
        val body = data["body"] ?: "A new emergency blood request matching your group was posted nearby."
        val requestId = data["requestId"] ?: System.currentTimeMillis().toString()
        val urgency = data["urgency"] ?: "Routine"

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("requestId", requestId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isCritical = urgency.equals("Critical", ignoreCase = true)
        val vibrationPattern = if (isCritical) {
            longArrayOf(0, 500, 200, 500, 200, 800)
        } else {
            longArrayOf(0, 250, 250, 250)
        }

        val notification = NotificationCompat.Builder(this, "emergency_requests")
            .setSmallIcon(R.drawable.blood_drop_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setVibrate(vibrationPattern)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(requestId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Ignore permission security exceptions
        }
    }
}
