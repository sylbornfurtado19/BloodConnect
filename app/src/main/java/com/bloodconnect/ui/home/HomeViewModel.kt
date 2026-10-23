package com.bloodconnect.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.bloodconnect.data.model.DonationEligibility
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.UserRepository
import com.bloodconnect.notifications.DonationReminderScheduler
import com.bloodconnect.notifications.TopicManager
import com.bloodconnect.utils.DonationSyncManager
import com.bloodconnect.utils.LocationSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.round

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
    private val donationSyncManager: DonationSyncManager,
    private val notificationRepository: com.bloodconnect.data.repository.NotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentUser = kotlinx.coroutines.flow.MutableStateFlow<com.bloodconnect.data.model.User?>(null)
    val currentUser: kotlinx.coroutines.flow.StateFlow<com.bloodconnect.data.model.User?> = _currentUser

    val unreadNotificationCount: kotlinx.coroutines.flow.StateFlow<Int> = notificationRepository.unreadCount

    var permissionRequested by mutableStateOf(false)
        private set

    var notificationPermissionRequested by mutableStateOf(false)
        private set

    init {
        val uid = authRepository.currentUserId
        if (uid != null) {
            viewModelScope.launch {
                userRepository.observeUser(uid).collect { user ->
                    _currentUser.value = user
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    donationSyncManager.syncFromPledges(uid)
                    val user = userRepository.getUser(uid).getOrNull()
                    if (user != null) {
                        val lastDonation = user.lastDonationAt
                        if (lastDonation != null && lastDonation > 0L) {
                            if (!DonationEligibility.isEligible(lastDonation)) {
                                DonationReminderScheduler.schedule(context, lastDonation)
                            }
                        }
                        if (user.bloodGroup.isNotBlank()) {
                            topicManager.subscribe(user.bloodGroup)
                        }
                        if (user.latitude != 0.0 || user.longitude != 0.0) {
                            val coarseLat = round(user.latitude * 100) / 100
                            val coarseLon = round(user.longitude * 100) / 100
                            if (user.latitude != coarseLat || user.longitude != coarseLon) {
                                userRepository.updateLocation(uid, user.latitude, user.longitude)
                            }
                        }
                    }
                } catch (_: Exception) {}
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
