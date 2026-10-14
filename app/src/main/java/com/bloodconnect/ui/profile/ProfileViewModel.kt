package com.bloodconnect.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var user by mutableStateOf<User?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            errorMessage = "No authenticated user"
            isLoading = false
            return
        }
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = userRepository.getUser(uid)
                if (result.isSuccess) {
                    user = result.getOrNull()
                } else {
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load profile"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load profile"
            } finally {
                isLoading = false
            }
        }
    }

    fun signOut(onSignOut: () -> Unit) {
        authRepository.signOut()
        onSignOut()
    }
}
