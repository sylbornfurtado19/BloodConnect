package com.bloodconnect.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.Badge
import com.bloodconnect.data.model.DonationStreak
import com.bloodconnect.data.model.DonorTier
import com.bloodconnect.data.model.User
import com.bloodconnect.data.repository.AuthRepository
import com.bloodconnect.data.repository.BloodRequestRepository
import com.bloodconnect.data.repository.UserRepository
import android.content.Context
import com.bloodconnect.notifications.DonationReminderScheduler
import com.bloodconnect.notifications.TopicManager
import com.bloodconnect.utils.DonationSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val requestRepository: BloodRequestRepository,
    private val topicManager: TopicManager,
    private val donationSyncManager: DonationSyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var user by mutableStateOf<User?>(null)
        private set

    var donationCount by mutableStateOf(0)
        private set

    var donationStreak by mutableStateOf(0)
        private set

    var badges by mutableStateOf<List<Badge>>(emptyList())
        private set

    var blockedUsers by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    val donorTier: DonorTier
        get() = DonorTier.fromCount(donationCount)

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadUserProfile()
        observeBlockedUsers()
    }

    private fun observeBlockedUsers() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.observeBlocked(uid).collect {
                blockedUsers = it
            }
        }
    }

    fun unblockUser(targetUid: String) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.unblockUser(uid, targetUid)
        }
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
                donationSyncManager.syncFromPledges(uid)

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

                val donatedPledgesResult = requestRepository.getMyDonatedPledges(uid)
                if (donatedPledgesResult.isSuccess) {
                    val pledges = donatedPledgesResult.getOrDefault(emptyList())
                    donationCount = pledges.size
                    val timestamps = pledges.map { it.donatedAt ?: it.createdAt }
                    val streak = DonationStreak.compute(timestamps)
                    donationStreak = streak
                    badges = Badge.earnedBadges(donationCount, streak)
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
            val now = System.currentTimeMillis()
            val result = userRepository.markDonatedToday(uid)
            if (result.isSuccess) {
                DonationReminderScheduler.schedule(context, now)
            }
            loadUserProfile()
        }
    }

    fun updateAvailability(isAvailable: Boolean) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.updateAvailability(uid, isAvailable)
            loadUserProfile()
        }
    }

    fun updateContactable(contactable: Boolean) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.updateContactable(uid, contactable)
            loadUserProfile()
        }
    }

    fun signOut(onSignOut: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DonationReminderScheduler.cancel(context)
            } catch (_: Exception) {}
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
