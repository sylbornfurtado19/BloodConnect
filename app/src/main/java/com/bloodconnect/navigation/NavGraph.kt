package com.bloodconnect.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bloodconnect.ui.auth.LoginScreen
import com.bloodconnect.ui.auth.RegisterScreen
import com.bloodconnect.ui.home.HomeScreen
import com.bloodconnect.ui.map.DonorMapScreen
import com.bloodconnect.ui.search.DonorDetailScreen
import com.bloodconnect.ui.search.DonorSearchScreen
import com.bloodconnect.ui.request.BloodRequestsScreen
import com.bloodconnect.ui.request.MyRequestsScreen
import com.bloodconnect.ui.profile.ProfileScreen

/**
 * Top-level Navigation Graph for BloodConnect.
 * Orchestrates navigation transitions between app destinations and handles push notification navigation intents.
 */
@Composable
fun BloodConnectNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Login.route,
    pendingRequestId: String? = null,
    onPendingRequestIdHandled: () -> Unit = {}
) {
    LaunchedEffect(pendingRequestId) {
        if (!pendingRequestId.isNullOrBlank()) {
            navController.navigate(Screen.BloodRequests.route) {
                launchSingleTop = true
            }
            onPendingRequestIdHandled()
        }
    }

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
                onNavigateToMyRequests = {
                    navController.navigate(Screen.MyRequests.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToDonorMap = {
                    navController.navigate(Screen.DonorMap.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.DonorSearch.route) {
            DonorSearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDonorDetail = { donorId ->
                    navController.navigate(Screen.DonorDetail.createRoute(donorId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.DonorDetail.route,
            arguments = listOf(
                navArgument("donorId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val donorId = backStackEntry.arguments?.getString("donorId")
            DonorDetailScreen(
                donorId = donorId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.BloodRequests.route) {
            BloodRequestsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.MyRequests.route) {
            MyRequestsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.DonorMap.route) {
            DonorMapScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
