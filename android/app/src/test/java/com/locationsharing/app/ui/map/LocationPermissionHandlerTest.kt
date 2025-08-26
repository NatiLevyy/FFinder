package com.locationsharing.app.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.app.ActivityCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LocationPermissionHandler
 * Tests permission checking, requesting, and result handling
 */
class LocationPermissionHandlerTest {
    
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockActivity = mockk<ComponentActivity>(relaxed = true)
    private val mockLauncher = mockk<ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>>(relaxed = true)
    
    private var permissionGrantedCalled = false
    private var permissionDeniedCalled = false
    
    private lateinit var permissionHandler: LocationPermissionHandler
    
    @Before
    fun setup() {
        permissionGrantedCalled = false
        permissionDeniedCalled = false
        
        permissionHandler = LocationPermissionHandler(
            context = mockContext,
            onPermissionGranted = { permissionGrantedCalled = true },
            onPermissionDenied = { permissionDeniedCalled = true }
        )
        
        // Mock static methods
        mockkStatic("androidx.core.content.ContextCompat")
        mockkStatic("androidx.core.app.ActivityCompat")
    }
    
    @After
    fun tearDown() {
        unmockkStatic("androidx.core.content.ContextCompat")
        unmockkStatic("androidx.core.app.ActivityCompat")
    }
    
    @Test
    fun `hasLocationPermission should return true when both permissions are granted`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val hasPermission = permissionHandler.hasLocationPermission()
        
