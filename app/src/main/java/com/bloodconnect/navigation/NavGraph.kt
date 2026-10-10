package com.bloodconnect.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bloodconnect.ui.auth.LoginScreen
import com.bloodconnect.ui.auth.RegisterScreen
import com.bloodconnect.ui.home.HomeScreen
import com.bloodconnect.ui.search.DonorSearchScreen

/**
 * Top-level Navigation Graph for BloodConnect.
 * Orchestrates navigation transitions between app destinations.
 */
@Composable
fun BloodConnectNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToDonorSearch = {
                    navController.navigate(Screen.DonorSearch.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToBloodRequests = {
                    navController.navigate(Screen.BloodRequests.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.DonorSearch.route) {
            DonorSearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.BloodRequests.route) {
            BloodRequestsPlaceholderScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Profile.route) {
            ProfilePlaceholderScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

