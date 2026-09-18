package com.bloodconnect

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Base Application class for BloodConnect.
 * Annotated with @HiltAndroidApp to trigger Hilt code generation
 * and initialize the application-level dependency container.
 */
@HiltAndroidApp
class BloodConnectApp : Application()
