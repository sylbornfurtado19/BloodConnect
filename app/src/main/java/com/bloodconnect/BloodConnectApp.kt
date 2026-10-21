package com.bloodconnect

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

/*
 * MANUAL FIREBASE CONSOLE STEPS FOR APP CHECK:
 * 1. Go to Firebase Console -> Build -> App Check -> Get Started.
 * 2. Under 'Apps', select your Android app and click 'Register' for Play Integrity.
 * 3. For local debug testing, look in Logcat for 'DebugAppCheckProvider' on app startup,
 *    copy the secret debug token, and add it to Firebase Console -> App Check -> Apps -> Manage debug tokens.
 */
@HiltAndroidApp
class BloodConnectApp : Application() {

    override fun onCreate() {
        try {
            FirebaseApp.initializeApp(this)
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                if (BuildConfig.DEBUG) {
                    DebugAppCheckProviderFactory.getInstance()
                } else {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }
            )
        } catch (_: Exception) {}

        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        } catch (_: Exception) {}

        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val emergencyName = "Emergency blood requests"
            val emergencyDesc = "High priority emergency blood donation alerts"
            val emergencyImportance = NotificationManager.IMPORTANCE_HIGH
            val emergencyChannel = NotificationChannel("emergency_requests", emergencyName, emergencyImportance).apply {
                description = emergencyDesc
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(emergencyChannel)

            val reminderName = "Donation reminders"
            val reminderDesc = "Reminders when your 90-day donation cooldown has ended"
            val reminderImportance = NotificationManager.IMPORTANCE_DEFAULT
            val reminderChannel = NotificationChannel("donation_reminders", reminderName, reminderImportance).apply {
                description = reminderDesc
            }
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }
}
