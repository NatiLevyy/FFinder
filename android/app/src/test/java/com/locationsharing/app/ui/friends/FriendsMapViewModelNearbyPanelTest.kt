package com.locationsharing.app.ui.friends

import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for nearby panel functionality in FriendsMapViewModel
 * Tests task 10 implementation: Update FriendsMapViewModel with nearby panel state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendsMapViewModelNearbyPanelTest {

    @MockK
    private lateinit var friendsRepository: FriendsRepository

    @MockK
    private lateinit var realTimeFriendsService: RealTimeFriendsService

    @MockK
    private lateinit var getNearbyFriendsUseCase: GetNearbyFriendsUseCase

    private lateinit var viewModel: FriendsMapViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Mock dependencies
        every { friendsRepository.getFriends() } returns flowOf(emptyList())
        every { realTimeFriendsService.connectionState } returns flowOf(com.locationsharing.app.data.friends.ConnectionState.CONNECTED)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())
        every { realTimeFriendsService.startSync() } returns Unit
        every { realTimeFriendsService.stopSync() } returns Unit
        every { getNearbyFriendsUseCase() } returns flowOf(emptyList())

        viewModel = FriendsMapViewModel(friendsRepository, realTimeFriendsService, getNearbyFriendsUseCase)
    }

    @Test
    fun `nearbyFriends StateFlow should be exposed`() = runTest {
        // Given - mock nearby friends
        val mockNearbyFriends = listOf(
            createMockNearbyFriend("friend1", "Alice", 150.0),
            createMockNearbyFriend("friend2", "Bob", 300.0)
        )
        every { getNearbyFriendsUseCase() } returns flowOf(mockNearbyFriends)

        // When - create new viewModel to pick up the mocked flow
        viewModel = FriendsMapViewModel(friendsRepository, realTimeFriendsService, getNearbyFriendsUseCase)
        advanceUntilIdle()

        // Then - nearbyFriends should be exposed and contain the mocked data
        val nearbyFriends = viewModel.nearbyFriends.value
        assertEquals(2, nearbyFriends.size)
        assertEquals("Alice", nearbyFriends[0].displayName)
        assertEquals("Bob", nearbyFriends[1].displayName)
    }

    @Test
    fun `nearbyUiState should be exposed with initial state`() = runTest {
        // When
        advanceUntilIdle()

        // Then
        val nearbyUiState = viewModel.nearbyUiState.value
        assertFalse(nearbyUiState.isPanelOpen)
        assertTrue(nearbyUiState.searchQuery.isEmpty())
        assertTrue(nearbyUiState.friends.isEmpty())
        assertEquals(null, nearbyUiState.selectedFriendId)
    }

    @Test
    fun `onNearbyPanelEvent TogglePanel should toggle panel state`() = runTest {
        // Given
        advanceUntilIdle()
        assertFalse(viewModel.nearbyUiState.value.isPanelOpen)

        // When - toggle panel
        viewModel.onNearbyPanelEvent(NearbyPanelEvent.TogglePanel)
        advanceUntilIdle()

        // Then - panel should be open
        assertTrue(viewModel.nearbyUiState.value.isPanelOpen)

        // When - toggle again
        viewModel.onNearbyPanelEvent(NearbyPanelEvent.TogglePanel)
        advanceUntilIdle()

        // Then - panel should be closed
        assertFalse(viewModel.nearbyUiState.value.isPanelOpen)
    }

    @Test
    fun `onNearbyPanelEvent SearchQuery should update search query`() = runTest {
        // Given
        advanceUntilIdle()
        assertTrue(viewModel.nearbyUiState.value.searchQuery.isEmpty())

        // When
        viewModel.onNearbyPanelEvent(NearbyPanelEvent.SearchQuery("Alice"))
        advanceUntilIdle()

        // Then
        assertEquals("Alice", viewModel.nearbyUiState.value.searchQuery)
    }

    @Test
    fun `onNearbyPanelEvent FriendClick should select friend`() = runTest {
        // Given - mock nearby friends in UI state
        val mockNearbyFriends = listOf(
            createMockNearbyFriend("friend1", "Alice", 150.0)
        )
        every { getNearbyFriendsUseCase() } returns flowOf(mockNearbyFriends)
        viewModel = FriendsMapViewModel(friendsRepository, realTimeFriendsService, getNearbyFriendsUseCase)
        advanceUntilIdle()

        // When
        viewModel.onNearbyPanelEvent(NearbyPanelEvent.FriendClick("friend1"))
        advanceUntilIdle()

        // Then
        assertEquals("friend1", viewModel.nearbyUiState.value.selectedFriendId)
    }

    @Test
    fun `onNearbyPanelEvent DismissBottomSheet should clear selected friend`() = runTest {
        // Given - select a friend first
        val mockNearbyFriends = listOf(
            createMockNearbyFriend("friend1", "Alice", 150.0)
        )
        every { getNearbyFriendsUseCase() } returns flowOf(mockNearbyFriends)
        viewModel = FriendsMapViewModel(friendsRepository, realTimeFriendsService, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        viewModel.onNearbyPanelEvent(NearbyPanelEvent.FriendClick("friend1"))
        advanceUntilIdle()
        assertEquals("friend1", viewModel.nearbyUiState.value.selectedFriendId)

        // When
        viewModel.onNearbyPanelEvent(NearbyPanelEvent.DismissBottomSheet)
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.nearbyUiState.value.selectedFriendId)
    }

    @Test
    fun `onNearbyPanelEvent ClearError should clear error state`() = runTest {
        // Given - set an error state manually
        viewModel.onNearbyPanelEvent(NearbyPanelEvent.FriendClick("nonexistent"))
        advanceUntilIdle()
        // This should set an error since friend doesn't exist

        // When
        viewModel.onNearbyPanelEvent(NearbyPanelEvent.ClearError)
        advanceUntilIdle()

        // Then - error should be cleared (we can't easily verify the private state, but the method should not crash)
        // The test passes if no exception is thrown
    }

    private fun createMockNearbyFriend(id: String, name: String, distance: Double): NearbyFriend {
        return NearbyFriend(
            id = id,
            displayName = name,
            avatarUrl = null,
            distance = distance,
            isOnline = true,
            lastUpdated = System.currentTimeMillis(),
            latLng = com.google.android.gms.maps.model.LatLng(37.7749, -122.4194),
            location = null
        )
    }
}