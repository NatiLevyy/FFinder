package com.locationsharing.app.data.friends

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.domain.model.FriendRequestStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for friends data operations
 */
interface FriendsRepository {
    fun getFriends(): Flow<List<Friend>>
    fun getOnlineFriends(): Flow<List<Friend>>
    fun getFriendById(friendId: String): Flow<Friend?>
    fun getFriendRequests(): Flow<List<FriendRequest>>
    fun getLocationUpdates(): Flow<List<LocationUpdateEvent>>
    fun getFriendActivities(): Flow<List<FriendActivityEvent>>
    
    suspend fun sendFriendRequest(toUserId: String, message: String? = null): Result<String>
    suspend fun checkFriendRequestStatus(toUserId: String): FriendRequestStatus
    suspend fun cancelFriendRequest(toUserId: String): Result<Unit>
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>
    suspend fun declineFriendRequest(requestId: String): Result<Unit>
    suspend fun removeFriend(friendId: String): Result<Unit>
    suspend fun blockFriend(friendId: String): Result<Unit>
    
    suspend fun updateLocationSharing(enabled: Boolean): Result<Unit>
    suspend fun updatePrivacySettings(preferences: FriendPreferences): Result<Unit>
    suspend fun updateOnlineStatus(isOnline: Boolean): Result<Unit>
    
    // Nearby panel interaction methods (stub implementations)
    suspend fun sendPing(friendId: String): Result<Unit>
    suspend fun stopReceivingLocation(friendId: String): Result<Unit>
}

/**
 * Firebase implementation of FriendsRepository
 * Provides real-time friends data with location updates
 */
