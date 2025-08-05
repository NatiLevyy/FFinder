package com.locationsharing.app.data.friends.enhanced

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus


/**
 * Enhanced Friend data model with brand colors, status, and location accuracy
 * Extends the base Friend model with flagship-quality features
 */
data class EnhancedFriend(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val brandColor: BrandColor,
    val status: EnhancedFriendStatus,
    val location: EnhancedLocation?,
    val lastSeen: Long?,
    val accuracy: LocationAccuracy?,
    val isMoving: Boolean,
    val movementSpeed: Float?,
    val preferences: FriendPreferences = FriendPreferences()
) {
    /**
     * Convert to LatLng for map display
     */
    fun getLatLng(): LatLng? {
        return location?.let { LatLng(it.latitude, it.longitude) }
    }
    
    /**
     * Check if friend is currently online (last seen within 5 minutes)
     */
    fun isOnline(): Boolean {
        return status == EnhancedFriendStatus.ONLINE
    }
    
    /**
     * Get relative time string for last seen
     */
    fun getLastSeenText(): String {
        return lastSeen?.let { lastSeenMillis ->
            val now = System.currentTimeMillis()
            val diffMillis = now - lastSeenMillis
            
            when {
                diffMillis < 60 * 1000 -> "Just now"
                diffMillis < 60 * 60 * 1000 -> "${diffMillis / (60 * 1000)} min ago"
                diffMillis < 24 * 60 * 60 * 1000 -> "${diffMillis / (60 * 60 * 1000)} hours ago"
                diffMillis < 7 * 24 * 60 * 60 * 1000 -> "${diffMillis / (24 * 60 * 60 * 1000)} days ago"
                else -> "Over a week ago"
            }
        } ?: "Unknown"
    }
    
    /**
     * Get status text for accessibility
     */
    fun getStatusAccessibilityText(): String {
        return when (status) {
            EnhancedFriendStatus.ONLINE -> {
                if (isMoving) "Online and moving" else "Online"
            }
            EnhancedFriendStatus.OFFLINE -> "Offline, last seen ${getLastSeenText()}"
            EnhancedFriendStatus.MOVING -> "Moving"
            EnhancedFriendStatus.STATIONARY -> "Stationary"
            EnhancedFriendStatus.UNKNOWN -> "Status unknown"
        }
    }
    
    companion object {
        /**
         * Convert from base Friend model to EnhancedFriend
         */
        fun fromFriend(friend: Friend): EnhancedFriend {
            return EnhancedFriend(
                id = friend.id,
                name = friend.name,
                avatarUrl = friend.avatarUrl.takeIf { it.isNotBlank() },
                brandColor = BrandColor.fromHex(friend.profileColor),
                status = when {
                    friend.isOnline() && friend.isMoving() -> EnhancedFriendStatus.MOVING
                    friend.isOnline() -> EnhancedFriendStatus.ONLINE
                    else -> EnhancedFriendStatus.OFFLINE
                },
                location = friend.location?.let { EnhancedLocation.fromFriendLocation(it) },
                lastSeen = if (friend.status.lastSeen > 0) {
                    friend.status.lastSeen
                } else null,
                accuracy = friend.location?.let { LocationAccuracy.fromAccuracyMeters(it.accuracy) },
                isMoving = friend.isMoving(),
                movementSpeed = friend.location?.speed,
                preferences = FriendPreferences()
            )
        }
    }
}

/**
 * Enhanced friend status with rich state information
 */
sealed class EnhancedFriendStatus {
    object ONLINE : EnhancedFriendStatus()
    object OFFLINE : EnhancedFriendStatus()
    object MOVING : EnhancedFriendStatus()
    object STATIONARY : EnhancedFriendStatus()
    object UNKNOWN : EnhancedFriendStatus()
    
    fun getStatusColor(): Color {
        return when (this) {
            ONLINE -> Color(0xFF4CAF50)      // Green
            OFFLINE -> Color(0xFF9E9E9E)     // Gray
            MOVING -> Color(0xFFFF9800)      // Orange
            STATIONARY -> Color(0xFF2196F3)  // Blue
            UNKNOWN -> Color(0xFF757575)     // Dark Gray
        }
    }
    
    fun getDisplayText(): String {
        return when (this) {
            ONLINE -> "Online"
            OFFLINE -> "Offline"
            MOVING -> "Moving"
            STATIONARY -> "Stationary"
            UNKNOWN -> "Unknown"
        }
    }
}

