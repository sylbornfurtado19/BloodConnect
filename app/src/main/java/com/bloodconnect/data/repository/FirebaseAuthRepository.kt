package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [AuthRepository] integrating Firebase Authentication
 * and Cloud Firestore user profile synchronization.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                trySend(null)
            } else {
                launch {
                    trySend(fetchUserProfile(firebaseUser))
                }
            }
        }

        firebaseAuth.addAuthStateListener(authListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authListener)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Authentication succeeded but user profile was null."))

            val user = fetchUserProfile(firebaseUser)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(sanitizeAuthException(e))
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        bloodGroup: BloodGroup,
        locality: String
    ): Result<User> {
        return try {
            val trimmedEmail = email.trim()
            val trimmedName = displayName.trim()
            val trimmedLocality = locality.trim()

            val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Account creation succeeded but user was null."))

            if (trimmedName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()
                try {
                    firebaseUser.updateProfile(profileUpdates).await()
                } catch (_: Exception) {
                    // Profile displayName update failure is non-fatal since Firestore retains source-of-truth
                }
            }

            val user = User(
                id = firebaseUser.uid,
                email = trimmedEmail,
                displayName = trimmedName,
                bloodGroup = bloodGroup,
                isAvailable = true,
                locality = trimmedLocality,
                fcmToken = null
            )

            val userMap = hashMapOf(
                "id" to user.id,
                "email" to user.email,
                "displayName" to user.displayName,
                "bloodGroup" to user.bloodGroup.displayName,
                "isAvailable" to user.isAvailable,
                "locality" to user.locality,
                "fcmToken" to user.fcmToken,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(userMap)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(sanitizeAuthException(e))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchUserProfile(firebaseUser: FirebaseUser): User {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .get()
                .await()

            if (snapshot.exists()) {
                mapDocumentToUser(firebaseUser.uid, snapshot, firebaseUser)
            } else {
                fallbackUser(firebaseUser)
            }
        } catch (_: Exception) {
            fallbackUser(firebaseUser)
        }
    }

    private fun mapDocumentToUser(
        uid: String,
        snapshot: DocumentSnapshot,
        firebaseUser: FirebaseUser
    ): User {
        val bloodGroupString = snapshot.getString("bloodGroup") ?: "O+"
        val bloodGroup = BloodGroup.fromString(bloodGroupString) ?: BloodGroup.O_POSITIVE

        return User(
            id = uid,
            email = snapshot.getString("email") ?: firebaseUser.email.orEmpty(),
            displayName = snapshot.getString("displayName") ?: firebaseUser.displayName.orEmpty(),
            bloodGroup = bloodGroup,
            isAvailable = snapshot.getBoolean("isAvailable") ?: true,
            locality = snapshot.getString("locality").orEmpty(),
            fcmToken = snapshot.getString("fcmToken")
        )
    }

    private fun fallbackUser(firebaseUser: FirebaseUser): User {
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email.orEmpty(),
            displayName = firebaseUser.displayName.orEmpty(),
            bloodGroup = BloodGroup.O_POSITIVE,
            isAvailable = true,
            locality = "",
            fcmToken = null
        )
    }

    private fun sanitizeAuthException(e: Exception): Exception {
        val safeMessage = e.localizedMessage ?: "Authentication operation failed."
        return Exception(safeMessage, e)
    }
}
