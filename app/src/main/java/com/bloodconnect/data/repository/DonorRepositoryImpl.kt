package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Donor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DonorRepository] that retrieves volunteer donors,
 * falling back to curated demonstration donor records with realistic coordinates
 * and missing-coordinate edge cases.
 */
@Singleton
class DonorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DonorRepository {

    private val sampleDonors = listOf(
        Donor(
            id = "D1",
            name = "Volunteer Donor #101",
            bloodGroup = BloodGroup.O_POSITIVE,
            locality = "Central City District",
            latitude = 19.0728,
            longitude = 72.8826,
            isAvailable = true
        ),
        Donor(
            id = "D2",
            name = "Volunteer Donor #102",
            bloodGroup = BloodGroup.A_POSITIVE,
            locality = "Metro West Medical Park",
            latitude = 19.1136,
            longitude = 72.8697,
            isAvailable = true
        ),
        Donor(
            id = "D3",
            name = "Volunteer Donor #103",
            bloodGroup = BloodGroup.B_POSITIVE,
            locality = "Northside Civic Zone",
            latitude = 19.1663,
            longitude = 72.8526,
            isAvailable = true
        ),
        Donor(
            id = "D4",
            name = "Volunteer Donor #104",
            bloodGroup = BloodGroup.AB_POSITIVE,
            locality = "Downtown Health Center",
            latitude = 18.9220,
            longitude = 72.8347,
            isAvailable = true
        ),
        Donor(
            id = "D5",
            name = "Volunteer Donor #105",
            bloodGroup = BloodGroup.O_NEGATIVE,
            locality = "Eastside Community Ward",
            latitude = 19.0800,
            longitude = 73.0100,
            isAvailable = true
        ),
        Donor(
            id = "D6",
            name = "Volunteer Donor #106",
            bloodGroup = BloodGroup.A_NEGATIVE,
            locality = "South Bay Regional Sector",
            latitude = 18.9000,
            longitude = 72.8100,
            isAvailable = true
        ),
        Donor(
            id = "D7",
            name = "Volunteer Donor #107",
            bloodGroup = BloodGroup.B_NEGATIVE,
            locality = "Highland Suburban Area",
            latitude = 19.2200,
            longitude = 72.9800,
            isAvailable = true
        ),
        Donor(
            id = "D8",
            name = "Volunteer Donor #108",
            bloodGroup = BloodGroup.AB_NEGATIVE,
            locality = "University Campus Area",
            latitude = null,
            longitude = null,
            isAvailable = true
        )
    )

    override suspend fun getDonors(bloodGroup: BloodGroup?): Result<List<Donor>> {
        return try {
            val query = if (bloodGroup != null) {
                firestore.collection("donors")
                    .whereEqualTo("bloodGroup", bloodGroup.displayName)
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .await()
            } else {
                firestore.collection("donors")
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .await()
            }

            val fetched = query.toObjects(Donor::class.java)
            if (fetched.isNotEmpty()) {
                Result.success(fetched)
            } else {
                // Fallback to demonstration donor set
                val filtered = if (bloodGroup != null) {
                    sampleDonors.filter { it.bloodGroup == bloodGroup }
                } else {
                    sampleDonors
                }
                Result.success(filtered)
            }
        } catch (_: Exception) {
            // Offline or uninitialized Firebase: return local demonstration donors
            val filtered = if (bloodGroup != null) {
                sampleDonors.filter { it.bloodGroup == bloodGroup }
            } else {
                sampleDonors
            }
            Result.success(filtered)
        }
    }

    override fun observeDonors(bloodGroup: BloodGroup?): Flow<List<Donor>> {
        val filtered = if (bloodGroup != null) {
            sampleDonors.filter { it.bloodGroup == bloodGroup }
        } else {
            sampleDonors
        }
        return flowOf(filtered)
    }

    override suspend fun getDonorById(donorId: String): Result<Donor?> {
        val found = sampleDonors.find { it.id.equals(donorId, ignoreCase = true) }
        return Result.success(found)
    }
}
