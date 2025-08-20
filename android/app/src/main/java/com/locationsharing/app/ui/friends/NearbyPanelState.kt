package com.locationsharing.app.ui.friends

import com.locationsharing.app.domain.model.NearbyFriend

/**
 * UI state for the Friends Nearby Panel feature.
 * Manages panel visibility, loading states, search functionality, and friend data.
 * Implements state preservation during configuration changes.
 */
data class NearbyUiState(
    val isPanelOpen: Boolean = false,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val friends: List<NearbyFriend> = emptyList(),
    val error: String? = null,
    val feedbackMessage: String? = null,
    val snackbarMessage: String? = null,
    val userLocation: android.location.Location? = null,
    val selectedFriendId: String? = null,
    // Performance optimization: Track scroll position for large lists
    val scrollPosition: Int = 0,
    // Configuration change preservation
    val lastUpdateTime: Long = System.currentTimeMillis()
) {
    /**
     * Filtered friends based on search query with performance optimization.
     * Friends are already sorted by distance from GetNearbyFriendsUseCase,
     * so we only need to filter, not re-sort.
     */
    val filteredFriends: List<NearbyFriend>
        get() = if (searchQuery.isBlank()) {
            // Friends are already sorted by distance - no need to re-sort
            friends
        } else {
            // Performance optimization: Use sequence for large lists, preserve sort order
            if (friends.size > 100) {
                friends.asSequence()
                    .filter { friend ->
                        friend.displayName.contains(searchQuery, ignoreCase = true)
                    }
                    .toList() // Maintains existing sort order
            } else {
                friends.filter { friend ->
                    friend.displayName.contains(searchQuery, ignoreCase = true)
                } // Maintains existing sort order
            }
        }
    
    /**
     * Selected friend for bottom sheet display
     */
    val selectedFriend: NearbyFriend?
        get() = selectedFriendId?.let { id ->
            friends.find { it.id == id }
        }
}

/**
 * Events that can be triggered from the Friends Nearby Panel UI.
 * Handles all user interactions and state changes.
 * Includes performance optimization events.
 */
sealed class NearbyPanelEvent {
    object TogglePanel : NearbyPanelEvent()
    data class SearchQuery(val query: String) : NearbyPanelEvent()
    data class FriendClick(val friendId: String) : NearbyPanelEvent()
    data class Navigate(val friendId: String) : NearbyPanelEvent()
    data class Ping(val friendId: String) : NearbyPanelEvent()
    data class StopSharing(val friendId: String) : NearbyPanelEvent()
    data class Message(val friendId: String) : NearbyPanelEvent()
    object DismissBottomSheet : NearbyPanelEvent()
    object RefreshFriends : NearbyPanelEvent()
    object ClearError : NearbyPanelEvent()
    object ClearFeedback : NearbyPanelEvent()
    object DismissSnackbar : NearbyPanelEvent()
    // Performance optimization events
    data class UpdateScrollPosition(val position: Int) : NearbyPanelEvent()
    object PreserveState : NearbyPanelEvent()
    object InviteFriends : NearbyPanelEvent()
}