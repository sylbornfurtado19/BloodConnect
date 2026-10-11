package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloodRequestRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val requestsCollection = firestore.collection("blood_requests")

    suspend fun createRequest(request: BloodRequest): Result<String> {
        return try {
            val docRef = requestsCollection.document()
            val newRequest = request.copy(requestId = docRef.id)
            docRef.set(newRequest).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveRequests(): Result<List<BloodRequest>> {
        return try {
            val snapshot = requestsCollection
                .whereEqualTo("status", "Active")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val list = snapshot.toObjects(BloodRequest::class.java)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatus(requestId: String, status: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
