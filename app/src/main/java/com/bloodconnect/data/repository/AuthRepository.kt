package com.bloodconnect.data.repository

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface managing user authentication, registration, and active session observation.
 */
interface AuthRepository {

    /**
     * Observes the currently authenticated user session as a continuous [Flow].
     * Emits `null` when no user is signed in.
     */
    fun observeCurrentUser(): Flow<User?>

    /**
     * Signs in an existing user with email and password credentials.
     *
     * @param email User's registered email address.
     * @param password User's secret password.
     * @return [Result] enclosing the authenticated [User] on success or an error on failure.
     */
    suspend fun signIn(email: String, password: String): Result<User>

    /**
     * Registers a new donor or recipient account.
     *
     * @param email Email address for the new account.
     * @param password Password for account security.
     * @param displayName User's full or preferred name.
     * @param bloodGroup Registered blood group.
     * @param locality User's city, district, or operational area.
     * @return [Result] enclosing the created [User] on success or an error on failure.
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        bloodGroup: BloodGroup,
        locality: String = ""
    ): Result<User>

    /**
     * Terminates the active user session and clears authentication tokens.
     *
     * @return [Result] indicating success or failure.
     */
    suspend fun signOut(): Result<Unit>
}
