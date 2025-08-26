package com.locationsharing.app.ui.map

import com.google.android.gms.maps.model.LatLng

/**
 * Sealed class for MapScreen user interactions and events
 * Implements requirements 2.2, 2.3, 7.1, 7.2, 7.3
 */
sealed class MapScreenEvent {
    
    // Navigation events
    object OnBackPressed : MapScreenEvent()
    object OnNearbyFriendsToggle : MapScreenEvent()
    object OnSearchFriendsClick : MapScreenEvent()
    
    // Location events
    object OnSelfLocationCenter : MapScreenEvent()
    object OnLocationPermissionRequested : MapScreenEvent()
    object OnLocationPermissionGranted : MapScreenEvent()
    object OnLocationPermissionDenied : MapScreenEvent()
    data class OnLocationUpdated(val location: LatLng) : MapScreenEvent()
    
    // Location sharing events
    object OnQuickShare : MapScreenEvent()
    object OnStartLocationSharing : MapScreenEvent()
    object OnStopLocationSharing : MapScreenEvent()
    object OnStatusSheetDismiss : MapScreenEvent()
    object OnStatusSheetShow : MapScreenEvent()
    
    // Friend interaction events
    data class OnFriendMarkerClick(val friendId: String) : MapScreenEvent()
    data class OnClusterClick(val friends: List<com.locationsharing.app.data.friends.Friend>) : MapScreenEvent()
    object OnFriendSelectionClear : MapScreenEvent()
    data class OnFriendSearch(val query: String) : MapScreenEvent()
    data class OnFriendSelectedFromSearch(val friendId: String) : MapScreenEvent()
    
    // Map interaction events
    data class OnMapClick(val location: LatLng) : MapScreenEvent()
    data class OnMapLongClick(val location: LatLng) : MapScreenEvent()
    data class OnCameraMove(val center: LatLng, val zoom: Float) : MapScreenEvent()
    
    // Debug events (only available in debug builds)
    object OnDebugAddFriends : MapScreenEvent()
    object OnDebugClearFriends : MapScreenEvent()
    object OnDebugToggleHighAccuracy : MapScreenEvent()
    
    // Error handling events
    object OnRetry : MapScreenEvent()
    object OnErrorDismiss : MapScreenEvent()
    
    // Drawer events
    object OnDrawerOpen : MapScreenEvent()
    object OnDrawerClose : MapScreenEvent()
    object OnDrawerDismiss : MapScreenEvent()
    
    // Performance events
    object OnEnableHighAccuracyMode : MapScreenEvent()
    object OnDisableHighAccuracyMode : MapScreenEvent()
    data class OnBatteryLevelChanged(val level: Int) : MapScreenEvent()
    
    // Lifecycle events
    object OnScreenResume : MapScreenEvent()
    object OnScreenPause : MapScreenEvent()
    object OnScreenDestroy : MapScreenEvent()
    
    // Settings events
    object OnOpenSettings : MapScreenEvent()
    object OnRefreshData : MapScreenEvent()
    
    // Animation events
    object OnAnimationComplete : MapScreenEvent()
    data class OnFABAnimationStart(val fabType: FABType) : MapScreenEvent()
    data class OnFABAnimationEnd(val fabType: FABType) : MapScreenEvent()
}

/**
 * Types of FABs for animation tracking
 */
enum class FABType {
    QUICK_SHARE,
    SELF_LOCATION,
    DEBUG
}

/**
 * Location permission states for handling permission flow
 */
enum class LocationPermissionState {
    NOT_REQUESTED,
    REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

/**
 * Map interaction types for analytics and behavior tracking
 */
enum class MapInteractionType {
    TAP,
    LONG_PRESS,
    DRAG,
    ZOOM,
    ROTATE,
    TILT
}

/**
 * Error types for better error handling and user feedback
 */
enum class MapScreenErrorType {
    LOCATION_PERMISSION_DENIED,
    LOCATION_SERVICE_UNAVAILABLE,
    NETWORK_ERROR,
    FRIENDS_LOAD_ERROR,
    LOCATION_SHARING_ERROR,
    GENERAL_ERROR
}

/**
 * Loading states for different components
 */
enum class LoadingState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR,
    RETRYING
}

/**
 * Event result wrapper for handling async operations
 */
sealed class EventResult<out T> {
    object Loading : EventResult<Nothing>()
    data class Success<T>(val data: T) : EventResult<T>()
    data class Error(val exception: Throwable, val type: MapScreenErrorType) : EventResult<Nothing>()
}

/**
 * Location sharing status for UI feedback
 */
enum class LocationSharingStatus {
    INACTIVE,
    STARTING,
    ACTIVE,
    STOPPING,
    ERROR
}

/**
 * Friend marker states for animation and interaction
 */
enum class FriendMarkerState {
    NORMAL,
    SELECTED,
    HIGHLIGHTED,
    MOVING,
    OFFLINE
}

/**
 * Map camera animation types
 */
enum class CameraAnimationType {
    INSTANT,
    SMOOTH,
    BOUNCE,
    EASE_IN_OUT
}

/**
 * Debug action types for development and testing
 */
enum class DebugActionType {
    ADD_TEST_FRIENDS,
    CLEAR_FRIENDS,
    TOGGLE_HIGH_ACCURACY,
    SIMULATE_MOVEMENT,
    FORCE_ERROR,
    RESET_STATE
}