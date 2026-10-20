package com.bloodconnect.data.repository

import com.bloodconnect.BuildConfig
import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.DonationEligibility
import com.bloodconnect.data.model.Donor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Constant toggle for enabling sample donor records during debug testing.
 * Defaults to false so real Firestore data is always served by default.
 */
const val USE_SAMPLE_DONORS: Boolean = false

private fun shouldUseSampleDonors(): Boolean {
    return BuildConfig.DEBUG && USE_SAMPLE_DONORS
}

/**
 * Implementation of [DonorRepository] that queries Firestore for registered donors.
 */
@Singleton
class DonorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DonorRepository {

    private val usersCollection = firestore.collection("users")

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
                val lastDonated = doc.getLong("lastDonationAt")
                val isEligible = DonationEligibility.isEligible(lastDonated)
                val available = resolveDonorAvailability(
                    isAvailable = doc.getBoolean("isAvailable"),
                    legacyAvailable = doc.getBoolean("available")
                ) && isEligible

                if (!available) return@mapNotNull null

                Donor(
                    id = uid,
                    name = name,
                    bloodGroup = bg,
                    locality = locality,
                    latitude = lat,
                    longitude = lon,
                    isAvailable = true,
                    lastDonationAt = lastDonated
                )
            }

            if (donors.isNotEmpty() || !shouldUseSampleDonors()) {
                Result.success(donors)
            } else {
                val sampleFiltered = if (bloodGroup != null) {
                    sampleDonors.filter { it.bloodGroup == bloodGroup }
                } else {
                    sampleDonors
                }
                Result.success(sampleFiltered)
            }
        } catch (e: Exception) {
            if (shouldUseSampleDonors()) {
                val sampleFiltered = if (bloodGroup != null) {
                    sampleDonors.filter { it.bloodGroup == bloodGroup }
                } else {
                    sampleDonors
                }
                Result.success(sampleFiltered)
            } else {
                Result.failure(e)
            }
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
                if (shouldUseSampleDonors()) {
                    val sampleFiltered = if (bloodGroup != null) {
                        sampleDonors.filter { it.bloodGroup == bloodGroup }
                    } else {
                        sampleDonors
                    }
                    trySend(sampleFiltered)
                } else {
                    close(error)
                }
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
                val lastDonated = doc.getLong("lastDonationAt")
                val isEligible = DonationEligibility.isEligible(lastDonated)
                val available = resolveDonorAvailability(
                    isAvailable = doc.getBoolean("isAvailable"),
                    legacyAvailable = doc.getBoolean("available")
                ) && isEligible

                if (!available) return@mapNotNull null

                Donor(
                    id = uid,
                    name = name,
                    bloodGroup = bg,
                    locality = locality,
                    latitude = lat,
                    longitude = lon,
                    isAvailable = true,
                    lastDonationAt = lastDonated
                )
            } ?: emptyList()

            if (donors.isNotEmpty() || !shouldUseSampleDonors()) {
                trySend(donors)
            } else {
                val sampleFiltered = if (bloodGroup != null) {
                    sampleDonors.filter { it.bloodGroup == bloodGroup }
                } else {
                    sampleDonors
                }
                trySend(sampleFiltered)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getDonorById(donorId: String): Result<Donor?> {
        return try {
            val doc = usersCollection.document(donorId).get().await()
            if (doc.exists()) {
                val uid = doc.getString("uid") ?: doc.id
                val name = doc.getString("fullName") ?: doc.getString("displayName") ?: "Anonymous"
                val bgStr = doc.getString("bloodGroup") ?: "O+"
                val bg = BloodGroup.fromString(bgStr) ?: BloodGroup.O_POSITIVE
                val locality = doc.getString("locality") ?: "City Center"
                val lat = doc.getDouble("latitude")
                val lon = doc.getDouble("longitude")
                val lastDonated = doc.getLong("lastDonationAt")
                val isEligible = DonationEligibility.isEligible(lastDonated)
                val available = resolveDonorAvailability(
                    isAvailable = doc.getBoolean("isAvailable"),
                    legacyAvailable = doc.getBoolean("available")
                ) && isEligible

                val donor = Donor(
                    id = uid,
                    name = name,
                    bloodGroup = bg,
                    locality = locality,
                    latitude = lat,
                    longitude = lon,
                    isAvailable = available,
                    lastDonationAt = lastDonated
                )
                Result.success(donor)
            } else if (shouldUseSampleDonors()) {
                val found = sampleDonors.find { it.id.equals(donorId, ignoreCase = true) }
                Result.success(found)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            if (shouldUseSampleDonors()) {
                val found = sampleDonors.find { it.id.equals(donorId, ignoreCase = true) }
                Result.success(found)
            } else {
                Result.failure(e)
            }
        }
    }
}

/**
 * Resolves donor availability with backward compatibility for legacy records.
 * Prefers the standardized "isAvailable" field, falling back to legacy "available",
 * and finally defaulting to [default] (true) if neither field is present.
 */
fun resolveDonorAvailability(
    isAvailable: Boolean?,
    legacyAvailable: Boolean?,
    default: Boolean = true
): Boolean {
    return isAvailable ?: legacyAvailable ?: default
}
