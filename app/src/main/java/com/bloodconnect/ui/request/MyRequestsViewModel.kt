package com.bloodconnect.ui.request

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.Pledge
import com.bloodconnect.data.model.RequestStatus
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.BloodRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRequestsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val requestRepository: BloodRequestRepository
) : ViewModel() {

    var requests by mutableStateOf<List<BloodRequest>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadMyRequests()
    }

    fun loadMyRequests() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            errorMessage = "No authenticated user"
            return
        }
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val result = requestRepository.getRequestsByRequester(uid)
                if (result.isSuccess) {
                    requests = result.getOrDefault(emptyList())
                } else {
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load your requests"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load your requests"
            } finally {
                isLoading = false
            }
        }
    }

    fun observePledges(requestId: String): Flow<List<Pledge>> {
        return requestRepository.observePledges(requestId)
    }

    fun markPledgeDonated(requestId: String, donorUid: String) {
        viewModelScope.launch {
            try {
                requestRepository.markPledgeDonated(requestId, donorUid)
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to update pledge status"
            }
        }
    }

    fun updateStatus(requestId: String, status: RequestStatus) {
        viewModelScope.launch {
            try {
                val result = requestRepository.updateRequestStatus(requestId, status)
                if (result.isSuccess) {
                    loadMyRequests()
                } else {
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to update status"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to update status"
            }
        }
    }
}
