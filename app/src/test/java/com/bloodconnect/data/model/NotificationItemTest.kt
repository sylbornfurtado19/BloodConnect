package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationItemTest {

    @Test
    fun testNotificationItemDefaultValues() {
        val item = NotificationItem(
            id = "test_1",
            title = "Emergency Request",
            message = "O- needed at KEM Hospital",
            type = NotificationType.EMERGENCY_REQUEST,
            targetId = "req_123"
        )

        assertEquals("test_1", item.id)
        assertEquals("Emergency Request", item.title)
        assertEquals(NotificationType.EMERGENCY_REQUEST, item.type)
        assertFalse(item.isRead)
        assertEquals("Routine", item.urgency)
    }

    @Test
    fun testNotificationItemReadToggle() {
        val unreadItem = NotificationItem(id = "notif_1", isRead = false)
        assertFalse(unreadItem.isRead)

        val readItem = unreadItem.copy(isRead = true)
        assertTrue(readItem.isRead)
    }

    @Test
    fun testNotificationTypeCategorization() {
        val emergency = NotificationItem(type = NotificationType.EMERGENCY_REQUEST)
        val drive = NotificationItem(type = NotificationType.BLOOD_DRIVE)
        val eligible = NotificationItem(type = NotificationType.COOLDOWN_ELIGIBLE)
        val general = NotificationItem(type = NotificationType.GENERAL)

        assertEquals(NotificationType.EMERGENCY_REQUEST, emergency.type)
        assertEquals(NotificationType.BLOOD_DRIVE, drive.type)
        assertEquals(NotificationType.COOLDOWN_ELIGIBLE, eligible.type)
        assertEquals(NotificationType.GENERAL, general.type)
    }
}
