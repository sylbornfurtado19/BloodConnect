package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodBank
import com.bloodconnect.data.model.BloodDrive
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for reading community blood drives (camps), managing attendee RSVPs,
 * and querying registered blood banks and their real-time inventory levels.
 */
@Singleton
class CommunityRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val campsCollection = firestore.collection("camps")
    private val bloodBanksCollection = firestore.collection("blood_banks")

    /**
     * Real-time stream of upcoming blood donation camps.
     * Queries camps where endAt >= current timestamp, limited to 100 entries,
     * and sorted by startAt in-memory to avoid requiring composite Firestore indexes.
     */
    fun observeUpcomingDrives(): Flow<List<BloodDrive>> = callbackFlow {
        val now = System.currentTimeMillis()
        val query = campsCollection
            .whereGreaterThanOrEqualTo("endAt", now)
            .limit(100)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val drives = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(BloodDrive::class.java)?.copy(id = doc.id)
            }?.sortedBy { it.startAt } ?: emptyList()

            trySend(drives)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Real-time stream of verified blood banks and regional blood storage facilities.
     */
    fun observeBloodBanks(): Flow<List<BloodBank>> = callbackFlow {
        val query = bloodBanksCollection.limit(200)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val banks = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(BloodBank::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            trySend(banks)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Adds or removes an RSVP record for the currently signed-in user on the specified blood camp.
     */
    suspend fun setRsvp(driveId: String, going: Boolean): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User must be signed in to RSVP"))

            val rsvpRef = campsCollection.document(driveId).collection("rsvps").document(uid)
            if (going) {
                rsvpRef.set(mapOf("createdAt" to System.currentTimeMillis())).await()
            } else {
                rsvpRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes whether the currently signed-in user has confirmed an RSVP for the given drive.
     */
    fun observeMyRsvp(driveId: String): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val rsvpRef = campsCollection.document(driveId).collection("rsvps").document(uid)
        val listener = rsvpRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(false)
                return@addSnapshotListener
            }
            trySend(snapshot?.exists() == true)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Retrieves the current server-side count of attendees RSVP'd for this drive.
     */
    suspend fun rsvpCount(driveId: String): Int {
        return try {
            val countSnapshot = campsCollection.document(driveId)
                .collection("rsvps")
                .count()
                .get(AggregateSource.SERVER)
                .await()
            countSnapshot.count.toInt()
        } catch (_: Exception) {
            0
        }
    }
}
