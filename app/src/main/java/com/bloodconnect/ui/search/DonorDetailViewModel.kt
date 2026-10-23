package com.bloodconnect.ui.search

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.Donor
import com.bloodconnect.data.repository.DonorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

sealed class DonorDetailUiState {
    object Loading : DonorDetailUiState()
    data class Success(val donor: Donor) : DonorDetailUiState()
    data class Error(val message: String) : DonorDetailUiState()
}

@HiltViewModel
class DonorDetailViewModel @Inject constructor(
    private val donorRepository: DonorRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val donorId: String? = savedStateHandle.get<String>("donorId")

    private val _uiState = MutableStateFlow<DonorDetailUiState>(DonorDetailUiState.Loading)
    val uiState: StateFlow<DonorDetailUiState> = _uiState.asStateFlow()

    init {
        loadDonor()
    }

    private fun loadDonor() {
        if (donorId.isNullOrBlank()) {
            _uiState.value = DonorDetailUiState.Error("Invalid donor ID")
            return
        }

        viewModelScope.launch {
            val result = donorRepository.getDonorById(donorId)
            if (result.isSuccess) {
                val donor = result.getOrNull()
                if (donor != null) {
                    var finalDonor = donor
                    val isUnknown = finalDonor.locality.isBlank() ||
                            finalDonor.locality.equals("City Center", ignoreCase = true) ||
                            finalDonor.locality.equals("Unknown Location", ignoreCase = true)

                    val lat = finalDonor.latitude
                    val lon = finalDonor.longitude

                    if (isUnknown && lat != null && lon != null && !(lat == 0.0 && lon == 0.0)) {
                        try {
                            val resolvedLocality = withContext(Dispatchers.IO) {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(lat, lon, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    address.subLocality ?: address.locality ?: address.adminArea
                                } else null
                            }
                            if (!resolvedLocality.isNullOrBlank()) {
                                finalDonor = finalDonor.copy(locality = resolvedLocality)
                            }
                        } catch (e: Exception) {
                            // Safely swallow Geocoder IOExceptions (e.g. no network)
                        }
                    }
                    _uiState.value = DonorDetailUiState.Success(finalDonor)
                } else {
                    _uiState.value = DonorDetailUiState.Error("Donor not found")
                }
            } else {
                _uiState.value = DonorDetailUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Failed to fetch donor")
            }
        }
    }
}
