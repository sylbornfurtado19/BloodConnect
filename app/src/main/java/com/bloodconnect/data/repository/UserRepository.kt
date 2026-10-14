package com.bloodconnect.data.repository

import com.bloodconnect.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(uid: String): Result<User?> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val user = snapshot.toObject(User::class.java)?.let { parsed ->
                // Ensure backward compatibility when legacy documents only contain "available"
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
}
