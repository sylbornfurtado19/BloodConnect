package com.bloodconnect.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.Donor
import com.bloodconnect.data.repository.DonorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DonorDetailUiState {
    object Loading : DonorDetailUiState()
    data class Success(val donor: Donor) : DonorDetailUiState()
    data class Error(val message: String) : DonorDetailUiState()
}

@HiltViewModel
class DonorDetailViewModel @Inject constructor(
    private val donorRepository: DonorRepository,
    savedStateHandle: SavedStateHandle
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
                    _uiState.value = DonorDetailUiState.Success(donor)
                } else {
                    _uiState.value = DonorDetailUiState.Error("Donor not found")
                }
            } else {
                _uiState.value = DonorDetailUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Failed to fetch donor")
            }
        }
    }
}
