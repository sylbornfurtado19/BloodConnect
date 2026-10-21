package com.bloodconnect.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bloodconnect.data.model.DonationEligibility
import java.util.concurrent.TimeUnit

/**
 * Pure function that calculates the delay in milliseconds until a donor becomes eligible to donate again.
 *
 * @param lastDonationAt Epoch millisecond of the user's last recorded donation, or null.
 * @param now Current epoch millisecond timestamp.
 * @return Delay in milliseconds until eligible, or null if [lastDonationAt] is null or already eligible.
 */
fun reminderDelayMillis(lastDonationAt: Long?, now: Long = System.currentTimeMillis()): Long? {
    if (lastDonationAt == null) return null
    val eligibleTime = DonationEligibility.eligibleOn(lastDonationAt, now) ?: return null
    val delay = eligibleTime - now
    return if (delay > 0L) delay else null
}

/**
 * Singleton scheduler for managing the 90-day donation reminder background work.
 */
object DonationReminderScheduler {
    const val WORK_NAME = "donation_reminder"

    /**
     * Enqueues a unique OneTimeWorkRequest for [DonationReminderWorker] with [ExistingWorkPolicy.REPLACE].
     * If the user is already eligible or no delay is required, no reminder is scheduled.
     */
    fun schedule(context: Context, lastDonationAt: Long) {
        val delay = reminderDelayMillis(lastDonationAt) ?: return
        val workRequest = OneTimeWorkRequestBuilder<DonationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancels any scheduled donation reminder worker.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun reminderDelayMillis(lastDonationAt: Long?, now: Long): Long? =
        com.bloodconnect.notifications.reminderDelayMillis(lastDonationAt, now)
}