/**
 * Brand color system for friends with gradient support
 */
data class BrandColor(
    val name: String,
    val hex: String,
    val gradientColors: List<String>
) {
    /**
     * Get primary color as Compose Color
     */
    fun getPrimaryColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color(0xFF2196F3) // Default blue
        }
    }
    
    /**
     * Get gradient colors as Compose Colors
     */
    fun getGradientColorsAsCompose(): List<Color> {
        return gradientColors.mapNotNull { colorHex ->
            try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                null
            }
        }.takeIf { it.isNotEmpty() } ?: listOf(getPrimaryColor())
    }
    
    companion object {
        /**
         * Predefined brand colors for friends
         */
        val BRAND_COLORS = listOf(
            BrandColor("Ocean Blue", "#2196F3", listOf("#2196F3", "#1976D2")),
            BrandColor("Emerald Green", "#4CAF50", listOf("#4CAF50", "#388E3C")),
            BrandColor("Sunset Orange", "#FF9800", listOf("#FF9800", "#F57C00")),
            BrandColor("Royal Purple", "#9C27B0", listOf("#9C27B0", "#7B1FA2")),
            BrandColor("Cherry Red", "#F44336", listOf("#F44336", "#D32F2F")),
            BrandColor("Golden Yellow", "#FFC107", listOf("#FFC107", "#F57F17")),
            BrandColor("Teal", "#009688", listOf("#009688", "#00695C")),
            BrandColor("Deep Pink", "#E91E63", listOf("#E91E63", "#C2185B")),
            BrandColor("Indigo", "#3F51B5", listOf("#3F51B5", "#303F9F")),
            BrandColor("Lime Green", "#8BC34A", listOf("#8BC34A", "#689F38"))
        )
        
        /**
         * Get brand color from hex string
         */
        fun fromHex(hex: String): BrandColor {
            return BRAND_COLORS.find { it.hex.equals(hex, ignoreCase = true) }
                ?: BrandColor("Custom", hex, listOf(hex))
        }
        
        /**
         * Get random brand color
         */
        fun random(): BrandColor {
            return BRAND_COLORS.random()
        }
    }
}

/**
 * Enhanced location with accuracy levels
 */
data class EnhancedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double?,
    val bearing: Float?,
    val speed: Float?,
    val address: String?,
    val timestamp: Long
) {
    companion object {
        fun fromFriendLocation(location: FriendLocation): EnhancedLocation {
            return EnhancedLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                altitude = location.altitude,
                bearing = location.bearing,
                speed = location.speed,
                address = location.address,
                timestamp = location.timestamp?.time ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Location accuracy levels with visual indicators
 */
data class LocationAccuracy(
    val level: AccuracyLevel,
    val meters: Float
) {
    fun getAccuracyText(): String {
        return when (level) {
            AccuracyLevel.HIGH -> "High accuracy location"
            AccuracyLevel.MEDIUM -> "Medium accuracy location"
            AccuracyLevel.LOW -> "Low accuracy location"
            AccuracyLevel.UNKNOWN -> "Location accuracy unknown"
        }
    }
    
    fun getAccuracyColor(): Color {
        return when (level) {
            AccuracyLevel.HIGH -> Color(0xFF4CAF50)      // Green
            AccuracyLevel.MEDIUM -> Color(0xFFFF9800)    // Orange
            AccuracyLevel.LOW -> Color(0xFFF44336)       // Red
            AccuracyLevel.UNKNOWN -> Color(0xFF9E9E9E)   // Gray
        }
    }
    
    companion object {
        fun fromAccuracyMeters(meters: Float): LocationAccuracy {
            val level = when {
                meters <= 10f -> AccuracyLevel.HIGH
                meters <= 50f -> AccuracyLevel.MEDIUM
                meters <= 100f -> AccuracyLevel.LOW
                else -> AccuracyLevel.UNKNOWN
            }
            return LocationAccuracy(level, meters)
        }
    }
}

enum class AccuracyLevel {
    HIGH, MEDIUM, LOW, UNKNOWN
}

/**
 * Friend preferences for enhanced features
 */
data class FriendPreferences(
    val shareLocation: Boolean = true,
    val shareMovementStatus: Boolean = true,
    val shareLastSeen: Boolean = true,
    val allowNotifications: Boolean = true,
    val showAccuracy: Boolean = true
)