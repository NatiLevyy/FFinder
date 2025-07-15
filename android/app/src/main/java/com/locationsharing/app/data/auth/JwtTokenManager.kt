package com.locationsharing.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages JWT tokens including storage, retrieval, and validation.
 * Uses encrypted shared preferences for secure token storage.
 */
@Singleton
class JwtTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Stores authentication tokens securely.
     * 
     * @param accessToken The JWT access token
     * @param refreshToken The refresh token
     * @param expiresAt The timestamp when the access token expires
     */
    suspend fun storeTokens(
        accessToken: String,
        refreshToken: String,
        expiresAt: Long
    ) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            .apply()
    }
    
    /**
     * Retrieves the stored access token.
     * 
     * @return The access token or null if not found
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Retrieves the stored refresh token.
     * 
     * @return The refresh token or null if not found
     */
    suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * Gets the token expiration timestamp.
     * 
     * @return The expiration timestamp or 0 if not found
     */
    suspend fun getTokenExpiresAt(): Long = withContext(Dispatchers.IO) {
        encryptedPrefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)
    }
    
    /**
     * Checks if the current access token is valid and not expired.
     * 
     * @return True if the token is valid, false otherwise
     */
    suspend fun isTokenValid(): Boolean = withContext(Dispatchers.IO) {
        val token = getAccessToken()
        val expiresAt = getTokenExpiresAt()
        val currentTime = System.currentTimeMillis()
        
        token != null && expiresAt > currentTime + TOKEN_REFRESH_BUFFER_MS
    }
    
    /**
     * Checks if the token needs to be refreshed.
     * 
     * @return True if the token should be refreshed, false otherwise
     */
    suspend fun shouldRefreshToken(): Boolean = withContext(Dispatchers.IO) {
        val expiresAt = getTokenExpiresAt()
        val currentTime = System.currentTimeMillis()
        
        expiresAt > 0 && expiresAt <= currentTime + TOKEN_REFRESH_BUFFER_MS
    }
    
    /**
     * Extracts user ID from JWT token payload.
     * 
     * @param token The JWT token
     * @return The user ID or null if extraction fails
     */
    fun extractUserIdFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            // Simple extraction - in production, use a proper JWT library
            val userIdRegex = "\"sub\":\"([^\"]+)\"".toRegex()
            userIdRegex.find(payload)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Checks if a JWT token is expired based on its payload.
     * 
     * @param token The JWT token
     * @return True if the token is expired, false otherwise
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            val expRegex = "\"exp\":(\\d+)".toRegex()
            val expMatch = expRegex.find(payload)
            
            if (expMatch != null) {
                val exp = expMatch.groupValues[1].toLong() * 1000 // Convert to milliseconds
                exp <= System.currentTimeMillis()
            } else {
                true // If no exp claim, consider expired
            }
        } catch (e: Exception) {
            true // If parsing fails, consider expired
        }
    }
    
    /**
     * Clears all stored authentication tokens.
     */
    suspend fun clearTokens() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRES_AT)
            .apply()
    }
    
    /**
     * Checks if any authentication tokens are stored.
     * 
     * @return True if tokens exist, false otherwise
     */
    suspend fun hasStoredTokens(): Boolean = withContext(Dispatchers.IO) {
        encryptedPrefs.contains(KEY_ACCESS_TOKEN) && 
        encryptedPrefs.contains(KEY_REFRESH_TOKEN)
    }
}