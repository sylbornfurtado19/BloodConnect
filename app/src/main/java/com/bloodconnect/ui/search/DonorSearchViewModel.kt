package com.bloodconnect.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DonorSearchViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var donors by mutableStateOf<List<User>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        searchDonors("")
    }

    fun searchDonors(bloodGroup: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = userRepository.searchDonors(bloodGroup)
            if (result.isSuccess) {
                donors = result.getOrDefault(emptyList())
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to fetch donors"
            }
            isLoading = false
        }
    }
}
