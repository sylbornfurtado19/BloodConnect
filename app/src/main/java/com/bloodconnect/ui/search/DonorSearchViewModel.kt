package com.bloodconnect.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Donor
import com.bloodconnect.data.repository.DonorRepository
import com.bloodconnect.utils.UserLocation
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
 * @property donorDistances Map of donor ID to calculated proximity distance in kilometers.
 * @property userLocation Current geographic coordinates obtained from LocationHelper, or null if unavailable.
 * @property isLocationPermissionGranted True if location permission was granted by user.
 * @property isDistanceFilterAvailable True when device coordinates are available for distance filtering.
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
    val donorDistances: Map<String, Double> = emptyMap(),
    val userLocation: UserLocation? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isDistanceFilterAvailable: Boolean = false,
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
                    val (filtered, distances) = applyFiltersAndSort(
                        donors = fetchedDonors,
                        bloodGroup = currentState.selectedBloodGroup,
                        locationQuery = currentState.locationQuery,
                        maxDistanceKm = currentState.maxDistanceKm,
                        sortOrder = currentState.sortOrder,
                        userLocation = currentState.userLocation
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allDonors = fetchedDonors,
                            donors = filtered,
                            donorDistances = distances,
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
     * Updates device user location coordinates obtained through LocationHelper.
     * Recalculates donor distances and reapplies radius filtering and distance sorting.
     */
    fun setUserLocation(location: UserLocation?) {
        _uiState.update { state ->
            val (filtered, distances) = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = state.selectedBloodGroup,
                locationQuery = state.locationQuery,
                maxDistanceKm = state.maxDistanceKm,
                sortOrder = state.sortOrder,
                userLocation = location
            )
            state.copy(
                userLocation = location,
                isDistanceFilterAvailable = location != null,
                donorDistances = distances,
                donors = filtered,
                isEmpty = filtered.isEmpty()
            )
        }
    }

    /**
     * Updates permission status. If revoked, clears cached location.
     */
    fun setLocationPermissionGranted(isGranted: Boolean) {
        _uiState.update { state ->
            state.copy(isLocationPermissionGranted = isGranted)
        }
        if (!isGranted) {
            setUserLocation(null)
        }
    }

    /**
     * Updates the selected blood group filter and recalculates results.
     */
    fun setBloodGroup(bloodGroup: BloodGroup?) {
        _uiState.update { state ->
            val (filtered, distances) = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = bloodGroup,
                locationQuery = state.locationQuery,
                maxDistanceKm = state.maxDistanceKm,
                sortOrder = state.sortOrder,
                userLocation = state.userLocation
            )
            state.copy(
                selectedBloodGroup = bloodGroup,
                donorDistances = distances,
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
            val (filtered, distances) = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = state.selectedBloodGroup,
                locationQuery = query,
                maxDistanceKm = state.maxDistanceKm,
                sortOrder = state.sortOrder,
                userLocation = state.userLocation
            )
            state.copy(
                locationQuery = query,
                donorDistances = distances,
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
            val (filtered, distances) = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = state.selectedBloodGroup,
                locationQuery = state.locationQuery,
                maxDistanceKm = distanceKm,
                sortOrder = state.sortOrder,
                userLocation = state.userLocation
            )
            state.copy(
                maxDistanceKm = distanceKm,
                donorDistances = distances,
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
            val (filtered, distances) = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = state.selectedBloodGroup,
                locationQuery = state.locationQuery,
                maxDistanceKm = state.maxDistanceKm,
                sortOrder = sortOrder,
                userLocation = state.userLocation
            )
            state.copy(
                sortOrder = sortOrder,
                donorDistances = distances,
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
            val (filtered, distances) = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = null,
                locationQuery = "",
                maxDistanceKm = 50,
                sortOrder = DistanceSortOrder.NEAREST,
                userLocation = state.userLocation
            )
            state.copy(
                selectedBloodGroup = null,
                locationQuery = "",
                maxDistanceKm = 50,
                sortOrder = DistanceSortOrder.NEAREST,
                donorDistances = distances,
                donors = filtered,
                isEmpty = filtered.isEmpty(),
                errorMessage = null
            )
        }
    }

    fun applyFiltersAndSort(
        donors: List<Donor>,
        bloodGroup: BloodGroup?,
        locationQuery: String,
        maxDistanceKm: Int,
        sortOrder: DistanceSortOrder,
        userLocation: UserLocation?
    ): Pair<List<Donor>, Map<String, Double>> {
        return filterAndSortDonors(
            donors = donors,
            bloodGroup = bloodGroup,
            locationQuery = locationQuery,
            maxDistanceKm = maxDistanceKm,
            sortOrder = sortOrder,
            userLocation = userLocation
        )
    }
}

/**
 * Pure function executing donor filtering by blood group, locality query,
 * and distance radius, as well as distance-aware sorting.
 */
fun filterAndSortDonors(
    donors: List<Donor>,
    bloodGroup: BloodGroup? = null,
    locationQuery: String = "",
    maxDistanceKm: Int = 50,
    sortOrder: DistanceSortOrder = DistanceSortOrder.NEAREST,
    userLocation: UserLocation? = null
): Pair<List<Donor>, Map<String, Double>> {
    val distances = mutableMapOf<String, Double>()
    if (userLocation != null) {
        for (donor in donors) {
            if (donor.latitude != null && donor.longitude != null) {
                val dist = calculateDistanceKm(
                    userLat = userLocation.latitude,
                    userLon = userLocation.longitude,
                    donorLat = donor.latitude,
                    donorLon = donor.longitude
                )
                distances[donor.id] = dist
            }
        }
    }

    val filtered = donors.filter { donor ->
        val matchesBlood = bloodGroup == null || donor.bloodGroup == bloodGroup
        val matchesLocation = locationQuery.isBlank() || donor.locality.contains(locationQuery.trim(), ignoreCase = true)

        val matchesDistance = if (userLocation == null) {
            // If location is unavailable, distance filtering is disabled
            true
        } else {
            val dist = distances[donor.id]
            if (dist != null) {
                dist <= maxDistanceKm
            } else {
                // Gracefully retain donors with missing coordinates without claiming fake distances
                true
            }
        }
        matchesBlood && matchesLocation && matchesDistance
    }

    val sorted = when (sortOrder) {
        DistanceSortOrder.NEAREST -> {
            if (userLocation == null) {
                filtered
            } else {
                filtered.sortedWith(
                    compareBy<Donor> { distances[it.id] == null }
                        .thenBy { distances[it.id] ?: Double.MAX_VALUE }
                )
            }
        }
        DistanceSortOrder.FARTHEST -> {
            if (userLocation == null) {
                filtered
            } else {
                filtered.sortedWith(
                    compareBy<Donor> { distances[it.id] == null }
                        .thenByDescending { distances[it.id] ?: Double.MIN_VALUE }
                )
            }
        }
    }

    return Pair(sorted, distances)
}

/**
 * Calculates spherical distance in kilometers between two coordinate pairs using Haversine formula.
 */
fun calculateDistanceKm(
    userLat: Double,
    userLon: Double,
    donorLat: Double,
    donorLon: Double
): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(donorLat - userLat)
    val dLon = Math.toRadians(donorLon - userLon)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(userLat)) * cos(Math.toRadians(donorLat)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}
