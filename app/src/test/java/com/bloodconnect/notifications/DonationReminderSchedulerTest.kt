package com.bloodconnect.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class DonationReminderSchedulerTest {

    private val now = 1_700_000_000_000L // arbitrary fixed timestamp

    @Test
    fun reminderDelayMillis_nullLastDonation_returnsNull() {
        val delay = reminderDelayMillis(null, now)
        assertNull("Expected null delay when lastDonationAt is null", delay)

        val schedulerDelay = DonationReminderScheduler.reminderDelayMillis(null, now)
        assertNull("Expected null delay via DonationReminderScheduler", schedulerDelay)
    }

    @Test
    fun reminderDelayMillis_alreadyEligible_returnsNull() {
        // Exactly 90 days ago
        val exactlyNinetyDaysAgo = now - TimeUnit.DAYS.toMillis(90)
        assertNull(
            "Expected null delay when exactly 90 days have elapsed (eligible)",
            reminderDelayMillis(exactlyNinetyDaysAgo, now)
        )

        // 95 days ago (well past cooldown)
        val ninetyFiveDaysAgo = now - TimeUnit.DAYS.toMillis(95)
        assertNull(
            "Expected null delay when 95 days have elapsed (already eligible)",
            reminderDelayMillis(ninetyFiveDaysAgo, now)
        )

        // 120 days ago
        val hundredTwentyDaysAgo = now - TimeUnit.DAYS.toMillis(120)
        assertNull(
            "Expected null delay when 120 days have elapsed",
            DonationReminderScheduler.reminderDelayMillis(hundredTwentyDaysAgo, now)
        )
    }

    @Test
    fun reminderDelayMillis_tenDaysAfterDonation_returnsRemainingEightyDays() {
        val tenDaysAgo = now - TimeUnit.DAYS.toMillis(10)
        val expectedDelay = TimeUnit.DAYS.toMillis(80)

        val actualDelay = reminderDelayMillis(tenDaysAgo, now)
        assertEquals(
            "Expected exactly 80 days of delay remaining when donation was 10 days ago",
            expectedDelay,
            actualDelay
        )

        val schedulerActualDelay = DonationReminderScheduler.reminderDelayMillis(tenDaysAgo, now)
        assertEquals(
            "Expected exactly 80 days of delay via DonationReminderScheduler",
            expectedDelay,
            schedulerActualDelay
        )
    }
}
