package com.bloodconnect.navigation

/**
 * Defines navigation route destinations for BloodConnect.
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object DonorSearch : Screen("donor_search")
    data object DonorDetail : Screen("donor_detail/{donorId}") {
        fun createRoute(donorId: String): String = "donor_detail/$donorId"
    }
    data object BloodRequests : Screen("blood_requests")
    data object MyRequests : Screen("my_requests")
    data object Profile : Screen("profile")
    data object DonorMap : Screen("donor_map")
    data object BloodDrives : Screen("blood_drives")
    data object BloodBanks : Screen("blood_banks")
}
