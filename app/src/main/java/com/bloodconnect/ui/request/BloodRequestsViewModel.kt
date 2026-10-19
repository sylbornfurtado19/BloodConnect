package com.bloodconnect.ui.request

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.RequestStatus
import com.bloodconnect.data.model.Urgency
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.BloodRequestRepository
import com.bloodconnect.data.repository.UserRepository
import com.bloodconnect.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class BloodRequestsViewModel @Inject constructor(
    private val requestRepository: BloodRequestRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    var requests by mutableStateOf<List<BloodRequest>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var listJob: Job? = null

    init {
        loadRequests()
    }

    fun loadRequests() {
        listJob?.cancel()
        isLoading = true
        errorMessage = null
        listJob = viewModelScope.launch {
            try {
                requestRepository.observeActiveRequests()
                    .catch { e ->
                        errorMessage = e.localizedMessage ?: "Failed to load active requests"
                        isLoading = false
                    }
                    .collect { list ->
                        requests = list
                        isLoading = false
                        errorMessage = null
                    }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load requests"
                isLoading = false
            }
        }
    }

    override fun onCleared() {
        listJob?.cancel()
    }

    fun createRequestWithLocation(
        bloodGroup: String,
        hospital: String,
        unitsNeeded: Int,
        urgency: String,
        city: String,
        notes: String = "",
        onSuccess: () -> Unit
    ) {
        if (isSubmitting) return

        val currentUid = authRepository.currentUserId
        if (currentUid.isNullOrBlank()) {
            errorMessage = "Please sign in to request blood"
            return
        }

        isSubmitting = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val userResult = userRepository.getUser(currentUid)
                val userObj = userResult.getOrNull()
                val requesterName = userObj?.fullName?.ifBlank { null }
                    ?: userObj?.displayName?.ifBlank { null }
                    ?: authRepository.currentUser?.email
                    ?: "Anonymous Donor"

                val locResult = withTimeoutOrNull(5000L) {
                    locationHelper.getCurrentLocation(highAccuracy = false)
                }
                val lat = locResult?.getOrNull()?.latitude ?: 0.0
                val lon = locResult?.getOrNull()?.longitude ?: 0.0

                val canonicalUrgency = Urgency.fromStringOrDefault(urgency).displayName
                val request = BloodRequest(
                    requesterId = currentUid,
                    requesterName = requesterName,
                    bloodGroup = bloodGroup,
                    hospital = hospital,
                    unitsNeeded = unitsNeeded,
                    urgency = canonicalUrgency,
                    city = city,
                    latitude = lat,
                    longitude = lon,
                    status = RequestStatus.PENDING.displayName,
                    notes = notes
                )
                val result = requestRepository.createRequest(request)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create request"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to create request"
            } finally {
                isSubmitting = false
            }
        }
    }

    fun createRequest(
        requesterId: String,
        requesterName: String,
        bloodGroup: String,
        hospital: String,
        unitsNeeded: Int,
        urgency: String,
        onSuccess: () -> Unit
    ) {
        createRequestWithLocation(
            bloodGroup = bloodGroup,
            hospital = hospital,
            unitsNeeded = unitsNeeded,
            urgency = urgency,
            city = "",
            notes = "",
            onSuccess = onSuccess
        )
    }

    fun createRequest(
        requesterId: String,
        requesterName: String,
        bloodGroup: String,
        hospital: String,
        unitsNeeded: Int,
        urgency: Urgency,
        onSuccess: () -> Unit
    ) {
        createRequestWithLocation(
            bloodGroup = bloodGroup,
            hospital = hospital,
            unitsNeeded = unitsNeeded,
            urgency = urgency.displayName,
            city = "",
            notes = "",
            onSuccess = onSuccess
        )
    }

    /**
     * Backward-compatible overload for legacy parameter names (hospitalName, unitsRequired).
     */
    fun createRequest(
        requesterId: String,
        requesterName: String,
        bloodGroup: String,
        hospitalName: String,
        unitsRequired: Int,
        urgency: String,
        onSuccess: () -> Unit,
        @Suppress("UNUSED_PARAMETER") legacyFlag: Boolean = true
    ) {
        createRequestWithLocation(
            bloodGroup = bloodGroup,
            hospital = hospitalName,
            unitsNeeded = unitsRequired,
            urgency = urgency,
            city = "",
            notes = "",
            onSuccess = onSuccess
        )
    }
}
