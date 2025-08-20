package com.locationsharing.app.data.user

import com.google.firebase.firestore.FirebaseFirestore
import com.locationsharing.app.data.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user profile creation and updates with phone hash
 */
@Singleton
class UserProfileManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager
) {
    
    companion object {
        private const val USERS_PUBLIC_COLLECTION = "users_public"
        // TODO: Move to Remote Config in production
        private const val PHONE_HASH_SALT = "FFinder_2024_Salt_v1"
        private const val HASH_VERSION = 1
    }
    
    /**
     * Create or update user public profile after successful phone linking
     * 
     * @param phoneE164 Phone number in E.164 format
     * @param displayName Optional display name for the user
     * @return Result indicating success or failure
     */
    suspend fun createOrUpdateUserProfile(
        phoneE164: String,
        displayName: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = authManager.ensureSignedIn()
            val userId = user.uid
            
            Timber.d("Creating/updating user profile for: $userId")
            
            // Compute phone hash
            val phoneHash = computePhoneHash(phoneE164)
            
            val profileData = mutableMapOf<String, Any>(
                "phoneHash" to phoneHash,
                "discoverable" to true,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            // Add display name if provided
            displayName?.let { 
                profileData["displayName"] = it 
            }
            
            // Check if profile already exists to set createdAt
            val existingDoc = firestore.collection(USERS_PUBLIC_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!existingDoc.exists()) {
                profileData["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                Timber.d("Creating new user profile")
            } else {
                Timber.d("Updating existing user profile")
            }
            
            // Upsert the user profile
            firestore.collection(USERS_PUBLIC_COLLECTION)
                .document(userId)
                .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            Timber.d("User profile created/updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create/update user profile")
            Result.failure(e)
        }
    }
    
    /**
     * Update user discoverability setting
     * 
     * @param discoverable Whether user should be discoverable by contacts
     * @return Result indicating success or failure
     */
    suspend fun updateDiscoverability(discoverable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = authManager.ensureSignedIn()
            val userId = user.uid
            
            firestore.collection(USERS_PUBLIC_COLLECTION)
                .document(userId)
                .update(
                    "discoverable", discoverable,
                    "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                .await()
            
            Timber.d("User discoverability updated: $discoverable")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update discoverability")
            Result.failure(e)
        }
    }
    
    /**
     * Update user display name
     * 
     * @param displayName New display name for the user
     * @return Result indicating success or failure
     */
    suspend fun updateDisplayName(displayName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = authManager.ensureSignedIn()
            val userId = user.uid
            
            firestore.collection(USERS_PUBLIC_COLLECTION)
                .document(userId)
                .update(
                    "displayName", displayName,
                    "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                .await()
            
            Timber.d("User display name updated: $displayName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update display name")
            Result.failure(e)
        }
    }
    
    /**
     * Get user's public profile
     * 
     * @param userId User ID to get profile for (defaults to current user)
     * @return UserPublicProfile or null if not found
     */
    suspend fun getUserProfile(userId: String? = null): UserPublicProfile? = withContext(Dispatchers.IO) {
        try {
            val targetUserId = userId ?: authManager.getCurrentUser()?.uid 
                ?: return@withContext null
            
            val doc = firestore.collection(USERS_PUBLIC_COLLECTION)
                .document(targetUserId)
                .get()
                .await()
            
            if (doc.exists()) {
                UserPublicProfile(
                    uid = targetUserId,
                    phoneHash = doc.getString("phoneHash"),
                    displayName = doc.getString("displayName"),
                    discoverable = doc.getBoolean("discoverable") ?: false,
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time,
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user profile")
            null
        }
    }
    
    /**
     * Compute SHA-256 hash of phone number with salt
     * 
     * @param phoneE164 Phone number in E.164 format
     * @return SHA-256 hash as hex string
     */
    private fun computePhoneHash(phoneE164: String): String {
        val input = "$PHONE_HASH_SALT$phoneE164"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Compute phone hash for contact discovery (static method for use in UserDiscoveryService)
     */
    fun computeContactPhoneHash(phoneE164: String): String {
        return computePhoneHash(phoneE164)
    }
}

/**
 * Data class representing a user's public profile
 */
data class UserPublicProfile(
    val uid: String,
    val phoneHash: String?,
    val displayName: String?,
    val discoverable: Boolean,
    val createdAt: Long?,
    val updatedAt: Long?
)