package com.locationsharing.app.ui.friends

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FriendsNearbyViewModel friend interaction handlers
 * Tests requirements: 5.1, 5.4, 5.5, 5.6, 5.7, 8.5
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendsNearbyViewModelTest {
    
    private lateinit var viewModel: FriendsNearbyViewModel
    private lateinit var mockGetNearbyFriendsUseCase: GetNearbyFriendsUseCase
    private lateinit var mockFriendsRepository: FriendsRepository
    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private lateinit var mockCameraPositionState: CameraPositionState
    
    private val testDispatcher = StandardTestDispatcher()
    
    private val testFriend = NearbyFriend(
        id = "test_friend_1",
        displayName = "Test Friend",
        avatarUrl = null,
        distance = 150.0,
        isOnline = true,
        lastUpdated = System.currentTimeMillis(),
        latLng = LatLng(37.7749, -122.4194),
        location = null
    )
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        mockGetNearbyFriendsUseCase = mockk()
        mockFriendsRepository = mockk()
        mockContext = mockk()
        mockPackageManager = mockk()
        mockCameraPositionState = mockk(relaxed = true)
        
        every { mockGetNearbyFriendsUseCase() } returns flowOf(listOf(testFriend))
        every { mockContext.packageManager } returns mockPackageManager
        
        viewModel = FriendsNearbyViewModel(
            getNearbyFriendsUseCase = mockGetNearbyFriendsUseCase,
            friendsRepository = mockFriendsRepository
        )
    }
    
    @Test
    fun `focusOnFriend should animate camera to friend location and select friend`() = runTest {
        // Given
        val friendId = "test_friend_1"
        
        // When
        viewModel.focusOnFriend(friendId, mockCameraPositionState)
        advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals(friendId, uiState.selectedFriendId)
        assertNull(uiState.error)
        
        // Verify camera animation was triggered
        verify { mockCameraPositionState.animate(any(), any()) }
    }
    
    @Test
    fun `focusOnFriend should handle friend not found error`() = runTest {
        // Given
        val nonExistentFriendId = "non_existent_friend"
        
        // When
        viewModel.focusOnFriend(nonExistentFriendId, mockCameraPositionState)
        advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals("Friend not found", uiState.error)
        assertNull(uiState.selectedFriendId)
    }
    
    @Test
    fun `handleNavigate should prepare navigation intent data`() = runTest {
        // Given
        val friendId = "test_friend_1"
        
        // When
        viewModel.onEvent(NearbyPanelEvent.Navigate(friendId))
        advanceUntilIdle()
        
        // Then
        val navigationIntent = viewModel.navigationIntent.value
        assertNotNull(navigationIntent)
        assertEquals(friendId, navigationIntent?.friendId)
        assertEquals("Test Friend", navigationIntent?.friendName)
        assertEquals(testFriend.latLng, navigationIntent?.location)
    }
    
    @Test
    fun `createNavigationIntent should create Google Maps intent when app is installed`() {
        // Given
        val navigationData = NavigationIntentData(
            friendId = "test_friend_1",
            friendName = "Test Friend",
            location = LatLng(37.7749, -122.4194)
        )
        val mockIntent = mockk<Intent>()
        every { mockIntent.resolveActivity(mockPackageManager) } returns mockk()
        
        // When
        val intent = viewModel.createNavigationIntent(mockContext, navigationData)
        
        // Then
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent.action)
    }
    
    @Test
    fun `handlePing should call repository sendPing and show success feedback`() = runTest {
        // Given
        val friendId = "test_friend_1"
        coEvery { mockFriendsRepository.sendPing(friendId) } returns Result.success(Unit)
        
        // When
        viewModel.onEvent(NearbyPanelEvent.Ping(friendId))
        advanceUntilIdle()
        
        // Then
        coVerify { mockFriendsRepository.sendPing(friendId) }
        assertEquals("Ping sent to Test Friend! 📍", viewModel.feedbackMessage.value)
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `handlePing should handle repository error`() = runTest {
        // Given
        val friendId = "test_friend_1"
        val errorMessage = "Network error"
        coEvery { mockFriendsRepository.sendPing(friendId) } returns Result.failure(Exception(errorMessage))
        
        // When
        viewModel.onEvent(NearbyPanelEvent.Ping(friendId))
        advanceUntilIdle()
        
        // Then
        coVerify { mockFriendsRepository.sendPing(friendId) }
        assertEquals("Failed to send ping: $errorMessage", viewModel.uiState.value.error)
        assertNull(viewModel.feedbackMessage.value)
    }
    
    @Test
    fun `handleStopSharing should call repository stopReceivingLocation and show success feedback`() = runTest {
        // Given
        val friendId = "test_friend_1"
        coEvery { mockFriendsRepository.stopReceivingLocation(friendId) } returns Result.success(Unit)
        
        // Set friend as selected first
        viewModel.onEvent(NearbyPanelEvent.FriendClick(friendId))
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(NearbyPanelEvent.StopSharing(friendId))
        advanceUntilIdle()
        
        // Then
        coVerify { mockFriendsRepository.stopReceivingLocation(friendId) }
        assertEquals("Stopped sharing location with Test Friend 🔕", viewModel.feedbackMessage.value)
        assertNull(viewModel.uiState.value.selectedFriendId) // Should clear selection
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `handleStopSharing should handle repository error`() = runTest {
        // Given
        val friendId = "test_friend_1"
        val errorMessage = "Permission denied"
        coEvery { mockFriendsRepository.stopReceivingLocation(friendId) } returns Result.failure(Exception(errorMessage))
        
        // When
        viewModel.onEvent(NearbyPanelEvent.StopSharing(friendId))
        advanceUntilIdle()
        
        // Then
        coVerify { mockFriendsRepository.stopReceivingLocation(friendId) }
        assertEquals("Failed to stop location sharing: $errorMessage", viewModel.uiState.value.error)
        assertNull(viewModel.feedbackMessage.value)
    }
    
    @Test
    fun `handleMessage should prepare message intent data`() = runTest {
        // Given
        val friendId = "test_friend_1"
        
        // When
        viewModel.onEvent(NearbyPanelEvent.Message(friendId))
        advanceUntilIdle()
        
        // Then
        val messageIntent = viewModel.messageIntent.value
        assertNotNull(messageIntent)
        assertEquals(friendId, messageIntent?.friendId)
        assertEquals("Test Friend", messageIntent?.friendName)
        assertEquals("Hey Test Friend! I can see you're nearby on FFinder. Want to meet up? 📍", messageIntent?.messageText)
    }
    
    @Test
    fun `createMessageIntent should create share intent with predefined text`() {
        // Given
        val messageData = MessageIntentData(
            friendId = "test_friend_1",
            friendName = "Test Friend",
            messageText = "Hey Test Friend! I can see you're nearby on FFinder. Want to meet up? 📍"
        )
        
        // When
        val intent = viewModel.createMessageIntent(messageData)
        
        // Then
        assertNotNull(intent)
        assertEquals(Intent.ACTION_CHOOSER, intent.action)
    }
    
    @Test
    fun `handleFriendClick should select friend for bottom sheet`() = runTest {
        // Given
        val friendId = "test_friend_1"
        
        // When
        viewModel.onEvent(NearbyPanelEvent.FriendClick(friendId))
        advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals(friendId, uiState.selectedFriendId)
        assertNull(uiState.error)
    }
    
    @Test
    fun `handleFriendClick should handle friend not found error`() = runTest {
        // Given
        val nonExistentFriendId = "non_existent_friend"
        
        // When
        viewModel.onEvent(NearbyPanelEvent.FriendClick(nonExistentFriendId))
        advanceUntilIdle()
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals("Friend not found", uiState.error)
        assertNull(uiState.selectedFriendId)
    }
    
    @Test
    fun `togglePanel should toggle panel open state`() = runTest {
        // Given
        val initialState = viewModel.uiState.value.isPanelOpen
        
        // When
        viewModel.onEvent(NearbyPanelEvent.TogglePanel)
        advanceUntilIdle()
        
        // Then
        assertEquals(!initialState, viewModel.uiState.value.isPanelOpen)
    }
    
    @Test
    fun `updateSearchQuery should update search query in state`() = runTest {
        // Given
        val searchQuery = "test query"
        
        // When
        viewModel.onEvent(NearbyPanelEvent.SearchQuery(searchQuery))
        advanceUntilIdle()
        
        // Then
        assertEquals(searchQuery, viewModel.uiState.value.searchQuery)
    }
    
    @Test
    fun `dismissBottomSheet should clear selected friend`() = runTest {
        // Given - select a friend first
        viewModel.onEvent(NearbyPanelEvent.FriendClick("test_friend_1"))
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(NearbyPanelEvent.DismissBottomSheet)
        advanceUntilIdle()
        
        // Then
        assertNull(viewModel.uiState.value.selectedFriendId)
    }
    
    @Test
    fun `clearError should clear error state`() = runTest {
        // Given - trigger an error first
        viewModel.onEvent(NearbyPanelEvent.FriendClick("non_existent_friend"))
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(NearbyPanelEvent.ClearError)
        advanceUntilIdle()
        
        // Then
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `clearFeedback should clear feedback message`() = runTest {
        // Given - trigger feedback first
        coEvery { mockFriendsRepository.sendPing("test_friend_1") } returns Result.success(Unit)
        viewModel.onEvent(NearbyPanelEvent.Ping("test_friend_1"))
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(NearbyPanelEvent.ClearFeedback)
        advanceUntilIdle()
        
        // Then
        assertNull(viewModel.feedbackMessage.value)
    }
}