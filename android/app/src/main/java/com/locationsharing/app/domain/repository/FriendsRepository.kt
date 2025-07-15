package com.locationsharing.app.domain.repository

import com.locationsharing.app.data.models.Friend
import com.locationsharing.app.data.models.FriendRequest

interface FriendsRepository {
    suspend fun getFriends(): Result<List<Friend>>
    suspend fun sendFriendRequest(email: String): Result<Unit>
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>
    suspend fun removeFriend(friendId: String): Result<Unit>
    suspend fun updateLocationSharingPermission(friendId: String, enabled: Boolean): Result<Unit>
    suspend fun getFriendRequests(): Result<List<FriendRequest>>
    suspend fun searchUserByEmail(email: String): Result<com.locationsharing.app.data.models.User?>
}