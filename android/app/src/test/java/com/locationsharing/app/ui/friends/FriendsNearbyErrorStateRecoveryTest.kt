package com.locationsharing.app.ui.friends

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for error states and recovery scenarios in the Friends Nearby Panel.
 * 
 * Tests requirements:
 * - 8.3: Graceful handling when user location is unavailable
 * - 8.4: Error logging with proper context for troubleshooting
 * - 8.6: Error recovery scenarios and user feedback
 * - 4.7: Location unavailable error handling
 * - 6.6: Performance under error conditions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendsNearbyErrorStateRecoveryTest {

    private lateinit var friendsRepository: FriendsRepository
    private lateinit var locationService: EnhancedLocationService
    private lateinit var getNearbyFriendsUseCase: GetNearbyFriendsUseCase
    private lateinit var viewModel: FriendsNearbyViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val sampleFriends = listOf(
        NearbyFriend(
            id = "1",
            displayName = "Alice Johnson",
            avatarUrl = null,
            distance = 150.0,
            isOnline = true,
            lastUpdated = System.currentTimeMillis(),
            latLng = LatLng(37.7749, -122.4194)
        ),
        NearbyFriend(
            id = "2",
            displayName = "Bob Smith",
            avatarUrl = null,
            distance = 1200.0,
            isOnline = false,
            lastUpdated = System.currentTimeMillis() - 300000,
            latLng = LatLng(37.7849, -122.4094)
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        friendsRepository = mockk()
        locationService = mockk()
        
        val performanceMonitor = mockk<com.locationsharing.app.ui.friends.components.NearbyPanelPerformanceMonitor>(relaxed = true)
        
        getNearbyFriendsUseCase = GetNearbyFriendsUseCase(
            friendsRepository = friendsRepository,
            locationService = locationService,
            performanceMonitor = performanceMonitor,
            dispatcher = testDispatcher
        )
        
        viewModel = FriendsNearbyViewModel(
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            friendsRepository = friendsRepository
        )
    }

    @Test
    fun locationPermissionDenied_showsErrorState() = runTest {
        // Given - Location permission denied
        every { friendsRepository.getFriends() } returns flowOf(sampleFriends.map { it.toFriend() })
        every { locationService.getLocationUpdates() } returns flow { 
            throw SecurityException("Location permission denied") 
        }

        // When - ViewModel initializes
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Error state is shown
        val uiState = viewModel.uiState.value
        assertTrue("Should show error state", uiState.error != null)
        assertTrue("Should show permission error", 
            uiState.error?.contains("permission") == true)
        assertFalse("Should not be loading", uiState.isLoading)
        assertEquals("Friends list should be empty", 0, uiState.friends.size)
    }

    @Test
    fun locationServiceUnavailable_showsErrorState() = runTest {
        // Given - Location service unavailable
        every { friendsRepository.getFriends() } returns flowOf(sampleFriends.map { it.toFriend() })
        every { locationService.getLocationUpdates() } returns flow { 
            throw Exception("Location service unavailable") 
        }

        // When - ViewModel initializes
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Error state is shown
        val uiState = viewModel.uiState.value
        assertTrue("Should show error state", uiState.error != null)
        assertTrue("Should show location service error", 
            uiState.error?.contains("Location") == true)
        assertFalse("Should not be loading", uiState.isLoading)
    }

    @Test
    fun networkError_showsErrorStateWithRetry() = runTest {
        // Given - Network error when fetching friends
        every { friendsRepository.getFriends() } returns flow { 
            throw Exception("Network connection failed") 
        }
        every { locationService.getLocationUpdates() } returns flowOf(createLocationUpdate())

        // When - ViewModel initializes
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Error state is shown
        val uiState = viewModel.uiState.value
        assertTrue("Should show error state", uiState.error != null)
        assertTrue("Should show network error", 
            uiState.error?.contains("Network") == true)
        assertFalse("Should not be loading", uiState.isLoading)
    }

    @Test
    fun errorRecovery_networkErrorToSuccess() = runTest {
        // Given - Initial network error
        every { friendsRepository.getFriends() } returns flow { 
            throw Exception("Network connection failed") 
        }
        every { locationService.getLocationUpdates() } returns flowOf(createLocationUpdate())

        // When - ViewModel initializes with error
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Error state is shown
        var uiState = viewModel.uiState.value
        assertTrue("Should show error state", uiState.error != null)

        // Given - Network recovers
        every { friendsRepository.getFriends() } returns flowOf(sampleFriends.map { it.toFriend() })

        // When - User triggers refresh
        viewModel.onEvent(NearbyPanelEvent.RefreshFriends)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Error is cleared and friends are loaded
        uiState = viewModel.uiState.value
        assertNull("Error should be cleared", uiState.error)
        assertFalse("Should not be loading", uiState.isLoading)
        assertEquals("Should load friends", 2, uiState.friends.size)
    }

    @Test
    fun locationPermissionRecovery_permissionGrantedAfterDenial() = runTest {
        // Given - Initial permission denied
        every { friendsRepository.getFriends() } returns flowOf(sampleFriends.map { it.toFriend() })
        every { locationService.getLocationUpdates() } returns flow { 
            throw SecurityException("Location permission denied") 
        }

        // When - ViewModel initializes with permission error
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Error state is shown
        var uiState = viewModel.uiState.value
        assertTrue("Should show permission error", uiState.error != null)

        // Given - Permission is granted
        every { locationService.getLocationUpdates() } returns flowOf(createLocationUpdate())

        // When - User triggers refresh after granting permission
        viewModel.onEvent(NearbyPanelEvent.RefreshFriends)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Error is cleared and friends with distances are loaded
        uiState = viewModel.uiState.value
        assertNull("Error should be cleared", uiState.error)
        assertEquals("Should load friends", 2, uiState.friends.size)
    }

    @Test
    fun partialDataError_somefriendsLoadSuccessfully() = runTest {
        // Given - Some friends load, others fail
        val partialFriends = listOf(sampleFriends[0].toFriend()) // Only Alice loads
        every { friendsRepository.getFriends() } returns flowOf(partialFriends)
        every { locationService.getLocationUpdates() } returns flowOf(createLocationUpdate())

        // When - ViewModel initializes
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Partial data is shown without error
        val uiState = viewModel.uiState.value
        assertNull("Should not show error for partial data", uiState.error)
        assertEquals("Should show available friends", 1, uiState.friends.size)
        assertEquals("Should show Alice", "Alice Johnson", uiState.friends[0].displayName)
    }

    @Test
    fun intermittentLocationUpdates_handledGracefully() = runTest {
        // Given - Intermittent location updates
        every { friendsRepository.getFriends() } returns flowOf(sampleFriends.map { it.toFriend() })
        every { locationService.getLocationUpdates() } returns flow {
            emit(createLocationUpdate()) // First update succeeds
            throw Exception("GPS signal lost") // Then fails
        }

        // When - ViewModel processes updates
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should handle gracefully without crashing
        val uiState = viewModel.uiState.value
        // Should either show friends with last known location or show error
        assertTrue("Should handle intermittent updates", 
            uiState.friends.isNotEmpty() || uiState.error != null)
    }

    @Test
    fun rapidErrorRecoveryAttempts_handledCorrectly() = runTest {
        // Given - Initial error state
        every { friendsRepository.getFriends() } returns flow { 
            throw Exception("Network error") 
        }
        every { locationService.getLocationUpdates() } returns flowOf(createLocationUpdate())

        // When - Multiple rapid refresh attempts
        repeat(5) {
            viewModel.onEvent(NearbyPanelEvent.RefreshFriends)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should handle multiple attempts without crashing
        val uiState = viewModel.uiState.value
        assertTrue("Should show error state", uiState.error != null)
        assertFalse("Should not be stuck in loading", uiState.isLoading)
    }

    @Test
    fun errorDuringFriendInteraction_showsAppropriateMessage() = runTest {
        // Given - Friends loaded successfully
        every { friendsRepository.getFriends() } returns flowOf(sampleFriends.map { it.toFriend() })
        every { locationService.getLocationUpdates() } returns flowOf(createLocationUpdate())
        coEvery { friendsRepository.sendPing(any()) } returns Result.failure(Exception("Server error"))

        // When - User tries to ping friend but it fails
        viewModel.onEvent(NearbyPanelEvent.Ping("1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Appropriate error message is shown
        val uiState = viewModel.uiState.value
        assertTrue("Should show ping error message", 
            uiState.snackbarMessage?.contains("Failed to send ping") == true)
        assertNull("Main error should not be set", uiState.error)
    }

    @Test
    fun errorStateAccessibility_providesProperFeedback() = runTest {
        // Given - Error state
        every { friendsRepository.getFriends() } returns flow { 
            throw Exception("Location permission denied. Please enable location access in settings.") 
        }
        every { locationService.getLocationUpdates() } returns flowOf(createLocationUpdate())

        // When - Error occurs
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Error message is accessible and actionable
        val uiState = viewModel.uiState.value
        assertTrue("Should have descriptive error message", 
            uiState.error?.contains("Please enable location access") == true)
        assertFalse("Should not be loading", uiState.isLoading)
    }

    @Test
    fun errorLogging_providesProperContext() = runTest {
        // Given - Error with context
        val contextualError = Exception("Failed to calculate distance: GPS accuracy too low (accuracy: 500m)")
        every { friendsRepository.getFriends() } returns flowOf(sampleFriends.map { it.toFriend() })
        every { locationService.getLocationUpdates() } returns flow { 
            throw contextualError 
        }

        // When - Error occurs
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Error contains helpful context
        val uiState = viewModel.uiState.value
        assertTrue("Should contain contextual error information", 
            uiState.error?.contains("GPS accuracy") == true)
    }

    @Test
    fun errorRecovery_maintainsUserState() = runTest {
        // Given - User has search query and panel open
        viewModel.onEvent(NearbyPanelEvent.TogglePanel)
        viewModel.onEvent(NearbyPanelEvent.SearchQuery("Alice"))
        
        // Given - Error occurs
        every { friendsRepository.getFriends() } returns flow { 
            throw Exception("Network error") 
        }
        every { locationService.getLocationUpdates() } returns flowOf(createLocationUpdate())

        // When - Error occurs
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - User state is maintained
        var uiState = viewModel.uiState.value
        assertTrue("Panel should remain open", uiState.isPanelOpen)
        assertEquals("Search query should be maintained", "Alice", uiState.searchQuery)

        // When - Error recovers
        every { friendsRepository.getFriends() } returns flowOf(sampleFriends.map { it.toFriend() })
        viewModel.onEvent(NearbyPanelEvent.RefreshFriends)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - User state is still maintained
        uiState = viewModel.uiState.value
        assertTrue("Panel should remain open", uiState.isPanelOpen)
        assertEquals("Search query should be maintained", "Alice", uiState.searchQuery)
        assertEquals("Should show filtered results", 1, uiState.filteredFriends.size)
    }

    @Test
    fun multipleSimultaneousErrors_handledGracefully() = runTest {
        // Given - Multiple error sources
        every { friendsRepository.getFriends() } returns flow { 
            throw Exception("Database connection failed") 
        }
        every { locationService.getLocationUpdates() } returns flow { 
            throw SecurityException("Location permission denied") 
        }
        coEvery { friendsRepository.sendPing(any()) } returns Result.failure(Exception("Network timeout"))

        // When - Multiple errors occur
        viewModel.onEvent(NearbyPanelEvent.Ping("1")) // This will fail
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Most relevant error is shown
        val uiState = viewModel.uiState.value
        assertTrue("Should show an error", 
            uiState.error != null || uiState.snackbarMessage != null)
        assertFalse("Should not be loading", uiState.isLoading)
    }

    // Helper functions
    
    private fun createLocationUpdate(): com.locationsharing.app.data.friends.FriendLocationUpdate {
        return com.locationsharing.app.data.friends.FriendLocationUpdate(
            friendId = "current_user",
            previousLocation = null,
            newLocation = LatLng(37.7749, -122.4194),
            timestamp = System.currentTimeMillis(),
            isOnline = true,
            updateType = com.locationsharing.app.data.friends.LocationUpdateType.POSITION_CHANGE
        )
    }
    
    private fun NearbyFriend.toFriend(): com.locationsharing.app.data.friends.Friend {
        return com.locationsharing.app.data.friends.Friend(
            id = this.id,
            userId = this.id,
            name = this.displayName,
            email = "${this.displayName.replace(" ", "").lowercase()}@test.com",
            avatarUrl = this.avatarUrl ?: "",
            location = com.locationsharing.app.data.friends.FriendLocation(
                latitude = this.latLng.latitude,
                longitude = this.latLng.longitude,
                accuracy = 10f,
                timestamp = java.util.Date(this.lastUpdated)
            ),
            status = com.locationsharing.app.data.friends.FriendStatus(
                isOnline = this.isOnline,
                lastSeen = this.lastUpdated,
                isLocationSharingEnabled = true
            )
        )
    }
}