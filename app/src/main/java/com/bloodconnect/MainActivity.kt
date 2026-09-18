package com.bloodconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.bloodconnect.navigation.BloodConnectNavHost
import com.bloodconnect.ui.theme.BloodConnectTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main launcher Activity for BloodConnect.
 * Annotated with @AndroidEntryPoint to enable Hilt dependency injection in Compose screens.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloodConnectTheme {
                val navController = rememberNavController()
                BloodConnectNavHost(navController = navController)
            }
        }
    }
}
