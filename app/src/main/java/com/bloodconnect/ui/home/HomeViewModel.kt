package com.bloodconnect.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.UserRepository
import com.bloodconnect.notifications.TopicManager
import com.bloodconnect.utils.DonationSyncManager
import com.bloodconnect.utils.LocationSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing Home screen dashboard synchronization and background location updates.
 * Tracks [permissionRequested] and [notificationPermissionRequested] so location and notification permissions
 * are prompted only once per app session.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val locationSyncManager: LocationSyncManager,
    private val topicManager: TopicManager,
    private val donationSyncManager: DonationSyncManager
) : ViewModel() {

    var permissionRequested by mutableStateOf(false)
        private set

    var notificationPermissionRequested by mutableStateOf(false)
        private set

    init {
        val uid = authRepository.currentUserId
        if (uid != null) {
            viewModelScope.launch(Dispatchers.IO) {
                donationSyncManager.syncFromPledges(uid)
                val user = userRepository.getUser(uid).getOrNull()
                if (user != null && user.bloodGroup.isNotBlank()) {
                    topicManager.subscribe(user.bloodGroup)
                }
            }
        }
    }

    fun markPermissionRequested() {
        permissionRequested = true
    }

    fun markNotificationPermissionRequested() {
        notificationPermissionRequested = true
    }

    fun syncLocation() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            locationSyncManager.captureLocationIfMissing(uid)
        }
    }
}
