package com.bloodconnect.ui.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodBank
import com.bloodconnect.data.repository.CommunityRepository
import com.bloodconnect.ui.search.calculateDistanceKm
import com.bloodconnect.utils.LocationHelper
import com.bloodconnect.utils.UserLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BloodBanksUiState(
    val isLoading: Boolean = true,
    val allBanks: List<BloodBank> = emptyList(),
    val displayedBanks: List<BloodBank> = emptyList(),
    val selectedBloodGroup: String? = null,
    val userLocation: UserLocation? = null,
    val distances: Map<String, Double> = emptyMap(),
    val errorMessage: String? = null
)

@HiltViewModel
class BloodBanksViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    var uiState by mutableStateOf(BloodBanksUiState())
        private set

    private var loadJob: Job? = null

    init {
        loadBloodBanks()
    }

    fun loadBloodBanks() {
        loadJob?.cancel()
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        loadJob = viewModelScope.launch {
            // Resolve user location if available
            val location = locationHelper.getLastKnownLocation().getOrNull()
                ?: locationHelper.getCurrentLocation(highAccuracy = false).getOrNull()

            communityRepository.observeBloodBanks()
                .catch { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load blood banks"
                    )
                }
                .collect { banks ->
                    val distances = computeDistances(banks, location)
                    val displayed = filterAndSortBanks(
                        banks = banks,
                        selectedGroup = uiState.selectedBloodGroup,
                        userLocation = location,
                        distances = distances
                    )
                    uiState = uiState.copy(
                        isLoading = false,
                        allBanks = banks,
                        displayedBanks = displayed,
                        userLocation = location,
                        distances = distances,
                        errorMessage = null
                    )
                }
        }
    }

    fun selectBloodGroupFilter(group: String?) {
        val newSelection = if (uiState.selectedBloodGroup == group) null else group
        val displayed = filterAndSortBanks(
            banks = uiState.allBanks,
            selectedGroup = newSelection,
            userLocation = uiState.userLocation,
            distances = uiState.distances
        )
        uiState = uiState.copy(
            selectedBloodGroup = newSelection,
            displayedBanks = displayed
        )
    }

    private fun computeDistances(banks: List<BloodBank>, userLocation: UserLocation?): Map<String, Double> {
        if (userLocation == null) return emptyMap()
        val distances = mutableMapOf<String, Double>()
        for (bank in banks) {
            if (bank.latitude != 0.0 && bank.longitude != 0.0) {
                val dist = calculateDistanceKm(
                    userLat = userLocation.latitude,
                    userLon = userLocation.longitude,
                    donorLat = bank.latitude,
                    donorLon = bank.longitude
                )
                distances[bank.id] = dist
            }
        }
        return distances
    }

    private fun filterAndSortBanks(
        banks: List<BloodBank>,
        selectedGroup: String?,
        userLocation: UserLocation?,
        distances: Map<String, Double>
    ): List<BloodBank> {
        val filtered = if (selectedGroup.isNullOrBlank()) {
            banks
        } else {
            banks.filter { bank ->
                (bank.stock[selectedGroup] ?: 0) > 0
            }
        }

        return if (userLocation != null) {
            filtered.sortedWith(
                compareBy<BloodBank> { distances[it.id] == null }
                    .thenBy { distances[it.id] ?: Double.MAX_VALUE }
            )
        } else {
            filtered
        }
    }
}
