package com.bloodconnect

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.bloodconnect.navigation.BloodConnectNavHost
import com.bloodconnect.navigation.Screen
import com.bloodconnect.ui.theme.BloodConnectTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingRequestId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        handleIntentExtra(intent)

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val alphaAnimator = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.ALPHA,
                1f,
                0f
            ).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 350L
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        splashScreenView.remove()
                    }
                })
            }
            alphaAnimator.start()
        }

        enableEdgeToEdge()
        setContent {
            BloodConnectTheme {
                val navController = rememberNavController()
                val requestId = pendingRequestId.value
                val startDest = if (FirebaseAuth.getInstance().currentUser != null) {
                    Screen.Home.route
                } else {
                    Screen.Login.route
                }

                BloodConnectNavHost(
                    navController = navController,
                    startDestination = startDest,
                    pendingRequestId = requestId,
                    onPendingRequestIdHandled = {
                        pendingRequestId.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtra(intent)
    }

    private fun handleIntentExtra(intent: Intent?) {
        val requestId = intent?.getStringExtra("requestId")
        if (!requestId.isNullOrBlank() && FirebaseAuth.getInstance().currentUser != null) {
            pendingRequestId.value = requestId
            intent.removeExtra("requestId")
        }
    }
}
