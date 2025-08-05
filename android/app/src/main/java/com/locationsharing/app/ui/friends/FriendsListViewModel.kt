package com.locationsharing.app.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import com.locationsharing.app.data.friends.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Friends List screen with real-time Firebase integration
 * Handles friends data, status updates, and UI state management
 */
@HiltViewModel
class FriendsListViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val realTimeFriendsService: RealTimeFriendsService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FriendsListUiState())
    val uiState: StateFlow<FriendsListUiState> = _uiState.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    init {
        startRealTimeSync()
        observeFriendsData()
        observeConnectionState()
    }
    
    /**
     * Start real-time synchronization with Firebase
     */
    private fun startRealTimeSync() {
        viewModelScope.launch {
            try {
                realTimeFriendsService.startSync()
                Timber.d("Real-time friends sync started")
            } catch (e: Exception) {
                Timber.e(e, "Error starting real-time sync")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to connect to real-time updates: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Observe friends data from Firebase with real-time updates
     */
    private fun observeFriendsData() {
        viewModelScope.launch {
            combine(
                friendsRepository.getFriends(),
                friendsRepository.getOnlineFriends()
            ) { allFriends, onlineFriends ->
                Pair(allFriends, onlineFriends)
            }
            .catch { exception ->
                Timber.e(exception, "Error observing friends data")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load friends: ${exception.message}",
                    isLoading = false
                )
            }
            .collect { (allFriends, onlineFriends) ->
                _uiState.value = _uiState.value.copy(
                    friends = allFriends,
                    onlineFriends = onlineFriends,
                    isLoading = false,
                    error = null,
                    lastUpdateTime = System.currentTimeMillis()
                )
                
                Timber.d("Friends updated: ${allFriends.size} total, ${onlineFriends.size} online")
            }
        }
    }
    
    /**
     * Observe connection state changes
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            realTimeFriendsService.connectionState.collect { state ->
                _connectionState.value = state
                
                _uiState.value = _uiState.value.copy(
                    isConnected = state == ConnectionState.CONNECTED,
                    isLoading = state == ConnectionState.CONNECTING
                )
                
                // Show connection status messages
                when (state) {
                    ConnectionState.ERROR -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Connection lost. Retrying..."
                        )
                    }
                    ConnectionState.CONNECTED -> {
                        if (_uiState.value.lastUpdateTime > 0) {
                            // Only show success message if we had data before
                            _uiState.value = _uiState.value.copy(
                                successMessage = "Connected to real-time updates"
                            )
                        }
                    }
                    else -> { /* Handle other states if needed */ }
                }
                
                Timber.d("Connection state changed: $state")
            }
        }
    }
    
    /**
     * Refresh friends data manually
     */
    fun refreshFriends() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = true,
                    error = null
                )
                
                // Restart real-time sync to refresh data
                realTimeFriendsService.stopSync()
                kotlinx.coroutines.delay(500) // Brief pause
                realTimeFriendsService.startSync()
                
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    successMessage = "Friends list refreshed"
                )
                
                Timber.d("Friends data refreshed manually")
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing friends")
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Failed to refresh friends: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Handle friend selection
     */
    fun selectFriend(friend: Friend) {
        _uiState.value = _uiState.value.copy(selectedFriend = friend)
        Timber.d("Friend selected: ${friend.name}")
    }
    
    /**
     * Clear friend selection
     */
    fun clearFriendSelection() {
        _uiState.value = _uiState.value.copy(selectedFriend = null)
        Timber.d("Friend selection cleared")
    }
    
    /**
     * Send friend request
     */
    suspend fun sendFriendRequest(toUserId: String, message: String? = null): Result<String> {
        return try {
            friendsRepository.sendFriendRequest(toUserId, message)
        } catch (e: Exception) {
            Timber.e(e, "Error sending friend request")
            Result.failure(e)
        }
    }
    
    /**
     * Remove friend with confirmation
     */
    suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            val result = friendsRepository.removeFriend(friendId)
            
            if (result.isSuccess) {
                // Clear selection if removed friend was selected
                if (_uiState.value.selectedFriend?.id == friendId) {
                    clearFriendSelection()
                }
                
                _uiState.value = _uiState.value.copy(
                    successMessage = "Friend removed successfully"
                )
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Error removing friend")
            Result.failure(e)
        }
    }
    
    /**
     * Get friend by ID
     */
    fun getFriendById(id: String): Friend? {
        return _uiState.value.friends.find { it.id == id }
    }
    
    /**
     * Check if friend is currently online
     */
    fun isFriendOnline(friendId: String): Boolean {
        return getFriendById(friendId)?.isOnline() ?: false
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    /**
     * Handle network connectivity changes
     */
    fun onNetworkConnectivityChanged(isConnected: Boolean) {
        if (isConnected) {
            // Restart sync when network is restored
            startRealTimeSync()
        } else {
            _uiState.value = _uiState.value.copy(
                error = "No internet connection. Some features may not work."
            )
        }
    }
    
    /**
     * Handle app lifecycle changes
     */
    fun onAppResumed() {
        // Refresh data when app comes to foreground
        refreshFriends()
    }
    
    fun onAppPaused() {
        // Optionally pause real-time updates to save battery
        // realTimeFriendsService.pauseSync()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Stop real-time sync when ViewModel is cleared
        realTimeFriendsService.stopSync()
        Timber.d("FriendsListViewModel cleared, real-time sync stopped")
    }
}

/**
 * UI state for the Friends List screen
 */
data class FriendsListUiState(
    val friends: List<Friend> = emptyList(),
    val onlineFriends: List<Friend> = emptyList(),
    val selectedFriend: Friend? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val lastUpdateTime: Long = 0L
) {
    val hasOnlineFriends: Boolean
        get() = onlineFriends.isNotEmpty()
    
    val hasAnyFriends: Boolean
        get() = friends.isNotEmpty()
    
    val offlineFriends: List<Friend>
        get() = friends.filter { !it.isOnline() }
    
    val friendsCount: Int
        get() = friends.size
    
    val onlineFriendsCount: Int
        get() = onlineFriends.size
    
    val offlineFriendsCount: Int
        get() = offlineFriends.size
}