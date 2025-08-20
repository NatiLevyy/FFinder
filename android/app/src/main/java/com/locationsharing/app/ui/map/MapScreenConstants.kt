package com.locationsharing.app.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Constants and specifications for the MapScreen redesign
 * Contains all the design specifications from the requirements document
 */
object MapScreenConstants {
    
    /**
     * Color specifications from design document - DEPRECATED
     * Use MaterialTheme.colorScheme instead for proper theming support
     */
    @Deprecated("Use MaterialTheme.colorScheme instead for proper theming support")
    object Colors {
        @Deprecated("Use MaterialTheme.colorScheme.primary instead")
        val PRIMARY_GREEN = Color(0xFF2E7D32)      // Brand green
        @Deprecated("Use MaterialTheme.colorScheme.secondary instead")
        val SECONDARY_PURPLE = Color(0xFF6B4F8F)   // Brand purple
        @Deprecated("Use MaterialTheme.colorScheme.surface instead")
        val SURFACE_WHITE = Color.White            // Surface color
        @Deprecated("Use MaterialTheme.colorScheme.background instead")
        val BACKGROUND_GRAY = Color(0xFFF1F1F1)    // Background color
        @Deprecated("Use MaterialTheme.colorScheme.onSurface instead")
        val ON_SURFACE_BLACK = Color(0xFF212121)   // Text on surface
    }
    
    /**
     * Component dimensions
     */
    object Dimensions {
        // FAB specifications
        val FAB_SIZE = 56.dp
        val FAB_MARGIN = 16.dp
        val FAB_ELEVATION = 6.dp
        val FAB_PRESSED_ELEVATION = 8.dp
        
        // AppBar specifications
        val APP_BAR_HEIGHT = 56.dp
        val APP_BAR_ELEVATION = 0.dp
        
        // Drawer specifications
        val DRAWER_WIDTH = 280.dp
        val DRAWER_ELEVATION = 16.dp
        
        // Sheet specifications
        val SHEET_CORNER_RADIUS = 16.dp
        val SHEET_ELEVATION = 8.dp
        
        // Standard spacing
        val STANDARD_PADDING = 16.dp
        val LARGE_PADDING = 20.dp
        val SMALL_PADDING = 8.dp
    }
    
    /**
     * Animation specifications
     */
    object Animations {
        // Duration constants
        const val QUICK_DURATION = 150
        const val STANDARD_DURATION = 300
        const val EMPHASIZED_DURATION = 500
        const val LOCATION_PULSE_DURATION = 3000
        
        // Scale values for FAB press animation
        const val FAB_NORMAL_SCALE = 1.0f
        const val FAB_PRESSED_SCALE = 0.9f
        
        // Location marker pulse animation
        const val MARKER_NORMAL_SCALE = 1.0f
        const val MARKER_PULSE_SCALE = 1.2f
        
        // Drawer animation
        const val DRAWER_CLOSED_OFFSET = 280f
        const val DRAWER_OPEN_OFFSET = 0f
        
        // Sheet animation
        const val SHEET_HIDDEN_ALPHA = 0f
        const val SHEET_VISIBLE_ALPHA = 1f
    }
    
    /**
     * Icon specifications
     */
    object Icons {
        const val ICON_SIZE = 24 // dp
        const val LARGE_ICON_SIZE = 32 // dp
        
        // Icon resource names (to be created)
        const val IC_PIN_FINDER = "ic_pin_finder"
        const val IC_FLASK = "ic_flask"
        const val IC_LOCATION = "ic_location"
        const val IC_PEOPLE = "ic_people"
        const val IC_ARROW_BACK = "ic_arrow_back"
    }
    
