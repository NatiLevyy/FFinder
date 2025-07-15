package com.locationsharing.app.data.models

data class Friend(
    val id: String,
    val user: User,
    val friendshipStatus: FriendshipStatus,
    val locationSharingEnabled: Boolean,
    val lastKnownLocation: Location? = null,
    val locationSharingPermission: LocationSharingPermission
)

enum class FriendshipStatus {
    PENDING, ACCEPTED, BLOCKED
}

enum class LocationSharingPermission {
    NONE, REQUESTED, GRANTED, DENIED
}