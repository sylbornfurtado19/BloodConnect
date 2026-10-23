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
import com.bloodconnect.ui.profile.DonationHistoryScreen
import com.bloodconnect.ui.community.BloodDrivesScreen
import com.bloodconnect.ui.community.BloodBanksScreen
import com.bloodconnect.ui.assistant.AssistantScreen
import com.bloodconnect.ui.notifications.NotificationsScreen
import com.bloodconnect.ui.hospital.HospitalDashboardScreen

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
            navController.navigate(Screen.BloodRequests.createRoute(pendingRequestId)) {
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
                    navController.navigate(Screen.BloodRequests.createRoute(null)) {
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
                },
                onNavigateToBloodDrives = {
                    navController.navigate(Screen.BloodDrives.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToBloodBanks = {
                    navController.navigate(Screen.BloodBanks.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAssistant = {
                    navController.navigate(Screen.Assistant.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHospitalDashboard = {
                    navController.navigate(Screen.HospitalDashboard.route) {
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

        composable(
            route = Screen.BloodRequests.route,
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val targetRequestId = backStackEntry.arguments?.getString("requestId")
            BloodRequestsScreen(
                targetRequestId = targetRequestId,
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
                onNavigateToDonationHistory = {
                    navController.navigate(Screen.DonationHistory.route) {
                        launchSingleTop = true
                    }
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.BloodDrives.route) {
            BloodDrivesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.BloodBanks.route) {
            BloodBanksScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.DonationHistory.route) {
            DonationHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Assistant.route) {
            AssistantScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToBloodRequests = { requestId ->
                    navController.navigate(Screen.BloodRequests.createRoute(requestId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToBloodDrives = {
                    navController.navigate(Screen.BloodDrives.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.HospitalDashboard.route) {
            HospitalDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