    /**
     * Accessibility specifications
     */
    object Accessibility {
        // Content descriptions for interactive elements
        const val BACK_BUTTON_DESC = "Navigate back"
        const val NEARBY_FRIENDS_DESC = "View nearby friends"
        const val QUICK_SHARE_FAB_DESC = "Share your location instantly"
        const val SELF_LOCATION_FAB_DESC = "Center map on your location"
        const val DEBUG_FAB_DESC = "Add test friends to map"
        const val MAP_CONTENT_DESC = "Map showing your location and nearby friends"
        const val DRAWER_CONTENT_DESC = "Nearby friends navigation drawer"
        const val STATUS_SHEET_DESC = "Location sharing status dialog"
        const val SEARCH_FIELD_DESC = "Search nearby friends"
        const val FRIEND_LIST_DESC = "List of nearby friends"
        const val STOP_SHARING_BUTTON_DESC = "Stop location sharing"
        
        // State descriptions for screen readers
        const val LOCATION_SHARING_ACTIVE = "Location sharing is active"
        const val LOCATION_SHARING_INACTIVE = "Location sharing is off"
        const val LOCATION_LOADING = "Loading your location..."
        const val LOCATION_ERROR = "Location unavailable"
        const val DRAWER_OPEN = "Nearby friends drawer is open"
        const val DRAWER_CLOSED = "Nearby friends drawer is closed"
        const val SHEET_VISIBLE = "Status sheet is visible"
        const val SHEET_HIDDEN = "Status sheet is hidden"
        
        // Permission-related descriptions
        const val LOCATION_PERMISSION_REQUIRED = "Request location permission to center map"
        const val LOCATION_PERMISSION_DENIED = "Location permission denied"
        const val BACKGROUND_LOCATION_REQUIRED = "Background location permission required for continuous sharing"
        
        // Loading state descriptions
        const val CENTERING_MAP = "Centering map on your location..."
        const val SHARING_LOCATION = "Sharing your location..."
        const val LOADING_FRIENDS = "Loading nearby friends..."
        
        // Dynamic content descriptions
        fun nearbyFriendsWithCount(count: Int) = when (count) {
            0 -> "View nearby friends"
            1 -> "View nearby friends, 1 friend available"
            else -> "View nearby friends, $count friends available"
        }
        
        fun locationSharingStatus(isActive: Boolean) = if (isActive) {
            LOCATION_SHARING_ACTIVE
        } else {
            LOCATION_SHARING_INACTIVE
        }
        
        fun locationCoordinates(lat: Double, lng: Double) = 
            "Current coordinates: Lat: ${"%.6f".format(lat)}\nLng: ${"%.6f".format(lng)}"
        
        fun friendDistance(name: String, distance: Double): String {
            val distanceText = when {
                distance < 100 -> "very close"
                distance < 500 -> "nearby"
                distance < 1000 -> "${distance.toInt()} meters away"
                else -> "${"%.1f".format(distance / 1000)} kilometers away"
            }
            return "$name is $distanceText"
        }
        
        fun friendListCount(count: Int) = when (count) {
            0 -> "No nearby friends"
            1 -> "List of 1 nearby friend"
            else -> "List of $count nearby friends"
        }
        
        // Semantic roles for screen readers
        const val ROLE_HEADER = "header"
        const val ROLE_BUTTON = "button"
        const val ROLE_NAVIGATION = "navigation"
        const val ROLE_DIALOG = "dialog"
        const val ROLE_LIST = "list"
        const val ROLE_LISTITEM = "listitem"
        const val ROLE_SEARCH = "search"
        const val ROLE_IMAGE = "image"
        
        // Live region announcements
        const val ANNOUNCE_LOCATION_SHARING_STARTED = "Location sharing started"
        const val ANNOUNCE_LOCATION_SHARING_STOPPED = "Location sharing stopped"
        const val ANNOUNCE_FRIENDS_UPDATED = "Nearby friends list updated"
        const val ANNOUNCE_LOCATION_UPDATED = "Your location updated"
        const val ANNOUNCE_DRAWER_OPENED = "Nearby friends drawer opened"
        const val ANNOUNCE_DRAWER_CLOSED = "Nearby friends drawer closed"
        const val ANNOUNCE_STATUS_SHEET_OPENED = "Location status sheet opened"
        const val ANNOUNCE_STATUS_SHEET_CLOSED = "Location status sheet closed"
        
