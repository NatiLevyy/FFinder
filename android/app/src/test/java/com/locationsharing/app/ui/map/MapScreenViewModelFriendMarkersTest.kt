package com.locationsharing.app.ui.map

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for MapScreenViewModel friend marker functionality
 * Tests requirements 2.3, 2.4, 2.5, 8.5
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapScreenViewModelFriendMarkersTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var locationService: EnhancedLocationService

    @MockK
    private lateinit var friendsRepository: FriendsRepository

    @MockK
    private lateinit var realTimeFriendsService: RealTimeFriendsService

    @MockK
    private lateinit var getNearbyFriendsUseCase: GetNearbyFriendsUseCase

    private lateinit var context: Context
    private lateinit var viewModel: MapScreenViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        context = RuntimeEnvironment.getApplication()
        
        // Setup default mock behaviors
        every { locationService.stopLocationUpdates() } returns Unit
        every { realTimeFriendsService.startSync() } returns Unit
        every { realTimeFriendsService.stopSync() } returns Unit
        coEvery { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())
        
        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase
        )
    }

    @Test
    fun `onFriendMarkerClick should select friend and center map`() = runTest {
        // Given
        val testFriends = createTestFriends()
        val selectedFriendId = "friend_1"
        val selectedFriend = testFriends.first { it.id == selectedFriendId }
        
        // Setup mock to return test friends
        coEvery { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(
            testFriends.map { 
                mockk {
                    every { friend } returns it
                    every { updateType } returns com.locationsharing.app.data.friends.LocationUpdateType.INITIAL_LOAD
                    every { animationType } returns com.locationsharing.app.data.friends.AnimationType.FLY_IN
                }
            }
        )
        
        advanceUntilIdle()

        // When
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick(selectedFriendId))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(selectedFriendId, state.selectedFriendId)
        assertEquals(selectedFriend.getLatLng(), state.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, state.mapZoom)
    }

    @Test
    fun `onClusterClick should focus camera on cluster bounds`() = runTest {
        // Given
        val clusteredFriends = createClusteredFriends()
        
        // When
        viewModel.onEvent(MapScreenEvent.OnClusterClick(clusteredFriends))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertNotNull(state.mapCenter)
        
        // Verify that the map center is within the cluster bounds
        val locations = clusteredFriends.mapNotNull { it.getLatLng() }
        val minLat = locations.minOf { it.latitude }
        val maxLat = locations.maxOf { it.latitude }
        val minLng = locations.minOf { it.longitude }
        val maxLng = locations.maxOf { it.longitude }
        
        val expectedCenterLat = (minLat + maxLat) / 2
        val expectedCenterLng = (minLng + maxLng) / 2
        
        assertEquals(expectedCenterLat, state.mapCenter.latitude, 0.0001)
        assertEquals(expectedCenterLng, state.mapCenter.longitude, 0.0001)
    }

    @Test
    fun `onFriendSelectionClear should clear selected friend`() = runTest {
        // Given - Set initial selected friend
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("friend_1"))
        advanceUntilIdle()
        
        // Verify friend is selected
        assertEquals("friend_1", viewModel.state.value.selectedFriendId)

        // When
        viewModel.onEvent(MapScreenEvent.OnFriendSelectionClear)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.state.value.selectedFriendId)
    }

    @Test
    fun `onMapClick should clear friend selection`() = runTest {
        // Given - Set initial selected friend
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("friend_1"))
        advanceUntilIdle()
        
        // Verify friend is selected
        assertEquals("friend_1", viewModel.state.value.selectedFriendId)

        // When
        viewModel.onEvent(MapScreenEvent.OnMapClick(LatLng(37.7749, -122.4194)))
        advanceUntilIdle()

        // Then
        assertNull(viewModel.state.value.selectedFriendId)
    }

    @Test
    fun `real-time friends service should start on initialization`() = runTest {
        // Given - ViewModel is initialized in setup()
        advanceUntilIdle()

        // Then
        verify { realTimeFriendsService.startSync() }
    }

    @Test
    fun `real-time friends service should stop on cleanup`() = runTest {
        // Given
        advanceUntilIdle()

        // When
        viewModel.onCleared()

        // Then
        verify { realTimeFriendsService.stopSync() }
    }

    @Test
    fun `friends state should update when real-time service provides updates`() = runTest {
        // Given
        val testFriends = createTestFriends()
        val friendUpdates = testFriends.map { friend ->
            mockk<com.locationsharing.app.data.friends.FriendUpdateWithAnimation> {
                every { this@mockk.friend } returns friend
                every { updateType } returns com.locationsharing.app.data.friends.LocationUpdateType.INITIAL_LOAD
                every { animationType } returns com.locationsharing.app.data.friends.AnimationType.FLY_IN
            }
        }
        
        // Setup mock to return test friends
        coEvery { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(friendUpdates)

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(testFriends.size, state.friends.size)
        assertEquals(testFriends.size, state.nearbyFriendsCount)
        assertEquals(false, state.isFriendsLoading)
        assertNull(state.friendsError)
    }

    @Test
    fun `friends error should be handled correctly`() = runTest {
        // Given
        val errorMessage = "Network connection failed"
        coEvery { realTimeFriendsService.getFriendUpdatesWithAnimations() } throws Exception(errorMessage)

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(false, state.isFriendsLoading)
        assertNotNull(state.friendsError)
        assertEquals("Failed to load friends: $errorMessage", state.friendsError)
    }

    @Test
    fun `cluster click with single friend should behave like friend marker click`() = runTest {
        // Given
        val singleFriend = createTestFriends().take(1)
        val friendId = singleFriend.first().id

        // When
        viewModel.onEvent(MapScreenEvent.OnClusterClick(singleFriend))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        // For a single friend cluster, it should focus on that friend's location
        assertEquals(singleFriend.first().getLatLng(), state.mapCenter)
    }

    @Test
    fun `cluster click with empty list should not crash`() = runTest {
        // Given
        val emptyFriendsList = emptyList<Friend>()

        // When
        viewModel.onEvent(MapScreenEvent.OnClusterClick(emptyFriendsList))
        advanceUntilIdle()

        // Then
        // Should not crash and state should remain stable
        val state = viewModel.state.value
        assertNotNull(state) // Basic sanity check
    }

    @Test
    fun `friend marker click with invalid friend ID should not crash`() = runTest {
        // Given
        val invalidFriendId = "non_existent_friend"

        // When
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick(invalidFriendId))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(invalidFriendId, state.selectedFriendId) // Should still set the ID
        // Map center should remain unchanged since friend doesn't exist
    }

    // Helper methods for creating test data

    private fun createTestFriends(): List<Friend> {
        return listOf(
            Friend(
                id = "friend_1",
                userId = "user_1",
                name = "Alice Johnson",
                email = "alice@example.com",
                avatarUrl = "",
                profileColor = "#2E7D32",
                location = FriendLocation(
                    latitude = 37.7749,
                    longitude = -122.4194,
                    accuracy = 10f,
                    isMoving = false,
                    timestamp = Date()
                ),
                status = FriendStatus(
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    isLocationSharingEnabled = true
                )
            ),
            Friend(
                id = "friend_2",
                userId = "user_2",
                name = "Bob Smith",
                email = "bob@example.com",
                avatarUrl = "",
                profileColor = "#1976D2",
                location = FriendLocation(
                    latitude = 37.7849,
                    longitude = -122.4094,
                    accuracy = 15f,
                    isMoving = true,
                    timestamp = Date()
                ),
                status = FriendStatus(
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    isLocationSharingEnabled = true
                )
            ),
            Friend(
                id = "friend_3",
                userId = "user_3",
                name = "Carol Davis",
                email = "carol@example.com",
                avatarUrl = "",
                profileColor = "#7B1FA2",
                location = FriendLocation(
                    latitude = 37.7649,
                    longitude = -122.4294,
                    accuracy = 8f,
                    isMoving = false,
                    timestamp = Date()
                ),
                status = FriendStatus(
                    isOnline = false,
                    lastSeen = System.currentTimeMillis() - 300000, // 5 minutes ago
                    isLocationSharingEnabled = true
                )
            )
        )
    }

    private fun createClusteredFriends(): List<Friend> {
        // Create friends that are close together
        val baseLocation = LatLng(37.7749, -122.4194)
        return (1..5).map { index ->
            Friend(
                id = "clustered_friend_$index",
                userId = "clustered_user_$index",
                name = "Friend $index",
                email = "friend$index@example.com",
                avatarUrl = "",
                profileColor = "#2E7D32",
                location = FriendLocation(
                    latitude = baseLocation.latitude + (index * 0.001), // Close together
                    longitude = baseLocation.longitude + (index * 0.001),
                    accuracy = 10f,
                    isMoving = false,
                    timestamp = Date()
                ),
                status = FriendStatus(
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    isLocationSharingEnabled = true
                )
            )
        }
    }
}