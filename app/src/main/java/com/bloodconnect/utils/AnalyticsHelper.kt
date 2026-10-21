package com.bloodconnect.utils

import android.content.Context
import android.os.Bundle
import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Urgency
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton wrapper around [FirebaseAnalytics] for application telemetry.
 *
 * Privacy requirement:
 * NEVER log names, phone numbers, emails, hospital names, or coordinates.
 */
@Singleton
class AnalyticsHelper {

    private var context: Context? = null

    @Inject
    constructor(@ApplicationContext context: Context) {
        this.context = context
    }

    /**
     * Fallback no-argument constructor for un-injected instances and JVM unit tests.
     */
    constructor() {
        this.context = null
    }

    private val firebaseAnalytics: FirebaseAnalytics? by lazy {
        context?.let { ctx ->
            try {
                FirebaseAnalytics.getInstance(ctx)
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun logSignUp() {
        firebaseAnalytics?.let { analytics ->
            try {
                analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null)
            } catch (_: Throwable) {}
        }
        log("logSignUp")
    }

    fun logRequestCreated(bloodGroup: String, urgency: String) {
        firebaseAnalytics?.let { analytics ->
            try {
                val bundle = Bundle().apply {
                    putString("blood_group", bloodGroup)
                    putString("urgency", urgency)
                }
                analytics.logEvent("request_created", bundle)
            } catch (_: Throwable) {}
        }
        log("logRequestCreated: bloodGroup=$bloodGroup, urgency=$urgency")
    }

    fun logRequestCreated(bloodGroup: BloodGroup, urgency: Urgency) {
        logRequestCreated(bloodGroup.displayName, urgency.displayName)
    }

    fun logPledgeMade() {
        firebaseAnalytics?.let { analytics ->
            try {
                analytics.logEvent("pledge_made", null)
            } catch (_: Throwable) {}
        }
        log("logPledgeMade")
    }

    fun logPledgeWithdrawn() {
        firebaseAnalytics?.let { analytics ->
            try {
                analytics.logEvent("pledge_withdrawn", null)
            } catch (_: Throwable) {}
        }
        log("logPledgeWithdrawn")
    }

    fun logDonationConfirmed() {
        firebaseAnalytics?.let { analytics ->
            try {
                analytics.logEvent("donation_confirmed", null)
            } catch (_: Throwable) {}
        }
        log("logDonationConfirmed")
    }

    fun logShareImpact() {
        firebaseAnalytics?.let { analytics ->
            try {
                analytics.logEvent("share_impact", null)
            } catch (_: Throwable) {}
        }
        log("logShareImpact")
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
