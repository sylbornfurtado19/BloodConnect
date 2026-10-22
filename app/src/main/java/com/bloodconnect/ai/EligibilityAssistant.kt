package com.bloodconnect.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton

const val ELIGIBILITY_SYSTEM_INSTRUCTION =
    "You are the BloodConnect assistant for blood donors in India. " +
    "Answer questions about blood donation eligibility, preparation and aftercare in short, plain language. " +
    "You are not a doctor. Never diagnose. If the question involves medication, pregnancy, recent surgery, " +
    "tattoos or piercings, infections, travel to malaria areas, chronic illness or anything uncertain, " +
    "say the rules differ and tell the person to confirm with the blood bank or a doctor before donating. " +
    "Do not answer questions unrelated to blood donation. Keep answers under 120 words."

@Singleton
class EligibilityAssistant @Inject constructor() {

    private val generativeModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.3f
            },
            systemInstruction = content {
                text(ELIGIBILITY_SYSTEM_INSTRUCTION)
            }
        )
    }

    suspend fun ask(history: List<ChatTurn>, question: String): Result<String> {
        val trimmed = question.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Question cannot be blank"))
        }

        return try {
            val recentHistory = trimHistory(history, maxTurns = 10)
            val chatHistory = mutableListOf<Content>()
            for (turn in recentHistory) {
                if (turn.user.isNotBlank()) {
                    chatHistory.add(content(role = "user") { text(turn.user) })
                }
                if (turn.assistant.isNotBlank()) {
                    chatHistory.add(content(role = "model") { text(turn.assistant) })
                }
            }

            val chat = generativeModel.startChat(history = chatHistory)
            val response = chat.sendMessage(trimmed)
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text.trim())
            } else {
                Result.failure(IllegalStateException("No response received from assistant"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
