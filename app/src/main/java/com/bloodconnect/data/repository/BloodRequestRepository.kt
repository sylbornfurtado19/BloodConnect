package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.RequestStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for creating, querying, and updating emergency blood requests.
 */
interface BloodRequestRepository {

    /**
     * Submits and broadcasts a new emergency blood request.
     *
     * @param request Immutable [BloodRequest] entity to persist.
     * @return [Result] containing the saved [BloodRequest] on success.
     */
    suspend fun createBloodRequest(request: BloodRequest): Result<BloodRequest>

    /**
     * Retrieves all blood requests submitted by a specific user.
     *
     * @param userId Unique identifier of the requester.
     * @return [Result] containing the list of [BloodRequest] records.
     */
    suspend fun getRequestsForUser(userId: String): Result<List<BloodRequest>>

    /**
     * Observes blood requests submitted by a specific user as a continuous [Flow].
     *
     * @param userId Unique identifier of the requester.
     * @return [Flow] emitting the updated list of user blood requests.
     */
    fun observeRequestsForUser(userId: String): Flow<List<BloodRequest>>

    /**
     * Updates the lifecycle status of an existing blood request.
     *
     * @param requestId Identifier of the request to update.
     * @param status New [RequestStatus] to apply.
     * @return [Result] indicating update outcome.
     */
    suspend fun updateRequestStatus(requestId: String, status: RequestStatus): Result<Unit>

    /**
     * Retrieves all active blood requests across the network.
     *
     * @return [Result] containing the list of all active [BloodRequest]s.
     */
    suspend fun getAllRequests(): Result<List<BloodRequest>>
}
