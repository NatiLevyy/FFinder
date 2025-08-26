package com.locationsharing.app.data.location

import android.content.Context
import com.locationsharing.app.data.friends.FriendsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LocationSharingService
 * Tests requirements 3.3, 5.6, 5.7
 */
@ExperimentalCoroutinesApi
class LocationSharingServiceTest {
    
    private lateinit var locationSharingService: LocationSharingService
    private lateinit var mockContext: Context
    private lateinit var mockEnhancedLocationService: EnhancedLocationService
    private lateinit var mockFriendsRepository: FriendsRepository
    
    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockEnhancedLocationService = mockk(relaxed = true)
        mockFriendsRepository = mockk(relaxed = true)
        
        locationSharingService = LocationSharingService(
            context = mockContext,
            enhancedLocationService = mockEnhancedLocationService,
            friendsRepository = mockFriendsRepository
        )
    }
    
    @Test
    fun `startLocationSharing should succeed when repository succeeds`() = runTest {
        // Given
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        
        // When
        val result = locationSharingService.startLocationSharing()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockFriendsRepository.startLocationSharing() }
        verify { mockEnhancedLocationService.enableHighAccuracyMode(true) }
        
        val state = locationSharingService.sharingState.first()
        assertEquals(LocationSharingStatus.ACTIVE, state.status)
    }
    
    @Test
    fun `startLocationSharing should fail when repository fails`() = runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.failure(exception)
        
        // When
        val result = locationSharingService.startLocationSharing()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        val state = locationSharingService.sharingState.first()
        assertEquals(LocationSharingStatus.ERROR, state.status)
        assertEquals("Network error", state.error)
    }
    
    @Test
    fun `stopLocationSharing should succeed when repository succeeds`() = runTest {
        // Given - start sharing first
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        coEvery { mockFriendsRepository.stopLocationSharing() } returns Result.success(Unit)
        
        locationSharingService.startLocationSharing()
        
        // When
        val result = locationSharingService.stopLocationSharing()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockFriendsRepository.stopLocationSharing() }
        verify { mockEnhancedLocationService.enableHighAccuracyMode(false) }
        
        val state = locationSharingService.sharingState.first()
        assertEquals(LocationSharingStatus.INACTIVE, state.status)
    }
    
    @Test
    fun `stopLocationSharing should fail when repository fails`() = runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { mockFriendsRepository.stopLocationSharing() } returns Result.failure(exception)
        
        // When
        val result = locationSharingService.stopLocationSharing()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        
        val state = locationSharingService.sharingState.first()
        assertEquals(LocationSharingStatus.ERROR, state.status)
        assertEquals("Network error", state.error)
    }
    
    @Test
    fun `toggleLocationSharing should start when inactive`() = runTest {
        // Given
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        
        // When
        val result = locationSharingService.toggleLocationSharing()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockFriendsRepository.startLocationSharing() }
        
        val state = locationSharingService.sharingState.first()
        assertEquals(LocationSharingStatus.ACTIVE, state.status)
    }
    
    @Test
    fun `toggleLocationSharing should stop when active`() = runTest {
        // Given - start sharing first
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        coEvery { mockFriendsRepository.stopLocationSharing() } returns Result.success(Unit)
        
        locationSharingService.startLocationSharing()
        
        // When
        val result = locationSharingService.toggleLocationSharing()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockFriendsRepository.stopLocationSharing() }
        
        val state = locationSharingService.sharingState.first()
        assertEquals(LocationSharingStatus.INACTIVE, state.status)
    }
    
    @Test
    fun `retryLocationSharing should succeed after failure`() = runTest {
        // Given - simulate initial failure
        coEvery { mockFriendsRepository.startLocationSharing() } returns 
            Result.failure(Exception("Initial failure")) andThen Result.success(Unit)
        
        locationSharingService.startLocationSharing() // This will fail
        
        // When
        val result = locationSharingService.retryLocationSharing()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 2) { mockFriendsRepository.startLocationSharing() }
        
        val state = locationSharingService.sharingState.first()
        assertEquals(LocationSharingStatus.ACTIVE, state.status)
    }
    
    @Test
    fun `retryLocationSharing should fail after max attempts`() = runTest {
        // Given - simulate multiple failures
        coEvery { mockFriendsRepository.startLocationSharing() } returns 
            Result.failure(Exception("Persistent failure"))
        
        // Exhaust retry attempts
        repeat(3) {
            locationSharingService.startLocationSharing()
            locationSharingService.retryLocationSharing()
        }
        
        // When
        val result = locationSharingService.retryLocationSharing()
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Maximum retry attempts") == true)
    }
    
    @Test
    fun `getStatusText should return correct text for each status`() = runTest {
        // Test inactive status
        assertEquals("Location Sharing Off", locationSharingService.getStatusText())
        
        // Test active status
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        locationSharingService.startLocationSharing()
        assertEquals("Location Sharing Active", locationSharingService.getStatusText())
        
        // Test error status
        coEvery { mockFriendsRepository.stopLocationSharing() } returns 
            Result.failure(Exception("Error"))
        locationSharingService.stopLocationSharing()
        assertEquals("Location Sharing Error", locationSharingService.getStatusText())
    }
    
    @Test
    fun `isLocationSharingActive should return correct status`() = runTest {
        // Initially inactive
        assertFalse(locationSharingService.isLocationSharingActive())
        
        // After starting
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        locationSharingService.startLocationSharing()
        assertTrue(locationSharingService.isLocationSharingActive())
        
        // After stopping
        coEvery { mockFriendsRepository.stopLocationSharing() } returns Result.success(Unit)
        locationSharingService.stopLocationSharing()
        assertFalse(locationSharingService.isLocationSharingActive())
    }
    
    @Test
    fun `canToggleSharing should return correct availability`() = runTest {
        // Initially can toggle
        assertTrue(locationSharingService.canToggleSharing())
        
        // After starting, can still toggle
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        locationSharingService.startLocationSharing()
        assertTrue(locationSharingService.canToggleSharing())
        
        // In error state, can toggle (retry)
        coEvery { mockFriendsRepository.stopLocationSharing() } returns 
            Result.failure(Exception("Error"))
        locationSharingService.stopLocationSharing()
        assertTrue(locationSharingService.canToggleSharing())
    }
    
    @Test
    fun `getSharingDuration should return correct duration when active`() = runTest {
        // Initially no duration
        assertEquals(null, locationSharingService.getSharingDuration())
        
        // After starting, should have duration
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        locationSharingService.startLocationSharing()
        
        val duration = locationSharingService.getSharingDuration()
        assertTrue(duration != null && duration >= 0)
    }
    
    @Test
    fun `resetSharingState should reset to initial state`() = runTest {
        // Given - start sharing
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        locationSharingService.startLocationSharing()
        
        // When
        locationSharingService.resetSharingState()
        
        // Then
        val state = locationSharingService.sharingState.first()
        assertEquals(LocationSharingStatus.INACTIVE, state.status)
        assertEquals(null, state.error)
        assertEquals(0, state.retryCount)
        
        verify { mockEnhancedLocationService.enableHighAccuracyMode(false) }
    }
    
    @Test
    fun `notifications should be emitted on success and error`() = runTest {
        // Test success notification
        coEvery { mockFriendsRepository.startLocationSharing() } returns Result.success(Unit)
        locationSharingService.startLocationSharing()
        
        val successNotification = locationSharingService.notifications.first()
        assertEquals(NotificationType.SUCCESS, successNotification?.type)
        assertEquals("Location sharing started", successNotification?.message)
        
        // Test error notification
        coEvery { mockFriendsRepository.stopLocationSharing() } returns 
            Result.failure(Exception("Test error"))
        locationSharingService.stopLocationSharing()
        
        // Clear previous notification first
        locationSharingService.clearNotification()
        
        // Trigger error again to get new notification
        locationSharingService.stopLocationSharing()
        
        val errorNotification = locationSharingService.notifications.first()
        assertEquals(NotificationType.ERROR, errorNotification?.type)
        assertTrue(errorNotification?.message?.contains("Test error") == true)
    }
}