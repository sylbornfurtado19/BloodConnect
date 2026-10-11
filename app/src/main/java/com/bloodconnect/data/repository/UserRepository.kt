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
            val user = snapshot.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAvailability(uid: String, isAvailable: Boolean): Result<Unit> {
        return try {
            usersCollection.document(uid).update("available", isAvailable).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchDonors(bloodGroup: String): Result<List<User>> {
        return try {
            val query = if (bloodGroup.isBlank()) {
                usersCollection.whereEqualTo("available", true).get().await()
            } else {
                usersCollection
                    .whereEqualTo("bloodGroup", bloodGroup)
                    .whereEqualTo("available", true)
                    .get().await()
            }
            val donors = query.toObjects(User::class.java)
            Result.success(donors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
