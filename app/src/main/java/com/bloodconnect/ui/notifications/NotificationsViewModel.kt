package com.bloodconnect.ui.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.model.NotificationItem
import com.bloodconnect.data.model.NotificationType
import com.bloodconnect.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    var selectedFilter by mutableStateOf("All")
        private set

    val unreadCount: StateFlow<Int> = notificationRepository.unreadCount

    val filteredNotifications: StateFlow<List<NotificationItem>> = combine(
        notificationRepository.notifications,
        androidx.compose.runtime.snapshotFlow { selectedFilter }
    ) { items, filter ->
        when (filter) {
            "Emergency" -> items.filter { it.type == NotificationType.EMERGENCY_REQUEST }
            "Drives" -> items.filter { it.type == NotificationType.BLOOD_DRIVE }
            else -> items
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilter(filter: String) {
        selectedFilter = filter
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationRepository.clearAll()
        }
    }
}
