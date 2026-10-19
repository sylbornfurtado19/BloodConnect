package com.bloodconnect.ui.home

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
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val locationSyncManager: LocationSyncManager
) : ViewModel() {

    fun syncLocation() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            locationSyncManager.captureLocationIfMissing(uid)
        }
    }
}
