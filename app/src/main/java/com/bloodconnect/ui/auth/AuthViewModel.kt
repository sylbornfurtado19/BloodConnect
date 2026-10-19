package com.bloodconnect.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.UserRepository
import com.bloodconnect.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Represents the distinct UI states for authentication flows.
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/**
 * ViewModel managing user authentication workflows (login, registration, sign-out).
 * Exposes observable StateFlow alongside Compose state and executes all auth operations
 * off the main thread. Never stores or logs passwords in UI state.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiStateFlow: StateFlow<AuthUiState> = _uiStateFlow.asStateFlow()

    var uiState: AuthUiState by mutableStateOf(AuthUiState.Idle)
        private set

    private fun updateState(newState: AuthUiState) {
        _uiStateFlow.value = newState
        uiState = newState
    }

    /**
     * Authenticates an existing user with email and password credentials.
     * Keeps execution on Dispatchers.IO and never stores passwords in state.
     */
    fun signIn(email: String, pass: String, onSuccess: () -> Unit = {}) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            updateState(AuthUiState.Error("Please fill in all fields"))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            updateState(AuthUiState.Loading)
            try {
                val result = authRepository.signIn(trimmedEmail, pass)
                if (result.isSuccess) {
                    updateState(AuthUiState.Success)
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Sign in failed"
                    updateState(AuthUiState.Error(errorMessage))
                }
            } catch (e: Exception) {
                updateState(AuthUiState.Error(e.localizedMessage ?: "Sign in failed"))
            }
        }
    }

    /**
     * Registers a new donor account with profile details and credentials.
     * Captures user location coordinates if permission is granted; falls back gracefully if denied.
     */
    fun signUp(
        fullName: String,
        bloodGroup: String,
        email: String,
        pass: String,
        phone: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val trimmedFullName = fullName.trim()
        val trimmedBloodGroup = bloodGroup.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        if (trimmedFullName.isBlank() || trimmedBloodGroup.isBlank() || trimmedEmail.isBlank() || pass.isBlank()) {
            updateState(AuthUiState.Error("Please fill in all required fields"))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            updateState(AuthUiState.Loading)
            try {
                val authResult = authRepository.signUp(trimmedEmail, pass)
                if (authResult.isSuccess) {
                    val firebaseUser = authResult.getOrNull()
                    if (firebaseUser != null) {
                        val user = User(
                            uid = firebaseUser.uid,
                            id = firebaseUser.uid,
                            email = trimmedEmail,
                            fullName = trimmedFullName,
                            displayName = trimmedFullName,
                            bloodGroup = trimmedBloodGroup,
                            phone = trimmedPhone
                        )
                        val saveResult = userRepository.saveUser(user)
                        if (saveResult.isSuccess) {
                            // Non-blocking location capture
                            try {
                                val locationResult = locationHelper.getCurrentLocation()
                                if (locationResult.isSuccess) {
                                    val userLocation = locationResult.getOrNull()
                                    if (userLocation != null) {
                                        userRepository.updateLocation(
                                            firebaseUser.uid,
                                            userLocation.latitude,
                                            userLocation.longitude
                                        )
                                    }
                                }
                            } catch (_: Exception) {
                                // Location capture failure must not block registration
                            }

                            updateState(AuthUiState.Success)
                            withContext(Dispatchers.Main) {
                                onSuccess()
                            }
                        } else {
                            val errorMessage = saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
                            updateState(AuthUiState.Error(errorMessage))
                        }
                    } else {
                        updateState(AuthUiState.Error("User creation failed"))
                    }
                } else {
                    val errorMessage = authResult.exceptionOrNull()?.localizedMessage ?: "Sign up failed"
                    updateState(AuthUiState.Error(errorMessage))
                }
            } catch (e: Exception) {
                updateState(AuthUiState.Error(e.localizedMessage ?: "Sign up failed"))
            }
        }
    }

    /**
     * Authenticates with Google and creates a Firestore profile if it's a new user.
     * Captures location coordinates if existing or new doc has default 0.0 coordinates.
     */
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(AuthUiState.Loading)
            try {
                val authResult = authRepository.signInWithGoogle(idToken)
                if (authResult.isSuccess) {
                    val firebaseUser = authResult.getOrNull()
                    if (firebaseUser != null) {
                        val existingUserResult = userRepository.getUser(firebaseUser.uid)
                        val existingUser = existingUserResult.getOrNull()
                        if (existingUser == null) {
                            val newUser = User(
                                uid = firebaseUser.uid,
                                id = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                fullName = firebaseUser.displayName ?: "Google User",
                                displayName = firebaseUser.displayName ?: "Google User"
                            )
                            userRepository.saveUser(newUser)
                        }

                        // Non-blocking location capture if coordinates are 0.0
                        val currentDoc = userRepository.getUser(firebaseUser.uid).getOrNull()
                        if (currentDoc != null && currentDoc.latitude == 0.0 && currentDoc.longitude == 0.0) {
                            try {
                                val locationResult = locationHelper.getCurrentLocation()
                                if (locationResult.isSuccess) {
                                    val userLocation = locationResult.getOrNull()
                                    if (userLocation != null) {
                                        userRepository.updateLocation(
                                            firebaseUser.uid,
                                            userLocation.latitude,
                                            userLocation.longitude
                                        )
                                    }
                                }
                            } catch (_: Exception) {
                                // Location capture failure must not block sign-in
                            }
                        }

                        updateState(AuthUiState.Success)
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        updateState(AuthUiState.Error("Google Sign-In failed: User object was null"))
                    }
                } else {
                    val errorMessage = authResult.exceptionOrNull()?.localizedMessage ?: "Google Sign-In failed"
                    updateState(AuthUiState.Error(errorMessage))
                }
            } catch (e: Exception) {
                updateState(AuthUiState.Error(e.localizedMessage ?: "Google Sign-In failed due to an exception"))
            }
        }
    }

    /**
     * Sets a custom error message on the UI state.
     */
    fun setError(message: String) {
        updateState(AuthUiState.Error(message))
    }

    /**
     * Alias for [signUp] for API consistency.
     */
    fun register(
        fullName: String,
        bloodGroup: String,
        email: String,
        pass: String,
        phone: String = "",
        onSuccess: () -> Unit = {}
    ) = signUp(fullName, bloodGroup, email, pass, phone, onSuccess)

    /**
     * Signs out the currently authenticated user.
     */
    fun signOut(onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.signOut()
            updateState(AuthUiState.Idle)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    /**
     * Resets the active error state back to Idle.
     */
    fun clearError() {
        if (uiState is AuthUiState.Error) {
            updateState(AuthUiState.Idle)
        }
    }

    /**
     * Resets state back to Idle.
     */
    fun resetState() {
        updateState(AuthUiState.Idle)
    }
}
