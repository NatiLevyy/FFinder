package com.locationsharing.app.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Location permission handler for MapScreen
 * Handles location permission requests and status checking
 */
class LocationPermissionHandler(
    private val context: Context,
    private val onPermissionGranted: () -> Unit,
    private val onPermissionDenied: () -> Unit
) {
    
    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if fine location permission is granted
     */
    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if coarse location permission is granted
     */
    fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Handle permission result with detailed feedback
     */
    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        Timber.d("Permission result - Fine: $fineLocationGranted, Coarse: $coarseLocationGranted")
        
        when {
            fineLocationGranted -> {
                Timber.i("Fine location permission granted - high accuracy available")
                onPermissionGranted()
            }
            coarseLocationGranted -> {
                Timber.i("Coarse location permission granted - approximate location available")
                onPermissionGranted()
            }
            else -> {
                Timber.w("All location permissions denied")
                onPermissionDenied()
            }
        }
    }
    
    /**
     * Get detailed permission error message for user feedback
     */
    fun getPermissionErrorMessage(): String {
        return when {
            !hasLocationPermission() -> "Location access is required to show your position on the map and share your location with friends. Please grant location permission in settings."
            !hasFineLocationPermission() -> "For the best experience, please enable precise location access in your device settings."
            else -> "Location services are working properly."
        }
    }
    
    /**
     * Check if location services are enabled on the device
     */
    fun isLocationServicesEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Get location services error message
     */
    fun getLocationServicesErrorMessage(): String {
        return if (!isLocationServicesEnabled()) {
            "Location services are disabled. Please enable GPS or network location in your device settings."
        } else {
            "Location services are enabled."
        }
    }
    
    /**
     * Request location permissions
     */
    fun requestPermissions(launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>) {
        Timber.d("Requesting location permissions")
        launcher.launch(REQUIRED_PERMISSIONS)
    }
    
    /**
     * Get permission status text for UI display
     */
    fun getPermissionStatusText(): String {
        return when {
            hasFineLocationPermission() -> "Precise location access granted"
            hasCoarseLocationPermission() -> "Approximate location access granted"
            else -> "Location access denied"
        }
    }
    
    /**
     * Check if we should show permission rationale
     */
    fun shouldShowPermissionRationale(activity: androidx.activity.ComponentActivity): Boolean {
        return REQUIRED_PERMISSIONS.any { permission ->
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
}

/**
 * Composable function to handle location permissions
 */
@Composable
fun rememberLocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): LocationPermissionHandler {
    val context = LocalContext.current
    
    return remember(context) {
        LocationPermissionHandler(
            context = context,
            onPermissionGranted = onPermissionGranted,
            onPermissionDenied = onPermissionDenied
        )
    }
}

/**
 * Effect to automatically check permissions on composition
 */
@Composable
fun LocationPermissionEffect(
    permissionHandler: LocationPermissionHandler,
    onPermissionStatusChanged: (Boolean) -> Unit
) {
    LaunchedEffect(permissionHandler) {
        val hasPermission = permissionHandler.hasLocationPermission()
        onPermissionStatusChanged(hasPermission)
    }
}