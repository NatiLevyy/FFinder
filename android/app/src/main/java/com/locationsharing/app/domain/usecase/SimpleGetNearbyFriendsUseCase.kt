package com.locationsharing.app.domain.usecase

import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.domain.model.NearbyFriend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Simple implementation of GetNearbyFriendsUseCase for testing/demo purposes
 * Returns empty list to prevent crashes when dependency injection is not available
 */
class SimpleGetNearbyFriendsUseCase(
    private val friendsRepository: FriendsRepository
) {
    
    operator fun invoke(): Flow<List<NearbyFriend>> {
        return flowOf(emptyList())
    }
}