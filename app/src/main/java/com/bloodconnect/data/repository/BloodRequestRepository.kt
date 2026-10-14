package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.RequestStatus
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
            val canonicalStatus = RequestStatus.fromString(request.status)?.displayName
                ?: RequestStatus.PENDING.displayName
            val newRequest = request.copy(
                requestId = docRef.id,
                status = canonicalStatus
            )
            docRef.set(newRequest).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveRequests(): Result<List<BloodRequest>> {
        return try {
            // Query for canonical pending requests ("Pending")
            val canonicalSnapshot = requestsCollection
                .whereEqualTo("status", RequestStatus.PENDING.displayName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val canonicalList = canonicalSnapshot.toObjects(BloodRequest::class.java)

            // Backward compatibility for legacy documents that have "Active" or "PENDING"
            val legacyActiveList = try {
                val legacySnapshot = requestsCollection
                    .whereEqualTo("status", "Active")
                    .get()
                    .await()
                legacySnapshot.toObjects(BloodRequest::class.java)
            } catch (_: Exception) {
                emptyList()
            }

            val legacyPendingList = try {
                val legacySnapshot = requestsCollection
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .await()
                legacySnapshot.toObjects(BloodRequest::class.java)
            } catch (_: Exception) {
                emptyList()
            }

            val combined = (canonicalList + legacyActiveList + legacyPendingList)
                .distinctBy { it.requestId.ifBlank { it.id } }
                .map { it.withCanonicalStatus() }

            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRequestsByStatus(status: RequestStatus): Result<List<BloodRequest>> {
        return try {
            val snapshot = requestsCollection
                .whereEqualTo("status", status.displayName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val list = snapshot.toObjects(BloodRequest::class.java).map { it.withCanonicalStatus() }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatus(requestId: String, status: String): Result<Unit> {
        return try {
            val canonicalStatus = RequestStatus.fromString(status)?.displayName ?: status
            requestsCollection.document(requestId).update("status", canonicalStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatus(requestId: String, status: RequestStatus): Result<Unit> {
        return updateRequestStatus(requestId, status.displayName)
    }
}
