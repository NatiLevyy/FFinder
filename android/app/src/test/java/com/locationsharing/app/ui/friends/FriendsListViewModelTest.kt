package com.locationsharing.app.ui.friends

import com.locationsharing.app.data.friends.ConnectionState
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsListViewModelTest {
    
    private lateinit var viewModel: FriendsListViewModel
    private lateinit var mockFriendsRepository: FriendsRepository
    private lateinit var mockRealTimeFriendsService: RealTimeFriendsService
    
    private val testDispatcher = StandardTestDispatcher()
    
    // Test data
    private val onlineFriend = Friend(
        id = "1",
        name = "Online Friend",
        email = "online@test.com",
        status = FriendStatus(isOnline = true, lastSeen = System.currentTimeMillis())
    )
    
    private val offlineFriend = Friend(
        id = "2",
        name = "Offline Friend",
        email = "offline@test.com",
        status = FriendStatus(isOnline = false, lastSeen = System.currentTimeMillis() - 300000)
    )
    
    private val testFriends = listOf(onlineFriend, offlineFriend)
    private val testOnlineFriends = listOf(onlineFriend)
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockFriendsRepository = mockk(relaxed = true)
        mockRealTimeFriendsService = mockk(relaxed = true)
        
        // Setup default mock behaviors
        every { mockFriendsRepository.getFriends() } returns flowOf(testFriends)
        every { mockFriendsRepository.getOnlineFriends() } returns flowOf(testOnlineFriends)
        every { mockRealTimeFriendsService.connectionState } returns flowOf(ConnectionState.CONNECTED)
        coEvery { mockRealTimeFriendsService.startSync() } returns Unit
        
        viewModel = FriendsListViewModel(mockFriendsRepository, mockRealTimeFriendsService)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state should be loading`() {
        // Given - fresh ViewModel
        
        // When - checking initial state
        val initialState = viewModel.uiState.value
        
        // Then - should be in loading state
        assertTrue(initialState.isLoading)
        assertTrue(initialState.friends.isEmpty())
        assertTrue(initialState.onlineFriends.isEmpty())
        assertNull(initialState.error)
    }
    
    @Test
    fun `should load friends data on initialization`() = runTest {
        // Given - ViewModel is initialized
        
        // When - waiting for data to load
        advanceUntilIdle()
        
        // Then - should have loaded friends data
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testFriends, state.friends)
        assertEquals(testOnlineFriends, state.onlineFriends)
        assertNull(state.error)
        assertTrue(state.lastUpdateTime > 0)
        
        // And - should have started real-time sync
        coVerify { mockRealTimeFriendsService.startSync() }
    }
    
    @Test
    fun `should handle connection state changes`() = runTest {
        // Given - ViewModel is initialized
        every { mockRealTimeFriendsService.connectionState } returns flowOf(ConnectionState.ERROR)
        
        // When - connection state changes to error
        advanceUntilIdle()
        
        // Then - should update UI state accordingly
        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Connection lost"))
    }
    
    @Test
    fun `refreshFriends should restart sync and update state`() = runTest {
        // Given - ViewModel is initialized and loaded
        advanceUntilIdle()
        
        // When - refreshing friends
        viewModel.refreshFriends()
        advanceUntilIdle()
        
        // Then - should restart sync
        coVerify { mockRealTimeFriendsService.stopSync() }
        coVerify(atLeast = 2) { mockRealTimeFriendsService.startSync() }
        
        // And - should update state
        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("refreshed"))
    }
    
    @Test
    fun `selectFriend should update selected friend`() = runTest {
        // Given - ViewModel is initialized and loaded
        advanceUntilIdle()
        
        // When - selecting a friend
        viewModel.selectFriend(onlineFriend)
        
        // Then - should update selected friend
        val state = viewModel.uiState.value
        assertEquals(onlineFriend, state.selectedFriend)
    }
    
    @Test
    fun `clearFriendSelection should clear selected friend`() = runTest {
        // Given - ViewModel with selected friend
        advanceUntilIdle()
        viewModel.selectFriend(onlineFriend)
        
        // When - clearing selection
        viewModel.clearFriendSelection()
        
        // Then - should clear selected friend
        val state = viewModel.uiState.value
        assertNull(state.selectedFriend)
    }
    
    @Test
    fun `sendFriendRequest should call repository`() = runTest {
        // Given - ViewModel is initialized
        val toUserId = "user123"
        val message = "Hello!"
        coEvery { mockFriendsRepository.sendFriendRequest(toUserId, message) } returns Result.success("request123")
        
        // When - sending friend request
        val result = viewModel.sendFriendRequest(toUserId, message)
        
        // Then - should call repository and return result
        coVerify { mockFriendsRepository.sendFriendRequest(toUserId, message) }
        assertTrue(result.isSuccess)
        assertEquals("request123", result.getOrNull())
    }
    
    @Test
    fun `removeFriend should call repository and clear selection if needed`() = runTest {
        // Given - ViewModel with selected friend
        advanceUntilIdle()
        viewModel.selectFriend(onlineFriend)
        coEvery { mockFriendsRepository.removeFriend(onlineFriend.id) } returns Result.success(Unit)
        
        // When - removing the selected friend
        val result = viewModel.removeFriend(onlineFriend.id)
        
        // Then - should call repository and clear selection
        coVerify { mockFriendsRepository.removeFriend(onlineFriend.id) }
        assertTrue(result.isSuccess)
        
        val state = viewModel.uiState.value
        assertNull(state.selectedFriend)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("removed"))
    }
    
    @Test
    fun `getFriendById should return correct friend`() = runTest {
        // Given - ViewModel is loaded with friends
        advanceUntilIdle()
        
        // When - getting friend by ID
        val friend = viewModel.getFriendById(onlineFriend.id)
        
        // Then - should return correct friend
        assertEquals(onlineFriend, friend)
    }
    
    @Test
    fun `getFriendById should return null for non-existent friend`() = runTest {
        // Given - ViewModel is loaded with friends
        advanceUntilIdle()
        
        // When - getting non-existent friend by ID
        val friend = viewModel.getFriendById("non-existent")
        
        // Then - should return null
        assertNull(friend)
    }
    
    @Test
    fun `isFriendOnline should return correct status`() = runTest {
        // Given - ViewModel is loaded with friends
        advanceUntilIdle()
        
        // When - checking online status
        val isOnlineTrue = viewModel.isFriendOnline(onlineFriend.id)
        val isOnlineFalse = viewModel.isFriendOnline(offlineFriend.id)
        
        // Then - should return correct status
        assertTrue(isOnlineTrue)
        assertFalse(isOnlineFalse)
    }
    
    @Test
    fun `clearError should clear error message`() = runTest {
        // Given - ViewModel with error
        every { mockFriendsRepository.getFriends() } returns flowOf()
        every { mockRealTimeFriendsService.connectionState } returns flowOf(ConnectionState.ERROR)
        advanceUntilIdle()
        
        // When - clearing error
        viewModel.clearError()
        
        // Then - should clear error
        val state = viewModel.uiState.value
        assertNull(state.error)
    }
    
    @Test
    fun `clearSuccessMessage should clear success message`() = runTest {
        // Given - ViewModel with success message
        advanceUntilIdle()
        viewModel.refreshFriends()
        advanceUntilIdle()
        
        // When - clearing success message
        viewModel.clearSuccessMessage()
        
        // Then - should clear success message
        val state = viewModel.uiState.value
        assertNull(state.successMessage)
    }
    
    @Test
    fun `onNetworkConnectivityChanged should handle network changes`() = runTest {
        // Given - ViewModel is initialized
        advanceUntilIdle()
        
        // When - network connectivity changes
        viewModel.onNetworkConnectivityChanged(false)
        
        // Then - should update error state
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("No internet connection"))
        
        // When - network is restored
        viewModel.onNetworkConnectivityChanged(true)
        
        // Then - should restart sync
        coVerify(atLeast = 2) { mockRealTimeFriendsService.startSync() }
    }
    
    @Test
    fun `onAppResumed should refresh friends`() = runTest {
        // Given - ViewModel is initialized
        advanceUntilIdle()
        
        // When - app is resumed
        viewModel.onAppResumed()
        advanceUntilIdle()
        
        // Then - should refresh friends
        coVerify(atLeast = 2) { mockRealTimeFriendsService.startSync() }
    }
    
    @Test
    fun `UI state computed properties should work correctly`() = runTest {
        // Given - ViewModel is loaded with friends
        advanceUntilIdle()
        
        // When - checking computed properties
        val state = viewModel.uiState.value
        
        // Then - should return correct values
        assertTrue(state.hasOnlineFriends)
        assertTrue(state.hasAnyFriends)
        assertEquals(1, state.offlineFriends.size)
        assertEquals(2, state.friendsCount)
        assertEquals(1, state.onlineFriendsCount)
        assertEquals(1, state.offlineFriendsCount)
    }
    
    @Test
    fun `should handle repository errors gracefully`() = runTest {
        // Given - repository that throws error
        val error = RuntimeException("Network error")
        every { mockFriendsRepository.getFriends() } throws error
        
        // When - initializing ViewModel
        val errorViewModel = FriendsListViewModel(mockFriendsRepository, mockRealTimeFriendsService)
        advanceUntilIdle()
        
        // Then - should handle error gracefully
        val state = errorViewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Network error"))
    }
}