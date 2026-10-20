package com.bloodconnect.utils

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Urgency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor() {

    fun logRequestCreated(bloodGroup: String, urgency: String) {
        log("logRequestCreated: bloodGroup=$bloodGroup, urgency=$urgency")
    }

    fun logRequestCreated(bloodGroup: BloodGroup, urgency: Urgency) {
        logRequestCreated(bloodGroup.displayName, urgency.displayName)
    }

    fun logPledgeMade() {
        log("logPledgeMade")
    }

    fun logPledgeWithdrawn() {
        log("logPledgeWithdrawn")
    }

    fun logDonationConfirmed() {
        log("logDonationConfirmed")
    }

    private fun log(message: String) {
        try {
            android.util.Log.d(TAG, message)
        } catch (_: Throwable) {
            println("[$TAG] $message")
        }
    }

    companion object {
        private const val TAG = "AnalyticsHelper"
    }
}
