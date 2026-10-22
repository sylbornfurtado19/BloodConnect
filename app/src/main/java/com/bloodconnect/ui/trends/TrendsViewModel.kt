package com.bloodconnect.ui.trends

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.data.repository.BloodRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendsUiState(
    val isLoading: Boolean = true,
    val trends: List<GroupTrend> = emptyList(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && trends.all { it.last7 == 0 && it.previous7 == 0 }
}

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val requestRepository: BloodRequestRepository
) : ViewModel() {

    var uiState by mutableStateOf(TrendsUiState())
        private set

    private var loadJob: Job? = null

    init {
        loadTrends()
    }

    fun loadTrends() {
        loadJob?.cancel()
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        loadJob = viewModelScope.launch {
            try {
                // Fetch recent requests from the last 14 days
                val result = requestRepository.getRecentRequests(days = 14)
                if (result.isSuccess) {
                    val requests = result.getOrDefault(emptyList())
                    val computed = computeTrends(requests)
                    uiState = uiState.copy(
                        isLoading = false,
                        trends = computed,
                        errorMessage = null
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                            ?: "Failed to load blood demand trends"
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load blood demand trends"
                )
            }
        }
    }
}
