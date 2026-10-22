package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.Pledge
import com.bloodconnect.data.model.RequestStatus
import com.bloodconnect.data.model.Urgency
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloodRequestRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val requestsCollection = firestore.collection("blood_requests")

    suspend fun createRequest(request: BloodRequest): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: request.requesterId
            val docRef = requestsCollection.document()
            val canonicalStatus = RequestStatus.fromString(request.status)?.displayName
                ?: RequestStatus.PENDING.displayName
            val canonicalUrgency = Urgency.fromStringOrDefault(request.urgency).displayName
            val newRequest = request.copy(
                id = docRef.id,
                status = canonicalStatus,
                urgency = canonicalUrgency
            )

            val batch = firestore.batch()
            batch.set(docRef, newRequest)

            if (uid.isNotBlank()) {
                val limitsRef = firestore.collection("users")
                    .document(uid)
                    .collection("private")
                    .document("limits")
                batch.set(
                    limitsRef,
                    mapOf("lastRequestAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
            }

            batch.commit().await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves active blood requests using a single-field status filter.
     * Filters for active requests (Pending or Partially Fulfilled and not expired).
     */
    suspend fun getActiveRequests(): Result<List<BloodRequest>> {
        return try {
            val snapshot = requestsCollection
                .whereIn("status", listOf("Pending", "Active", "PENDING", "Partially Fulfilled"))
                .limit(200)
                .get()
                .await()

            val activeList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(BloodRequest::class.java)?.let { req ->
                    if (req.id.isBlank()) req.id = doc.id
                    req.withCanonicalStatus().withCanonicalUrgency()
                }
            }.filter { it.isActive }
             .sortedByDescending { it.createdAt }

            Result.success(activeList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves recent blood requests across all statuses ordered by timestamp descending (limit 500),
     * filtered in memory to createdAt >= now - days to avoid composite index requirements.
     */
    suspend fun getRecentRequests(days: Int = 30): Result<List<BloodRequest>> {
        return try {
            val now = System.currentTimeMillis()
            val cutoff = now - (days.toLong() * 24L * 60L * 60L * 1000L)
            val snapshot = requestsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(500)
                .get()
                .await()

            val recentList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(BloodRequest::class.java)?.let { req ->
                    if (req.id.isBlank()) req.id = doc.id
                    req.withCanonicalStatus().withCanonicalUrgency()
                }
            }.filter { it.createdAt >= cutoff }

            Result.success(recentList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes active blood requests using a single-field status filter.
     * Filters for active requests (Pending or Partially Fulfilled and not expired).
     */
    fun observeActiveRequests(): Flow<List<BloodRequest>> = callbackFlow {
        val query = requestsCollection
            .whereIn("status", listOf("Pending", "Active", "PENDING", "Partially Fulfilled"))
            .limit(200)

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
            }?.filter { it.isActive }
             ?.sortedByDescending { it.createdAt } ?: emptyList()

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
                .limit(200)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(BloodRequest::class.java)?.let { req ->
                    if (req.id.isBlank()) req.id = doc.id
                    req.withCanonicalStatus().withCanonicalUrgency()
                }
            }.filter { it.effectiveStatus == status }
             .sortedByDescending { it.createdAt }

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
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(BloodRequest::class.java)?.let { req ->
                    if (req.id.isBlank()) req.id = doc.id
                    req.withCanonicalStatus().withCanonicalUrgency()
                }
            }.filter { it.urgencyLevel == targetUrgency }

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

    /**
     * Records a unit received for a request using a Firestore transaction.
     * Increments unitsReceived capped at unitsNeeded, and updates status to "Fulfilled" or "Partially Fulfilled".
     */
    suspend fun recordUnitReceived(requestId: String): Result<Unit> {
        return try {
            val docRef = requestsCollection.document(requestId)
            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (snap.exists()) {
                    val unitsNeeded = snap.getLong("unitsNeeded")?.toInt() ?: 1
                    val currentReceived = snap.getLong("unitsReceived")?.toInt() ?: 0
                    val newReceived = minOf(currentReceived + 1, unitsNeeded)
                    val newStatus = if (newReceived >= unitsNeeded) "Fulfilled" else "Partially Fulfilled"

                    tx.update(
                        docRef,
                        mapOf(
                            "unitsReceived" to newReceived,
                            "status" to newStatus
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            val updates = mapOf<String, Any>(
                "status" to "Donated",
                "donatedAt" to System.currentTimeMillis()
            )
            requestsCollection.document(requestId)
                .collection("pledges")
                .document(donorUid)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    suspend fun getMyDonatedPledges(uid: String): Result<List<Pledge>> {
        return try {
            val snapshot = firestore.collectionGroup("pledges")
                .whereEqualTo("donorId", uid)
                .whereEqualTo("status", "Donated")
                .get()
                .await()
            val pledges = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Pledge::class.java)?.let { pledge ->
                    val resolvedRequestId = if (pledge.requestId.isNotBlank()) {
                        pledge.requestId
                    } else {
                        doc.reference.parent.parent?.id ?: ""
                    }
                    pledge.copy(requestId = resolvedRequestId)
                }
            }.sortedByDescending { it.donatedAt ?: it.createdAt }
            Result.success(pledges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
