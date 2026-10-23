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
import com.bloodconnect.data.repository.ReportRepository
import com.bloodconnect.data.repository.UserRepository
import com.bloodconnect.utils.AnalyticsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRequestsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val requestRepository: BloodRequestRepository,
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository,
    private val analyticsHelper: AnalyticsHelper = AnalyticsHelper()
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
                requestRepository.observeRequestsByRequester(uid).collect { list ->
                    requests = list
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load your requests"
                isLoading = false
            }
        }
    }

    fun observePledges(requestId: String): Flow<List<Pledge>> {
        val uid = authRepository.currentUserId
        val pledgesFlow = requestRepository.observePledges(requestId)
        if (uid == null) return pledgesFlow
        return combine(pledgesFlow, userRepository.observeBlocked(uid)) { pledges, blocked ->
            pledges.filter { it.donorId !in blocked.keys }
        }
    }

    fun blockDonor(targetUid: String, targetName: String, onSuccess: () -> Unit = {}) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val result = userRepository.blockUser(uid, targetUid, targetName)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        requestId: String?,
        reason: String,
        details: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = reportRepository.submitReport(
                targetType = targetType,
                targetId = targetId,
                requestId = requestId,
                reason = reason,
                details = details
            )
            if (result.isSuccess) {
                onSuccess()
            } else {
                onFailure(result.exceptionOrNull()?.localizedMessage ?: "Failed to submit report")
            }
        }
    }

    fun markPledgeDonated(requestId: String, donorUid: String) {
        viewModelScope.launch {
            try {
                val res = requestRepository.markPledgeDonated(requestId, donorUid)
                if (res.isSuccess) {
                    analyticsHelper.logDonationConfirmed()
                    requestRepository.recordUnitReceived(requestId)
                    loadMyRequests()
                } else {
                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to update pledge status"
                }
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
