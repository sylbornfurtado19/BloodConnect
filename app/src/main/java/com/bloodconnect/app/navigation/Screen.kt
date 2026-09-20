package com.bloodconnect.app.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object DonorSearch : Screen("donor_search")
    object CreateRequest : Screen("create_request")
    object RequestList : Screen("request_list")
    object RequestDetail : Screen("request_detail/{requestId}") {
        fun createRoute(requestId: String) = "request_detail/$requestId"
    }
}
