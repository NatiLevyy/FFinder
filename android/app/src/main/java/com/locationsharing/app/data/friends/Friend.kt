package com.locationsharing.app.data.friends

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Real friend data model for Firebase integration
 * Represents a friend with location sharing capabilities
 */
data class Friend(
    val id: String = "",
    val userId: String = "", // Firebase Auth user ID
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val profileColor: String = "#2196F3", // Default blue
    val location: FriendLocation? = null,
    val status: FriendStatus = FriendStatus(),
    val preferences: FriendPreferences = FriendPreferences(),
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    /**
     * Convert to LatLng for map display
     */
    fun getLatLng(): LatLng? {
        return location?.let { LatLng(it.latitude, it.longitude) }
    }
    
    /**
     * Get display color as Android Color
     */
    fun getDisplayColor(): androidx.compose.ui.graphics.Color {
        return try {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(profileColor))
        } catch (e: Exception) {
            androidx.compose.ui.graphics.Color(0xFF2196F3) // Default blue
        }
    }
    
    /**
     * Check if friend is currently online (last seen within 5 minutes)
     */
    fun isOnline(): Boolean {
        return status.isOnline && 
               (System.currentTimeMillis() - status.lastSeen) < 5 * 60 * 1000
    }
    
    /**
     * Check if friend is currently moving
     */
    fun isMoving(): Boolean {
        return location?.isMoving == true && isOnline()
    }
    
    /**
     * Get status text for display
     */
    fun getStatusText(): String {
        return when {
            isOnline() -> "Online now"
            status.lastSeen > 0 -> {
                val timeDiff = System.currentTimeMillis() - status.lastSeen
                when {
                    timeDiff < 60 * 1000 -> "Just now"
                    timeDiff < 60 * 60 * 1000 -> "${timeDiff / (60 * 1000)} min ago"
                    timeDiff < 24 * 60 * 60 * 1000 -> "${timeDiff / (60 * 60 * 1000)} hours ago"
                    else -> "${timeDiff / (24 * 60 * 60 * 1000)} days ago"
                }
            }
            else -> "Offline"
        }
    }
    
    companion object {
        /**
         * Create Friend from Firestore DocumentSnapshot
         */
        fun fromDocument(document: DocumentSnapshot): Friend? {
            return try {
                val data = document.data ?: return null
                
                Friend(
                    id = document.id,
                    userId = data["userId"] as? String ?: "",
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    avatarUrl = data["avatarUrl"] as? String ?: "",
                    profileColor = data["profileColor"] as? String ?: "#2196F3",
                    location = (data["location"] as? Map<String, Any>)?.let { locationData ->
                        FriendLocation.fromMap(locationData)
                    },
                    status = (data["status"] as? Map<String, Any>)?.let { statusData ->
                        FriendStatus.fromMap(statusData)
                    } ?: FriendStatus(),
                    preferences = (data["preferences"] as? Map<String, Any>)?.let { prefsData ->
                        FriendPreferences.fromMap(prefsData)
                    } ?: FriendPreferences(),
                    createdAt = data["createdAt"] as? Date,
                    updatedAt = data["updatedAt"] as? Date
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Friend location data with movement tracking
 */
data class FriendLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val isMoving: Boolean = false,
    val address: String? = null,
    @ServerTimestamp
    val timestamp: Date? = null
) {
    /**
     * Convert to GeoPoint for Firestore
     */
    fun toGeoPoint(): GeoPoint {
        return GeoPoint(latitude, longitude)
    }
    
    /**
     * Calculate distance to another location in meters
     */
    fun distanceTo(other: FriendLocation): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            latitude, longitude,
            other.latitude, other.longitude,
            results
        )
        return results[0]
    }
    
    companion object {
        fun fromMap(data: Map<String, Any>): FriendLocation {
            return FriendLocation(
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                accuracy = (data["accuracy"] as? Number)?.toFloat() ?: 0f,
                altitude = (data["altitude"] as? Number)?.toDouble(),
                bearing = (data["bearing"] as? Number)?.toFloat(),
                speed = (data["speed"] as? Number)?.toFloat(),
                isMoving = data["isMoving"] as? Boolean ?: false,
                address = data["address"] as? String,
                timestamp = data["timestamp"] as? Date
            )
        }
        
        fun fromGeoPoint(geoPoint: GeoPoint): FriendLocation {
            return FriendLocation(
                latitude = geoPoint.latitude,
                longitude = geoPoint.longitude,
                timestamp = Date()
            )
        }
    }
}

/**
 * Friend status information
 */
data class FriendStatus(
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val isLocationSharingEnabled: Boolean = false,
    val batteryOptimized: Boolean = false,
    val deviceInfo: String? = null
) {
    companion object {
        fun fromMap(data: Map<String, Any>): FriendStatus {
            return FriendStatus(
                isOnline = data["isOnline"] as? Boolean ?: false,
                lastSeen = (data["lastSeen"] as? Number)?.toLong() ?: 0L,
                isLocationSharingEnabled = data["isLocationSharingEnabled"] as? Boolean ?: false,
                batteryOptimized = data["batteryOptimized"] as? Boolean ?: false,
                deviceInfo = data["deviceInfo"] as? String
            )
        }
    }
}

/**
 * Friend preferences and privacy settings
 */
data class FriendPreferences(
    val shareLocation: Boolean = true,
    val shareMovementStatus: Boolean = true,
    val shareLastSeen: Boolean = true,
    val allowNotifications: Boolean = true,
    val privacyLevel: PrivacyLevel = PrivacyLevel.FRIENDS
) {
    companion object {
        fun fromMap(data: Map<String, Any>): FriendPreferences {
            return FriendPreferences(
                shareLocation = data["shareLocation"] as? Boolean ?: true,
                shareMovementStatus = data["shareMovementStatus"] as? Boolean ?: true,
                shareLastSeen = data["shareLastSeen"] as? Boolean ?: true,
                allowNotifications = data["allowNotifications"] as? Boolean ?: true,
                privacyLevel = PrivacyLevel.valueOf(
                    data["privacyLevel"] as? String ?: "FRIENDS"
                )
            )
        }
    }
}

/**
 * Privacy levels for location sharing
 */
enum class PrivacyLevel {
    PUBLIC,     // Visible to all users
    FRIENDS,    // Visible to friends only
    CLOSE,      // Visible to close friends only
    PRIVATE     // Not visible to anyone
}

/**
 * Friend relationship status
 */
enum class FriendshipStatus {
    PENDING,    // Friend request sent/received
    ACCEPTED,   // Friends
    BLOCKED,    // User blocked
    DECLINED    // Friend request declined
}

/**
 * Friend request data model
 */
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromUserName: String = "",
    val fromUserAvatar: String = "",
    val toUserName: String = "",
    val toUserAvatar: String = "",
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val message: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): FriendRequest? {
            return try {
                val data = document.data ?: return null
                
                FriendRequest(
                    id = document.id,
                    fromUserId = data["fromUserId"] as? String ?: "",
                    toUserId = data["toUserId"] as? String ?: "",
                    fromUserName = data["fromUserName"] as? String ?: "",
                    fromUserAvatar = data["fromUserAvatar"] as? String ?: "",
                    toUserName = data["toUserName"] as? String ?: "",
                    toUserAvatar = data["toUserAvatar"] as? String ?: "",
                    status = FriendshipStatus.valueOf(
                        data["status"] as? String ?: "PENDING"
                    ),
                    message = data["message"] as? String,
                    createdAt = data["createdAt"] as? Date,
                    updatedAt = data["updatedAt"] as? Date
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Real-time location update event
 */
data class LocationUpdateEvent(
    val friendId: String,
    val previousLocation: FriendLocation?,
    val newLocation: FriendLocation,
    val updateType: LocationUpdateType,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Friend activity event for real-time updates
 */
data class FriendActivityEvent(
    val friendId: String,
    val activityType: FriendActivityType,
    val data: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class FriendActivityType {
    CAME_ONLINE,
    WENT_OFFLINE,
    LOCATION_UPDATED,
    STARTED_MOVING,
    STOPPED_MOVING,
    PRIVACY_CHANGED,
    PROFILE_UPDATED
}