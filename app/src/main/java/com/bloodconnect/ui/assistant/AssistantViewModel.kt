package com.bloodconnect.ui.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bloodconnect.ai.ChatTurn
import com.bloodconnect.ai.EligibilityAssistant
import com.bloodconnect.ai.trimHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

val SUGGESTED_QUESTIONS = listOf(
    "Can I donate after a tattoo?",
    "How long should I wait between donations?",
    "Can I donate if I have a cold?",
    "What should I eat before donating?",
    "Can I donate while taking antibiotics?",
    "How old do I need to be?"
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val history: List<ChatTurn> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastFailedQuestion: String? = null
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val eligibilityAssistant: EligibilityAssistant
) : ViewModel() {

    var uiState by mutableStateOf(AssistantUiState())
        private set

    fun updateInputText(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    /**
     * Sends a question to the assistant.
     * Enforces requirement 4F:
     * - Limits one request at a time (!isLoading)
     * - Ignores empty input (trimmed.isNotBlank())
     * - Never transmits user's name, phone, email, or location
     * - Keeps the last 10 turns as context (requirement 4D)
     */
    fun sendQuestion(questionToSend: String? = null) {
        val query = (questionToSend ?: uiState.inputText).trim()

        // 4F: Ignore empty input and limit one request at a time
        if (query.isBlank() || uiState.isLoading) {
            return
        }

        val userMessage = ChatMessage(text = query, isUser = true)
        val updatedMessages = uiState.messages + userMessage

        uiState = uiState.copy(
            messages = updatedMessages,
            inputText = "",
            isLoading = true,
            errorMessage = null,
            lastFailedQuestion = null
        )

        viewModelScope.launch {
            // Context strictly trimmed to last 10 turns per 4D
            val contextHistory = trimHistory(uiState.history, maxTurns = 10)
            val result = eligibilityAssistant.ask(history = contextHistory, question = query)

            if (result.isSuccess) {
                val answer = result.getOrNull().orEmpty()
                val assistantMessage = ChatMessage(text = answer, isUser = false)
                val newTurn = ChatTurn(user = query, assistant = answer)
                val newHistory = trimHistory(uiState.history + newTurn, maxTurns = 10)

                uiState = uiState.copy(
                    messages = uiState.messages + assistantMessage,
                    history = newHistory,
                    isLoading = false,
                    errorMessage = null
                )
            } else {
                val err = result.exceptionOrNull()?.localizedMessage
                    ?: "Could not reach the assistant. Please try again."
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = err,
                    lastFailedQuestion = query
                )
            }
        }
    }

    fun retryLast() {
        val failed = uiState.lastFailedQuestion ?: return
        sendQuestion(failed)
    }
}
