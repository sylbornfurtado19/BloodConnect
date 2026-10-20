package com.bloodconnect.ui.request

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.Pledge
import com.bloodconnect.data.model.RequestStatus
import com.bloodconnect.data.model.Urgency
import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.BloodRequestRepository
import com.bloodconnect.data.repository.ReportRepository
import com.bloodconnect.data.repository.UserRepository
import com.bloodconnect.utils.LocationHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class BloodRequestsViewModel @Inject constructor(
    private val requestRepository: BloodRequestRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    var requests by mutableStateOf<List<BloodRequest>>(emptyList())
        private set

    var currentUserProfile by mutableStateOf<User?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val currentUserId: String?
        get() = authRepository.currentUserId

    private var listJob: Job? = null

    init {
        loadRequests()
        loadCurrentUserProfile()
    }

    fun loadCurrentUserProfile() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            currentUserProfile = userRepository.getUser(uid).getOrNull()
        }
    }

    fun loadRequests() {
        listJob?.cancel()
        isLoading = true
        errorMessage = null
        val uid = currentUserId
        listJob = viewModelScope.launch {
            try {
                val requestsFlow = requestRepository.observeActiveRequests()
                val blockedFlow = if (uid != null) {
                    userRepository.observeBlocked(uid)
                } else {
                    flowOf(emptyMap())
                }

                combine(requestsFlow, blockedFlow) { reqList, blocked ->
                    reqList.filter { it.requesterId !in blocked.keys }
                }.catch { e ->
                    errorMessage = e.localizedMessage ?: "Failed to load active requests"
                    isLoading = false
                }.collect { list ->
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

    fun observeMyPledge(requestId: String): Flow<Pledge?>? {
        val uid = currentUserId ?: return null
        return requestRepository.observeMyPledge(requestId, uid)
    }

    fun pledgeToRequest(
        request: BloodRequest,
        etaMinutes: Int? = null,
        onSuccess: () -> Unit = {}
    ) {
        val reqId = request.id.ifBlank { request.requestId }
        pledgeToRequest(
            requestId = reqId,
            hospital = request.hospital,
            city = request.city,
            etaMinutes = etaMinutes,
            onSuccess = onSuccess
        )
    }

    fun pledgeToRequest(
        requestId: String,
        hospital: String,
        city: String,
        etaMinutes: Int? = null,
        onSuccess: () -> Unit = {}
    ) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val user = userRepository.getUser(uid).getOrNull()
            val donorName = user?.fullName?.ifBlank { null } ?: user?.displayName?.ifBlank { "Volunteer Donor" } ?: "Volunteer Donor"
            val bloodGroup = user?.bloodGroup ?: ""
            val phone = if (user?.contactable == true) user.phone else ""

            val pledge = Pledge(
                donorId = uid,
                donorName = donorName,
                bloodGroup = bloodGroup,
                phone = phone,
                createdAt = System.currentTimeMillis(),
                status = "Pledged",
                etaMinutes = etaMinutes,
                requestId = requestId,
                hospital = hospital,
                city = city
            )

            val result = requestRepository.pledge(requestId, pledge)
            if (result.isSuccess) {
                onSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit pledge"
            }
        }
    }

    fun pledgeToRequest(
        requestId: String,
        etaMinutes: Int? = null,
        onSuccess: () -> Unit = {}
    ) {
        pledgeToRequest(
            requestId = requestId,
            hospital = "",
            city = "",
            etaMinutes = etaMinutes,
            onSuccess = onSuccess
        )
    }

    fun withdrawPledge(requestId: String, onSuccess: () -> Unit = {}) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val result = requestRepository.withdrawPledge(requestId, uid)
            if (result.isSuccess) {
                onSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to withdraw pledge"
            }
        }
    }

    fun blockRequester(targetUid: String, targetName: String, onSuccess: () -> Unit = {}) {
        val uid = currentUserId ?: return
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

    fun createRequestWithLocation(
        bloodGroup: String,
        hospital: String,
        unitsNeeded: Int,
        urgency: String,
        city: String,
        notes: String = "",
        contactNumber: String = "",
        patientReference: String = "",
        onSuccess: () -> Unit
    ) {
        if (isSubmitting) return

        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            errorMessage = "Please sign in to request blood"
            return
        }

        isSubmitting = true
        errorMessage = null
        viewModelScope.launch {
            try {
                // Client check: user cannot have 3 or more active requests
                val existingRequests = requestRepository.getRequestsByRequester(uid).getOrDefault(emptyList())
                val activeCount = existingRequests.count { it.isActive }
                if (activeCount >= 3) {
                    errorMessage = "You already have 3 active requests. Fulfil or cancel one first."
                    isSubmitting = false
                    return@launch
                }

                val userResult = userRepository.getUser(uid)
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
                val urgencyEnum = Urgency.fromStringOrDefault(urgency)

                val now = System.currentTimeMillis()
                val durationMillis = when (urgencyEnum) {
                    Urgency.CRITICAL -> 24 * 60 * 60 * 1000L
                    Urgency.URGENT -> 48 * 60 * 60 * 1000L
                    Urgency.ROUTINE -> 7 * 24 * 60 * 60 * 1000L
                }
                val expiresAt = now + durationMillis

                val request = BloodRequest(
                    requesterId = uid,
                    requesterName = requesterName,
                    bloodGroup = bloodGroup,
                    hospital = hospital,
                    unitsNeeded = unitsNeeded,
                    unitsReceived = 0,
                    urgency = canonicalUrgency,
                    city = city,
                    latitude = lat,
                    longitude = lon,
                    status = RequestStatus.PENDING.displayName,
                    createdAt = now,
                    expiresAt = expiresAt,
                    notes = notes,
                    contactNumber = contactNumber,
                    patientReference = patientReference,
                    verified = false
                )
                val result = requestRepository.createRequest(request)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    val ex = result.exceptionOrNull()
                    val msg = ex?.message.orEmpty()
                    if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
                        (ex is FirebaseFirestoreException && ex.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)) {
                        errorMessage = "Please wait a couple of minutes before posting another request."
                    } else {
                        errorMessage = ex?.localizedMessage ?: "Failed to create request"
                    }
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
                    (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)) {
                    errorMessage = "Please wait a couple of minutes before posting another request."
                } else {
                    errorMessage = e.localizedMessage ?: "Failed to create request"
                }
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
