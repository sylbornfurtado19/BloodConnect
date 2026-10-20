package com.bloodconnect.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
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

private fun isAlreadyNotified(context: Context, requestId: String): Boolean {
    val prefs = context.getSharedPreferences("notified_requests_prefs", Context.MODE_PRIVATE)
    val csv = prefs.getString("notified_requests_csv", "") ?: ""
    return NotifiedRequests.contains(csv, requestId)
}

private fun markAsNotified(context: Context, requestId: String) {
    val prefs = context.getSharedPreferences("notified_requests_prefs", Context.MODE_PRIVATE)
    val csv = prefs.getString("notified_requests_csv", "") ?: ""
    val updated = NotifiedRequests.add(csv, requestId, 50)
    prefs.edit().putString("notified_requests_csv", updated).apply()
}

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

        val requestId = data["requestId"] ?: System.currentTimeMillis().toString()

        // De-duplication check at top
        if (isAlreadyNotified(applicationContext, requestId)) {
            return
        }

        // Dynamic Radius check from payload (default 30.0 km)
        val radiusKm = data["radiusKm"]?.toDoubleOrNull() ?: 30.0
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
                if (distance > radiusKm) {
                    return // Ignore requests beyond current broadcast radius
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
            // Mark as notified only immediately before showing notification
            markAsNotified(applicationContext, requestId)
            NotificationManagerCompat.from(this).notify(requestId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Ignore permission security exceptions
        }
    }
}
