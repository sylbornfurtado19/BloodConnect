package com.bloodconnect.ui.hospital

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.BloodBank
import com.bloodconnect.data.model.BloodDrive
import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.BloodRequestRepository
import com.bloodconnect.data.repository.CommunityRepository
import com.bloodconnect.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HospitalDashboardUiState(
    val isLoading: Boolean = true,
    val hospitalName: String = "Lilavati Hospital & Research Centre",
    val hospitalAddress: String = "Bandra Reclamation, Bandra West",
    val hospitalCity: String = "Mumbai",
    val hospitalPhone: String = "022-26751000",
    val bloodBank: BloodBank? = null,
    val stock: Map<String, Int> = mapOf(
        "A+" to 8, "A-" to 3,
        "B+" to 10, "B-" to 2,
        "O+" to 12, "O-" to 1,
        "AB+" to 5, "AB-" to 2
    ),
    val myDrives: List<BloodDrive> = emptyList(),
    val myRequests: List<BloodRequest> = emptyList(),
    val isUpdatingStock: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class HospitalDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val communityRepository: CommunityRepository,
    private val bloodRequestRepository: BloodRequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HospitalDashboardUiState())
    val uiState: StateFlow<HospitalDashboardUiState> = _uiState.asStateFlow()

    private var currentUserId: String = authRepository.currentUserId ?: ""

    init {
        loadHospitalData()
    }

    fun loadHospitalData() {
        val uid = authRepository.currentUserId ?: return
        currentUserId = uid

        viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                if (user != null) {
                    val resolvedName = user.organizationName.ifBlank {
                        user.fullName.ifBlank { "Lilavati Hospital & Research Centre" }
                    }
                    val resolvedCity = user.locality.ifBlank { "Mumbai" }

                    _uiState.value = _uiState.value.copy(
                        hospitalName = resolvedName,
                        hospitalCity = resolvedCity,
                        hospitalPhone = user.phone.ifBlank { "022-26751000" }
                    )

                    observeBloodBank(resolvedName, user)
                    observeHospitalDrives(resolvedName)
                    observeHospitalRequests(uid, resolvedName)
                }
            }
        }
    }

    private fun observeBloodBank(hospitalName: String, user: User) {
        viewModelScope.launch {
            communityRepository.observeBloodBanks().collect { banks ->
                val matched = banks.firstOrNull { bank ->
                    bank.name.equals(hospitalName, ignoreCase = true) || bank.id == user.uid
                }

                if (matched != null) {
                    val currentStock = if (matched.stock.isNotEmpty()) matched.stock else _uiState.value.stock
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        bloodBank = matched,
                        stock = currentStock,
                        hospitalName = matched.name.ifBlank { hospitalName },
                        hospitalAddress = matched.address.ifBlank { _uiState.value.hospitalAddress },
                        hospitalPhone = matched.phone.ifBlank { _uiState.value.hospitalPhone }
                    )
                } else {
                    // Seed initial blood bank for this hospital in Firestore
                    val initialBank = BloodBank(
                        id = user.uid,
                        name = hospitalName,
                        address = user.locality.ifBlank { "Bandra Reclamation, Bandra West" },
                        city = "Mumbai",
                        latitude = if (user.latitude != 0.0) user.latitude else 19.0522,
                        longitude = if (user.longitude != 0.0) user.longitude else 72.8315,
                        phone = user.phone.ifBlank { "022-26751000" },
                        stock = _uiState.value.stock,
                        updatedAt = System.currentTimeMillis()
                    )
                    viewModelScope.launch(Dispatchers.IO) {
                        communityRepository.upsertBloodBank(initialBank)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        bloodBank = initialBank
                    )
                }
            }
        }
    }

    private fun observeHospitalDrives(hospitalName: String) {
        viewModelScope.launch {
            communityRepository.observeUpcomingDrives().collect { drives ->
                val filtered = drives.filter { drive ->
                    drive.organizer.contains(hospitalName, ignoreCase = true) ||
                    drive.organizer.contains("Hospital", ignoreCase = true) ||
                    drive.organizer.contains("Lilavati", ignoreCase = true)
                }
                _uiState.value = _uiState.value.copy(myDrives = filtered)
            }
        }
    }

    private fun observeHospitalRequests(uid: String, hospitalName: String) {
        viewModelScope.launch {
            bloodRequestRepository.observeRequestsByRequester(uid).collect { requests ->
                _uiState.value = _uiState.value.copy(myRequests = requests)
            }
        }
    }

    fun updateStock(group: String, delta: Int) {
        val currentStock = _uiState.value.stock.toMutableMap()
        val currentCount = currentStock[group] ?: 0
        val newCount = maxOf(0, currentCount + delta)
        currentStock[group] = newCount

        _uiState.value = _uiState.value.copy(stock = currentStock)

        val bank = _uiState.value.bloodBank ?: return
        viewModelScope.launch(Dispatchers.IO) {
            communityRepository.updateBloodBankStock(bank.id, currentStock)
        }
    }

    fun setExactStock(group: String, exactCount: Int) {
        val currentStock = _uiState.value.stock.toMutableMap()
        val newCount = maxOf(0, exactCount)
        currentStock[group] = newCount

        _uiState.value = _uiState.value.copy(stock = currentStock)

        val bank = _uiState.value.bloodBank ?: return
        viewModelScope.launch(Dispatchers.IO) {
            communityRepository.updateBloodBankStock(bank.id, currentStock)
        }
    }

    fun scheduleDrive(
        title: String,
        address: String,
        city: String,
        startAt: Long,
        endAt: Long,
        contact: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val drive = BloodDrive(
                title = title,
                organizer = _uiState.value.hospitalName,
                address = address,
                city = city.ifBlank { "Mumbai" },
                startAt = startAt,
                endAt = endAt,
                contactNumber = contact.ifBlank { _uiState.value.hospitalPhone },
                description = description
            )

            val result = communityRepository.scheduleBloodDrive(drive)
            if (result.isSuccess) {
                launch(Dispatchers.Main) { onSuccess() }
            } else {
                launch(Dispatchers.Main) { onError(result.exceptionOrNull()?.message ?: "Failed to schedule drive") }
            }
        }
    }

    fun deleteDrive(driveId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = communityRepository.deleteBloodDrive(driveId)
            if (result.isSuccess) {
                launch(Dispatchers.Main) { onSuccess() }
            } else {
                launch(Dispatchers.Main) { onError(result.exceptionOrNull()?.message ?: "Failed to delete drive") }
            }
        }
    }

    fun broadcastUrgentNeed(
        bloodGroup: String,
        unitsNeeded: Int,
        urgency: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val request = BloodRequest(
                requesterId = currentUserId,
                requesterName = _uiState.value.hospitalName,
                bloodGroup = bloodGroup,
                unitsNeeded = unitsNeeded,
                hospital = _uiState.value.hospitalName,
                city = _uiState.value.hospitalCity,
                urgency = urgency,
                contactNumber = _uiState.value.hospitalPhone,
                patientReference = notes,
                notes = notes,
                status = "Pending",
                createdAt = System.currentTimeMillis()
            )

            val result = bloodRequestRepository.createRequest(request)
            if (result.isSuccess) {
                launch(Dispatchers.Main) { onSuccess() }
            } else {
                launch(Dispatchers.Main) { onError(result.exceptionOrNull()?.message ?: "Failed to broadcast request") }
            }
        }
    }
}
