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
     * When going is true and userProfile is provided, stores the user's name, blood group, and phone
     * so the hospital organizer can manage the attendee guest list.
     */
    suspend fun setRsvp(driveId: String, going: Boolean, userProfile: com.bloodconnect.data.model.User? = null): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User must be signed in to RSVP"))

            val rsvpRef = campsCollection.document(driveId).collection("rsvps").document(uid)
            val attendeeRef = campsCollection.document(driveId).collection("attendees").document(uid)
            
            firestore.runBatch { batch ->
                if (going) {
                    batch.set(rsvpRef, mapOf("createdAt" to System.currentTimeMillis()))
                    if (userProfile != null) {
                        val attendeeData = mapOf(
                            "uid" to uid,
                            "name" to userProfile.displayName.ifBlank { userProfile.fullName },
                            "bloodGroup" to userProfile.bloodGroup,
                            "phone" to userProfile.phone,
                            "createdAt" to System.currentTimeMillis()
                        )
                        batch.set(attendeeRef, attendeeData)
                    }
                } else {
                    batch.delete(rsvpRef)
                    batch.delete(attendeeRef)
                }
            }.await()
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
     * Retrieves the total count of attendees who RSVP'd for the specified drive.
     */
    suspend fun rsvpCount(driveId: String): Int {
        return try {
            val snapshot = campsCollection.document(driveId).collection("rsvps")
                .count()
                .get(AggregateSource.SERVER)
                .await()
            snapshot.count.toInt()
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Schedules and publishes a new blood donation drive into the camps collection.
     */
    suspend fun scheduleBloodDrive(drive: BloodDrive): Result<String> {
        return try {
            val docRef = if (drive.id.isNotBlank()) campsCollection.document(drive.id) else campsCollection.document()
            val newDrive = drive.copy(id = docRef.id)
            docRef.set(newDrive).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a blood drive by ID.
     */
    suspend fun deleteBloodDrive(driveId: String): Result<Unit> {
        return try {
            campsCollection.document(driveId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates real-time stock counts for a specific blood bank.
     */
    suspend fun updateBloodBankStock(bankId: String, stock: Map<String, Int>): Result<Unit> {
        return try {
            bloodBanksCollection.document(bankId).update(
                mapOf(
                    "stock" to stock,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates or updates a blood bank facility record in Firestore.
     */
    suspend fun upsertBloodBank(bloodBank: BloodBank): Result<String> {
        return try {
            val docRef = if (bloodBank.id.isNotBlank()) bloodBanksCollection.document(bloodBank.id) else bloodBanksCollection.document()
            val toSave = bloodBank.copy(id = docRef.id, updatedAt = System.currentTimeMillis())
            docRef.set(toSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
