package com.locationsharing.app.ui.home

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for HomeScreenState data class.
 * 
 * Tests the state management functionality and ensures proper default values
 * and state transitions for the FFinder Home Screen.
 */
class HomeScreenStateTest {

    @Test
    fun `homeScreenState should have correct default values`() {
        // When
        val state = HomeScreenState()
        
        // Then
        assertFalse("isLoading should default to false", state.isLoading)
        assertFalse("hasLocationPermission should default to false", state.hasLocationPermission)
        assertFalse("isLocationSharing should default to false", state.isLocationSharing)
        assertFalse("showWhatsNewDialog should default to false", state.showWhatsNewDialog)
        assertNull("mapPreviewLocation should default to null", state.mapPreviewLocation)
        assertTrue("animationsEnabled should default to true", state.animationsEnabled)
        assertFalse("isNarrowScreen should default to false", state.isNarrowScreen)
        assertFalse("mapLoadError should default to false", state.mapLoadError)
        assertNull("locationError should default to null", state.locationError)
    }

    @Test
    fun `homeScreenState should allow property updates`() {
        // Given
        val initialState = HomeScreenState()
        val testLocation = LatLng(37.7749, -122.4194)
        val testError = "Location services unavailable"
        
        // When
        val updatedState = initialState.copy(
            isLoading = true,
            hasLocationPermission = true,
            isLocationSharing = true,
            showWhatsNewDialog = true,
            mapPreviewLocation = testLocation,
            animationsEnabled = false,
            isNarrowScreen = true,
            mapLoadError = true,
            locationError = testError
        )
        
        // Then
        assertTrue("isLoading should be updated", updatedState.isLoading)
        assertTrue("hasLocationPermission should be updated", updatedState.hasLocationPermission)
        assertTrue("isLocationSharing should be updated", updatedState.isLocationSharing)
        assertTrue("showWhatsNewDialog should be updated", updatedState.showWhatsNewDialog)
        assertEquals("mapPreviewLocation should be updated", testLocation, updatedState.mapPreviewLocation)
        assertFalse("animationsEnabled should be updated", updatedState.animationsEnabled)
        assertTrue("isNarrowScreen should be updated", updatedState.isNarrowScreen)
        assertTrue("mapLoadError should be updated", updatedState.mapLoadError)
        assertEquals("locationError should be updated", testError, updatedState.locationError)
    }

    @Test
    fun `homeScreenState should support partial updates`() {
        // Given
        val initialState = HomeScreenState(
            isLoading = true,
            hasLocationPermission = true
        )
        
        // When
        val partiallyUpdatedState = initialState.copy(
            isLocationSharing = true
        )
        
        // Then
        assertTrue("isLoading should remain unchanged", partiallyUpdatedState.isLoading)
        assertTrue("hasLocationPermission should remain unchanged", partiallyUpdatedState.hasLocationPermission)
        assertTrue("isLocationSharing should be updated", partiallyUpdatedState.isLocationSharing)
        assertFalse("showWhatsNewDialog should remain default", partiallyUpdatedState.showWhatsNewDialog)
    }

    @Test
    fun `homeScreenState should handle location data correctly`() {
        // Given
        val sanFrancisco = LatLng(37.7749, -122.4194)
        val newYork = LatLng(40.7128, -74.0060)
        
        // When
        val stateWithSF = HomeScreenState(mapPreviewLocation = sanFrancisco)
        val stateWithNY = stateWithSF.copy(mapPreviewLocation = newYork)
        val stateWithoutLocation = stateWithNY.copy(mapPreviewLocation = null)
        
        // Then
        assertEquals("Should store San Francisco location", sanFrancisco, stateWithSF.mapPreviewLocation)
        assertEquals("Should update to New York location", newYork, stateWithNY.mapPreviewLocation)
        assertNull("Should clear location", stateWithoutLocation.mapPreviewLocation)
    }

    @Test
    fun `homeScreenState should handle error states correctly`() {
        // Given
        val errorMessage = "GPS signal lost"
        val state = HomeScreenState()
        
        // When
        val stateWithError = state.copy(
            locationError = errorMessage,
            mapLoadError = true
        )
        val stateWithoutError = stateWithError.copy(
            locationError = null,
            mapLoadError = false
        )
        
        // Then
        assertEquals("Should store error message", errorMessage, stateWithError.locationError)
        assertTrue("Should indicate map load error", stateWithError.mapLoadError)
        assertNull("Should clear error message", stateWithoutError.locationError)
        assertFalse("Should clear map load error", stateWithoutError.mapLoadError)
    }
}