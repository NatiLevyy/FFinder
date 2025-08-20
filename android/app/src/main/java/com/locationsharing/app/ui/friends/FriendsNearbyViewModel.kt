package com.locationsharing.app.ui.friends

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import com.locationsharing.app.ui.friends.logging.NearbyPanelLogger
// import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
// import javax.inject.Inject

/**
 * ViewModel for the Friends Nearby Panel feature.
 * Handles all nearby panel interactions and state management.
 * Requirements: 6.1, 6.3, 6.4
 */
// @HiltViewModel
class FriendsNearbyViewModel constructor(
    // Temporarily removed getNearbyFriendsUseCase to prevent EnhancedLocationService from starting
    // private val getNearbyFriendsUseCase: GetNearbyFriendsUseCase,
    private val friendsRepository: FriendsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()
    
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()
    
    init {
        observeNearbyFriends()
    }
    
    /**
     * Observe nearby friends updates with reactive distance calculations
     * Requirements: 6.1, 6.3, 6.4
     * TEMPORARILY DISABLED to prevent EnhancedLocationService from starting
     */
    private fun observeNearbyFriends() {
        // Temporarily disabled to prevent location service conflicts
        /*
        viewModelScope.launch {
            getNearbyFriendsUseCase().collect { nearbyFriends ->
                _uiState.value = _uiState.value.copy(
                    friends = nearbyFriends,
                    isLoading = false,
                    error = null
                )
                
                NearbyPanelLogger.logFriendListState(nearbyFriends)
                NearbyPanelLogger.logRepositoryOperation(
                    "observeNearbyFriends",
                    true,
                    null
                )
            }
        }
        */
        
        // Set initial state with empty friends list
        _uiState.value = _uiState.value.copy(
            friends = emptyList(),
            isLoading = false,
            error = null
        )
    }
    
    /**
     * Handle all nearby panel events
     * Requirements: 5.1, 5.2, 5.4, 5.5, 5.6, 5.7, 8.5
     */
    fun onEvent(event: NearbyPanelEvent) {
        when (event) {
            is NearbyPanelEvent.TogglePanel -> togglePanel()
            is NearbyPanelEvent.SearchQuery -> updateSearchQuery(event.query)
            is NearbyPanelEvent.FriendClick -> handleFriendClick(event.friendId)
            is NearbyPanelEvent.Navigate -> handleNavigate(event.friendId)
            is NearbyPanelEvent.Ping -> handlePing(event.friendId)
            is NearbyPanelEvent.StopSharing -> handleStopSharing(event.friendId)
            is NearbyPanelEvent.Message -> handleMessage(event.friendId)
            is NearbyPanelEvent.DismissBottomSheet -> dismissBottomSheet()
            is NearbyPanelEvent.RefreshFriends -> refreshFriends()
            is NearbyPanelEvent.ClearError -> clearError()
            is NearbyPanelEvent.ClearFeedback -> clearFeedback()
            is NearbyPanelEvent.DismissSnackbar -> dismissSnackbar()
            is NearbyPanelEvent.UpdateScrollPosition -> updateScrollPosition(event.position)
            is NearbyPanelEvent.PreserveState -> preserveState()
            is NearbyPanelEvent.InviteFriends -> handleInviteFriends()
        }
    }
    
    /**
     * Toggle panel open/closed state
     * Requirement 1.2: When the user taps the friends button THEN the system SHALL toggle a Modal Navigation Drawer
     */
    private fun togglePanel() {
        val newState = !_uiState.value.isPanelOpen
        _uiState.value = _uiState.value.copy(
            isPanelOpen = newState
        )
        NearbyPanelLogger.logPanelStateChange(newState, _uiState.value.friends.size)
    }
    
    /**
     * Update search query and filter friends
     * Requirement 3.2: When the user types in the search bar THEN the system SHALL filter the friends list by display name in real-time
     */
    private fun updateSearchQuery(query: String) {
        val filteredCount = _uiState.value.copy(searchQuery = query).filteredFriends.size
        _uiState.value = _uiState.value.copy(searchQuery = query)
        NearbyPanelLogger.logSearchQuery(query, filteredCount)
    }
    
    /**
     * Handle friend click - focus on friend and show bottom sheet
     * Requirements: 5.1, 5.2
     */
    private fun handleFriendClick(friendId: String) {
        viewModelScope.launch {
            try {
                val friend = _uiState.value.friends.find { it.id == friendId }
                if (friend != null) {
                    _uiState.value = _uiState.value.copy(selectedFriendId = friendId)
                    
                    NearbyPanelLogger.logFriendInteraction(
                        "FRIEND_SELECTED",
                        friendId,
                        friend.displayName
                    )
                } else {
                    NearbyPanelLogger.logError(
                        "handleFriendClick",
                        RuntimeException("Friend not found"),
                        mapOf("friendId" to friendId)
                    )
                    _uiState.value = _uiState.value.copy(error = "Friend not found")
                }
            } catch (e: Exception) {
                NearbyPanelLogger.logError(
                    "handleFriendClick",
                    e,
                    mapOf("friendId" to friendId)
                )
                _uiState.value = _uiState.value.copy(error = "Error selecting friend: ${e.message}")
            }
        }
    }
    
    /**
     * Focus camera on friend's location with smooth animation
     * Requirement 5.1: When the user taps on a friend in the list THEN the system SHALL call focusOnFriend(friendId) to animate the camera to the friend's location
     */
    fun focusOnFriend(friendId: String, cameraPositionState: CameraPositionState) {
        viewModelScope.launch {
            try {
                Timber.d("📍 NearbyPanel: Focusing on friend: $friendId")
                
                val friend = _uiState.value.friends.find { it.id == friendId }
                if (friend == null) {
                    Timber.w("📍 NearbyPanel: Friend not found for focus: $friendId")
                    _uiState.value = _uiState.value.copy(error = "Friend not found")
                    return@launch
                }
                
                // Animate camera to friend's location
                val targetPosition = CameraPosition.fromLatLngZoom(friend.latLng, 17f)
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(targetPosition),
                    durationMs = 1200
                )
                
                // Select the friend for bottom sheet
                _uiState.value = _uiState.value.copy(selectedFriendId = friendId)
                
                Timber.d("📍 NearbyPanel: Successfully focused on friend ${friend.displayName} at ${friend.latLng.latitude}, ${friend.latLng.longitude}")
                
            } catch (e: Exception) {
                Timber.e(e, "📍 NearbyPanel: Error focusing on friend: $friendId")
                _uiState.value = _uiState.value.copy(error = "Failed to focus on friend: ${e.message}")
            }
        }
    }
    
    /**
     * Handle navigation to friend's location
     * Requirement 5.4: When the user taps Navigate THEN the system SHALL open Google Maps with the friend's location as destination
     */
    private fun handleNavigate(friendId: String) {
        viewModelScope.launch {
            try {
                val friend = _uiState.value.friends.find { it.id == friendId }
                if (friend == null) {
                    val errorMsg = "Friend not found"
                    _uiState.value = _uiState.value.copy(error = errorMsg)
                    
                    NearbyPanelLogger.logError(
                        "handleNavigate",
                        RuntimeException(errorMsg),
                        mapOf("friendId" to friendId)
                    )
                    return@launch
                }
                
                // Store navigation intent data for UI to handle
                _navigationIntent.value = NavigationIntentData(
                    friendId = friendId,
                    friendName = friend.displayName,
                    location = friend.latLng
                )
                
                NearbyPanelLogger.logFriendInteraction(
                    "NAVIGATE_TO_FRIEND",
                    friendId,
                    friend.displayName
                )
                
            } catch (e: Exception) {
                NearbyPanelLogger.logError(
                    "handleNavigate",
                    e,
                    mapOf("friendId" to friendId)
                )
                _uiState.value = _uiState.value.copy(error = "Failed to prepare navigation: ${e.message}")
            }
        }
    }
    
    /**
     * Handle ping friend action
     * Requirement 5.5: When the user taps Ping THEN the system SHALL call sendPing(friendId) (stub implementation)
     */
    private fun handlePing(friendId: String) {
        viewModelScope.launch {
            try {
                val friend = _uiState.value.friends.find { it.id == friendId }
                if (friend == null) {
                    val errorMsg = "Friend not found"
                    _uiState.value = _uiState.value.copy(error = errorMsg)
                    
                    NearbyPanelLogger.logError(
                        "handlePing",
                        RuntimeException(errorMsg),
                        mapOf("friendId" to friendId)
                    )
                    return@launch
                }
                
                val result = friendsRepository.sendPing(friendId)
                if (result.isSuccess) {
                    _feedbackMessage.value = "Ping sent to ${friend.displayName}! 📍"
                    
                    NearbyPanelLogger.logFriendInteraction(
                        "PING_FRIEND",
                        friendId,
                        friend.displayName
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _uiState.value = _uiState.value.copy(error = "Failed to send ping: $error")
                    
                    NearbyPanelLogger.logError(
                        "handlePing",
                        RuntimeException("Failed to send ping: $error"),
                        mapOf(
                            "friendId" to friendId,
                            "friendName" to friend.displayName
                        )
                    )
                }
                
            } catch (e: Exception) {
                NearbyPanelLogger.logError(
                    "handlePing",
                    e,
                    mapOf("friendId" to friendId)
                )
                _uiState.value = _uiState.value.copy(error = "Failed to send ping: ${e.message}")
            }
        }
    }
    
    /**
     * Handle stop sharing location with friend
     * Requirement 5.6: When the user taps Stop Sharing THEN the system SHALL call stopReceivingLocation(friendId)
     */
    private fun handleStopSharing(friendId: String) {
        viewModelScope.launch {
            try {
                val friend = _uiState.value.friends.find { it.id == friendId }
                if (friend == null) {
                    val errorMsg = "Friend not found"
                    _uiState.value = _uiState.value.copy(error = errorMsg)
                    
                    NearbyPanelLogger.logError(
                        "handleStopSharing",
                        RuntimeException(errorMsg),
                        mapOf("friendId" to friendId)
                    )
                    return@launch
                }
                
                val result = friendsRepository.stopReceivingLocation(friendId)
                if (result.isSuccess) {
                    _feedbackMessage.value = "Stopped sharing location with ${friend.displayName} 🔕"
                    // Clear selection if this friend was selected
                    if (_uiState.value.selectedFriendId == friendId) {
                        _uiState.value = _uiState.value.copy(selectedFriendId = null)
                    }
                    
                    NearbyPanelLogger.logFriendInteraction(
                        "STOP_SHARING_LOCATION",
                        friendId,
                        friend.displayName
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _uiState.value = _uiState.value.copy(error = "Failed to stop location sharing: $error")
                    
                    NearbyPanelLogger.logError(
                        "handleStopSharing",
                        RuntimeException("Failed to stop location sharing: $error"),
                        mapOf(
                            "friendId" to friendId,
                            "friendName" to friend.displayName
                        )
                    )
                }
                
            } catch (e: Exception) {
                NearbyPanelLogger.logError(
                    "handleStopSharing",
                    e,
                    mapOf("friendId" to friendId)
                )
                _uiState.value = _uiState.value.copy(error = "Failed to stop location sharing: ${e.message}")
            }
        }
    }
    
    /**
     * Handle message friend action
     * Requirement 5.7: When the user taps Message THEN the system SHALL open a share intent for SMS/WhatsApp with predefined text
     */
    private fun handleMessage(friendId: String) {
        viewModelScope.launch {
            try {
                val friend = _uiState.value.friends.find { it.id == friendId }
                if (friend == null) {
                    val errorMsg = "Friend not found"
                    _uiState.value = _uiState.value.copy(error = errorMsg)
                    
                    NearbyPanelLogger.logError(
                        "handleMessage",
                        RuntimeException(errorMsg),
                        mapOf("friendId" to friendId)
                    )
                    return@launch
                }
                
                // Store message intent data for UI to handle
                _messageIntent.value = MessageIntentData(
                    friendId = friendId,
                    friendName = friend.displayName,
                    messageText = "Hey ${friend.displayName}! I can see you're nearby on FFinder. Want to meet up? 📍"
                )
                
                NearbyPanelLogger.logFriendInteraction(
                    "MESSAGE_FRIEND",
                    friendId,
                    friend.displayName
                )
                
            } catch (e: Exception) {
                NearbyPanelLogger.logError(
                    "handleMessage",
                    e,
                    mapOf("friendId" to friendId)
                )
                _uiState.value = _uiState.value.copy(error = "Failed to prepare message: ${e.message}")
            }
        }
    }
    
    /**
     * Handle invite friends action
     * Opens the system share intent to invite friends to use FFinder
     */
    private fun handleInviteFriends() {
        viewModelScope.launch {
            try {
                _feedbackMessage.value = "Opening invite friends..."
                
                NearbyPanelLogger.logFriendInteraction(
                    "INVITE_FRIENDS",
                    "system",
                    "Invite Friends Action"
                )
                
            } catch (e: Exception) {
                NearbyPanelLogger.logError(
                    "handleInviteFriends",
                    e,
                    emptyMap()
                )
                _uiState.value = _uiState.value.copy(error = "Failed to open invite: ${e.message}")
            }
        }
    }
    
    /**
     * Dismiss bottom sheet
     */
    private fun dismissBottomSheet() {
        _uiState.value = _uiState.value.copy(selectedFriendId = null)
        Timber.d("📍 NearbyPanel: Bottom sheet dismissed")
    }
    
    /**
     * Refresh friends data
     */
    private fun refreshFriends() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                // The GetNearbyFriendsUseCase will automatically refresh when underlying data changes
                
                NearbyPanelLogger.logRepositoryOperation(
                    "refreshFriends",
                    true,
                    null
                )
            } catch (e: Exception) {
                NearbyPanelLogger.logError(
                    "refreshFriends",
                    e
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh friends: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error state
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Clear feedback message
     */
    private fun clearFeedback() {
        _feedbackMessage.value = null
    }

    /**
     * Dismiss snackbar message
     */
    private fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
    
    /**
     * Update scroll position for performance optimization
     */
    private fun updateScrollPosition(position: Int) {
        _uiState.value = _uiState.value.copy(scrollPosition = position)
    }
    
    /**
     * Preserve state during configuration changes
     */
    private fun preserveState() {
        _uiState.value = _uiState.value.copy(
            lastUpdateTime = System.currentTimeMillis()
        )
        Timber.d("📍 NearbyPanel: State preserved for configuration change")
    }
    
    // Intent data for UI to handle
    private val _navigationIntent = MutableStateFlow<NavigationIntentData?>(null)
    val navigationIntent: StateFlow<NavigationIntentData?> = _navigationIntent.asStateFlow()
    
    private val _messageIntent = MutableStateFlow<MessageIntentData?>(null)
    val messageIntent: StateFlow<MessageIntentData?> = _messageIntent.asStateFlow()
    
    /**
     * Create navigation intent for Google Maps
     */
    fun createNavigationIntent(context: Context, data: NavigationIntentData): Intent {
        val gmmIntentUri = android.net.Uri.parse(
            "google.navigation:q=${data.location.latitude},${data.location.longitude}&mode=d"
        )
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        
        // Check if Google Maps is installed
        return if (mapIntent.resolveActivity(context.packageManager) != null) {
            mapIntent
        } else {
            // Fallback to web version
            val webUri = android.net.Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=${data.location.latitude},${data.location.longitude}&travelmode=driving"
            )
            Intent(Intent.ACTION_VIEW, webUri)
        }
    }
    
    /**
     * Create share intent for messaging
     */
    fun createMessageIntent(data: MessageIntentData): Intent {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, data.messageText)
            putExtra(Intent.EXTRA_SUBJECT, "Message from FFinder")
        }
        
        return Intent.createChooser(shareIntent, "Send message to ${data.friendName}")
    }
    
    /**
     * Clear navigation intent after handling
     */
    fun clearNavigationIntent() {
        _navigationIntent.value = null
    }
    
    /**
     * Clear message intent after handling
     */
    fun clearMessageIntent() {
        _messageIntent.value = null
    }
}

/**
 * Data class for navigation intent information
 */
data class NavigationIntentData(
    val friendId: String,
    val friendName: String,
    val location: LatLng
)

/**
 * Data class for message intent information
 */
data class MessageIntentData(
    val friendId: String,
    val friendName: String,
    val messageText: String
)