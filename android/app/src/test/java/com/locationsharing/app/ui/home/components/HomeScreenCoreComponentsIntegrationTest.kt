package com.locationsharing.app.ui.home.components

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.home.HomeScreenEvent
import com.locationsharing.app.ui.home.HomeScreenState
import org.junit.Assert.*
import org.junit.Test

/**
 * Integration tests for the core home screen components.
 * 
 * Tests the interaction between HomeScreenState, HomeScreenEvent, and the
 * responsive layout system to ensure they work together correctly.
 */
class HomeScreenCoreComponentsIntegrationTest {

    @Test
    fun `homeScreenState and events should work together for complete user flow`() {
        // Given - Initial state
        val initialState = HomeScreenState()
        
        // When - User grants location permission
        val stateAfterPermission = initialState.copy(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194)
        )
        
        // Then - State should reflect permission granted
        assertTrue("Should have location permission", stateAfterPermission.hasLocationPermission)
        assertNotNull("Should have map preview location", stateAfterPermission.mapPreviewLocation)
        
        // When - User starts sharing
        val stateAfterSharing = stateAfterPermission.copy(
            isLocationSharing = true
        )
        
        // Then - State should reflect sharing started
        assertTrue("Should be location sharing", stateAfterSharing.isLocationSharing)
        
        // When - User opens what's new dialog
        val stateWithDialog = stateAfterSharing.copy(
            showWhatsNewDialog = true
        )
        
        // Then - Dialog should be shown
        assertTrue("Should show what's new dialog", stateWithDialog.showWhatsNewDialog)
    }

    @Test
    fun `responsive layout configuration should handle different screen sizes`() {
        // Given - Different screen configurations
        val compactConfig = ResponsiveLayoutConfig(
            isNarrowScreen = true,
            screenWidthDp = 320,
            screenHeightDp = 568,
            isLandscape = false
        )
        
        val mediumConfig = ResponsiveLayoutConfig(
            isNarrowScreen = false,
            screenWidthDp = 400,
            screenHeightDp = 800,
            isLandscape = false
        )
        
        val landscapeConfig = ResponsiveLayoutConfig(
            isNarrowScreen = false,
            screenWidthDp = 800,
            screenHeightDp = 400,
            isLandscape = true
        )
        
        // Then - Screen size categories should be correct
        assertEquals("Compact screen should be categorized correctly", 
            ScreenSizeCategory.Compact, compactConfig.getScreenSizeCategory())
        assertEquals("Medium screen should be categorized correctly", 
            ScreenSizeCategory.Medium, mediumConfig.getScreenSizeCategory())
        assertTrue("Landscape should be detected", landscapeConfig.isLandscape)
    }

    @Test
    fun `homeScreenEvents should cover all user interactions`() {
        // Given - All possible events
        val events = listOf(
            HomeScreenEvent.StartSharing,
            HomeScreenEvent.NavigateToFriends,
            HomeScreenEvent.NavigateToSettings,
            HomeScreenEvent.ShowWhatsNew,
            HomeScreenEvent.DismissWhatsNew,
            HomeScreenEvent.LocationPermissionGranted,
            HomeScreenEvent.LocationPermissionDenied,
            HomeScreenEvent.MapLoadError,
            HomeScreenEvent.ScreenConfigurationChanged(true),
            HomeScreenEvent.AnimationPreferencesChanged(false)
        )
        
        // Then - All events should be distinct
        assertEquals("Should have all expected events", 10, events.size)
        
        // And - Data events should work correctly
        val configEvent1 = HomeScreenEvent.ScreenConfigurationChanged(true)
        val configEvent2 = HomeScreenEvent.ScreenConfigurationChanged(true)
        val configEvent3 = HomeScreenEvent.ScreenConfigurationChanged(false)
        
        assertEquals("Same config events should be equal", configEvent1, configEvent2)
        assertNotEquals("Different config events should not be equal", configEvent1, configEvent3)
    }

    @Test
    fun `homeScreenState should handle error scenarios correctly`() {
        // Given - Initial state
        val initialState = HomeScreenState()
        
        // When - Map load error occurs
        val stateWithMapError = initialState.copy(
            mapLoadError = true,
            locationError = "Unable to load map preview"
        )
        
        // Then - Error state should be reflected
        assertTrue("Should indicate map load error", stateWithMapError.mapLoadError)
        assertEquals("Should store location error", "Unable to load map preview", stateWithMapError.locationError)
        
        // When - Error is resolved
        val stateAfterRecovery = stateWithMapError.copy(
            mapLoadError = false,
            locationError = null,
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194)
        )
        
        // Then - State should be clean
        assertFalse("Should clear map load error", stateAfterRecovery.mapLoadError)
        assertNull("Should clear location error", stateAfterRecovery.locationError)
        assertTrue("Should have location permission", stateAfterRecovery.hasLocationPermission)
        assertNotNull("Should have map preview location", stateAfterRecovery.mapPreviewLocation)
    }

    @Test
    fun `homeScreenState should handle accessibility preferences`() {
        // Given - Initial state with animations enabled
        val initialState = HomeScreenState(animationsEnabled = true)
        
        // When - User disables animations for accessibility
        val stateWithoutAnimations = initialState.copy(
            animationsEnabled = false
        )
        
        // Then - Animations should be disabled
        assertFalse("Should disable animations", stateWithoutAnimations.animationsEnabled)
        
        // When - Screen becomes narrow
        val stateWithNarrowScreen = stateWithoutAnimations.copy(
            isNarrowScreen = true
        )
        
        // Then - Should adapt to narrow screen
        assertTrue("Should detect narrow screen", stateWithNarrowScreen.isNarrowScreen)
        assertFalse("Should keep animations disabled", stateWithNarrowScreen.animationsEnabled)
    }

    @Test
    fun `responsive layout should provide correct breakpoints`() {
        // Test all screen size categories
        val compactScreen = ResponsiveLayoutConfig(true, 320, 568, false)
        val mediumScreen = ResponsiveLayoutConfig(false, 400, 800, false)
        val expandedScreen = ResponsiveLayoutConfig(false, 700, 1000, false)
        val largeScreen = ResponsiveLayoutConfig(false, 900, 1200, false)
        
        assertEquals(ScreenSizeCategory.Compact, compactScreen.getScreenSizeCategory())
        assertEquals(ScreenSizeCategory.Medium, mediumScreen.getScreenSizeCategory())
        assertEquals(ScreenSizeCategory.Expanded, expandedScreen.getScreenSizeCategory())
        assertEquals(ScreenSizeCategory.Large, largeScreen.getScreenSizeCategory())
    }
}