@Singleton
class FirebaseFriendsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FriendsRepository {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FRIENDS_COLLECTION = "friends"
        private const val FRIEND_REQUESTS_COLLECTION = "friendRequests"
        private const val LOCATION_UPDATES_COLLECTION = "locationUpdates"
        private const val ACTIVITIES_COLLECTION = "activities"
    }
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    /**
     * Get real-time stream of user's friends
     */
    override fun getFriends(): Flow<List<Friend>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(FRIENDS_COLLECTION)
            .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to friends")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val friends = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val friendUserId = doc.getString("userId") ?: return@mapNotNull null
                        // Get friend's profile data
                        runBlocking { getFriendProfile(friendUserId) }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing friend document: ${doc.id}")
                        null
                    }
                } ?: emptyList()
                
                // Send the loaded friends
                val loadedFriends = friends.mapNotNull { it }
                trySend(loadedFriends)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get real-time stream of online friends only
     */
    override fun getOnlineFriends(): Flow<List<Friend>> = callbackFlow {
        getFriends().collect { allFriends ->
            val onlineFriends = allFriends.filter { it.isOnline() }
            trySend(onlineFriends)
        }
    }
    
    /**
     * Get real-time stream of a specific friend
     */
    override fun getFriendById(friendId: String): Flow<Friend?> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .document(friendId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to friend: $friendId")
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val friend = snapshot?.let { Friend.fromDocument(it) }
                trySend(friend)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get real-time stream of friend requests
     */
    override fun getFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection(FRIEND_REQUESTS_COLLECTION)
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to friend requests")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    FriendRequest.fromDocument(doc)
                } ?: emptyList()
                
                trySend(requests)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get real-time stream of location updates from friends
     */
    override fun getLocationUpdates(): Flow<List<LocationUpdateEvent>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection(LOCATION_UPDATES_COLLECTION)
            .whereArrayContains("visibleTo", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Limit to recent updates
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to location updates")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val updates = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        LocationUpdateEvent(
                            friendId = data["friendId"] as? String ?: "",
                            previousLocation = (data["previousLocation"] as? Map<String, Any>)?.let {
                                FriendLocation.fromMap(it)
                            },
                            newLocation = FriendLocation.fromMap(
                                data["newLocation"] as? Map<String, Any> ?: emptyMap()
                            ),
                            updateType = LocationUpdateType.valueOf(
                                data["updateType"] as? String ?: "POSITION_CHANGE"
                            ),
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing location update: ${doc.id}")
                        null
                    }
                } ?: emptyList()
                
                trySend(updates)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get real-time stream of friend activities
     */
    override fun getFriendActivities(): Flow<List<FriendActivityEvent>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection(ACTIVITIES_COLLECTION)
            .whereArrayContains("visibleTo", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Limit to recent activities
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to friend activities")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val activities = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        FriendActivityEvent(
                            friendId = data["friendId"] as? String ?: "",
                            activityType = FriendActivityType.valueOf(
                                data["activityType"] as? String ?: "LOCATION_UPDATED"
                            ),
                            data = data["data"] as? Map<String, Any> ?: emptyMap(),
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing activity: ${doc.id}")
                        null
                    }
                } ?: emptyList()
                
                trySend(activities)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Send a friend request to another user
     */
    override suspend fun sendFriendRequest(toUserId: String, message: String?): Result<String> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            // Check if request already exists
            val existingRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", FriendshipStatus.PENDING.name)
                .get()
                .await()
            
            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Friend request already sent"))
            }
            
            // Check if they're already friends
            val existingFriendship = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FRIENDS_COLLECTION)
                .document(toUserId)
                .get()
                .await()
            
            if (existingFriendship.exists()) {
                return Result.failure(Exception("Already friends with this user"))
            }
            
            // Get current user profile
            val currentUser = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val toUser = firestore.collection(USERS_COLLECTION)
                .document(toUserId)
                .get()
                .await()
            
            val request = FriendRequest(
                fromUserId = userId,
                toUserId = toUserId,
                fromUserName = currentUser.getString("name") ?: "",
                fromUserAvatar = currentUser.getString("avatarUrl") ?: "",
                toUserName = toUser.getString("name") ?: "",
                toUserAvatar = toUser.getString("avatarUrl") ?: "",
                status = FriendshipStatus.PENDING,
                message = message
            )
            
            val docRef = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .add(request)
                .await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Timber.e(e, "Error sending friend request")
            Result.failure(e)
        }
    }
    
    /**
     * Check the friend request status with another user
     */
    override suspend fun checkFriendRequestStatus(toUserId: String): FriendRequestStatus {
        return try {
            val userId = currentUserId ?: return FriendRequestStatus.NONE
            
            // Check if they're already friends
            val existingFriendship = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FRIENDS_COLLECTION)
                .document(toUserId)
                .get()
                .await()
            
            if (existingFriendship.exists()) {
                val status = existingFriendship.getString("status")
                return when (status) {
                    FriendshipStatus.ACCEPTED.name -> FriendRequestStatus.ACCEPTED
                    FriendshipStatus.BLOCKED.name -> FriendRequestStatus.BLOCKED
                    else -> FriendRequestStatus.NONE
                }
            }
            
            // Check for pending friend request sent by current user
            val sentRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", FriendshipStatus.PENDING.name)
                .get()
                .await()
            
            if (!sentRequest.isEmpty) {
                return FriendRequestStatus.SENT
            }
            
            // Check for pending friend request received by current user
            val receivedRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", toUserId)
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", FriendshipStatus.PENDING.name)
                .get()
                .await()
            
            if (!receivedRequest.isEmpty) {
                return FriendRequestStatus.RECEIVED
            }
            
            // Check for declined requests
            val declinedRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", FriendshipStatus.DECLINED.name)
                .get()
                .await()
            
            if (!declinedRequest.isEmpty) {
                return FriendRequestStatus.DECLINED
            }
            
            FriendRequestStatus.NONE
        } catch (e: Exception) {
            Timber.e(e, "Error checking friend request status for user: $toUserId")
            FriendRequestStatus.NONE
        }
    }
    
    /**
     * Cancel a friend request sent to another user
     */
    override suspend fun cancelFriendRequest(toUserId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            // Find the pending request
            val requestQuery = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", FriendshipStatus.PENDING.name)
                .get()
                .await()
            
            if (requestQuery.isEmpty) {
                return Result.failure(Exception("No pending friend request found"))
            }
            
            // Delete the request
            val requestDoc = requestQuery.documents.first()
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestDoc.id)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error cancelling friend request")
            Result.failure(e)
        }
    }
    
    /**
     * Accept a friend request
     */
    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            firestore.runTransaction { transaction ->
                // Update request status
                val requestRef = firestore.collection(FRIEND_REQUESTS_COLLECTION).document(requestId)
                transaction.update(requestRef, "status", FriendshipStatus.ACCEPTED.name)
                
                // Get request data
                val request = transaction.get(requestRef)
                val fromUserId = request.getString("fromUserId") ?: ""
                
                // Add friendship for current user
                val userFriendRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(FRIENDS_COLLECTION)
                    .document(fromUserId)
                
                transaction.set(userFriendRef, mapOf(
                    "userId" to fromUserId,
                    "status" to FriendshipStatus.ACCEPTED.name,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
                
                // Add friendship for requester
                val friendFriendRef = firestore.collection(USERS_COLLECTION)
                    .document(fromUserId)
                    .collection(FRIENDS_COLLECTION)
                    .document(userId)
                
                transaction.set(friendFriendRef, mapOf(
                    "userId" to userId,
                    "status" to FriendshipStatus.ACCEPTED.name,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error accepting friend request")
            Result.failure(e)
        }
    }
    
    /**
     * Decline a friend request
     */
    override suspend fun declineFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", FriendshipStatus.DECLINED.name)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error declining friend request")
            Result.failure(e)
        }
    }
    
    /**
     * Remove a friend
     */
    override suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            firestore.runTransaction { transaction ->
                // Remove from current user's friends
                val userFriendRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(FRIENDS_COLLECTION)
                    .document(friendId)
                transaction.delete(userFriendRef)
                
                // Remove from friend's friends
                val friendFriendRef = firestore.collection(USERS_COLLECTION)
                    .document(friendId)
                    .collection(FRIENDS_COLLECTION)
                    .document(userId)
                transaction.delete(friendFriendRef)
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error removing friend")
            Result.failure(e)
        }
    }
    
    /**
     * Block a friend
     */
    override suspend fun blockFriend(friendId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            // Update friendship status to blocked
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FRIENDS_COLLECTION)
                .document(friendId)
                .update("status", FriendshipStatus.BLOCKED.name)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error blocking friend")
            Result.failure(e)
        }
    }
    
    /**
     * Update location sharing status
     */
    override suspend fun updateLocationSharing(enabled: Boolean): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    "status.isLocationSharingEnabled", enabled,
                    "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating location sharing")
            Result.failure(e)
        }
    }
    
    /**
     * Update privacy settings
     */
    override suspend fun updatePrivacySettings(preferences: FriendPreferences): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    "preferences", preferences,
                    "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating privacy settings")
            Result.failure(e)
        }
    }
    
    /**
     * Update online status
     */
    override suspend fun updateOnlineStatus(isOnline: Boolean): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            val updates = mutableMapOf<String, Any>(
                "status.isOnline" to isOnline,
                "status.lastSeen" to System.currentTimeMillis(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating online status")
            Result.failure(e)
        }
    }
    
    /**
     * Send ping to friend (stub implementation for nearby panel)
     * Requirements: 5.5, 5.6, 8.4
     */
    override suspend fun sendPing(friendId: String): Result<Unit> {
        return try {
            // Stub implementation - will be replaced with actual Firebase call
            if (BuildConfig.DEBUG) {
                Timber.d("📍 NearbyPanel: Ping sent to friend: $friendId")
            }
            kotlinx.coroutines.delay(500) // Simulate network call
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending ping to friend: $friendId")
            Result.failure(e)
        }
    }
    
    /**
     * Stop receiving location from friend (stub implementation for nearby panel)
     * Requirements: 5.5, 5.6, 8.4
     */
    override suspend fun stopReceivingLocation(friendId: String): Result<Unit> {
        return try {
            // Stub implementation - will be replaced with actual Firebase call
            if (BuildConfig.DEBUG) {
                Timber.d("📍 NearbyPanel: Stopped receiving location from friend: $friendId")
            }
            kotlinx.coroutines.delay(500) // Simulate network call
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error stopping location sharing with friend: $friendId")
            Result.failure(e)
        }
    }

    /**
     * Helper function to get friend profile data
     */
    private suspend fun getFriendProfile(friendUserId: String): Friend? {
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(friendUserId)
                .get()
                .await()
            
            Friend.fromDocument(doc)
        } catch (e: Exception) {
            Timber.e(e, "Error getting friend profile: $friendUserId")
            null
        }
    }
}