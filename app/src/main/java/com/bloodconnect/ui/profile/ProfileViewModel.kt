package com.bloodconnect.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.DonorTier
import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.BloodRequestRepository
import com.bloodconnect.data.repository.UserRepository
import com.bloodconnect.notifications.TopicManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val requestRepository: BloodRequestRepository,
    private val topicManager: TopicManager
) : ViewModel() {

    var user by mutableStateOf<User?>(null)
        private set

    var donationCount by mutableStateOf(0)
        private set

    val donorTier: DonorTier
        get() = DonorTier.fromCount(donationCount)

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
                    val loadedUser = result.getOrNull()
                    user = loadedUser
                    if (loadedUser != null && loadedUser.bloodGroup.isNotBlank()) {
                        viewModelScope.launch(Dispatchers.IO) {
                            topicManager.subscribe(loadedUser.bloodGroup)
                        }
                    }
                } else {
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load profile"
                }

                val countResult = requestRepository.countMyDonations(uid)
                if (countResult.isSuccess) {
                    donationCount = countResult.getOrDefault(0)
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load profile"
            } finally {
                isLoading = false
            }
        }
    }

    fun markDonatedToday() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.markDonatedToday(uid)
            loadUserProfile()
        }
    }

    fun signOut(onSignOut: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                topicManager.unsubscribeAll()
            } catch (_: Exception) {}
            authRepository.signOut()
            viewModelScope.launch(Dispatchers.Main) {
                onSignOut()
            }
        }
    }
}
