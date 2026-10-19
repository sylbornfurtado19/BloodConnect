package com.bloodconnect.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.Donor
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.BloodRequestRepository
import com.bloodconnect.data.repository.DonorRepository
import com.bloodconnect.utils.LocationHelper
import com.bloodconnect.utils.UserLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

enum class MapFilter {
    ALL,
    DONORS,
    REQUESTS
}

data class MapUiState(
    val donors: List<Donor> = emptyList(),
    val requests: List<BloodRequest> = emptyList(),
    val userLocation: UserLocation? = null,
    val filter: MapFilter = MapFilter.ALL,
    val bloodGroupFilter: BloodGroup? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DonorMapViewModel @Inject constructor(
    private val donorRepository: DonorRepository,
    private val requestRepository: BloodRequestRepository,
    private val authRepository: AuthRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val rawDonors = MutableStateFlow<List<Donor>>(emptyList())
    private val rawRequests = MutableStateFlow<List<BloodRequest>>(emptyList())
    private val filter = MutableStateFlow(MapFilter.ALL)
    private val bloodGroupFilter = MutableStateFlow<BloodGroup?>(null)
    private val userLocation = MutableStateFlow<UserLocation?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val isLoading = MutableStateFlow(true)

    val uiState: StateFlow<MapUiState> = combine(
        rawDonors,
        rawRequests,
        filter,
        bloodGroupFilter,
        userLocation
    ) { donors, requests, currentFilter, currentBg, location ->
        val currentUserId = authRepository.currentUserId

        val validDonors = donors.filter { donor ->
            donor.id != currentUserId &&
            donor.latitude != null && donor.longitude != null &&
            !(donor.latitude == 0.0 && donor.longitude == 0.0)
        }

        val validRequests = requests.filter { req ->
            req.latitude != 0.0 || req.longitude != 0.0
        }

        val filteredDonors = if (currentBg != null) {
            validDonors.filter { it.bloodGroup == currentBg }
        } else {
            validDonors
        }

        val filteredRequests = if (currentBg != null) {
            validRequests.filter { req ->
                val reqBg = BloodGroup.fromString(req.bloodGroup)
                reqBg == currentBg
            }
        } else {
            validRequests
        }

        val finalDonors = if (currentFilter == MapFilter.ALL || currentFilter == MapFilter.DONORS) {
            filteredDonors
        } else {
            emptyList()
        }

        val finalRequests = if (currentFilter == MapFilter.ALL || currentFilter == MapFilter.REQUESTS) {
            filteredRequests
        } else {
            emptyList()
        }

        // Return a temporary holder object
        MapIntermediateState(finalDonors, finalRequests, location, currentFilter, currentBg)
    }.combine(
        combine(errorMessage, isLoading) { err, load -> Pair(err, load) }
    ) { intermediate, errorLoad ->
        MapUiState(
            donors = intermediate.donors,
            requests = intermediate.requests,
            userLocation = intermediate.userLocation,
            filter = intermediate.filter,
            bloodGroupFilter = intermediate.bloodGroupFilter,
            isLoading = errorLoad.second,
            errorMessage = errorLoad.first
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState(isLoading = true)
    )

    private data class MapIntermediateState(
        val donors: List<Donor>,
        val requests: List<BloodRequest>,
        val userLocation: UserLocation?,
        val filter: MapFilter,
        val bloodGroupFilter: BloodGroup?
    )

    init {
        observeMapData()
        refreshUserLocation()
    }

    private fun observeMapData() {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                donorRepository.observeDonors(null)
                    .catch { e ->
                        errorMessage.value = e.localizedMessage ?: "Failed to load map data"
                        emit(emptyList())
                    }
                    .collect { donors ->
                        rawDonors.value = donors
                        isLoading.value = false
                    }
            }

            launch {
                requestRepository.observeActiveRequests()
                    .catch { e ->
                        errorMessage.value = e.localizedMessage ?: "Failed to load map data"
                        emit(emptyList())
                    }
                    .collect { requests ->
                        rawRequests.value = requests
                        isLoading.value = false
                    }
            }
        }
    }

    fun setFilter(newFilter: MapFilter) {
        filter.value = newFilter
    }

    fun setBloodGroupFilter(bg: BloodGroup?) {
        bloodGroupFilter.value = bg
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun refreshUserLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val locResult = withTimeoutOrNull(5000L) {
                    locationHelper.getCurrentLocation(highAccuracy = false)
                }
                val location = locResult?.getOrNull()
                if (location != null) {
                    userLocation.value = location
                }
            } catch (_: Exception) {
                // Ignore location fetch failures on map
            }
        }
    }
}
