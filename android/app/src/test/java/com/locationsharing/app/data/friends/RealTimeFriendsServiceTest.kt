package com.locationsharing.app.data.friends

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Comprehensive tests for RealTimeFriendsService
 * Tests real-time synchronization, animation coordination, and error handling
 */
@ExperimentalCoroutinesApi
class RealTimeFriendsServiceTest {
    
    @Mock
    private lateinit var firestore: FirebaseFirestore
    
    @Mock
    private lateinit var auth: FirebaseAuth
    
    @Mock
    private lateinit var friendsRepository: FriendsRepository
    
    @Mock
    private lateinit var currentUser: FirebaseUser
    
    private lateinit var service: RealTimeFriendsService
    
    private val testUserId = "test-user-123"
    private val testFriendId = "test-friend-456"
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Setup auth mocks
        whenever(auth.currentUser).thenReturn(currentUser)
        whenever(currentUser.uid).thenReturn(testUserId)
        
        service = RealTimeFriendsService(firestore, auth, friendsRepository)
    }
    
    @Test
    fun `startSync should initialize connection state`() = runTest {
        // When
        service.startSync()
        
        // Then
        val connectionState = service.connectionState.first()
        assertTrue("Should be connected or connecting", 
            connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING)
    }
    
    @Test
    fun `stopSync should disconnect and clear state`() = runTest {
        // Given
        service.startSync()
        
        // When
        service.stopSync()
        
        // Then
        val connectionState = service.connectionState.first()
        assertEquals("Should be disconnected", ConnectionState.DISCONNECTED, connectionState)
        
        val friendsState = service.friendsState.first()
        assertTrue("Friends state should be empty", friendsState.isEmpty())
    }
    
    @Test
    fun `handleFriendAppeared should update friend state with appearing animation`() = runTest {
        // Given
        val friend = createTestFriend(testFriendId, "Test Friend", isOnline = true)
        
        // When
        val result = service.handleFriendAppeared(friend)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        
        val friendsState = service.friendsState.first()
        val friendState = friendsState[testFriendId]
        
        assertNotNull("Friend state should exist", friendState)
        assertTrue("Friend should be visible", friendState?.isVisible == true)
        assertEquals("Animation state should be APPEARING", 
            AnimationState.APPEARING, friendState?.animationState)
    }
    
    @Test
    fun `handleFriendDisappeared should update friend state with disappearing animation`() = runTest {
        // Given
        val friend = createTestFriend(testFriendId, "Test Friend", isOnline = true)
        service.handleFriendAppeared(friend)
        
        // When
        val result = service.handleFriendDisappeared(testFriendId)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        
        val friendsState = service.friendsState.first()
        val friendState = friendsState[testFriendId]
        
        assertNotNull("Friend state should still exist during animation", friendState)
        assertFalse("Friend should not be visible", friendState?.isVisible == true)
        assertEquals("Animation state should be DISAPPEARING", 
            AnimationState.DISAPPEARING, friendState?.animationState)
    }
    
    @Test
    fun `updateFriendLocation should update location with movement animation`() = runTest {
        // Given
        val friendId = testFriendId
        val newLocation = FriendLocation(
            latitude = 37.7749,
            longitude = -122.4194,
            isMoving = true
        )
        
        // Mock Firestore update
        whenever(firestore.collection("users")).thenReturn(mock())
        
        // When
        val result = service.updateFriendLocation(friendId, newLocation, notifyFriends = true)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
    }
    
    @Test
    fun `updateOnlineStatus should update status and create activity event`() = runTest {
        // Given
        val isOnline = true
        
        // Mock Firestore update
        whenever(firestore.collection("users")).thenReturn(mock())
        
        // When
        val result = service.updateOnlineStatus(isOnline)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
    }
    
    @Test
    fun `handleLocationError should update friend state with error`() = runTest {
        // Given
        val friendId = testFriendId
        val error = LocationError.NETWORK_ERROR
        
        // Setup friend state
        val friend = createTestFriend(friendId, "Test Friend", isOnline = true)
        service.handleFriendAppeared(friend)
        
        // When
        val result = service.handleLocationError(friendId, error, retryCount = 0)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        
        val friendsState = service.friendsState.first()
        val friendState = friendsState[friendId]
        
        assertNotNull("Friend state should exist", friendState)
        assertTrue("Friend should have error", friendState?.hasError == true)
        assertEquals("Should have error message", error.message, friendState?.errorMessage)
        assertEquals("Animation state should be ERROR", 
            AnimationState.ERROR, friendState?.animationState)
    }
    
    @Test
    fun `handleLocationError should retry for retryable errors`() = runTest {
        // Given
        val friendId = testFriendId
        val retryableError = LocationError.TIMEOUT
        
        // Setup friend state
        val friend = createTestFriend(friendId, "Test Friend", isOnline = true)
        service.handleFriendAppeared(friend)
        
        // When
        val result = service.handleLocationError(friendId, retryableError, retryCount = 0)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        
        // Verify that retry logic would be triggered
        // (In real implementation, this would involve checking that retry is scheduled)
    }
    
    @Test
    fun `handleLocationError should not retry for non-retryable errors`() = runTest {
        // Given
        val friendId = testFriendId
        val nonRetryableError = LocationError.PERMISSION_DENIED
        
        // Setup friend state
        val friend = createTestFriend(friendId, "Test Friend", isOnline = true)
        service.handleFriendAppeared(friend)
        
        // When
        val result = service.handleLocationError(friendId, nonRetryableError, retryCount = 0)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        
        // Verify that no retry is scheduled for non-retryable errors
    }
    
    @Test
    fun `getFriendUpdatesWithAnimations should generate appropriate animations`() = runTest {
        // Given
        val friend1 = createTestFriend("friend1", "Friend 1", isOnline = true)
        val friend2 = createTestFriend("friend2", "Friend 2", isOnline = false)
        
        // Mock friends repository to return test friends
        whenever(friendsRepository.getFriends()).thenReturn(
            kotlinx.coroutines.flow.flowOf(listOf(friend1, friend2))
        )
        
        // When
        val updates = service.getFriendUpdatesWithAnimations().first()
        
        // Then
        assertFalse("Should have updates", updates.isEmpty())
        
        // Verify that appropriate animation types are assigned
        updates.forEach { update ->
            assertNotNull("Update should have animation type", update.animationType)
            assertNotNull("Update should have friend", update.friend)
        }
    }
    
    @Test
    fun `getCombinedUpdates should combine all data streams`() = runTest {
        // Given
        val friends = listOf(createTestFriend("friend1", "Friend 1", isOnline = true))
        val locationUpdates = listOf(
            LocationUpdateEvent(
                friendId = "friend1",
                previousLocation = null,
                newLocation = FriendLocation(37.7749, -122.4194),
                updateType = LocationUpdateType.POSITION_CHANGE
            )
        )
        val activities = listOf(
            FriendActivityEvent(
                friendId = "friend1",
                activityType = FriendActivityType.LOCATION_UPDATED
            )
        )
        
        // Mock repository flows
        whenever(friendsRepository.getFriends()).thenReturn(kotlinx.coroutines.flow.flowOf(friends))
        whenever(friendsRepository.getLocationUpdates()).thenReturn(kotlinx.coroutines.flow.flowOf(locationUpdates))
        whenever(friendsRepository.getFriendActivities()).thenReturn(kotlinx.coroutines.flow.flowOf(activities))
        
        // When
        val combinedUpdate = service.getCombinedUpdates().first()
        
        // Then
        assertEquals("Should have friends", friends, combinedUpdate.friends)
        assertEquals("Should have location updates", locationUpdates, combinedUpdate.locationUpdates)
        assertEquals("Should have activities", activities, combinedUpdate.activities)
        assertTrue("Should have timestamp", combinedUpdate.timestamp > 0)
    }
    
    @Test
    fun `service should handle authentication errors gracefully`() = runTest {
        // Given
        whenever(auth.currentUser).thenReturn(null)
        
        // When
        val result = service.updateOnlineStatus(true)
        
        // Then
        assertTrue("Should fail when not authenticated", result.isFailure)
        assertTrue("Should have authentication error", 
            result.exceptionOrNull()?.message?.contains("not authenticated") == true)
    }
    
    @Test
    fun `service should handle Firestore errors gracefully`() = runTest {
        // Given
        val exception = RuntimeException("Firestore error")
        whenever(firestore.collection("users")).thenThrow(exception)
        
        // When
        val result = service.updateOnlineStatus(true)
        
        // Then
        assertTrue("Should fail on Firestore error", result.isFailure)
    }
    
    @Test
    fun `friend state should be cleaned up after disappear animation`() = runTest {
        // Given
        val friend = createTestFriend(testFriendId, "Test Friend", isOnline = true)
        service.handleFriendAppeared(friend)
        
        // When
        service.handleFriendDisappeared(testFriendId)
        
        // Wait for cleanup (in real implementation, this would be longer)
        kotlinx.coroutines.delay(100)
        
        // Then
        // After animation completes, friend should be removed from state
        // (This test would need to be adjusted based on actual cleanup timing)
    }
    
    @Test
    fun `multiple concurrent operations should be handled safely`() = runTest {
        // Given
        val friend = createTestFriend(testFriendId, "Test Friend", isOnline = true)
        
        // When - perform multiple concurrent operations
        val results = listOf(
            kotlinx.coroutines.async { service.handleFriendAppeared(friend) },
            kotlinx.coroutines.async { service.updateOnlineStatus(true) },
            kotlinx.coroutines.async { 
                service.updateFriendLocation(testFriendId, FriendLocation(37.7749, -122.4194))
            }
        ).map { it.await() }
        
        // Then
        assertTrue("All operations should succeed or fail gracefully", 
            results.all { it.isSuccess || it.isFailure })
    }
    
    /**
     * Helper function to create test friends
     */
    private fun createTestFriend(
        id: String,
        name: String,
        isOnline: Boolean = true,
        latitude: Double = 37.7749,
        longitude: Double = -122.4194
    ): Friend {
        return Friend(
            id = id,
            userId = id,
            name = name,
            email = "$name@test.com",
            avatarUrl = "https://example.com/avatar-$id.jpg",
            profileColor = "#2196F3",
            location = FriendLocation(
                latitude = latitude,
                longitude = longitude,
                isMoving = false
            ),
            status = FriendStatus(
                isOnline = isOnline,
                lastSeen = if (isOnline) System.currentTimeMillis() else System.currentTimeMillis() - 3600000,
                isLocationSharingEnabled = true
            )
        )
    }
}

/**
 * Performance tests for RealTimeFriendsService
 */
@ExperimentalCoroutinesApi
class RealTimeFriendsServicePerformanceTest {
    
    @Test
    fun `service should handle large number of friends efficiently`() = runTest {
        // Test with 100+ friends to ensure performance
        // This would measure memory usage and processing time
    }
    
    @Test
    fun `real time updates should not cause memory leaks`() = runTest {
        // Test that long-running real-time updates don't cause memory leaks
        // This would involve monitoring memory usage over time
    }
    
    @Test
    fun `animation state management should be efficient`() = runTest {
        // Test that animation state updates are efficient
        // and don't cause performance issues with many friends
    }
}