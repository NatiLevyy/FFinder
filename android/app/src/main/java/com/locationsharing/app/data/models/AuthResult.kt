package com.locationsharing.app.data.models

/**
 * Represents the result of an authentication operation.
 * 
 * @property user The authenticated user information
 * @property token The JWT authentication token
 * @property refreshToken The refresh token for token renewal
 * @property expiresAt The timestamp when the token expires
 */
data class AuthResult(
    val user: User,
    val token: String,
    val refreshToken: String,
    val expiresAt: Long
)