package com.bloodconnect.ui.request

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.RequestStatus
import com.bloodconnect.data.model.Urgency
import com.bloodconnect.data.repository.BloodRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BloodRequestsViewModel @Inject constructor(
    private val requestRepository: BloodRequestRepository
) : ViewModel() {

    var requests by mutableStateOf<List<BloodRequest>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadRequests()
    }

    fun loadRequests() {
        if (isLoading) return
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val result = requestRepository.getActiveRequests()
                if (result.isSuccess) {
                    requests = result.getOrDefault(emptyList())
                } else {
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load requests"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load requests"
            } finally {
                isLoading = false
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
        if (isLoading) return
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val canonicalUrgency = Urgency.fromStringOrDefault(urgency).displayName
                val request = BloodRequest(
                    requesterId = requesterId,
                    requesterName = requesterName,
                    bloodGroup = bloodGroup,
                    hospital = hospital,
                    unitsNeeded = unitsNeeded,
                    urgency = canonicalUrgency,
                    status = RequestStatus.PENDING.displayName
                )
                val result = requestRepository.createRequest(request)
                if (result.isSuccess) {
                    val reloadResult = requestRepository.getActiveRequests()
                    if (reloadResult.isSuccess) {
                        requests = reloadResult.getOrDefault(emptyList())
                    }
                    onSuccess()
                } else {
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create request"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to create request"
            } finally {
                isLoading = false
            }
        }
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
        createRequest(
            requesterId = requesterId,
            requesterName = requesterName,
            bloodGroup = bloodGroup,
            hospital = hospital,
            unitsNeeded = unitsNeeded,
            urgency = urgency.displayName,
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
        createRequest(
            requesterId = requesterId,
            requesterName = requesterName,
            bloodGroup = bloodGroup,
            hospital = hospitalName,
            unitsNeeded = unitsRequired,
            urgency = urgency,
            onSuccess = onSuccess
        )
    }
}
