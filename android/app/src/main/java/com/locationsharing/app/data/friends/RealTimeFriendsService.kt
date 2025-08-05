package com.locationsharing.app.data.friends

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time friends service that manages live updates and synchronization
 * Handles friend presence, location updates, and activity events
 */
@Singleton
class RealTimeFriendsService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val friendsRepository: FriendsRepository
) {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _friendsState = MutableStateFlow<Map<String, FriendRealTimeState>>(emptyMap())
    val friendsState: StateFlow<Map<String, FriendRealTimeState>> = _friendsState.asStateFlow()
    
    private var presenceListener: ListenerRegistration? = null
    private var locationListener: ListenerRegistration? = null
    private var activityListener: ListenerRegistration? = null
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    /**
     * Start real-time synchronization
     */
    fun startSync() {
        serviceScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                
                setupPresenceListener()
                setupLocationListener()
                setupActivityListener()
                
                _connectionState.value = ConnectionState.CONNECTED
                Timber.d("Real-time friends sync started")
            } catch (e: Exception) {
                Timber.e(e, "Error starting real-time sync")
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }
    
    /**
     * Stop real-time synchronization
     */
    fun stopSync() {
        presenceListener?.remove()
        locationListener?.remove()
        activityListener?.remove()
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _friendsState.value = emptyMap()
        
        Timber.d("Real-time friends sync stopped")
    }
    
    /**
     * Get real-time friend updates with animation metadata
     */
    fun getFriendUpdatesWithAnimations(): Flow<List<FriendUpdateWithAnimation>> = callbackFlow {
        var previousFriends: Map<String, Friend> = emptyMap()
        
        friendsRepository.getFriends().collect { currentFriends ->
            val friendsMap = currentFriends.associateBy { it.id }
            val updates = generateAnimatedUpdates(previousFriends, friendsMap)
            
            trySend(updates)
            previousFriends = friendsMap
        }
        
        awaitClose { }
    }
    
    /**
     * Get combined real-time data stream
     */
    fun getCombinedUpdates(): Flow<RealTimeUpdate> = combine(
        friendsRepository.getFriends(),
        friendsRepository.getLocationUpdates(),
        friendsRepository.getFriendActivities()
    ) { friends, locationUpdates, activities ->
        RealTimeUpdate(
            friends = friends,
            locationUpdates = locationUpdates,
            activities = activities,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Update friend's location in real-time
     */
    suspend fun updateFriendLocation(
        friendId: String,
        location: FriendLocation,
        notifyFriends: Boolean = true
    ): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            // Update user's location
            firestore.collection("users")
                .document(userId)
                .update(
                    "location", location,
                    "status.lastSeen", System.currentTimeMillis(),
                    "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                .await()
            
            // Create location update event if needed
            if (notifyFriends) {
                createLocationUpdateEvent(userId, location)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating friend location")
            Result.failure(e)
        }
    }
    
    /**
     * Update friend's online status
     */
    suspend fun updateOnlineStatus(isOnline: Boolean): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))
            
            val updates = mapOf(
                "status.isOnline" to isOnline,
                "status.lastSeen" to System.currentTimeMillis(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()
            
            // Create activity event
            createActivityEvent(
                userId,
                if (isOnline) FriendActivityType.CAME_ONLINE else FriendActivityType.WENT_OFFLINE
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating online status")
            Result.failure(e)
        }
    }
    
    /**
     * Handle friend appearing (coming online or enabling location sharing)
     */
    suspend fun handleFriendAppeared(friend: Friend): Result<Unit> {
        return try {
            val currentState = _friendsState.value.toMutableMap()
            currentState[friend.id] = FriendRealTimeState(
                friend = friend,
                isVisible = true,
                lastUpdate = System.currentTimeMillis(),
                animationState = AnimationState.APPEARING
            )
            _friendsState.value = currentState
            
            // Create activity event
            createActivityEvent(friend.id, FriendActivityType.CAME_ONLINE)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error handling friend appeared")
            Result.failure(e)
        }
    }
    
    /**
     * Handle friend disappearing (going offline or disabling location sharing)
     */
    suspend fun handleFriendDisappeared(friendId: String): Result<Unit> {
        return try {
            val currentState = _friendsState.value.toMutableMap()
            currentState[friendId]?.let { state ->
                currentState[friendId] = state.copy(
                    isVisible = false,
                    animationState = AnimationState.DISAPPEARING
                )
            }
            _friendsState.value = currentState
            
            // Create activity event
            createActivityEvent(friendId, FriendActivityType.WENT_OFFLINE)
            
            // Remove from state after animation completes
            serviceScope.launch {
                kotlinx.coroutines.delay(500) // Wait for disappear animation
                val updatedState = _friendsState.value.toMutableMap()
                updatedState.remove(friendId)
                _friendsState.value = updatedState
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error handling friend disappeared")
            Result.failure(e)
        }
    }
    
    /**
     * Handle location errors and edge cases
     */
    suspend fun handleLocationError(
        friendId: String,
        error: LocationError,
        retryCount: Int = 0
    ): Result<Unit> {
        return try {
            Timber.w("Location error for friend $friendId: $error (retry: $retryCount)")
            
            val currentState = _friendsState.value.toMutableMap()
            currentState[friendId]?.let { state ->
                currentState[friendId] = state.copy(
                    hasError = true,
                    errorMessage = error.message,
                    animationState = AnimationState.ERROR
                )
            }
            _friendsState.value = currentState
            
            // Implement retry logic for transient errors
            if (error.isRetryable && retryCount < 3) {
                serviceScope.launch {
                    delay(1000L * (retryCount + 1)) // Exponential backoff
                    // Retry location update
                    retryLocationUpdate(friendId, retryCount + 1)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error handling location error")
            Result.failure(e)
        }
    }
    
    /**
     * Setup presence listener for real-time friend status
     */
    private fun setupPresenceListener() {
        val userId = currentUserId ?: return
        
        presenceListener = firestore.collection("users")
            .document(userId)
            .collection("friends")
            .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error in presence listener")
                    return@addSnapshotListener
                }
                
                serviceScope.launch {
                    snapshot?.documentChanges?.forEach { change ->
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                val friendId = change.document.getString("userId") ?: return@forEach
                                handleFriendPresenceChange(friendId, true)
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                val friendId = change.document.getString("userId") ?: return@forEach
                                handleFriendPresenceChange(friendId, false)
                            }
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                val friendId = change.document.getString("userId") ?: return@forEach
                                handleFriendPresenceChange(friendId, true)
                            }
                        }
                    }
                }
            }
    }
    
    /**
     * Setup location listener for real-time location updates
     */
    private fun setupLocationListener() {
        val userId = currentUserId ?: return
        
        locationListener = firestore.collection("locationUpdates")
            .whereArrayContains("visibleTo", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error in location listener")
                    return@addSnapshotListener
                }
                
                serviceScope.launch {
                    snapshot?.documentChanges?.forEach { change ->
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val data = change.document.data
                            val friendId = data["friendId"] as? String ?: return@forEach
                            val location = (data["newLocation"] as? Map<String, Any>)?.let {
                                FriendLocation.fromMap(it)
                            } ?: return@forEach
                            
                            handleLocationUpdate(friendId, location)
                        }
                    }
                }
            }
    }
    
    /**
     * Setup activity listener for real-time friend activities
     */
    private fun setupActivityListener() {
        val userId = currentUserId ?: return
        
        activityListener = firestore.collection("activities")
            .whereArrayContains("visibleTo", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error in activity listener")
                    return@addSnapshotListener
                }
                
                serviceScope.launch {
                    snapshot?.documentChanges?.forEach { change ->
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val data = change.document.data
                            val friendId = data["friendId"] as? String ?: return@forEach
                            val activityType = FriendActivityType.valueOf(
                                data["activityType"] as? String ?: "LOCATION_UPDATED"
                            )
                            
                            handleActivityUpdate(friendId, activityType, data)
                        }
                    }
                }
            }
    }
    
    /**
     * Handle friend presence changes
     */
    private suspend fun handleFriendPresenceChange(friendId: String, isPresent: Boolean) {
        if (isPresent) {
            // Friend came online or was added
            friendsRepository.getFriendById(friendId).collect { friend ->
                if (friend != null) {
                    handleFriendAppeared(friend)
                }
            }
        } else {
            // Friend went offline or was removed
            handleFriendDisappeared(friendId)
        }
    }
    
    /**
     * Handle location updates
     */
    private fun handleLocationUpdate(friendId: String, location: FriendLocation) {
        val currentState = _friendsState.value.toMutableMap()
        currentState[friendId]?.let { state ->
            val previousLocation = state.friend.location
            val updatedFriend = state.friend.copy(location = location)
            
            currentState[friendId] = state.copy(
                friend = updatedFriend,
                lastUpdate = System.currentTimeMillis(),
                animationState = if (previousLocation != null && 
                    previousLocation.distanceTo(location) > 10f) {
                    AnimationState.MOVING
                } else {
                    AnimationState.IDLE
                }
            )
        }
        _friendsState.value = currentState
    }
    
    /**
     * Handle activity updates
     */
    private fun handleActivityUpdate(
        friendId: String,
        activityType: FriendActivityType,
        data: Map<String, Any>
    ) {
        val currentState = _friendsState.value.toMutableMap()
        currentState[friendId]?.let { state ->
            val animationState = when (activityType) {
                FriendActivityType.CAME_ONLINE -> AnimationState.APPEARING
                FriendActivityType.WENT_OFFLINE -> AnimationState.DISAPPEARING
                FriendActivityType.STARTED_MOVING -> AnimationState.MOVING
                FriendActivityType.STOPPED_MOVING -> AnimationState.IDLE
                else -> state.animationState
            }
            
            currentState[friendId] = state.copy(
                lastActivity = activityType,
                lastUpdate = System.currentTimeMillis(),
                animationState = animationState
            )
        }
        _friendsState.value = currentState
    }
    
    /**
     * Create location update event
     */
    private suspend fun createLocationUpdateEvent(userId: String, location: FriendLocation) {
        try {
            // Get user's friends to determine visibility
            val friends = friendsRepository.getFriends()
            // Implementation would create the location update event
        } catch (e: Exception) {
            Timber.e(e, "Error creating location update event")
        }
    }
    
    /**
     * Create activity event
     */
    private suspend fun createActivityEvent(userId: String, activityType: FriendActivityType) {
        try {
            // Implementation would create the activity event
        } catch (e: Exception) {
            Timber.e(e, "Error creating activity event")
        }
    }
    
    /**
     * Retry location update
     */
    private suspend fun retryLocationUpdate(friendId: String, retryCount: Int) {
        // Implementation would retry the location update
    }
    
    /**
     * Generate animated updates from friend changes
     */
    private fun generateAnimatedUpdates(
        previous: Map<String, Friend>,
        current: Map<String, Friend>
    ): List<FriendUpdateWithAnimation> {
        val updates = mutableListOf<FriendUpdateWithAnimation>()
        
        // Check for new friends
        current.values.forEach { friend ->
            if (!previous.containsKey(friend.id)) {
                updates.add(
                    FriendUpdateWithAnimation(
                        friend = friend,
                        updateType = LocationUpdateType.FRIEND_APPEARED,
                        animationType = AnimationType.FLY_IN,
                        shouldShowTrail = false
                    )
                )
            }
        }
        
        // Check for removed friends
        previous.values.forEach { friend ->
            if (!current.containsKey(friend.id)) {
                updates.add(
                    FriendUpdateWithAnimation(
                        friend = friend,
                        updateType = LocationUpdateType.FRIEND_DISAPPEARED,
                        animationType = AnimationType.FADE_OUT,
                        shouldShowTrail = false
                    )
                )
            }
        }
        
        // Check for location changes
        current.values.forEach { currentFriend ->
            val previousFriend = previous[currentFriend.id]
            if (previousFriend != null && 
                previousFriend.location != currentFriend.location) {
                updates.add(
                    FriendUpdateWithAnimation(
                        friend = currentFriend,
                        updateType = LocationUpdateType.POSITION_CHANGE,
                        animationType = AnimationType.SMOOTH_MOVE,
                        shouldShowTrail = currentFriend.isMoving()
                    )
                )
            }
        }
        
        // Check for status changes
        current.values.forEach { currentFriend ->
            val previousFriend = previous[currentFriend.id]
            if (previousFriend != null && 
                previousFriend.isOnline() != currentFriend.isOnline()) {
                updates.add(
                    FriendUpdateWithAnimation(
                        friend = currentFriend,
                        updateType = LocationUpdateType.STATUS_CHANGE,
                        animationType = AnimationType.PULSE,
                        shouldShowTrail = false
                    )
                )
            }
        }
        
        return updates
    }
}

