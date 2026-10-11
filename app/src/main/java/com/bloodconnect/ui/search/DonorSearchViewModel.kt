package com.bloodconnect.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Donor
import com.bloodconnect.data.repository.DonorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * UI state for the Donor Search destination.
 *
 * @property isLoading Indicates whether a donor retrieval request is in progress.
 * @property allDonors Complete unfiltered list of donors retrieved from [DonorRepository].
 * @property donors Filtered and sorted donor results ready for UI display.
 * @property selectedBloodGroup Active blood group filter, or null if all groups are included.
 * @property locationQuery Active locality text query filter.
 * @property maxDistanceKm Maximum proximity distance threshold in kilometers.
 * @property sortOrder Sorting order by distance ([DistanceSortOrder.NEAREST] or [DistanceSortOrder.FARTHEST]).
 * @property errorMessage User-readable error message explaining any load failure, or null if healthy.
 * @property isEmpty True when search yielded 0 matching donors and no load is active.
 */
data class DonorSearchUiState(
    val isLoading: Boolean = false,
    val allDonors: List<Donor> = emptyList(),
    val donors: List<Donor> = emptyList(),
    val selectedBloodGroup: BloodGroup? = null,
    val locationQuery: String = "",
    val maxDistanceKm: Int = 50,
    val sortOrder: DistanceSortOrder = DistanceSortOrder.NEAREST,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false
)

/**
 * ViewModel managing donor discovery, search queries, radius thresholds, and sorting logic.
 * Exposes observable [StateFlow] and keeps filtering calculations off the main thread.
 */
@HiltViewModel
class DonorSearchViewModel @Inject constructor(
    private val donorRepository: DonorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DonorSearchUiState(isLoading = true))
    val uiState: StateFlow<DonorSearchUiState> = _uiState.asStateFlow()

    init {
        loadDonors()
    }

    /**
     * Loads available donors from the repository and applies active filters.
     */
    fun loadDonors() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = donorRepository.getDonors(_uiState.value.selectedBloodGroup)
            result.fold(
                onSuccess = { fetchedDonors ->
                    val currentState = _uiState.value
                    val filtered = applyFiltersAndSort(
                        donors = fetchedDonors,
                        bloodGroup = currentState.selectedBloodGroup,
                        locationQuery = currentState.locationQuery,
                        maxDistanceKm = currentState.maxDistanceKm,
                        sortOrder = currentState.sortOrder
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allDonors = fetchedDonors,
                            donors = filtered,
                            isEmpty = filtered.isEmpty(),
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to retrieve donors",
                            isEmpty = true
                        )
                    }
                }
            )
        }
    }

    /**
     * Updates the selected blood group filter and recalculates results.
     */
    fun setBloodGroup(bloodGroup: BloodGroup?) {
        _uiState.update { state ->
            val filtered = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = bloodGroup,
                locationQuery = state.locationQuery,
                maxDistanceKm = state.maxDistanceKm,
                sortOrder = state.sortOrder
            )
            state.copy(
                selectedBloodGroup = bloodGroup,
                donors = filtered,
                isEmpty = filtered.isEmpty()
            )
        }
    }

    /**
     * Updates the selected blood group by string token (e.g., "O+", "A-", or null/empty for all).
     */
    fun setBloodGroup(bloodGroupString: String?) {
        val parsed = bloodGroupString?.let { BloodGroup.fromString(it) }
        setBloodGroup(parsed)
    }

    /**
     * Updates the locality search query text and recalculates results.
     */
    fun setLocationQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = state.selectedBloodGroup,
                locationQuery = query,
                maxDistanceKm = state.maxDistanceKm,
                sortOrder = state.sortOrder
            )
            state.copy(
                locationQuery = query,
                donors = filtered,
                isEmpty = filtered.isEmpty()
            )
        }
    }

    /**
     * Updates the maximum search distance threshold in kilometers.
     */
    fun setMaxDistanceKm(distanceKm: Int) {
        _uiState.update { state ->
            val filtered = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = state.selectedBloodGroup,
                locationQuery = state.locationQuery,
                maxDistanceKm = distanceKm,
                sortOrder = state.sortOrder
            )
            state.copy(
                maxDistanceKm = distanceKm,
                donors = filtered,
                isEmpty = filtered.isEmpty()
            )
        }
    }

    /**
     * Updates the distance sort order (nearest first or farthest first).
     */
    fun setSortOrder(sortOrder: DistanceSortOrder) {
        _uiState.update { state ->
            val filtered = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = state.selectedBloodGroup,
                locationQuery = state.locationQuery,
                maxDistanceKm = state.maxDistanceKm,
                sortOrder = sortOrder
            )
            state.copy(
                sortOrder = sortOrder,
                donors = filtered,
                isEmpty = filtered.isEmpty()
            )
        }
    }

    /**
     * Resets all search filters and distance thresholds back to default values.
     */
    fun clearFilters() {
        _uiState.update { state ->
            val filtered = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = null,
                locationQuery = "",
                maxDistanceKm = 50,
                sortOrder = DistanceSortOrder.NEAREST
            )
            state.copy(
                selectedBloodGroup = null,
                locationQuery = "",
                maxDistanceKm = 50,
                sortOrder = DistanceSortOrder.NEAREST,
                donors = filtered,
                isEmpty = filtered.isEmpty(),
                errorMessage = null
            )
        }
    }

    private fun applyFiltersAndSort(
        donors: List<Donor>,
        bloodGroup: BloodGroup?,
        locationQuery: String,
        maxDistanceKm: Int,
        sortOrder: DistanceSortOrder
    ): List<Donor> {
        val filtered = donors.filter { donor ->
            val matchesBlood = bloodGroup == null || donor.bloodGroup == bloodGroup
            val matchesLocation = locationQuery.isBlank() || donor.locality.contains(locationQuery.trim(), ignoreCase = true)
            val distance = estimateDistanceKm(donor)
            val matchesDistance = distance <= maxDistanceKm
            matchesBlood && matchesLocation && matchesDistance
        }

        return when (sortOrder) {
            DistanceSortOrder.NEAREST -> filtered.sortedBy { estimateDistanceKm(it) }
            DistanceSortOrder.FARTHEST -> filtered.sortedByDescending { estimateDistanceKm(it) }
        }
    }

    private fun estimateDistanceKm(donor: Donor, refLat: Double = 19.0760, refLon: Double = 72.8777): Double {
        val lat = donor.latitude ?: return 10.0
        val lon = donor.longitude ?: return 10.0
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat - refLat)
        val dLon = Math.toRadians(lon - refLon)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(refLat)) * cos(Math.toRadians(lat)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
