package com.bloodconnect.data.model

data class User(
    val uid: String = "",
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val displayName: String = "",
    val bloodGroup: String = "",
    val phone: String = "",
    val locality: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isAvailable: Boolean = true,
    val livesSaved: Int = 0,
    val donorTier: String = "Bronze",
    val fcmToken: String? = null
)
