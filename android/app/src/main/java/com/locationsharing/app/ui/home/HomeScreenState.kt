package com.locationsharing.app.ui.home

import com.google.android.gms.maps.model.LatLng

/**
 * State data class for the FFinder Home Screen.
 * 
 * Enhanced with navigation state management for proper button responsiveness
 * and loading states. Manages the state of all components on the home screen
 * including loading states, permissions, location data, and UI visibility states.
 * 
 * @property isLoading Whether the home screen is in a loading state
 * @property hasLocationPermission Whether location permission has been granted
 * @property isLocationSharing Whether location sharing is currently active
 * @property showWhatsNewDialog Whether the "What's New" dialog should be displayed
 * @property mapPreviewLocation Current location for the map preview
 * @property animationsEnabled Whether animations should be played (respects accessibility preferences)
 * @property isNarrowScreen Whether the screen width is narrow (< 360dp) for responsive design
 * @property mapLoadError Whether there was an error loading the map preview
 * @property locationError Error message if location services failed
 * @property isNavigating Whether any navigation operation is in progress
 * @property isNavigatingToMap Whether navigation to map screen is in progress
 * @property isNavigatingToFriends Whether navigation to friends screen is in progress
 * @property isNavigatingToSettings Whether navigation to settings screen is in progress
 */
data class HomeScreenState(
    val isLoading: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val isLocationSharing: Boolean = false,
    val isWaitingForLocationFix: Boolean = false,
    val showWhatsNewDialog: Boolean = false,
    val mapPreviewLocation: LatLng? = null,
    val animationsEnabled: Boolean = true,
    val isNarrowScreen: Boolean = false,
    val mapLoadError: Boolean = false,
    val locationError: String? = null,
    val isNavigating: Boolean = false,
    val isNavigatingToMap: Boolean = false,
    val isNavigatingToFriends: Boolean = false,
    val isNavigatingToSettings: Boolean = false
)