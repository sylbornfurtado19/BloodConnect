package com.bloodconnect.ui.auth

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

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var uiState: AuthUiState by mutableStateOf(AuthUiState.Idle)
        private set

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            uiState = AuthUiState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            val result = authRepository.signIn(email, pass)
            if (result.isSuccess) {
                uiState = AuthUiState.Success
                onSuccess()
            } else {
                uiState = AuthUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signUp(fullName: String, bloodGroup: String, email: String, pass: String, phone: String, onSuccess: () -> Unit) {
        if (fullName.isBlank() || bloodGroup.isBlank() || email.isBlank() || pass.isBlank()) {
            uiState = AuthUiState.Error("Please fill in all required fields")
            return
        }
        viewModelScope.launch {
            uiState = AuthUiState.Loading
            val authResult = authRepository.signUp(email, pass)
            if (authResult.isSuccess) {
                val firebaseUser = authResult.getOrNull()
                if (firebaseUser != null) {
                    val user = User(
                        uid = firebaseUser.uid,
                        email = email,
                        fullName = fullName,
                        bloodGroup = bloodGroup,
                        phone = phone
                    )
                    val saveResult = userRepository.saveUser(user)
                    if (saveResult.isSuccess) {
                        uiState = AuthUiState.Success
                        onSuccess()
                    } else {
                        uiState = AuthUiState.Error(saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save profile")
                    }
                } else {
                    uiState = AuthUiState.Error("User creation failed")
                }
            } else {
                uiState = AuthUiState.Error(authResult.exceptionOrNull()?.localizedMessage ?: "Sign up failed")
            }
        }
    }
}
