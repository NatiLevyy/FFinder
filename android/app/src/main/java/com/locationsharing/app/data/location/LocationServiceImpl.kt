package com.locationsharing.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.locationsharing.app.data.models.Location
import com.locationsharing.app.data.models.LocationData
import com.locationsharing.app.data.models.LocationError
import com.locationsharing.app.data.models.LocationPermissionStatus
import com.locationsharing.app.domain.location.LocationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationService {

    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _locationUpdates = MutableStateFlow<LocationData>(LocationData.Loading)
    private val locationUpdates: StateFlow<LocationData> = _locationUpdates.asStateFlow()
    
    private var locationCallback: LocationCallback? = null
    private var isTrackingStarted = false

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
        .setWaitForAccurateLocation(false)
        .setMinUpdateIntervalMillis(5000L)
        .setMaxUpdateDelayMillis(10000L)
        .build()

    override fun startLocationUpdates() {
        if (isTrackingStarted) return
        
        when (checkLocationPermission()) {
            LocationPermissionStatus.DENIED -> {
                _locationUpdates.value = LocationData.Error(LocationError.PermissionDenied)
                return
            }
            LocationPermissionStatus.BACKGROUND_DENIED -> {
                _locationUpdates.value = LocationData.Error(LocationError.BackgroundPermissionDenied)
                return
            }
            LocationPermissionStatus.NOT_REQUESTED -> {
                _locationUpdates.value = LocationData.Error(LocationError.PermissionDenied)
                return
            }
            LocationPermissionStatus.GRANTED -> {
                // Continue with location updates
            }
        }

        if (!isLocationEnabled()) {
            _locationUpdates.value = LocationData.Error(LocationError.LocationDisabled)
            return
        }

        try {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val locationData = Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time,
                            altitude = if (location.hasAltitude()) location.altitude else null
                        )
                        _locationUpdates.value = LocationData.Success(locationData)
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        _locationUpdates.value = LocationData.Error(LocationError.LocationDisabled)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            
            isTrackingStarted = true
            
        } catch (securityException: SecurityException) {
            _locationUpdates.value = LocationData.Error(LocationError.PermissionDenied)
        } catch (exception: Exception) {
            _locationUpdates.value = LocationData.Error(LocationError.UnknownError(exception))
        }
    }

    override fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
        isTrackingStarted = false
    }

    override fun getLocationUpdates(): Flow<LocationData> = locationUpdates

    override suspend fun getCurrentLocation(): LocationData {
        return when (checkLocationPermission()) {
            LocationPermissionStatus.DENIED -> LocationData.Error(LocationError.PermissionDenied)
            LocationPermissionStatus.BACKGROUND_DENIED -> LocationData.Error(LocationError.BackgroundPermissionDenied)
            LocationPermissionStatus.NOT_REQUESTED -> LocationData.Error(LocationError.PermissionDenied)
            LocationPermissionStatus.GRANTED -> {
                if (!isLocationEnabled()) {
                    return LocationData.Error(LocationError.LocationDisabled)
                }
                
                try {
                    val cancellationTokenSource = CancellationTokenSource()
                    
                    suspendCancellableCoroutine { continuation ->
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancellationTokenSource.token
                        ).addOnSuccessListener { location ->
                            if (location != null) {
                                val locationData = Location(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy,
                                    timestamp = location.time,
                                    altitude = if (location.hasAltitude()) location.altitude else null
                                )
                                continuation.resume(LocationData.Success(locationData))
                            } else {
                                continuation.resume(LocationData.Error(LocationError.LocationDisabled))
                            }
                        }.addOnFailureListener { exception ->
                            continuation.resume(LocationData.Error(LocationError.UnknownError(exception)))
                        }
                        
                        continuation.invokeOnCancellation {
                            cancellationTokenSource.cancel()
                        }
                    }
                } catch (securityException: SecurityException) {
                    LocationData.Error(LocationError.PermissionDenied)
                } catch (exception: Exception) {
                    LocationData.Error(LocationError.UnknownError(exception))
                }
            }
        }
    }

    override fun checkLocationPermission(): LocationPermissionStatus {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            return LocationPermissionStatus.NOT_REQUESTED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!backgroundLocationGranted) {
                return LocationPermissionStatus.BACKGROUND_DENIED
            }
        }

        return LocationPermissionStatus.GRANTED
    }

    override fun isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }
}