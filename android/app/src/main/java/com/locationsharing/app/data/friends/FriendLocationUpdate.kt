package com.locationsharing.app.data.friends

import com.google.android.gms.maps.model.LatLng

/**
 * Represents a real-time location update for a friend
 */
data class FriendLocationUpdate(
    val friendId: String,
    val previousLocation: LatLng?,
    val newLocation: LatLng,
    val timestamp: Long,
    val isOnline: Boolean,
    val updateType: LocationUpdateType
)

enum class LocationUpdateType {
    INITIAL_LOAD,
    POSITION_CHANGE,
    STATUS_CHANGE,
    FRIEND_APPEARED,
    FRIEND_DISAPPEARED
}

/**
 * Animation metadata for location updates
 */
data class LocationUpdateAnimation(
    val friendId: String,
    val animationType: LocationAnimationType,
    val duration: Long = 1000L,
    val shouldShowTrail: Boolean = false
)

enum class LocationAnimationType {
    SMOOTH_MOVE,
    BOUNCE_IN,
    FADE_OUT,
    PULSE_UPDATE,
    STATUS_CHANGE
}

