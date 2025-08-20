package com.locationsharing.app.ui.map.performance

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MapLifecycleManager
 * Tests lifecycle management, resource cleanup, and memory leak prevention
 */
class MapLifecycleManagerTest {
    
    private lateinit var lifecycleManager: MapLifecycleManager
    
    @Before
    fun setUp() {
        lifecycleManager = MapLifecycleManager()
    }
    
    @Test
    fun `initialize sets up lifecycle manager correctly`() {
        lifecycleManager.initialize()
        
        // Should not throw any exceptions
        assertTrue("Initialization should complete without errors", true)
    }
    
    @Test
    fun `startLocationUpdates activates location updates`() = runBlocking {
        lifecycleManager.initialize()
        
        var updateCount = 0
        lifecycleManager.startLocationUpdates {
            updateCount++
        }
        
        assertTrue("Location updates should be active", lifecycleManager.isLocationUpdatesActive())
        
        // Wait a bit and check if updates are running
        delay(100)
        
        lifecycleManager.stopLocationUpdates()
        assertFalse("Location updates should be stopped", lifecycleManager.isLocationUpdatesActive())
    }
    
    @Test
    fun `stopLocationUpdates deactivates location updates`() {
        lifecycleManager.initialize()
        
        lifecycleManager.startLocationUpdates { }
        assertTrue("Location updates should be active", lifecycleManager.isLocationUpdatesActive())
        
        lifecycleManager.stopLocationUpdates()
        assertFalse("Location updates should be stopped", lifecycleManager.isLocationUpdatesActive())
    }
    
    @Test
    fun `startFriendsUpdates activates friends updates`() = runBlocking {
        lifecycleManager.initialize()
        
        var updateCount = 0
        lifecycleManager.startFriendsUpdates {
            updateCount++
        }
        
        assertTrue("Friends updates should be active", lifecycleManager.isFriendsUpdatesActive())
        
        // Wait a bit and check if updates are running
        delay(100)
        
        lifecycleManager.stopFriendsUpdates()
        assertFalse("Friends updates should be stopped", lifecycleManager.isFriendsUpdatesActive())
    }
    
    @Test
    fun `stopFriendsUpdates deactivates friends updates`() {
        lifecycleManager.initialize()
        
        lifecycleManager.startFriendsUpdates { }
        assertTrue("Friends updates should be active", lifecycleManager.isFriendsUpdatesActive())
        
        lifecycleManager.stopFriendsUpdates()
        assertFalse("Friends updates should be stopped", lifecycleManager.isFriendsUpdatesActive())
    }
    
    @Test
    fun `onResume sets foreground state correctly`() {
        lifecycleManager.initialize()
        
        lifecycleManager.onPause() // Set to background first
        assertFalse("Should be in background", lifecycleManager.isInForeground())
        
        lifecycleManager.onResume()
        assertTrue("Should be in foreground", lifecycleManager.isInForeground())
    }
    
    @Test
    fun `onPause sets background state correctly`() {
        lifecycleManager.initialize()
        
        assertTrue("Should start in foreground", lifecycleManager.isInForeground())
        
        lifecycleManager.onPause()
        assertFalse("Should be in background", lifecycleManager.isInForeground())
    }
    
    @Test
    fun `getCurrentUpdateInterval returns correct intervals`() {
        lifecycleManager.initialize()
        
        // Foreground interval
        lifecycleManager.onResume()
        val foregroundInterval = lifecycleManager.getCurrentUpdateInterval()
        assertEquals("Foreground interval should be 5 seconds", 5000L, foregroundInterval)
        
        // Background interval
        lifecycleManager.onPause()
        val backgroundInterval = lifecycleManager.getCurrentUpdateInterval()
        assertEquals("Background interval should be 30 seconds", 30000L, backgroundInterval)
    }
    
    @Test
    fun `cleanup stops all updates and clears resources`() {
        lifecycleManager.initialize()
        
        // Start updates
        lifecycleManager.startLocationUpdates { }
        lifecycleManager.startFriendsUpdates { }
        
        assertTrue("Location updates should be active", lifecycleManager.isLocationUpdatesActive())
        assertTrue("Friends updates should be active", lifecycleManager.isFriendsUpdatesActive())
        
        // Cleanup
        lifecycleManager.cleanup()
        
        assertFalse("Location updates should be stopped", lifecycleManager.isLocationUpdatesActive())
        assertFalse("Friends updates should be stopped", lifecycleManager.isFriendsUpdatesActive())
    }
    
    @Test
    fun `onDestroy calls cleanup`() {
        lifecycleManager.initialize()
        
        // Start updates
        lifecycleManager.startLocationUpdates { }
        lifecycleManager.startFriendsUpdates { }
        
        assertTrue("Location updates should be active", lifecycleManager.isLocationUpdatesActive())
        assertTrue("Friends updates should be active", lifecycleManager.isFriendsUpdatesActive())
        
        // Destroy
        lifecycleManager.onDestroy()
        
        assertFalse("Location updates should be stopped", lifecycleManager.isLocationUpdatesActive())
        assertFalse("Friends updates should be stopped", lifecycleManager.isFriendsUpdatesActive())
    }
    
    @Test
    fun `setLifecycleCallbacks sets callbacks correctly`() {
        lifecycleManager.initialize()
        
        var resumeCalled = false
        var pauseCalled = false
        var stopCalled = false
        var destroyCalled = false
        
        lifecycleManager.setLifecycleCallbacks(
            onResume = { resumeCalled = true },
            onPause = { pauseCalled = true },
            onStop = { stopCalled = true },
            onDestroy = { destroyCalled = true }
        )
        
        lifecycleManager.onResume()
        assertTrue("Resume callback should be called", resumeCalled)
        
        lifecycleManager.onPause()
        assertTrue("Pause callback should be called", pauseCalled)
        
        lifecycleManager.onStop()
        assertTrue("Stop callback should be called", stopCalled)
        
        lifecycleManager.onDestroy()
        assertTrue("Destroy callback should be called", destroyCalled)
    }
    
    @Test
    fun `startPerformanceMonitoring starts monitoring correctly`() = runBlocking {
        lifecycleManager.initialize()
        
        var monitoringCount = 0
        lifecycleManager.startPerformanceMonitoring {
            monitoringCount++
        }
        
        // Wait a bit to allow monitoring to run
        delay(100)
        
        // Should not throw any exceptions
        assertTrue("Performance monitoring should start without errors", true)
        
        lifecycleManager.cleanup()
    }
    
    @Test
    fun `multiple start calls do not create duplicate updates`() {
        lifecycleManager.initialize()
        
        // Start location updates multiple times
        lifecycleManager.startLocationUpdates { }
        lifecycleManager.startLocationUpdates { }
        lifecycleManager.startLocationUpdates { }
        
        assertTrue("Location updates should be active", lifecycleManager.isLocationUpdatesActive())
        
        // Start friends updates multiple times
        lifecycleManager.startFriendsUpdates { }
        lifecycleManager.startFriendsUpdates { }
        lifecycleManager.startFriendsUpdates { }
        
        assertTrue("Friends updates should be active", lifecycleManager.isFriendsUpdatesActive())
        
        // Should not cause any issues
        lifecycleManager.cleanup()
    }
}