/**
 * Connection state for real-time service
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    RECONNECTING
}

/**
 * Real-time state for individual friends
 */
data class FriendRealTimeState(
    val friend: Friend,
    val isVisible: Boolean = true,
    val lastUpdate: Long = 0L,
    val lastActivity: FriendActivityType? = null,
    val animationState: AnimationState = AnimationState.IDLE,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Animation states for friends
 */
enum class AnimationState {
    IDLE,
    APPEARING,
    DISAPPEARING,
    MOVING,
    ERROR
}

/**
 * Combined real-time update
 */
data class RealTimeUpdate(
    val friends: List<Friend>,
    val locationUpdates: List<LocationUpdateEvent>,
    val activities: List<FriendActivityEvent>,
    val timestamp: Long
)

/**
 * Friend update with animation metadata
 */
data class FriendUpdateWithAnimation(
    val friend: Friend,
    val updateType: LocationUpdateType,
    val animationType: AnimationType,
    val shouldShowTrail: Boolean = false,
    val duration: Long = 1000L
)

/**
 * Animation types for friend updates
 */
enum class AnimationType {
    FLY_IN,
    FADE_OUT,
    SMOOTH_MOVE,
    PULSE,
    BOUNCE,
    SPRING
}

/**
 * Location error types
 */
data class LocationError(
    val code: String,
    val message: String,
    val isRetryable: Boolean = true
) {
    companion object {
        val PERMISSION_DENIED = LocationError("PERMISSION_DENIED", "Location permission denied", false)
        val LOCATION_DISABLED = LocationError("LOCATION_DISABLED", "Location services disabled", false)
        val NETWORK_ERROR = LocationError("NETWORK_ERROR", "Network connection error", true)
        val TIMEOUT = LocationError("TIMEOUT", "Location request timeout", true)
        val ACCURACY_LOW = LocationError("ACCURACY_LOW", "Location accuracy too low", true)
    }
}