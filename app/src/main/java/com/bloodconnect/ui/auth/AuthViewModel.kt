package com.bloodconnect.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.UserRepository
import com.bloodconnect.notifications.TopicManager
import com.bloodconnect.utils.AnalyticsHelper
import com.bloodconnect.utils.LocationSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.annotation.StringRes
import com.bloodconnect.R
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Represents the distinct UI states for authentication flows.
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String = "", @StringRes val messageResId: Int? = null) : AuthUiState
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
    private val locationSyncManager: LocationSyncManager,
    private val topicManager: TopicManager,
    private val analyticsHelper: AnalyticsHelper = AnalyticsHelper()
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
            updateState(AuthUiState.Error("Please fill in all fields", R.string.auth_error_fill_all_fields))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            updateState(AuthUiState.Loading)
            try {
                val result = authRepository.signIn(trimmedEmail, pass)
                if (result.isSuccess) {
                    updateState(AuthUiState.Success)
                    val uid = authRepository.currentUserId
                    if (uid != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            locationSyncManager.captureLocationIfMissing(uid)
                            val user = userRepository.getUser(uid).getOrNull()
                            if (user != null && user.bloodGroup.isNotBlank()) {
                                topicManager.subscribe(user.bloodGroup)
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Sign in failed"
                    updateState(AuthUiState.Error(errorMessage, if (result.exceptionOrNull() == null) R.string.auth_error_sign_in_failed else null))
                }
            } catch (e: Exception) {
                updateState(AuthUiState.Error(e.localizedMessage ?: "Sign in failed", if (e.localizedMessage.isNullOrBlank()) R.string.auth_error_sign_in_failed else null))
            }
        }
    }

    /**
     * Registers a new donor account with profile details and credentials.
     * Location capture and topic subscription are non-blocking and execute after success/navigation.
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
            updateState(AuthUiState.Error("Please fill in all required fields", R.string.auth_error_fill_all_required_fields))
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
                            updateState(AuthUiState.Success)
                            analyticsHelper.logSignUp()
                            viewModelScope.launch(Dispatchers.IO) {
                                locationSyncManager.captureLocationIfMissing(firebaseUser.uid)
                                if (trimmedBloodGroup.isNotBlank()) {
                                    topicManager.subscribe(trimmedBloodGroup)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                onSuccess()
                            }
                        } else {
                            val errorMessage = saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
                            updateState(AuthUiState.Error(errorMessage, if (saveResult.exceptionOrNull() == null) R.string.auth_error_save_profile_failed else null))
                        }
                    } else {
                        updateState(AuthUiState.Error("User creation failed", R.string.auth_error_user_creation_failed))
                    }
                } else {
                    val errorMessage = authResult.exceptionOrNull()?.localizedMessage ?: "Sign up failed"
                    updateState(AuthUiState.Error(errorMessage, if (authResult.exceptionOrNull() == null) R.string.auth_error_sign_up_failed else null))
                }
            } catch (e: Exception) {
                updateState(AuthUiState.Error(e.localizedMessage ?: "Sign up failed", if (e.localizedMessage.isNullOrBlank()) R.string.auth_error_sign_up_failed else null))
            }
        }
    }

    /**
     * Authenticates with Google and creates a Firestore profile if it's a new user.
     * Location capture and topic subscription are non-blocking and run asynchronously after success.
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
                            analyticsHelper.logSignUp()
                        }

                        updateState(AuthUiState.Success)
                        viewModelScope.launch(Dispatchers.IO) {
                            locationSyncManager.captureLocationIfMissing(firebaseUser.uid)
                            val user = userRepository.getUser(firebaseUser.uid).getOrNull()
                            if (user != null && user.bloodGroup.isNotBlank()) {
                                topicManager.subscribe(user.bloodGroup)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        updateState(AuthUiState.Error("Google Sign-In failed: User object was null"))
                    }
                } else {
                    val errorMessage = authResult.exceptionOrNull()?.localizedMessage ?: "Google Sign-In failed"
                    updateState(AuthUiState.Error("Sign in error: $errorMessage"))
                }
            } catch (e: Exception) {
                updateState(AuthUiState.Error("Exception: ${e.localizedMessage}"))
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
     * Signs out the currently authenticated user and unsubscribes from topics.
     */
    fun signOut(onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                topicManager.unsubscribeAll()
            } catch (_: Exception) {}
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
