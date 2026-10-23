package com.bloodconnect.data.repository

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
import com.bloodconnect.data.model.BloodDrive
import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.NotificationItem
import com.bloodconnect.data.model.NotificationType
import com.bloodconnect.notifications.NotifiedRequests
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val readPrefs = context.getSharedPreferences("notification_read_prefs", Context.MODE_PRIVATE)
    private val notifiedPrefs = context.getSharedPreferences("notified_requests_prefs", Context.MODE_PRIVATE)

    private val _rawNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val _readIds = MutableStateFlow<Set<String>>(loadReadIds())

    val notifications: StateFlow<List<NotificationItem>> = combine(_rawNotifications, _readIds) { raw, readSet ->
        raw.map { item ->
            item.copy(isRead = readSet.contains(item.id))
        }.sortedByDescending { it.timestamp }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = combine(_rawNotifications, _readIds) { raw, readSet ->
        raw.count { !readSet.contains(it.id) }
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    init {
        startRealtimeSync()
    }

    private fun loadReadIds(): Set<String> {
        val stringSet = readPrefs.getStringSet("read_ids", emptySet()) ?: emptySet()
        return stringSet.toSet()
    }

    fun markAsRead(notificationId: String) {
        val current = _readIds.value.toMutableSet()
        current.add(notificationId)
        _readIds.value = current
        readPrefs.edit().putStringSet("read_ids", current).apply()
    }

    fun markAllAsRead() {
        val allIds = _rawNotifications.value.map { it.id }.toSet()
        _readIds.value = allIds
        readPrefs.edit().putStringSet("read_ids", allIds).apply()
    }

    fun clearAll() {
        markAllAsRead()
        _rawNotifications.value = emptyList()
    }

    private fun startRealtimeSync() {
        val currentUserId = auth.currentUser?.uid ?: ""

        // 1. Real-time Blood Requests Sync (Active emergency needs)
        firestore.collection("blood_requests")
            .whereIn("status", listOf("Pending", "Partially Fulfilled"))
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val requestItems = mutableListOf<NotificationItem>()
                val now = System.currentTimeMillis()

                for (doc in snapshot.documents) {
                    val req = doc.toObject(BloodRequest::class.java)?.copy(id = doc.id) ?: continue

                    // Build NotificationItem
                    val notifId = "req_${req.id}"
                    val title = if (req.urgency.equals("Critical", ignoreCase = true)) {
                        "🚨 CRITICAL: ${req.bloodGroup} Blood Needed!"
                    } else {
                        "Emergency: ${req.bloodGroup} Needed at ${req.hospital}"
                    }
                    val body = "${req.hospital}, ${req.city} requires ${req.unitsNeeded} unit(s). Urgency: ${req.urgency}."

                    requestItems.add(
                        NotificationItem(
                            id = notifId,
                            title = title,
                            message = body,
                            type = NotificationType.EMERGENCY_REQUEST,
                            timestamp = if (req.createdAt > 0) req.createdAt else now,
                            targetId = req.id,
                            urgency = req.urgency,
                            bloodGroup = req.bloodGroup,
                            location = "${req.hospital}, ${req.city}"
                        )
                    )

                    // Dispatch Real-time push notification to Android notification shade if eligible
                    if (req.requesterId != currentUserId && !isAlreadyNotified(req.id)) {
                        dispatchLocalPushNotification(
                            id = req.id,
                            title = title,
                            message = body,
                            isCritical = req.urgency.equals("Critical", ignoreCase = true)
                        )
                    }
                }

                mergeNotifications(requestItems, NotificationType.EMERGENCY_REQUEST)
            }

        // 2. Real-time Blood Drives Sync (Hospital scheduled camps)
        val now = System.currentTimeMillis()
        firestore.collection("camps")
            .whereGreaterThanOrEqualTo("endAt", now)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val driveItems = mutableListOf<NotificationItem>()
                for (doc in snapshot.documents) {
                    val drive = doc.toObject(BloodDrive::class.java)?.copy(id = doc.id) ?: continue

                    val notifId = "camp_${drive.id}"
                    val title = "🩸 Blood Donation Drive: ${drive.title}"
                    val body = "Organized by ${drive.organizer} at ${drive.address}, ${drive.city}."

                    driveItems.add(
                        NotificationItem(
                            id = notifId,
                            title = title,
                            message = body,
                            type = NotificationType.BLOOD_DRIVE,
                            timestamp = if (drive.startAt > 0) drive.startAt else now,
                            targetId = drive.id,
                            location = "${drive.address}, ${drive.city}"
                        )
                    )

                    if (!isAlreadyNotified("camp_${drive.id}")) {
                        dispatchLocalPushNotification(
                            id = "camp_${drive.id}",
                            title = title,
                            message = body,
                            isCritical = false
                        )
                    }
                }

                mergeNotifications(driveItems, NotificationType.BLOOD_DRIVE)
            }
    }

    private fun mergeNotifications(newItems: List<NotificationItem>, forType: NotificationType) {
        val currentList = _rawNotifications.value.toMutableList()
        currentList.removeAll { it.type == forType }
        currentList.addAll(newItems)
        _rawNotifications.value = currentList
    }

    private fun isAlreadyNotified(id: String): Boolean {
        val csv = notifiedPrefs.getString("notified_requests_csv", "") ?: ""
        return NotifiedRequests.contains(csv, id)
    }

    private fun markAsNotified(id: String) {
        val csv = notifiedPrefs.getString("notified_requests_csv", "") ?: ""
        val updated = NotifiedRequests.add(csv, id, 100)
        notifiedPrefs.edit().putString("notified_requests_csv", updated).apply()
    }

    private fun dispatchLocalPushNotification(
        id: String,
        title: String,
        message: String,
        isCritical: Boolean
    ) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("requestId", id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val vibrationPattern = if (isCritical) {
            longArrayOf(0, 500, 200, 500, 200, 800)
        } else {
            longArrayOf(0, 250, 250, 250)
        }

        val notification = NotificationCompat.Builder(context, "emergency_requests")
            .setSmallIcon(R.drawable.blood_drop_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setVibrate(vibrationPattern)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            markAsNotified(id)
            NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission security exception handled gracefully
        }
    }
}
