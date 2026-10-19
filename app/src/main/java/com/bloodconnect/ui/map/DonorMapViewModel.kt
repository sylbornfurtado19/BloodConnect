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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        observeMapData()
        refreshUserLocation()
    }

    private fun observeMapData() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = authRepository.currentUserId
            val donorsFlow = donorRepository.observeDonors(null)
                .catch { emit(emptyList()) }
            val requestsFlow = requestRepository.observeActiveRequests()
                .catch { emit(emptyList()) }

            combine(donorsFlow, requestsFlow) { allDonors, allRequests ->
                val validDonors = allDonors.filter { donor ->
                    donor.id != currentUserId &&
                    donor.latitude != null && donor.longitude != null &&
                    !(donor.latitude == 0.0 && donor.longitude == 0.0)
                }

                val validRequests = allRequests.filter { req ->
                    req.latitude != 0.0 || req.longitude != 0.0
                }

                val currentState = _uiState.value
                val bgFilter = currentState.bloodGroupFilter

                val filteredDonors = if (bgFilter != null) {
                    validDonors.filter { it.bloodGroup == bgFilter }
                } else {
                    validDonors
                }

                val filteredRequests = if (bgFilter != null) {
                    validRequests.filter { req ->
                        val reqBg = BloodGroup.fromString(req.bloodGroup)
                        reqBg == bgFilter
                    }
                } else {
                    validRequests
                }

                MapUiState(
                    donors = filteredDonors,
                    requests = filteredRequests,
                    userLocation = currentState.userLocation,
                    filter = currentState.filter,
                    bloodGroupFilter = currentState.bloodGroupFilter,
                    isLoading = false,
                    errorMessage = null
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setFilter(filter: MapFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setBloodGroupFilter(bloodGroup: BloodGroup?) {
        _uiState.update { it.copy(bloodGroupFilter = bloodGroup) }
    }

    fun refreshUserLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val locResult = withTimeoutOrNull(5000L) {
                    locationHelper.getCurrentLocation(highAccuracy = false)
                }
                val location = locResult?.getOrNull()
                if (location != null) {
                    _uiState.update { it.copy(userLocation = location) }
                }
            } catch (_: Exception) {
                // Ignore location fetch failures on map
            }
        }
    }
}
