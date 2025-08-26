package com.locationsharing.app.data.friends

import com.locationsharing.app.domain.model.FriendRequestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Simple implementation of FriendsRepository for testing/demo purposes
 * Returns empty data to prevent crashes when dependency injection is not available
 */
class SimpleFriendsRepository : FriendsRepository {
    
    override fun getFriends(): Flow<List<Friend>> = flowOf(emptyList())
    
    override fun getOnlineFriends(): Flow<List<Friend>> = flowOf(emptyList())
    
    override fun getFriendById(friendId: String): Flow<Friend?> = flowOf(null)
    
    override fun getFriendRequests(): Flow<List<FriendRequest>> = flowOf(emptyList())
    
    override fun getLocationUpdates(): Flow<List<LocationUpdateEvent>> = flowOf(emptyList())
    
    override suspend fun searchFriends(query: String): Flow<androidx.paging.PagingData<Friend>> {
        return androidx.paging.Pager(
            config = androidx.paging.PagingConfig(pageSize = 30),
            pagingSourceFactory = { 
                object : androidx.paging.PagingSource<Int, Friend>() {
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Friend> {
                        return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
                    }
                    override fun getRefreshKey(state: androidx.paging.PagingState<Int, Friend>): Int? = null
                }
            }
        ).flow
    }
    
    override fun getFriendActivities(): Flow<List<FriendActivityEvent>> = flowOf(emptyList())
    
    override suspend fun sendFriendRequest(toUserId: String, message: String?): Result<String> {
        return Result.success("mock_request_id")
    }
    
    override suspend fun checkFriendRequestStatus(toUserId: String): FriendRequestStatus {
        return FriendRequestStatus.NONE
    }
    
    override suspend fun cancelFriendRequest(toUserId: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun declineFriendRequest(requestId: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun removeFriend(friendId: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun blockFriend(friendId: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun updateLocationSharing(enabled: Boolean): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun updatePrivacySettings(preferences: FriendPreferences): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun updateOnlineStatus(isOnline: Boolean): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun startLocationSharing(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun stopLocationSharing(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun sendPing(friendId: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun stopReceivingLocation(friendId: String): Result<Unit> {
        return Result.success(Unit)
    }
}