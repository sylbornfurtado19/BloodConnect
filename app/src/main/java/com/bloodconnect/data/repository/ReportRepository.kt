package com.bloodconnect.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for submitting user and request moderation reports.
 */
@Singleton
class ReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /**
     * Submits a report to the "reports" Firestore collection.
     *
     * @param targetType Type of reported entity ("request" or "user").
     * @param targetId Identifier of the reported request or user.
     * @param requestId Associated request ID, if applicable.
     * @param reason Categorical reason for the report (max 40 chars).
     * @param details Additional user comments (trimmed to max 300 chars).
     */
    suspend fun submitReport(
        targetType: String,
        targetId: String,
        requestId: String?,
        reason: String,
        details: String
    ): Result<Unit> {
        return try {
            val reporterId = auth.currentUser?.uid ?: ""
            val trimmedDetails = if (details.length > 300) details.take(300) else details
            val reportData = hashMapOf<String, Any?>(
                "reporterId" to reporterId,
                "targetType" to targetType,
                "targetId" to targetId,
                "requestId" to requestId,
                "reason" to reason,
                "details" to trimmedDetails,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("reports").document().set(reportData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
