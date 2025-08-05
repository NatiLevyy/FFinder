package com.locationsharing.app.domain.model

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

/**
 * Data class representing a friend with proximity information for the nearby panel.
 * Contains distance calculation and formatting logic as specified in requirements 1.1, 4.5, 4.6, 6.5.
 */
data class NearbyFriend(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val distance: Double, // in meters
    val isOnline: Boolean,
    val lastUpdated: Long, // timestamp
    val latLng: LatLng, // for future live-route features
    val location: Location? = null
) {
    /**
     * Formats distance according to requirements:
     * - < 1000m shows "X m" 
     * - >= 1000m shows "X.X km"
     */
    val formattedDistance: String
        get() = when {
            distance < 1000 -> "${distance.roundToInt()} m"
            else -> "${"%.1f".format(distance / 1000)} km"
        }
    
    /**
     * Calculates distance between two locations using Haversine formula
     * via Android's Location.distanceBetween method
     */
    companion object {
        fun calculateDistance(userLocation: Location, friendLocation: Location): Double {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                friendLocation.latitude,
                friendLocation.longitude,
                results
            )
            return results[0].toDouble()
        }
        
        /**
         * Creates a NearbyFriend from a Friend with calculated distance
         */
        fun fromFriend(
            friend: com.locationsharing.app.data.friends.Friend,
            userLocation: Location?
        ): NearbyFriend? {
            val friendLocation = friend.location?.let { loc ->
                Location("friend").apply {
                    latitude = loc.latitude
                    longitude = loc.longitude
                }
            } ?: return null
            
            val distance = userLocation?.let { 
                calculateDistance(it, friendLocation) 
            } ?: Double.MAX_VALUE
            
            return NearbyFriend(
                id = friend.id,
                displayName = friend.name,
                avatarUrl = friend.avatarUrl.takeIf { it.isNotBlank() },
                distance = distance,
                isOnline = friend.isOnline(),
                lastUpdated = friend.status.lastSeen,
                latLng = friend.getLatLng() ?: LatLng(0.0, 0.0),
                location = friendLocation
            )
        }
    }
}