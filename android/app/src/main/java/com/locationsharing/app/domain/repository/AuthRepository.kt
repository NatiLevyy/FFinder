package com.locationsharing.app.domain.repository

import com.locationsharing.app.data.models.AuthResult
import com.locationsharing.app.data.models.User

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<AuthResult>
    suspend fun signUp(email: String, password: String, displayName: String): Result<AuthResult>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun refreshToken(): Result<String>
    suspend fun isUserAuthenticated(): Boolean
    suspend fun resetPassword(email: String): Result<Unit>
}