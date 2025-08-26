package com.locationsharing.app.ui.home.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * LocationPermissionHandler composable for managing location permission requests.
 * 
 * This component handles the complete location permission flow including:
 * - Checking current permission status
 * - Requesting permission when needed
 * - Handling permission grant/denial callbacks
 * - Managing permission state throughout the component lifecycle
 * 
 * Features:
 * - Automatic permission status checking on composition
 * - Clean callback interface for permission results
 * - Proper handling of permission rationale scenarios
 * - Integration with Android's permission request system
 * 
 * @param onPermissionGranted Callback invoked when location permission is granted
 * @param onPermissionDenied Callback invoked when location permission is denied
 * @param onPermissionPermanentlyDenied Callback invoked when permission is permanently denied
 * @param shouldRequestPermission Whether to automatically request permission if not granted
 */
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit = {},
    shouldRequestPermission: Boolean = true
) {
    val context = LocalContext.current
    
    // Track permission request state
    var hasRequestedPermission by remember { mutableStateOf(false) }
    
    // Permission launcher for requesting location permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            // Check if permission was permanently denied
            val shouldShowRationale = androidx.activity.ComponentActivity::class.java
                .isInstance(context) && 
                (context as androidx.activity.ComponentActivity)
                    .shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            
            if (!shouldShowRationale && hasRequestedPermission) {
                // Permission permanently denied
                onPermissionPermanentlyDenied()
            } else {
                // Permission denied but can be requested again
                onPermissionDenied()
            }
        }
    }
    
    // Check permission status on composition and when shouldRequestPermission changes
    LaunchedEffect(shouldRequestPermission) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        when (permissionStatus) {
            PackageManager.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }
            PackageManager.PERMISSION_DENIED -> {
                if (shouldRequestPermission && !hasRequestedPermission) {
                    hasRequestedPermission = true
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    onPermissionDenied()
                }
            }
        }
    }
}

/**
 * Data class representing the current location permission state.
 * 
 * @property isGranted Whether location permission is currently granted
 * @property isDenied Whether location permission was denied
 * @property isPermanentlyDenied Whether location permission was permanently denied
 * @property canRequestPermission Whether permission can be requested (not permanently denied)
 */
data class LocationPermissionState(
    val isGranted: Boolean = false,
    val isDenied: Boolean = false,
    val isPermanentlyDenied: Boolean = false,
    val canRequestPermission: Boolean = true
) {
    /**
     * Whether the permission is in a state where it needs to be requested.
     */
    val needsPermission: Boolean
        get() = !isGranted && !isPermanentlyDenied
    
    /**
     * Whether the app should show permission rationale to the user.
     */
    val shouldShowRationale: Boolean
        get() = isDenied && canRequestPermission && !isPermanentlyDenied
}

/**
 * Composable that provides location permission state management.
 * 
 * This composable tracks the location permission state and provides
 * a convenient way to access permission status throughout the component tree.
 * 
 * @param initialCheck Whether to perform an initial permission check on composition
 * @return LocationPermissionState representing the current permission status
 */
@Composable
fun rememberLocationPermissionState(
    initialCheck: Boolean = true
): LocationPermissionState {
    val context = LocalContext.current
    var permissionState by remember { 
        mutableStateOf(LocationPermissionState()) 
    }
    
    // Function to check current permission status
    val checkPermissionStatus = {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        permissionState = when (permissionStatus) {
            PackageManager.PERMISSION_GRANTED -> {
                LocationPermissionState(
                    isGranted = true,
                    isDenied = false,
                    isPermanentlyDenied = false,
                    canRequestPermission = true
                )
            }
            else -> {
                LocationPermissionState(
                    isGranted = false,
                    isDenied = true,
                    isPermanentlyDenied = false,
                    canRequestPermission = true
                )
            }
        }
    }
    
    // Perform initial check if requested
    LaunchedEffect(initialCheck) {
        if (initialCheck) {
            checkPermissionStatus()
        }
    }
    
    return permissionState
}

/**
 * Utility object for location permission management.
 */
object LocationPermissionUtils {
    
    /**
     * Checks if location permission is currently granted.
     * 
     * @param context Android context for permission checking
     * @return true if location permission is granted, false otherwise
     */
    fun isLocationPermissionGranted(context: android.content.Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Checks if the app should show permission rationale.
     * 
     * @param activity Activity context for rationale checking
     * @return true if rationale should be shown, false otherwise
     */
    fun shouldShowLocationPermissionRationale(
        activity: androidx.activity.ComponentActivity
    ): Boolean {
        return activity.shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    /**
     * Gets a user-friendly message for the current permission state.
     * 
     * @param state Current location permission state
     * @return User-friendly message explaining the permission state
     */
    fun getPermissionMessage(state: LocationPermissionState): String {
        return when {
            state.isGranted -> "Location access granted"
            state.isPermanentlyDenied -> "Location access permanently denied. Please enable in Settings."
            state.shouldShowRationale -> "Location access is needed to show your area on the map"
            state.isDenied -> "Location access denied"
            else -> "Location permission status unknown"
        }
    }
    
    /**
     * Gets appropriate action text for the current permission state.
     * 
     * @param state Current location permission state
     * @return Action text for buttons or UI elements
     */
    fun getPermissionActionText(state: LocationPermissionState): String {
        return when {
            state.isGranted -> "Location Enabled"
            state.isPermanentlyDenied -> "Open Settings"
            state.needsPermission -> "Enable Location"
            else -> "Check Permission"
        }
    }
}