package com.locationsharing.app.domain.repository

import com.locationsharing.app.domain.model.Contact
import com.locationsharing.app.domain.model.ContactHash
import com.locationsharing.app.domain.model.DiscoveredUser
import com.locationsharing.app.domain.model.UserDiscoveryResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface for discovering registered users from imported contacts
 * Handles secure contact hashing and Firebase user matching
 */
interface UserDiscoveryService {
    
    /**
     * Discover registered users from a list of contacts
     * Hashes contact information and queries Firebase for matches
     * 
     * @param contacts List of contacts to search for
     * @return Flow of UserDiscoveryResult with progress updates
     */
    fun discoverUsers(contacts: List<Contact>): Flow<UserDiscoveryResult>
    
    /**
     * Discover users from pre-hashed contacts
     * Useful when hashing is done separately or cached
     * 
     * @param contactHashes List of contacts with pre-computed hashes
     * @return Flow of UserDiscoveryResult
     */
    fun discoverUsersFromHashes(contactHashes: List<ContactHash>): Flow<UserDiscoveryResult>
    
    /**
     * Hash contacts for discovery without performing the search
     * Useful for caching or batch processing
     * 
     * @param contacts List of contacts to hash
     * @return List of ContactHash objects
     */
    suspend fun hashContacts(contacts: List<Contact>): List<ContactHash>
    
    /**
     * Get friend request status for discovered users
     * Updates the friendRequestStatus field in DiscoveredUser objects
     * 
     * @param discoveredUsers List of discovered users to check
     * @return Updated list with current friend request statuses
     */
    suspend fun updateFriendRequestStatuses(discoveredUsers: List<DiscoveredUser>): List<DiscoveredUser>
    
    /**
     * Search for a specific user by phone number or email
     * 
     * @param phoneNumber Phone number to search for (optional)
     * @param email Email address to search for (optional)
     * @return DiscoveredUser if found, null otherwise
     */
    suspend fun findUserByContact(phoneNumber: String? = null, email: String? = null): DiscoveredUser?
    
    /**
     * Get discovery statistics for analytics
     * 
     * @return DiscoveryStats with usage information
     */
    suspend fun getDiscoveryStats(): DiscoveryStats
    
    /**
     * Clear any cached discovery data
     */
    suspend fun clearCache()
}

/**
 * Statistics about user discovery operations
 */
data class DiscoveryStats(
    val totalDiscoveryAttempts: Int = 0,
    val totalContactsProcessed: Int = 0,
    val totalUsersDiscovered: Int = 0,
    val averageMatchRate: Float = 0f,
    val lastDiscoveryTime: Long = 0L,
    val cacheHitRate: Float = 0f
) {
    val hasStats: Boolean
        get() = totalDiscoveryAttempts > 0
}