package com.bloodconnect.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.Pledge
import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.BloodRequestRepository
import com.bloodconnect.data.repository.UserRepository
import com.bloodconnect.utils.generate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DonationHistoryUiState(
    val isLoading: Boolean = true,
    val donations: List<Pledge> = emptyList(),
    val donorName: String = "Volunteer Donor",
    val generatingCertificateId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DonationHistoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val requestRepository: BloodRequestRepository
) : ViewModel() {

    var uiState by mutableStateOf(DonationHistoryUiState())
        private set

    init {
        loadDonationHistory()
    }

    fun loadDonationHistory() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        val uid = authRepository.currentUserId
        if (uid == null) {
            uiState = uiState.copy(isLoading = false, errorMessage = "User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                // 2G: Donor name for the certificate comes from the loaded user profile (fullName, else displayName, else "Volunteer Donor")
                val userResult = userRepository.getUser(uid)
                val user = userResult.getOrNull()
                val resolvedName = resolveDonorName(user)

                // getMyDonatedPledges already returns newest first
                val pledgesResult = requestRepository.getMyDonatedPledges(uid)
                val donations = pledgesResult.getOrDefault(emptyList())

                uiState = uiState.copy(
                    isLoading = false,
                    donations = donations,
                    donorName = resolvedName,
                    errorMessage = null
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load donation history"
                )
            }
        }
    }

    /**
     * 2F: The button creates the PDF off the main thread, then starts an ACTION_SEND
     * (type application/pdf, EXTRA_STREAM, FLAG_GRANT_READ_URI_PERMISSION) chooser.
     */
    fun shareCertificate(context: Context, pledge: Pledge) {
        val pledgeId = pledge.requestId.ifBlank { pledge.createdAt.toString() }
        uiState = uiState.copy(generatingCertificateId = pledgeId)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val donorName = uiState.donorName
                val hospital = listOf(pledge.hospital.trim(), pledge.city.trim())
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .ifBlank { "Blood donation" }
                val dateMillis = pledge.donatedAt ?: pledge.createdAt
                val serial = pledge.requestId.take(8).uppercase().ifBlank { "DONATION" }

                val pdfFile = generate(
                    context = context,
                    donorName = donorName,
                    bloodGroup = pledge.bloodGroup.ifBlank { "Blood" },
                    hospital = hospital,
                    dateMillis = dateMillis,
                    serial = serial
                )

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(sendIntent, "Share Certificate").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }

                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(generatingCertificateId = null)
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(
                        generatingCertificateId = null,
                        errorMessage = "Failed to generate certificate: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }
}

/**
 * 2G: Donor name for the certificate comes from the loaded user profile
 * (fullName, else displayName, else "Volunteer Donor").
 */
fun resolveDonorName(user: User?): String {
    return user?.fullName?.trim()?.takeIf { it.isNotBlank() }
        ?: user?.displayName?.trim()?.takeIf { it.isNotBlank() }
        ?: "Volunteer Donor"
}
