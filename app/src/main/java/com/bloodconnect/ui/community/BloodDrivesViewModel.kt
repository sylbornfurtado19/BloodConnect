package com.bloodconnect.ui.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodDrive
import com.bloodconnect.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.UserRepository

data class BloodDrivesUiState(
    val isLoading: Boolean = true,
    val drives: List<BloodDrive> = emptyList(),
    val rsvpStatus: Map<String, Boolean> = emptyMap(),
    val rsvpCounts: Map<String, Int> = emptyMap(),
    val isHospitalAdmin: Boolean = false,
    val currentHospitalName: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class BloodDrivesViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var uiState by mutableStateOf(BloodDrivesUiState())
        private set

    private var loadJob: Job? = null
    private val rsvpJobs = mutableMapOf<String, Job>()

    init {
        observeCurrentUser()
        loadDrives()
    }

    private fun observeCurrentUser() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                if (user != null) {
                    uiState = uiState.copy(
                        isHospitalAdmin = user.isHospitalAdmin,
                        currentHospitalName = user.organizationName.ifBlank { user.displayName }
                    )
                }
            }
        }
    }

    fun scheduleDrive(
        title: String,
        organizer: String,
        address: String,
        city: String,
        startAt: Long,
        endAt: Long,
        contactNumber: String,
        description: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedTitle = title.trim()
        val trimmedOrg = organizer.trim().ifBlank { uiState.currentHospitalName }
        val trimmedAddr = address.trim()
        val trimmedCity = city.trim().ifBlank { "Mumbai" }

        if (trimmedTitle.isBlank() || trimmedAddr.isBlank()) {
            onError("Please fill in drive title and address")
            return
        }

        viewModelScope.launch {
            val drive = BloodDrive(
                title = trimmedTitle,
                organizer = trimmedOrg.ifBlank { "Hospital Authority" },
                address = trimmedAddr,
                city = trimmedCity,
                startAt = if (startAt > 0) startAt else System.currentTimeMillis() + 86400000L,
                endAt = if (endAt > startAt) endAt else (if (startAt > 0) startAt else System.currentTimeMillis()) + 86400000L + 28800000L,
                contactNumber = contactNumber.trim(),
                description = description.trim()
            )
            val result = communityRepository.scheduleBloodDrive(drive)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Failed to schedule blood drive")
            }
        }
    }

    fun deleteDrive(driveId: String) {
        viewModelScope.launch {
            communityRepository.deleteBloodDrive(driveId)
        }
    }

    fun loadDrives() {
        loadJob?.cancel()
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        loadJob = viewModelScope.launch {
            communityRepository.observeUpcomingDrives()
                .catch { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load upcoming blood drives"
                    )
                }
                .collect { drivesList ->
                    uiState = uiState.copy(
                        isLoading = false,
                        drives = drivesList,
                        errorMessage = null
                    )
                    // Subscribe to RSVP state and count for each drive
                    drivesList.forEach { drive ->
                        observeDriveRsvp(drive.id)
                    }
                }
        }
    }

    private fun observeDriveRsvp(driveId: String) {
        if (rsvpJobs.containsKey(driveId)) return

        val job = viewModelScope.launch {
            // Fetch initial server count
            val count = communityRepository.rsvpCount(driveId)
            uiState = uiState.copy(
                rsvpCounts = uiState.rsvpCounts + (driveId to count)
            )

            // Real-time observation of user's personal RSVP
            communityRepository.observeMyRsvp(driveId).collect { going ->
                uiState = uiState.copy(
                    rsvpStatus = uiState.rsvpStatus + (driveId to going)
                )
            }
        }
        rsvpJobs[driveId] = job
    }

    fun toggleRsvp(driveId: String) {
        val currentGoing = uiState.rsvpStatus[driveId] ?: false
        val newGoing = !currentGoing
        val currentCount = uiState.rsvpCounts[driveId] ?: 0
        val optimisticCount = if (newGoing) currentCount + 1 else maxOf(0, currentCount - 1)

        // Optimistic UI update
        uiState = uiState.copy(
            rsvpStatus = uiState.rsvpStatus + (driveId to newGoing),
            rsvpCounts = uiState.rsvpCounts + (driveId to optimisticCount)
        )

        viewModelScope.launch {
            val result = communityRepository.setRsvp(driveId, newGoing)
            if (result.isFailure) {
                // Rollback optimistic update on failure
                uiState = uiState.copy(
                    rsvpStatus = uiState.rsvpStatus + (driveId to currentGoing),
                    rsvpCounts = uiState.rsvpCounts + (driveId to currentCount),
                    errorMessage = "Failed to update RSVP. Please try again."
                )
            } else {
                // Refresh authoritative server count
                val serverCount = communityRepository.rsvpCount(driveId)
                uiState = uiState.copy(
                    rsvpCounts = uiState.rsvpCounts + (driveId to serverCount)
                )
            }
        }
    }
}
