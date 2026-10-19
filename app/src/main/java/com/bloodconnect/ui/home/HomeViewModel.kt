package com.bloodconnect.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.utils.LocationSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing Home screen dashboard synchronization and background location updates.
 * Tracks [permissionRequested] so location permissions are prompted only once per app session.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val locationSyncManager: LocationSyncManager
) : ViewModel() {

    var permissionRequested by mutableStateOf(false)
        private set

    fun markPermissionRequested() {
        permissionRequested = true
    }

    fun syncLocation() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            locationSyncManager.captureLocationIfMissing(uid)
        }
    }
}
