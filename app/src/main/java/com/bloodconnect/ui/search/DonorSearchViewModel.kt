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
 * Sorting modes available for donor search results.
 */
enum class SortMode {
    BEST_MATCH,
    NEAREST,
    FARTHEST
}

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
 * @property includeCompatible True if compatible donor blood groups should also be included in search results.
 * @property locationQuery Active locality text query filter.
 * @property maxDistanceKm Maximum proximity distance threshold in kilometers.
 * @property sortOrder Sorting order by distance ([DistanceSortOrder.NEAREST] or [DistanceSortOrder.FARTHEST]).
 * @property sortMode Active sorting mode ([SortMode.BEST_MATCH], [SortMode.NEAREST], or [SortMode.FARTHEST]).
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
    val includeCompatible: Boolean = true,
    val locationQuery: String = "",
    val maxDistanceKm: Int = 50,
    val sortOrder: DistanceSortOrder = DistanceSortOrder.NEAREST,
    val sortMode: SortMode = SortMode.BEST_MATCH,
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
            val result = donorRepository.getDonors(null)
            result.fold(
                onSuccess = { fetchedDonors ->
                    val currentState = _uiState.value
                    val (filtered, distances) = applyFiltersAndSort(
                        donors = fetchedDonors,
                        bloodGroup = currentState.selectedBloodGroup,
                        locationQuery = currentState.locationQuery,
                        maxDistanceKm = currentState.maxDistanceKm,
                        userLocation = currentState.userLocation,
                        includeCompatible = currentState.includeCompatible,
                        sortMode = currentState.sortMode
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
                userLocation = location,
                includeCompatible = state.includeCompatible,
                sortMode = state.sortMode
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
                userLocation = state.userLocation,
                includeCompatible = state.includeCompatible,
                sortMode = state.sortMode
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
     * Toggles whether compatible donor blood groups should be included in results.
     */
    fun setIncludeCompatible(include: Boolean) {
        _uiState.update { state ->
            val (filtered, distances) = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = state.selectedBloodGroup,
                locationQuery = state.locationQuery,
                maxDistanceKm = state.maxDistanceKm,
                userLocation = state.userLocation,
                includeCompatible = include,
                sortMode = state.sortMode
            )
            state.copy(
                includeCompatible = include,
                donorDistances = distances,
                donors = filtered,
                isEmpty = filtered.isEmpty()
            )
        }
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
                userLocation = state.userLocation,
                includeCompatible = state.includeCompatible,
                sortMode = state.sortMode
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
                userLocation = state.userLocation,
                includeCompatible = state.includeCompatible,
                sortMode = state.sortMode
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
        val mode = when (sortOrder) {
            DistanceSortOrder.NEAREST -> SortMode.NEAREST
            DistanceSortOrder.FARTHEST -> SortMode.FARTHEST
        }
        setSortMode(mode)
    }

    /**
     * Updates the active sorting mode (Best match, Nearest, or Farthest).
     */
    fun setSortMode(sortMode: SortMode) {
        val distanceOrder = when (sortMode) {
            SortMode.FARTHEST -> DistanceSortOrder.FARTHEST
            else -> DistanceSortOrder.NEAREST
        }
        _uiState.update { state ->
            val (filtered, distances) = applyFiltersAndSort(
                donors = state.allDonors,
                bloodGroup = state.selectedBloodGroup,
                locationQuery = state.locationQuery,
                maxDistanceKm = state.maxDistanceKm,
                sortOrder = null,
                userLocation = state.userLocation,
                includeCompatible = state.includeCompatible,
                sortMode = sortMode
            )
            state.copy(
                sortMode = sortMode,
                sortOrder = distanceOrder,
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
                sortOrder = null,
                userLocation = state.userLocation,
                includeCompatible = true,
                sortMode = SortMode.BEST_MATCH
            )
            state.copy(
                selectedBloodGroup = null,
                includeCompatible = true,
                locationQuery = "",
                maxDistanceKm = 50,
                sortOrder = DistanceSortOrder.NEAREST,
                sortMode = SortMode.BEST_MATCH,
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
        sortOrder: DistanceSortOrder? = null,
        userLocation: UserLocation?,
        includeCompatible: Boolean = true,
        sortMode: SortMode = SortMode.BEST_MATCH,
        now: Long = System.currentTimeMillis()
    ): Pair<List<Donor>, Map<String, Double>> {
        return filterAndSortDonors(
            donors = donors,
            bloodGroup = bloodGroup,
            locationQuery = locationQuery,
            maxDistanceKm = maxDistanceKm,
            sortOrder = sortOrder,
            userLocation = userLocation,
            includeCompatible = includeCompatible,
            sortMode = sortMode,
            now = now
        )
    }
}

/**
 * Pure function executing donor filtering by blood group, locality query,
 * and distance radius, as well as distance-aware and smart ranking sorting.
 * Excludes donors with latitude == 0.0 && longitude == 0.0 from distance calculation.
 */
fun filterAndSortDonors(
    donors: List<Donor>,
    bloodGroup: BloodGroup? = null,
    locationQuery: String = "",
    maxDistanceKm: Int = 50,
    sortOrder: DistanceSortOrder? = null,
    userLocation: UserLocation? = null,
    includeCompatible: Boolean = true,
    sortMode: SortMode = SortMode.BEST_MATCH,
    now: Long = System.currentTimeMillis()
): Pair<List<Donor>, Map<String, Double>> {
    val distances = mutableMapOf<String, Double>()
    if (userLocation != null) {
        for (donor in donors) {
            val lat = donor.latitude
            val lon = donor.longitude
            if (lat != null && lon != null && !(lat == 0.0 && lon == 0.0)) {
                val dist = calculateDistanceKm(
                    userLat = userLocation.latitude,
                    userLon = userLocation.longitude,
                    donorLat = lat,
                    donorLon = lon
                )
                distances[donor.id] = dist
            }
        }
    }

    val sanitizedQuery = locationQuery.trim()

    val filtered = donors.filter { donor ->
        val matchesBlood = if (bloodGroup == null) {
            true
        } else if (includeCompatible) {
            donor.bloodGroup.canDonateTo(bloodGroup)
        } else {
            donor.bloodGroup == bloodGroup
        }

        val matchesQuery = sanitizedQuery.isBlank() ||
            donor.locality.contains(sanitizedQuery, ignoreCase = true) ||
            donor.name.contains(sanitizedQuery, ignoreCase = true) ||
            donor.bloodGroup.displayName.equals(sanitizedQuery, ignoreCase = true) ||
            donor.bloodGroup.name.equals(sanitizedQuery, ignoreCase = true)

        val matchesDistance = if (userLocation == null) {
            true
        } else {
            val dist = distances[donor.id]
            if (dist != null) {
                dist <= maxDistanceKm
            } else {
                true
            }
        }
        matchesBlood && matchesQuery && matchesDistance
    }

    val effectiveSortMode = when {
        sortOrder == DistanceSortOrder.NEAREST -> SortMode.NEAREST
        sortOrder == DistanceSortOrder.FARTHEST -> SortMode.FARTHEST
        else -> sortMode
    }

    val sorted = when (effectiveSortMode) {
        SortMode.BEST_MATCH -> {
            filtered.sortedWith(
                compareByDescending<Donor> { donor ->
                    val dist = distances[donor.id]
                    DonorScorer.score(
                        donor = donor,
                        requested = bloodGroup,
                        distanceKm = dist,
                        maxDistanceKm = maxDistanceKm,
                        now = now
                    )
                }.thenBy { distances[it.id] ?: Double.MAX_VALUE }
            )
        }
        SortMode.NEAREST -> {
            if (userLocation == null) {
                filtered
            } else {
                filtered.sortedWith(
                    compareBy<Donor> { distances[it.id] == null }
                        .thenBy { distances[it.id] ?: Double.MAX_VALUE }
                )
            }
        }
        SortMode.FARTHEST -> {
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
