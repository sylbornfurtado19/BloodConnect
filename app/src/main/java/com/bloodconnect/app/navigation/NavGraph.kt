package com.bloodconnect.app.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun BloodConnectNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Login.route) {
            Text("Login Screen Placeholder")
        }
        composable(route = Screen.Register.route) {
            Text("Register Screen Placeholder")
        }
        composable(route = Screen.Home.route) {
            Text("Home Screen Placeholder")
        }
        composable(route = Screen.Profile.route) {
            Text("Profile Screen Placeholder")
        }
        composable(route = Screen.DonorSearch.route) {
            Text("Donor Search Screen Placeholder")
        }
        composable(route = Screen.CreateRequest.route) {
            Text("Create Request Screen Placeholder")
        }
        composable(route = Screen.RequestList.route) {
            Text("Request List Screen Placeholder")
        }
        composable(route = Screen.RequestDetail.route) {
            Text("Request Detail Screen Placeholder")
        }
    }
}
