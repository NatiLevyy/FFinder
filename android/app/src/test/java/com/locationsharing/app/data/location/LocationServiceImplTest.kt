package com.locationsharing.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.locationsharing.app.data.models.LocationData
import com.locationsharing.app.data.models.LocationError
import com.locationsharing.app.data.models.LocationPermissionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocationServiceImplTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Mock
    private lateinit var locationManager: LocationManager

    @Mock
    private lateinit var locationTask: Task<android.location.Location>

    private lateinit var locationService: LocationServiceImpl

    @BeforeEach
    fun setup() {
        // Mock system service
        whenever(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationManager)
        
        // Create service instance with mocked dependencies
        locationService = LocationServiceImpl(context)
        
        // Use reflection to inject mocked fusedLocationClient
        val field = LocationServiceImpl::class.java.getDeclaredField("fusedLocationClient")
        field.isAccessible = true
        field.set(locationService, fusedLocationClient)
    }

    @Test
    fun `checkLocationPermission returns GRANTED when all permissions are granted`() {
        // Given
        mockPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, true)
        mockPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mockPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION, true)
        }

        // When
        val result = locationService.checkLocationPermission()

        // Then
        assertEquals(LocationPermissionStatus.GRANTED, result)
    }

    @Test
    fun `checkLocationPermission returns NOT_REQUESTED when location permissions are denied`() {
        // Given
        mockPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, false)
        mockPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        // When
        val result = locationService.checkLocationPermission()

        // Then
        assertEquals(LocationPermissionStatus.NOT_REQUESTED, result)
    }

    @Test
    fun `checkLocationPermission returns BACKGROUND_DENIED when background permission is denied on Android Q+`() {
        // Given
        mockPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, true)
        mockPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mockPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false)
        }

        // When
        val result = locationService.checkLocationPermission()

        // Then
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertEquals(LocationPermissionStatus.BACKGROUND_DENIED, result)
        } else {
            assertEquals(LocationPermissionStatus.GRANTED, result)
        }
    }

    @Test
    fun `isLocationEnabled returns true when location is enabled`() {
        // Given
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            whenever(locationManager.isLocationEnabled).thenReturn(true)
        } else {
            whenever(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true)
        }

        // When
        val result = locationService.isLocationEnabled()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isLocationEnabled returns false when location is disabled`() {
        // Given
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            whenever(locationManager.isLocationEnabled).thenReturn(false)
        } else {
            whenever(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false)
            whenever(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false)
        }

        // When
        val result = locationService.isLocationEnabled()

        // Then
        assertFalse(result)
    }

    @Test
    fun `startLocationUpdates emits PermissionDenied when permissions are not granted`() = runTest {
        // Given
        mockPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, false)
        mockPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        // When
        locationService.startLocationUpdates()
        val result = locationService.getLocationUpdates().first()

        // Then
        assertTrue(result is LocationData.Error)
        assertTrue((result as LocationData.Error).error is LocationError.PermissionDenied)
    }

    @Test
    fun `startLocationUpdates emits LocationDisabled when location services are disabled`() = runTest {
        // Given
        mockAllPermissionsGranted()
        mockLocationEnabled(false)

        // When
        locationService.startLocationUpdates()
        val result = locationService.getLocationUpdates().first()

        // Then
        assertTrue(result is LocationData.Error)
        assertTrue((result as LocationData.Error).error is LocationError.LocationDisabled)
    }

    @Test
    fun `startLocationUpdates successfully starts location tracking when permissions are granted`() {
        // Given
        mockAllPermissionsGranted()
        mockLocationEnabled(true)

        // When
        locationService.startLocationUpdates()

        // Then
        verify(fusedLocationClient).requestLocationUpdates(
            any<LocationRequest>(),
            any<LocationCallback>(),
            any()
        )
    }

    @Test
    fun `stopLocationUpdates removes location callback`() {
        // Given
        mockAllPermissionsGranted()
        mockLocationEnabled(true)
        locationService.startLocationUpdates()

        // When
        locationService.stopLocationUpdates()

        // Then
        verify(fusedLocationClient).removeLocationUpdates(any<LocationCallback>())
    }

    @Test
    fun `getCurrentLocation returns PermissionDenied when permissions are not granted`() = runTest {
        // Given
        mockPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, false)
        mockPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        // When
        val result = locationService.getCurrentLocation()

        // Then
        assertTrue(result is LocationData.Error)
        assertTrue((result as LocationData.Error).error is LocationError.PermissionDenied)
    }

    @Test
    fun `getCurrentLocation returns LocationDisabled when location services are disabled`() = runTest {
        // Given
        mockAllPermissionsGranted()
        mockLocationEnabled(false)

        // When
        val result = locationService.getCurrentLocation()

        // Then
        assertTrue(result is LocationData.Error)
        assertTrue((result as LocationData.Error).error is LocationError.LocationDisabled)
    }

    @Test
    fun `getCurrentLocation returns Success when location is retrieved successfully`() = runTest {
        // Given
        mockAllPermissionsGranted()
        mockLocationEnabled(true)
        
        val mockLocation = mock<android.location.Location>().apply {
            whenever(latitude).thenReturn(37.7749)
            whenever(longitude).thenReturn(-122.4194)
            whenever(accuracy).thenReturn(5.0f)
            whenever(time).thenReturn(System.currentTimeMillis())
            whenever(hasAltitude()).thenReturn(false)
        }

        whenever(fusedLocationClient.getCurrentLocation(any<Int>(), any())).thenReturn(locationTask)
        whenever(locationTask.addOnSuccessListener(any<OnSuccessListener<android.location.Location>>())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnSuccessListener<android.location.Location>>(0)
            listener.onSuccess(mockLocation)
            locationTask
        }
        whenever(locationTask.addOnFailureListener(any<OnFailureListener>())).thenReturn(locationTask)

        // When
        val result = locationService.getCurrentLocation()

        // Then
        assertTrue(result is LocationData.Success)
        val location = (result as LocationData.Success).location
        assertEquals(37.7749, location.latitude)
        assertEquals(-122.4194, location.longitude)
        assertEquals(5.0f, location.accuracy)
    }

    @Test
    fun `getCurrentLocation returns UnknownError when location retrieval fails`() = runTest {
        // Given
        mockAllPermissionsGranted()
        mockLocationEnabled(true)
        
        val exception = RuntimeException("Location retrieval failed")
        whenever(fusedLocationClient.getCurrentLocation(any<Int>(), any())).thenReturn(locationTask)
        whenever(locationTask.addOnSuccessListener(any<OnSuccessListener<android.location.Location>>())).thenReturn(locationTask)
        whenever(locationTask.addOnFailureListener(any<OnFailureListener>())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnFailureListener>(0)
            listener.onFailure(exception)
            locationTask
        }

        // When
        val result = locationService.getCurrentLocation()

        // Then
        assertTrue(result is LocationData.Error)
        val error = (result as LocationData.Error).error
        assertTrue(error is LocationError.UnknownError)
        assertEquals(exception, (error as LocationError.UnknownError).cause)
    }

    private fun mockPermissionGranted(permission: String, granted: Boolean) {
        val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, permission) } returns result
    }

    private fun mockAllPermissionsGranted() {
        mockPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, true)
        mockPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mockPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION, true)
        }
    }

    private fun mockLocationEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            whenever(locationManager.isLocationEnabled).thenReturn(enabled)
        } else {
            whenever(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(enabled)
            whenever(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(enabled)
        }
    }
}