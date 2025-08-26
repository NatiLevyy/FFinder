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
    val location: Location? = null,
    val smartRankingScore: Float = 0f // Smart ranking score for sorting
) : Comparable<NearbyFriend> {
    
    // Distance in meters property for easy access
    val distanceMeters: Double get() = distance
    
    // Proximity bucket classification
    enum class ProximityBucket {
        VERY_CLOSE, // < 300m
        NEARBY,     // 300m-2km
        IN_TOWN     // 2-10km
    }
    
    // Get proximity bucket for this friend
    val proximityBucket: ProximityBucket
        get() = when {
            distance < 300.0 -> ProximityBucket.VERY_CLOSE
            distance < 2000.0 -> ProximityBucket.NEARBY
            else -> ProximityBucket.IN_TOWN
        }
    
    // Implement Comparable for sorting by smart ranking score (ascending = higher priority)
    override fun compareTo(other: NearbyFriend): Int {
        return smartRankingScore.compareTo(other.smartRankingScore)
    }
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
         * Creates a NearbyFriend from a Friend with calculated distance and smart ranking
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
            
            // Calculate smart ranking score: score = proximity*0.5f + timeSinceSeen*0.3f + statusWeight*0.2f
            val smartRankingScore = calculateSmartRankingScore(
                distance = distance,
                isOnline = friend.isOnline(),
                lastSeen = friend.status.lastSeen
            )
            
            return NearbyFriend(
                id = friend.id,
                displayName = friend.name,
                avatarUrl = friend.avatarUrl.takeIf { it.isNotBlank() },
                distance = distance,
                isOnline = friend.isOnline(),
                lastUpdated = friend.status.lastSeen,
                latLng = friend.getLatLng() ?: LatLng(0.0, 0.0),
                location = friendLocation,
                smartRankingScore = smartRankingScore
            )
        }
        
        /**
         * Calculate smart ranking score for prioritizing friends
         * Lower score = higher priority in list
         * Formula: score = proximity*0.5f + timeSinceSeen*0.3f + statusWeight*0.2f
         */
        private fun calculateSmartRankingScore(
            distance: Double,
            isOnline: Boolean,
            lastSeen: Long
        ): Float {
            // Proximity component (0-1, normalized by 10km max)
            val proximityScore = (distance / 10000.0).coerceAtMost(1.0).toFloat()
            
            // Time since seen component (0-1, normalized by 24 hours)
            val currentTime = System.currentTimeMillis()
            val timeSinceSeen = currentTime - lastSeen
            val maxTimeThreshold = 24 * 60 * 60 * 1000L // 24 hours in ms
            val timeScore = (timeSinceSeen.toDouble() / maxTimeThreshold).coerceAtMost(1.0).toFloat()
            
            // Status component (online = 0, offline = 1)
            val statusScore = if (isOnline) 0f else 1f
            
            // Calculate weighted score (lower = higher priority)
            return (proximityScore * 0.5f) + (timeScore * 0.3f) + (statusScore * 0.2f)
        }
    }
}