package com.locationsharing.app.ui.friends.hub

import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendRequest

data class FriendsHubUiState(
    val contactsOnFFinder: List<Friend> = emptyList(),
    val pendingRequests: List<FriendRequest> = emptyList(),
    val isLoading: Boolean = true
)