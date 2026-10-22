package com.bloodconnect.ai

/**
 * Represents a single conversational turn consisting of a user question and assistant response.
 */
data class ChatTurn(
    val user: String = "",
    val assistant: String = ""
) {
    val question: String get() = user
    val answer: String get() = assistant
}

/**
 * Pure helper function that trims chat history to the last [maxTurns] (default 10).
 */
fun trimHistory(history: List<ChatTurn>, maxTurns: Int = 10): List<ChatTurn> {
    if (history.isEmpty() || maxTurns <= 0) return emptyList()
    return history.takeLast(maxTurns)
}
