package com.bloodconnect.data.model

/**
 * Categorization of notification alerts across BloodConnect.
 */
enum class NotificationType {
    EMERGENCY_REQUEST,
    BLOOD_DRIVE,
    COOLDOWN_ELIGIBLE,
    GENERAL
}

/**
 * Domain model representing an in-app and push notification alert.
 */
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val timestamp: Long = System.currentTimeMillis(),
    val targetId: String = "",
    val isRead: Boolean = false,
    val urgency: String = "Routine",
    val bloodGroup: String = "",
    val location: String = ""
)
