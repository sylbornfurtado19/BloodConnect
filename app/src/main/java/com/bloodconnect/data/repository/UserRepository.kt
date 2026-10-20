package com.bloodconnect.data.repository

import com.bloodconnect.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersCollection = firestore.collection("users")

    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            val sanitizedUser = user.copy(phone = "")
            usersCollection.document(user.uid).set(sanitizedUser).await()

            if (user.phone.isNotBlank()) {
                try {
                    usersCollection.document(user.uid)
                        .collection("private")
                        .document("contact")
                        .set(mapOf("phone" to user.phone))
                        .await()
                } catch (_: Exception) {
                    // Swallow private contact save errors
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(uid: String): Result<User?> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            var user = snapshot.toObject(User::class.java)?.let { parsed ->
                if (snapshot.contains("isAvailable")) {
                    val modernVal = snapshot.getBoolean("isAvailable")
                    if (modernVal != null) {
                        parsed.applyModernAvailability(modernVal)
                        parsed.copy(isAvailable = modernVal)
                    } else parsed
                } else if (snapshot.contains("available")) {
                    val legacyVal = snapshot.getBoolean("available")
                    if (legacyVal != null) {
                        parsed.copy(isAvailable = legacyVal)
                    } else parsed
                } else {
                    parsed
                }
            }

            // Fill in phone for current user from private subcollection or migrate from root doc
            if (user != null && uid == auth.currentUser?.uid) {
                val rawPhone = snapshot.getString("phone") ?: ""
                if (rawPhone.isNotBlank()) {
                    try {
                        usersCollection.document(uid)
                            .collection("private")
                            .document("contact")
                            .set(mapOf("phone" to rawPhone))
                            .await()
                        usersCollection.document(uid)
                            .update("phone", FieldValue.delete())
                            .await()
                    } catch (_: Exception) {}
                    user = user.copy(phone = rawPhone)
                } else {
                    try {
                        val contactSnap = usersCollection.document(uid)
                            .collection("private")
                            .document("contact")
                            .get()
                            .await()
                        val privatePhone = contactSnap.getString("phone") ?: ""
                        user = user.copy(phone = privatePhone)
                    } catch (_: Exception) {}
                }
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAvailability(uid: String, isAvailable: Boolean): Result<Unit> {
        return try {
            usersCollection.document(uid).update("isAvailable", isAvailable).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateContactable(uid: String, contactable: Boolean): Result<Unit> {
        return try {
            usersCollection.document(uid).update("contactable", contactable).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLocation(uid: String, latitude: Double, longitude: Double): Result<Unit> {
        return try {
            val coarseLat = round(latitude * 100) / 100
            val coarseLon = round(longitude * 100) / 100
            val updates = mapOf<String, Any>(
                "latitude" to coarseLat,
                "longitude" to coarseLon
            )
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markDonatedToday(uid: String): Result<Unit> {
        return try {
            val updates = mapOf<String, Any>(
                "lastDonationAt" to System.currentTimeMillis(),
                "isAvailable" to false
            )
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordDonation(uid: String, donationTimestamp: Long): Result<Unit> {
        return try {
            val updates = mapOf<String, Any>(
                "lastDonationAt" to donationTimestamp,
                "isAvailable" to false
            )
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchDonors(bloodGroup: String): Result<List<User>> {
        return try {
            val query = if (bloodGroup.isBlank()) {
                usersCollection.whereEqualTo("isAvailable", true).get().await()
            } else {
                usersCollection
                    .whereEqualTo("bloodGroup", bloodGroup)
                    .whereEqualTo("isAvailable", true)
                    .get().await()
            }
            val donors = query.toObjects(User::class.java)
            Result.success(donors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Blocks a target user by writing their display name to users/{uid}/private/blocks.
     */
    suspend fun blockUser(uid: String, targetUid: String, targetName: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .collection("private")
                .document("blocks")
                .set(mapOf("blocked" to mapOf(targetUid to targetName)), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unblocks a target user by deleting their entry from the blocked map in users/{uid}/private/blocks.
     */
    suspend fun unblockUser(uid: String, targetUid: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .collection("private")
                .document("blocks")
                .update("blocked.$targetUid", FieldValue.delete())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes live changes to the user's blocked users map.
     */
    fun observeBlocked(uid: String): Flow<Map<String, String>> = callbackFlow {
        val docRef = usersCollection.document(uid)
            .collection("private")
            .document("blocks")

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyMap())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val blockedRaw = snapshot.get("blocked") as? Map<*, *>
                val blockedMap = blockedRaw?.entries?.mapNotNull { (k, v) ->
                    if (k is String && v != null) k to v.toString() else null
                }?.toMap() ?: emptyMap()
                trySend(blockedMap)
            } else {
                trySend(emptyMap())
            }
        }
        awaitClose { listener.remove() }
    }
}

