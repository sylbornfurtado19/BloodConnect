package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Donor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DonorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DonorRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun getDonors(bloodGroup: BloodGroup?): Result<List<Donor>> {
        return try {
            val query = if (bloodGroup != null) {
                usersCollection
                    .whereEqualTo("bloodGroup", bloodGroup.displayName)
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .await()
            } else {
                usersCollection
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .await()
            }
            val donors = query.documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: doc.id
                val name = doc.getString("fullName") ?: doc.getString("displayName") ?: "Anonymous"
                val bgStr = doc.getString("bloodGroup") ?: "O+"
                val bg = BloodGroup.fromString(bgStr) ?: BloodGroup.O_POSITIVE
                val locality = doc.getString("locality") ?: "City Center"
                val lat = doc.getDouble("latitude")
                val lon = doc.getDouble("longitude")
                val available = doc.getBoolean("isAvailable") ?: true

                Donor(
                    id = uid,
                    name = name,
                    bloodGroup = bg,
                    locality = locality,
                    latitude = lat,
                    longitude = lon,
                    isAvailable = available
                )
            }
            Result.success(donors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeDonors(bloodGroup: BloodGroup?): Flow<List<Donor>> = callbackFlow {
        val query = if (bloodGroup != null) {
            usersCollection
                .whereEqualTo("bloodGroup", bloodGroup.displayName)
                .whereEqualTo("isAvailable", true)
        } else {
            usersCollection
                .whereEqualTo("isAvailable", true)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val donors = snapshot?.documents?.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: doc.id
                val name = doc.getString("fullName") ?: doc.getString("displayName") ?: "Anonymous"
                val bgStr = doc.getString("bloodGroup") ?: "O+"
                val bg = BloodGroup.fromString(bgStr) ?: BloodGroup.O_POSITIVE
                val locality = doc.getString("locality") ?: "City Center"
                val lat = doc.getDouble("latitude")
                val lon = doc.getDouble("longitude")
                val available = doc.getBoolean("isAvailable") ?: true

                Donor(
                    id = uid,
                    name = name,
                    bloodGroup = bg,
                    locality = locality,
                    latitude = lat,
                    longitude = lon,
                    isAvailable = available
                )
            } ?: emptyList()
            trySend(donors)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getDonorById(donorId: String): Result<Donor?> {
        return try {
            val doc = usersCollection.document(donorId).get().await()
            if (!doc.exists()) {
                return Result.success(null)
            }
            val uid = doc.getString("uid") ?: doc.id
            val name = doc.getString("fullName") ?: doc.getString("displayName") ?: "Anonymous"
            val bgStr = doc.getString("bloodGroup") ?: "O+"
            val bg = BloodGroup.fromString(bgStr) ?: BloodGroup.O_POSITIVE
            val locality = doc.getString("locality") ?: "City Center"
            val lat = doc.getDouble("latitude")
            val lon = doc.getDouble("longitude")
            val available = doc.getBoolean("isAvailable") ?: true

            val donor = Donor(
                id = uid,
                name = name,
                bloodGroup = bg,
                locality = locality,
                latitude = lat,
                longitude = lon,
                isAvailable = available
            )
            Result.success(donor)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
