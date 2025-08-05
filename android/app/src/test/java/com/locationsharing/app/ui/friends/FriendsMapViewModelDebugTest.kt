package com.locationsharing.app.ui.friends

import com.locationsharing.app.BuildConfig
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
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
import kotlin.test.assertTrue

/**
 * Unit tests for debug functionality in FriendsMapViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendsMapViewModelDebugTest {

    @MockK
    private lateinit var friendsRepository: FriendsRepository

    @MockK
    private lateinit var realTimeFriendsService: RealTimeFriendsService

    @MockK
    private lateinit var getNearbyFriendsUseCase: com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase

    private lateinit var viewModel: FriendsMapViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Mock the repository to return empty friends list
        every { friendsRepository.getFriends() } returns flowOf(emptyList())
        every { realTimeFriendsService.connectionState } returns flowOf(com.locationsharing.app.data.friends.ConnectionState.CONNECTED)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())
        every { realTimeFriendsService.startSync() } returns Unit
        every { realTimeFriendsService.stopSync() } returns Unit
        every { getNearbyFriendsUseCase() } returns flowOf(emptyList())

        viewModel = FriendsMapViewModel(friendsRepository, realTimeFriendsService, getNearbyFriendsUseCase)
    }

    @Test
    fun `addTestFriendsOnMap should add test friends when BuildConfig DEBUG is true`() = runTest {
        // Given - assume we're in debug mode (this test only runs if BuildConfig.DEBUG is true)
        if (!BuildConfig.DEBUG) {
            // Skip test in release builds
            return@runTest
        }

        // When
        viewModel.addTestFriendsOnMap()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        val testFriends = uiState.friends.filter { it.id.startsWith("test_friend_") }
        
        assertTrue(testFriends.isNotEmpty(), "Test friends should be added")
        assertEquals(5, testFriends.size, "Should add exactly 5 test friends")
        
        // Verify all test friends are online
        testFriends.forEach { friend ->
            assertTrue(friend.isOnline(), "Test friend ${friend.name} should be online")
            assertTrue(friend.status.isLocationSharingEnabled, "Test friend ${friend.name} should have location sharing enabled")
        }
    }

    @Test
    fun `addTestFriendsOnMap should not crash when called multiple times`() = runTest {
        if (!BuildConfig.DEBUG) return@runTest

        // When - call multiple times
        viewModel.addTestFriendsOnMap()
        advanceUntilIdle()
        
        viewModel.addTestFriendsOnMap()
        advanceUntilIdle()

        // Then - should not crash and should still have test friends
        val uiState = viewModel.uiState.value
        val testFriends = uiState.friends.filter { it.id.startsWith("test_friend_") }
        
        assertTrue(testFriends.isNotEmpty(), "Test friends should still be present")
    }

    @Test
    fun `clearTestFriends should remove all test friends`() = runTest {
        if (!BuildConfig.DEBUG) return@runTest

        // Given - add test friends first
        viewModel.addTestFriendsOnMap()
        advanceUntilIdle()
        
        val uiStateWithTestFriends = viewModel.uiState.value
        assertTrue(uiStateWithTestFriends.friends.any { it.id.startsWith("test_friend_") })

        // When - clear test friends
        viewModel.clearTestFriends()
        advanceUntilIdle()

        // Then - test friends should be removed
        val uiStateAfterClear = viewModel.uiState.value
        val remainingTestFriends = uiStateAfterClear.friends.filter { it.id.startsWith("test_friend_") }
        
        assertTrue(remainingTestFriends.isEmpty(), "All test friends should be removed")
    }

    @Test
    fun `test friends should have valid locations`() = runTest {
        if (!BuildConfig.DEBUG) return@runTest

        // When
        viewModel.addTestFriendsOnMap()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        val testFriends = uiState.friends.filter { it.id.startsWith("test_friend_") }
        
        testFriends.forEach { friend ->
            val location = friend.getLatLng()
            assertTrue(location != null, "Test friend ${friend.name} should have a valid location")
            
            // Verify location is in San Francisco area (rough bounds)
            location?.let {
                assertTrue(it.latitude > 37.7, "Latitude should be in SF area")
                assertTrue(it.latitude < 37.8, "Latitude should be in SF area")
                assertTrue(it.longitude > -122.5, "Longitude should be in SF area")
                assertTrue(it.longitude < -122.3, "Longitude should be in SF area")
            }
        }
    }

    @Test
    fun `test friends should have distinct colors`() = runTest {
        if (!BuildConfig.DEBUG) return@runTest

        // When
        viewModel.addTestFriendsOnMap()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        val testFriends = uiState.friends.filter { it.id.startsWith("test_friend_") }
        
        val colors = testFriends.map { it.profileColor }.toSet()
        assertEquals(testFriends.size, colors.size, "All test friends should have distinct colors")
    }
}