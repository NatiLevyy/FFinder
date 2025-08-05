package com.locationsharing.app.data.discovery

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.locationsharing.app.domain.model.Contact
import com.locationsharing.app.domain.model.ContactHash
import com.locationsharing.app.domain.model.ContactMatchType
import com.locationsharing.app.domain.model.DiscoveredUser
import com.locationsharing.app.domain.repository.DiscoveryStats
import com.locationsharing.app.domain.model.FriendRequestStatus
import com.locationsharing.app.domain.model.UserDiscoveryResult
import com.locationsharing.app.domain.model.toContactHash
import com.locationsharing.app.domain.repository.UserDiscoveryService
import com.locationsharing.app.utils.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of UserDiscoveryService
 * Handles secure contact hashing and Firestore queries for user discovery
 */
@Singleton
class FirebaseUserDiscoveryService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserDiscoveryService {
    
    companion object {
        private const val TAG = "UserDiscoveryService"
        private const val USERS_COLLECTION = "users"
        private const val FRIEND_REQUESTS_COLLECTION = "friendRequests"
        private const val BATCH_SIZE = 10 // Firestore 'in' query limit
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    // Cache for discovery results
    private var cachedStats = DiscoveryStats(
        totalDiscoveryAttempts = 0,
        totalContactsProcessed = 0,
        totalUsersDiscovered = 0,
        averageMatchRate = 0f,
        lastDiscoveryTime = 0L,
        cacheHitRate = 0f
    )
    private val discoveryCache = mutableMapOf<String, DiscoveredUser>()
    
    override fun discoverUsers(contacts: List<Contact>): Flow<UserDiscoveryResult> = flow {
        try {
            Timber.d("Starting user discovery for ${contacts.size} contacts")
            
            if (contacts.isEmpty()) {
                emit(UserDiscoveryResult.NoMatches(0))
                return@flow
            }
            
            // Hash contacts first
            val contactHashes = hashContacts(contacts)
            val validHashes = contactHashes.filter { it.isValidForDiscovery }
            
            if (validHashes.isEmpty()) {
                Timber.w("No valid hashes generated from ${contacts.size} contacts")
                emit(UserDiscoveryResult.NoMatches(contacts.size))
                return@flow
            }
            
            // Discover users from hashes
            discoverUsersFromHashes(validHashes).collect { result ->
                emit(result)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error during user discovery")
            emit(UserDiscoveryResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun discoverUsersFromHashes(contactHashes: List<ContactHash>): Flow<UserDiscoveryResult> = flow {
        try {
            val validHashes = contactHashes.filter { it.isValidForDiscovery }
            if (validHashes.isEmpty()) {
                emit(UserDiscoveryResult.NoMatches(contactHashes.size))
                return@flow
            }
            
            val discoveredUsers = mutableListOf<DiscoveredUser>()
            
            // Process in batches to handle Firestore limitations
            val phoneHashes = validHashes.mapNotNull { it.hashedPhone }.distinct()
            val emailHashes = validHashes.mapNotNull { it.hashedEmail }.distinct()
            
            // Query by phone hashes
            if (phoneHashes.isNotEmpty()) {
                val phoneMatches = queryUsersByHashes(phoneHashes, "hashedPhone")
                discoveredUsers.addAll(
                    createDiscoveredUsers(phoneMatches, validHashes, ContactMatchType.PHONE)
                )
            }
            
            // Query by email hashes
            if (emailHashes.isNotEmpty()) {
                val emailMatches = queryUsersByHashes(emailHashes, "hashedEmail")
                discoveredUsers.addAll(
                    createDiscoveredUsers(emailMatches, validHashes, ContactMatchType.EMAIL)
                )
            }
            
            // Remove duplicates (same user matched by both phone and email)
            val uniqueUsers = discoveredUsers.groupBy { it.userId }
                .map { (_, users) ->
                    if (users.size > 1) {
                        // Merge multiple matches for same user
                        users.first().copy(matchType = ContactMatchType.BOTH)
                    } else {
                        users.first()
                    }
                }
            
            // Update friend request statuses
            val usersWithStatuses = updateFriendRequestStatuses(uniqueUsers)
            
            // Update cache and stats
            updateCache(usersWithStatuses)
            updateStats(contactHashes.size, usersWithStatuses.size)
            
            if (usersWithStatuses.isNotEmpty()) {
                emit(UserDiscoveryResult.Success(
                    discoveredUsers = usersWithStatuses,
                    totalContactsSearched = contactHashes.size
                ))
            } else {
                emit(UserDiscoveryResult.NoMatches(contactHashes.size))
            }
            
            Timber.d("Discovery completed: ${usersWithStatuses.size} users found from ${contactHashes.size} contacts")
            
        } catch (e: Exception) {
            Timber.e(e, "Error during hash-based discovery")
            emit(UserDiscoveryResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun hashContacts(contacts: List<Contact>): List<ContactHash> = withContext(Dispatchers.IO) {
        contacts.mapNotNull { contact ->
            try {
                val hashedPhone = contact.primaryPhoneNumber?.let { phone ->
                    CryptoUtils.hashPhoneNumber(phone)
                }
                
                val hashedEmail = contact.primaryEmailAddress?.let { email ->
                    CryptoUtils.hashEmail(email)
                }
                
                val compositeHash = if (hashedPhone != null && hashedEmail != null) {
                    CryptoUtils.createContactHash(
                        contact.primaryPhoneNumber,
                        contact.primaryEmailAddress
                    )
                } else null
                
                if (hashedPhone != null || hashedEmail != null) {
                    contact.toContactHash(
                        hashedPhone = hashedPhone,
                        hashedEmail = hashedEmail,
                        compositeHash = compositeHash
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to hash contact: ${contact.displayName}")
                null
            }
        }
    }
    
    override suspend fun updateFriendRequestStatuses(discoveredUsers: List<DiscoveredUser>): List<DiscoveredUser> = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Timber.w("No authenticated user for friend request status check")
            return@withContext discoveredUsers
        }
        
        try {
            val userIds = discoveredUsers.map { it.userId }
            val friendRequestStatuses = getFriendRequestStatuses(currentUserId, userIds)
            
            discoveredUsers.map { user ->
                user.copy(
                    friendRequestStatus = friendRequestStatuses[user.userId] ?: FriendRequestStatus.NONE
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating friend request statuses")
            discoveredUsers // Return original list if status update fails
        }
    }
    
    override suspend fun findUserByContact(phoneNumber: String?, email: String?): DiscoveredUser? = withContext(Dispatchers.IO) {
        try {
            val hashedPhone = phoneNumber?.let { CryptoUtils.hashPhoneNumber(it) }
            val hashedEmail = email?.let { CryptoUtils.hashEmail(it) }
            
            if (hashedPhone == null && hashedEmail == null) {
                return@withContext null
            }
            
            // Check cache first
            val cacheKey = hashedPhone ?: hashedEmail ?: return@withContext null
            discoveryCache[cacheKey]?.let { return@withContext it }
            
            // Query Firestore
            val query = when {
                hashedPhone != null -> firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("hashedPhone", hashedPhone)
                hashedEmail != null -> firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("hashedEmail", hashedEmail)
                else -> return@withContext null
            }
            
            val snapshot = query.limit(1).get().await()
            if (snapshot.isEmpty) {
                return@withContext null
            }
            
            val document = snapshot.documents.first()
            val contact = Contact(
                id = "temp_${System.currentTimeMillis()}",
                displayName = document.getString("displayName") ?: "Unknown",
                phoneNumbers = phoneNumber?.let { listOf(it) } ?: emptyList(),
                emailAddresses = email?.let { listOf(it) } ?: emptyList()
            )
            
            val discoveredUser = DiscoveredUser(
                userId = document.id,
                displayName = document.getString("displayName") ?: "Unknown",
                profilePictureUrl = document.getString("profilePictureUrl"),
                matchedContact = contact,
                matchType = when {
                    hashedPhone != null && hashedEmail != null -> ContactMatchType.BOTH
                    hashedPhone != null -> ContactMatchType.PHONE
                    else -> ContactMatchType.EMAIL
                },
                lastActive = document.getLong("lastActive"),
                isOnline = document.getBoolean("isOnline") ?: false
            )
            
            // Cache the result
            discoveryCache[cacheKey] = discoveredUser
            
            discoveredUser
            
        } catch (e: Exception) {
            Timber.e(e, "Error finding user by contact")
            null
        }
    }
    
    override suspend fun getDiscoveryStats(): DiscoveryStats {
        return cachedStats
    }
    
    override suspend fun clearCache() {
        discoveryCache.clear()
        cachedStats = DiscoveryStats(
            totalDiscoveryAttempts = 0,
            totalContactsProcessed = 0,
            totalUsersDiscovered = 0,
            averageMatchRate = 0f,
            lastDiscoveryTime = 0L,
            cacheHitRate = 0f
        )
        Timber.d("Discovery cache cleared")
    }
    
    /**
     * Query Firestore for users with matching hashes
     */
    private suspend fun queryUsersByHashes(hashes: List<String>, field: String): List<Map<String, Any?>> {
        val results = mutableListOf<Map<String, Any?>>()
        
        // Process in batches due to Firestore 'in' query limitations
        hashes.chunked(BATCH_SIZE).forEach { batch ->
            try {
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereIn(field, batch)
                    .get()
                    .await()
                
                snapshot.documents.forEach { document ->
                    val data = document.data?.toMutableMap() ?: mutableMapOf()
                    data["userId"] = document.id
                    results.add(data)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error querying users by $field")
            }
        }
        
        return results
    }
    
    /**
     * Create DiscoveredUser objects from Firestore results
     */
    private fun createDiscoveredUsers(
        firestoreResults: List<Map<String, Any?>>,
        contactHashes: List<ContactHash>,
        matchType: ContactMatchType
    ): List<DiscoveredUser> {
        return firestoreResults.mapNotNull { userData ->
            try {
                val userId = userData["userId"] as? String ?: return@mapNotNull null
                val userHashedPhone = userData["hashedPhone"] as? String
                val userHashedEmail = userData["hashedEmail"] as? String
                
                // Find matching contact
                val matchingContact = contactHashes.find { contactHash ->
                    when (matchType) {
                        ContactMatchType.PHONE -> contactHash.hashedPhone == userHashedPhone
                        ContactMatchType.EMAIL -> contactHash.hashedEmail == userHashedEmail
                        ContactMatchType.BOTH -> 
                            contactHash.hashedPhone == userHashedPhone || 
                            contactHash.hashedEmail == userHashedEmail
                    }
                }?.originalContact ?: return@mapNotNull null
                
                DiscoveredUser(
                    userId = userId,
                    displayName = userData["displayName"] as? String ?: "Unknown User",
                    profilePictureUrl = userData["profilePictureUrl"] as? String,
                    matchedContact = matchingContact,
                    matchType = matchType,
                    lastActive = userData["lastActive"] as? Long,
                    isOnline = userData["isOnline"] as? Boolean ?: false
                )
            } catch (e: Exception) {
                Timber.w(e, "Error creating DiscoveredUser from Firestore data")
                null
            }
        }
    }
    
    /**
     * Get friend request statuses for multiple users
     */
    private suspend fun getFriendRequestStatuses(currentUserId: String, userIds: List<String>): Map<String, FriendRequestStatus> {
        val statuses = mutableMapOf<String, FriendRequestStatus>()
        
        try {
            // Query sent requests
            val sentRequests = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", currentUserId)
                .whereIn("toUserId", userIds)
                .get()
                .await()
            
            sentRequests.documents.forEach { doc ->
                val toUserId = doc.getString("toUserId")
                val status = doc.getString("status")
                if (toUserId != null) {
                    statuses[toUserId] = when (status) {
                        "pending" -> FriendRequestStatus.SENT
                        "accepted" -> FriendRequestStatus.ACCEPTED
                        "declined" -> FriendRequestStatus.DECLINED
                        "blocked" -> FriendRequestStatus.BLOCKED
                        else -> FriendRequestStatus.NONE
                    }
                }
            }
            
            // Query received requests
            val receivedRequests = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("toUserId", currentUserId)
                .whereIn("fromUserId", userIds)
                .get()
                .await()
            
            receivedRequests.documents.forEach { doc ->
                val fromUserId = doc.getString("fromUserId")
                val status = doc.getString("status")
                if (fromUserId != null && !statuses.containsKey(fromUserId)) {
                    statuses[fromUserId] = when (status) {
                        "pending" -> FriendRequestStatus.RECEIVED
                        "accepted" -> FriendRequestStatus.ACCEPTED
                        "declined" -> FriendRequestStatus.DECLINED
                        "blocked" -> FriendRequestStatus.BLOCKED
                        else -> FriendRequestStatus.NONE
                    }
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error fetching friend request statuses")
        }
        
        return statuses
    }
    
    /**
     * Update discovery cache
     */
    private fun updateCache(users: List<DiscoveredUser>) {
        users.forEach { user ->
            user.matchedContact.primaryPhoneNumber?.let { phone ->
                CryptoUtils.hashPhoneNumber(phone)?.let { hash ->
                    discoveryCache[hash] = user
                }
            }
            user.matchedContact.primaryEmailAddress?.let { email ->
                CryptoUtils.hashEmail(email)?.let { hash ->
                    discoveryCache[hash] = user
                }
            }
        }
    }
    
    /**
     * Update discovery statistics
     */
    private fun updateStats(contactsProcessed: Int, usersDiscovered: Int) {
        cachedStats = cachedStats.copy(
            totalDiscoveryAttempts = cachedStats.totalDiscoveryAttempts + 1,
            totalContactsProcessed = cachedStats.totalContactsProcessed + contactsProcessed,
            totalUsersDiscovered = cachedStats.totalUsersDiscovered + usersDiscovered,
            lastDiscoveryTime = System.currentTimeMillis(),
            averageMatchRate = if (cachedStats.totalContactsProcessed > 0) {
                cachedStats.totalUsersDiscovered.toFloat() / cachedStats.totalContactsProcessed.toFloat()
            } else 0f
        )
    }
}