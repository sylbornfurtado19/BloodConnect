package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.Pledge
import com.bloodconnect.data.model.RequestStatus
import com.bloodconnect.data.model.Urgency
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
            val canonicalUrgency = Urgency.fromStringOrDefault(request.urgency).displayName
            val newRequest = request.copy(
                id = docRef.id,
                status = canonicalStatus,
                urgency = canonicalUrgency
            )
            docRef.set(newRequest).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveRequests(): Result<List<BloodRequest>> {
        return try {
            val canonicalSnapshot = requestsCollection
                .whereEqualTo("status", RequestStatus.PENDING.displayName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val canonicalList = canonicalSnapshot.toObjects(BloodRequest::class.java)

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
                .distinctBy { it.id.ifBlank { it.requestId } }
                .map { it.withCanonicalStatus().withCanonicalUrgency() }
                .sortedByDescending { it.createdAt }

            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes active blood requests using a server-side query with whereIn and orderBy limit(100).
     *
     * IMPORTANT: Firestore Composite Index Required in Firebase Console:
     * Collection: blood_requests
     * Fields: status (Ascending), timestamp (Descending)
     */
    fun observeActiveRequests(): Flow<List<BloodRequest>> = callbackFlow {
        val query = requestsCollection
            .whereIn("status", listOf("Pending", "Active", "PENDING"))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(BloodRequest::class.java)?.let { req ->
                    if (req.id.isBlank()) req.id = doc.id
                    req.withCanonicalStatus().withCanonicalUrgency()
                }
            } ?: emptyList()

            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Retrieves all requests submitted by a specific requester ID.
     */
    suspend fun getRequestsByRequester(uid: String): Result<List<BloodRequest>> {
        return try {
            val snapshot = requestsCollection
                .whereEqualTo("requesterId", uid)
                .get()
                .await()
            val list = snapshot.toObjects(BloodRequest::class.java)
                .map { req ->
                    if (req.id.isBlank()) req.id = req.requestId
                    req.withCanonicalStatus().withCanonicalUrgency()
                }
                .sortedByDescending { it.createdAt }
            Result.success(list)
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
            val list = snapshot.toObjects(BloodRequest::class.java)
                .map { it.withCanonicalStatus().withCanonicalUrgency() }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRequestsByUrgency(urgency: Urgency): Result<List<BloodRequest>> {
        return getRequestsByUrgency(urgency.displayName)
    }

    suspend fun getRequestsByUrgency(urgency: String): Result<List<BloodRequest>> {
        return try {
            val targetUrgency = Urgency.fromStringOrDefault(urgency)
            val snapshot = requestsCollection
                .whereEqualTo("urgency", targetUrgency.displayName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val canonicalList = snapshot.toObjects(BloodRequest::class.java)

            val legacyNormalList = if (targetUrgency == Urgency.ROUTINE) {
                try {
                    val legacySnapshot = requestsCollection
                        .whereEqualTo("urgency", "Normal")
                        .get()
                        .await()
                    legacySnapshot.toObjects(BloodRequest::class.java)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val combined = (canonicalList + legacyNormalList)
                .distinctBy { it.id.ifBlank { it.requestId } }
                .map { it.withCanonicalStatus().withCanonicalUrgency() }

            Result.success(combined)
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

    suspend fun updateRequestUrgency(requestId: String, urgency: String): Result<Unit> {
        return try {
            val canonicalUrgency = Urgency.fromStringOrDefault(urgency).displayName
            requestsCollection.document(requestId).update("urgency", canonicalUrgency).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRequestUrgency(requestId: String, urgency: Urgency): Result<Unit> {
        return updateRequestUrgency(requestId, urgency.displayName)
    }

    // ------------------------------------------------------------------------
    // Request Pledges Subcollection Methods
    // ------------------------------------------------------------------------

    suspend fun pledge(requestId: String, pledge: Pledge): Result<Unit> {
        return try {
            requestsCollection.document(requestId)
                .collection("pledges")
                .document(pledge.donorId)
                .set(pledge)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun withdrawPledge(requestId: String, donorUid: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId)
                .collection("pledges")
                .document(donorUid)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePledges(requestId: String): Flow<List<Pledge>> = callbackFlow {
        val listener = requestsCollection.document(requestId)
            .collection("pledges")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val pledges = snapshot?.toObjects(Pledge::class.java) ?: emptyList()
                trySend(pledges)
            }
        awaitClose { listener.remove() }
    }

    fun observeMyPledge(requestId: String, donorUid: String): Flow<Pledge?> = callbackFlow {
        val listener = requestsCollection.document(requestId)
            .collection("pledges")
            .document(donorUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val pledge = snapshot?.toObject(Pledge::class.java)
                trySend(pledge)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markPledgeDonated(requestId: String, donorUid: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId)
                .collection("pledges")
                .document(donorUid)
                .update("status", "Donated")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Counts confirmed donations for [uid] from collection group "pledges".
     *
     * IMPORTANT: Firestore Collection Group Index Required in Firebase Console:
     * Collection Group: pledges
     * Fields: donorId (Ascending), status (Ascending)
     */
    suspend fun countMyDonations(uid: String): Result<Int> {
        return try {
            val countQuery = firestore.collectionGroup("pledges")
                .whereEqualTo("donorId", uid)
                .whereEqualTo("status", "Donated")
                .count()
                .get(AggregateSource.SERVER)
                .await()
            Result.success(countQuery.count.toInt())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