        // Focus traversal order
        const val FOCUS_ORDER_BACK_BUTTON = 1
        const val FOCUS_ORDER_APP_TITLE = 2
        const val FOCUS_ORDER_NEARBY_FRIENDS = 3
        const val FOCUS_ORDER_MAP_CONTENT = 4
        const val FOCUS_ORDER_SELF_LOCATION_FAB = 5
        const val FOCUS_ORDER_QUICK_SHARE_FAB = 6
        const val FOCUS_ORDER_DEBUG_FAB = 7
        const val FOCUS_ORDER_DRAWER_CONTENT = 8
        const val FOCUS_ORDER_STATUS_SHEET = 9
    }
    
    /**
     * Layout specifications
     */
    object Layout {
        // FAB positioning
        val QUICK_SHARE_FAB_BOTTOM_MARGIN = 16.dp
        val QUICK_SHARE_FAB_END_MARGIN = 16.dp
        val SELF_LOCATION_FAB_BOTTOM_MARGIN = 88.dp // Above quick share FAB
        val SELF_LOCATION_FAB_END_MARGIN = 16.dp
        val DEBUG_FAB_BOTTOM_MARGIN = 16.dp
        val DEBUG_FAB_START_MARGIN = 16.dp
        
        // Scrim opacity
        const val DRAWER_SCRIM_OPACITY = 0.5f
        const val SHEET_SCRIM_OPACITY = 0.3f
    }
    
    /**
     * Map specifications
     */
    object Map {
        // Default zoom levels
        const val DEFAULT_ZOOM = 15f
        const val CLOSE_ZOOM = 18f
        const val FAR_ZOOM = 12f
        
        // Animation durations for map operations
        const val CAMERA_ANIMATION_DURATION = 500
        const val MARKER_ANIMATION_DURATION = 300
        
        // Marker clustering
        const val CLUSTER_THRESHOLD = 10
        const val CLUSTER_ZOOM_THRESHOLD = 14f
    }
    
    /**
     * Debug specifications
     */
    object Debug {
        // Debug colors - DEPRECATED: Use MaterialTheme.colorScheme instead
        @Deprecated("Use MaterialTheme.colorScheme.secondary instead")
        val DEBUG_FAB_COLOR = Color(0xFF6B4F8F) // Purple - use MaterialTheme.colorScheme.secondary
        @Deprecated("Use MaterialTheme.colorScheme.error instead")
        val DEBUG_MARKER_COLOR = Color(0xFFFF5722) // Orange - use MaterialTheme.colorScheme.error
        
        // Debug constants
        const val TEST_FRIENDS_COUNT = 5
        const val DEBUG_SNACKBAR_DURATION = 3000
    }
    
    /**
     * Performance specifications
     */
    object Performance {
        // Frame rate targets
        const val TARGET_FPS = 60
        const val MIN_ACCEPTABLE_FPS = 30
        
        // Memory thresholds
        const val MAX_MARKER_COUNT = 100
        const val MARKER_CACHE_SIZE = 50
        
        // Update intervals
        const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        const val FRIEND_UPDATE_INTERVAL = 10000L // 10 seconds
    }
    
    /**
     * Typography specifications
     */
    object Typography {
        // Text sizes (will use MaterialTheme.typography)
        const val APP_BAR_TITLE = "titleLarge"
        const val FRIEND_NAME = "bodyLarge"
        const val STATUS_TEXT = "bodyMedium"
        const val BUTTON_TEXT = "labelLarge"
        const val CAPTION_TEXT = "bodySmall"
    }
    
    /**
     * State management
     */
    object States {
        // Loading states
        const val LOADING_TIMEOUT = 30000L // 30 seconds
        const val RETRY_DELAY = 2000L // 2 seconds
        const val MAX_RETRY_ATTEMPTS = 3
        
        // Permission states
        const val PERMISSION_REQUEST_CODE = 1001
        const val BACKGROUND_PERMISSION_REQUEST_CODE = 1002
    }
}