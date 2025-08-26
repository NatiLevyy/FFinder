package com.locationsharing.app.data.friends

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Simple implementation of RealTimeFriendsService for testing/demo purposes
 * Provides basic state management without Firebase integration
 */
class SimpleRealTimeFriendsService {
    
    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _friendsState = MutableStateFlow<Map<String, FriendRealTimeState>>(emptyMap())
    val friendsState: StateFlow<Map<String, FriendRealTimeState>> = _friendsState.asStateFlow()
    
    fun startSync() {
        _connectionState.value = ConnectionState.CONNECTED
    }
    
    fun stopSync() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    fun getFriendUpdatesWithAnimations(): Flow<List<FriendUpdateWithAnimation>> {
        return flowOf(emptyList())
    }
    
    fun updateFriendLocation(friendId: String, location: FriendLocation) {
        // No-op for simple implementation
    }
    
    fun updateFriendStatus(friendId: String, status: FriendStatus) {
        // No-op for simple implementation
    }
}