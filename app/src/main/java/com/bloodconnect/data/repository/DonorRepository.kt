package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Donor
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for donor discovery, filtering, and profile lookup.
 */
interface DonorRepository {

    /**
     * Retrieves available donors, optionally filtered by target blood group.
     *
     * @param bloodGroup Optional [BloodGroup] filter. If null, retrieves all available donors.
     * @return [Result] containing the list of matching [Donor]s.
     */
    suspend fun getDonors(bloodGroup: BloodGroup? = null): Result<List<Donor>>

    /**
     * Observes available donors as a continuous [Flow], optionally filtered by blood group.
     *
     * @param bloodGroup Optional [BloodGroup] filter.
     * @return [Flow] emitting updated lists of [Donor]s.
     */
    fun observeDonors(bloodGroup: BloodGroup? = null): Flow<List<Donor>>

    /**
     * Looks up an individual donor by their unique identifier.
     *
     * @param donorId Unique donor ID.
     * @return [Result] containing the [Donor] if found, or null if no record exists.
     */
    suspend fun getDonorById(donorId: String): Result<Donor?>
}