        // Then
        assertTrue(hasPermission)
    }
    
    @Test
    fun `hasLocationPermission should return false when fine location is denied`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val hasPermission = permissionHandler.hasLocationPermission()
        
        // Then
        assertFalse(hasPermission)
    }
    
    @Test
    fun `hasLocationPermission should return false when coarse location is denied`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val hasPermission = permissionHandler.hasLocationPermission()
        
        // Then
        assertFalse(hasPermission)
    }
    
    @Test
    fun `hasLocationPermission should return false when both permissions are denied`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                any()
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val hasPermission = permissionHandler.hasLocationPermission()
        
        // Then
        assertFalse(hasPermission)
    }
    
    @Test
    fun `hasFineLocationPermission should return true when fine location is granted`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val hasPermission = permissionHandler.hasFineLocationPermission()
        
        // Then
        assertTrue(hasPermission)
    }
    
    @Test
    fun `hasFineLocationPermission should return false when fine location is denied`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val hasPermission = permissionHandler.hasFineLocationPermission()
        
        // Then
        assertFalse(hasPermission)
    }
    
    @Test
    fun `hasCoarseLocationPermission should return true when coarse location is granted`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val hasPermission = permissionHandler.hasCoarseLocationPermission()
        
        // Then
        assertTrue(hasPermission)
    }
    
    @Test
    fun `hasCoarseLocationPermission should return false when coarse location is denied`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val hasPermission = permissionHandler.hasCoarseLocationPermission()
        
        // Then
        assertFalse(hasPermission)
    }
    
    @Test
    fun `handlePermissionResult should call onPermissionGranted when fine location is granted`() {
        // Given
        val permissions = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.ACCESS_COARSE_LOCATION to false
        )
        
        // When
        permissionHandler.handlePermissionResult(permissions)
        
        // Then
        assertTrue(permissionGrantedCalled)
        assertFalse(permissionDeniedCalled)
    }
    
    @Test
    fun `handlePermissionResult should call onPermissionGranted when coarse location is granted`() {
        // Given
        val permissions = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to true
        )
        
        // When
        permissionHandler.handlePermissionResult(permissions)
        
        // Then
        assertTrue(permissionGrantedCalled)
        assertFalse(permissionDeniedCalled)
    }
    
    @Test
    fun `handlePermissionResult should call onPermissionGranted when both permissions are granted`() {
        // Given
        val permissions = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.ACCESS_COARSE_LOCATION to true
        )
        
        // When
        permissionHandler.handlePermissionResult(permissions)
        
        // Then
        assertTrue(permissionGrantedCalled)
        assertFalse(permissionDeniedCalled)
    }
    
    @Test
    fun `handlePermissionResult should call onPermissionDenied when both permissions are denied`() {
        // Given
        val permissions = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false
        )
        
        // When
        permissionHandler.handlePermissionResult(permissions)
        
        // Then
        assertFalse(permissionGrantedCalled)
        assertTrue(permissionDeniedCalled)
    }
    
    @Test
    fun `requestPermissions should launch permission request`() {
        // When
        permissionHandler.requestPermissions(mockLauncher)
        
        // Then
        verify { 
            mockLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) 
        }
    }
    
    @Test
    fun `getPermissionStatusText should return correct text for fine location`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val statusText = permissionHandler.getPermissionStatusText()
        
        // Then
        assertEquals("Precise location access granted", statusText)
    }
    
    @Test
    fun `getPermissionStatusText should return correct text for coarse location`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val statusText = permissionHandler.getPermissionStatusText()
        
        // Then
        assertEquals("Approximate location access granted", statusText)
    }
    
    @Test
    fun `getPermissionStatusText should return correct text for denied permissions`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                any()
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val statusText = permissionHandler.getPermissionStatusText()
        
        // Then
        assertEquals("Location access denied", statusText)
    }
    
    @Test
    fun `shouldShowPermissionRationale should return true when rationale should be shown`() {
        // Given
        every { 
            ActivityCompat.shouldShowRequestPermissionRationale(
                mockActivity, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) 
        } returns true
        
        every { 
            ActivityCompat.shouldShowRequestPermissionRationale(
                mockActivity, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) 
        } returns false
        
        // When
        val shouldShow = permissionHandler.shouldShowPermissionRationale(mockActivity)
        
        // Then
        assertTrue(shouldShow)
    }
    
    @Test
    fun `shouldShowPermissionRationale should return false when rationale should not be shown`() {
        // Given
        every { 
            ActivityCompat.shouldShowRequestPermissionRationale(
                mockActivity, 
                any()
            ) 
        } returns false
        
        // When
        val shouldShow = permissionHandler.shouldShowPermissionRationale(mockActivity)
        
        // Then
        assertFalse(shouldShow)
    }
    
    @Test
    fun `getPermissionErrorMessage should return detailed error for no permissions`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                any()
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val errorMessage = permissionHandler.getPermissionErrorMessage()
        
        // Then
        assertTrue(errorMessage.contains("Location access is required"))
        assertTrue(errorMessage.contains("grant location permission"))
    }
    
    @Test
    fun `getPermissionErrorMessage should suggest precise location when only coarse granted`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_DENIED
        
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val errorMessage = permissionHandler.getPermissionErrorMessage()
        
        // Then
        assertTrue(errorMessage.contains("precise location access"))
    }
    
    @Test
    fun `getPermissionErrorMessage should return success message when permissions granted`() {
        // Given
        every { 
            androidx.core.content.ContextCompat.checkSelfPermission(
                mockContext, 
                any()
            ) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val errorMessage = permissionHandler.getPermissionErrorMessage()
        
        // Then
        assertEquals("Location services are working properly.", errorMessage)
    }
    
    @Test
    fun `isLocationServicesEnabled should check location manager providers`() {
        // Given
        val mockLocationManager = mockk<android.location.LocationManager>(relaxed = true)
        every { mockContext.getSystemService(Context.LOCATION_SERVICE) } returns mockLocationManager
        every { mockLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) } returns true
        every { mockLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) } returns false
        
        // When
        val isEnabled = permissionHandler.isLocationServicesEnabled()
        
        // Then
        assertTrue(isEnabled)
    }
    
    @Test
    fun `isLocationServicesEnabled should return false when all providers disabled`() {
        // Given
        val mockLocationManager = mockk<android.location.LocationManager>(relaxed = true)
        every { mockContext.getSystemService(Context.LOCATION_SERVICE) } returns mockLocationManager
        every { mockLocationManager.isProviderEnabled(any()) } returns false
        
        // When
        val isEnabled = permissionHandler.isLocationServicesEnabled()
        
        // Then
        assertFalse(isEnabled)
    }
    
    @Test
    fun `getLocationServicesErrorMessage should return error when services disabled`() {
        // Given
        val mockLocationManager = mockk<android.location.LocationManager>(relaxed = true)
        every { mockContext.getSystemService(Context.LOCATION_SERVICE) } returns mockLocationManager
        every { mockLocationManager.isProviderEnabled(any()) } returns false
        
        // When
        val errorMessage = permissionHandler.getLocationServicesErrorMessage()
        
        // Then
        assertTrue(errorMessage.contains("Location services are disabled"))
        assertTrue(errorMessage.contains("enable GPS or network location"))
    }
    
    @Test
    fun `getLocationServicesErrorMessage should return success when services enabled`() {
        // Given
        val mockLocationManager = mockk<android.location.LocationManager>(relaxed = true)
        every { mockContext.getSystemService(Context.LOCATION_SERVICE) } returns mockLocationManager
        every { mockLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) } returns true
        
        // When
        val errorMessage = permissionHandler.getLocationServicesErrorMessage()
        
        // Then
        assertEquals("Location services are enabled.", errorMessage)
    }
}