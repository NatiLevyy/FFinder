package com.locationsharing.app.ui.home

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for HomeScreenEvent sealed class.
 * 
 * Tests the event system functionality and ensures proper event creation
 * and type checking for the FFinder Home Screen.
 */
class HomeScreenEventTest {

    @Test
    fun `startSharing event should be created correctly`() {
        // When
        val event = HomeScreenEvent.StartSharing
        
        // Then
        assertTrue("Should be StartSharing event", event is HomeScreenEvent.StartSharing)
        assertEquals("Should have correct string representation", "StartSharing", event.toString())
    }

    @Test
    fun `navigateToFriends event should be created correctly`() {
        // When
        val event = HomeScreenEvent.NavigateToFriends
        
        // Then
        assertTrue("Should be NavigateToFriends event", event is HomeScreenEvent.NavigateToFriends)
        assertEquals("Should have correct string representation", "NavigateToFriends", event.toString())
    }

    @Test
    fun `navigateToSettings event should be created correctly`() {
        // When
        val event = HomeScreenEvent.NavigateToSettings
        
        // Then
        assertTrue("Should be NavigateToSettings event", event is HomeScreenEvent.NavigateToSettings)
        assertEquals("Should have correct string representation", "NavigateToSettings", event.toString())
    }

    @Test
    fun `showWhatsNew event should be created correctly`() {
        // When
        val event = HomeScreenEvent.ShowWhatsNew
        
        // Then
        assertTrue("Should be ShowWhatsNew event", event is HomeScreenEvent.ShowWhatsNew)
        assertEquals("Should have correct string representation", "ShowWhatsNew", event.toString())
    }

    @Test
    fun `dismissWhatsNew event should be created correctly`() {
        // When
        val event = HomeScreenEvent.DismissWhatsNew
        
        // Then
        assertTrue("Should be DismissWhatsNew event", event is HomeScreenEvent.DismissWhatsNew)
        assertEquals("Should have correct string representation", "DismissWhatsNew", event.toString())
    }

    @Test
    fun `locationPermissionGranted event should be created correctly`() {
        // When
        val event = HomeScreenEvent.LocationPermissionGranted
        
        // Then
        assertTrue("Should be LocationPermissionGranted event", event is HomeScreenEvent.LocationPermissionGranted)
        assertEquals("Should have correct string representation", "LocationPermissionGranted", event.toString())
    }

    @Test
    fun `locationPermissionDenied event should be created correctly`() {
        // When
        val event = HomeScreenEvent.LocationPermissionDenied
        
        // Then
        assertTrue("Should be LocationPermissionDenied event", event is HomeScreenEvent.LocationPermissionDenied)
        assertEquals("Should have correct string representation", "LocationPermissionDenied", event.toString())
    }

    @Test
    fun `mapLoadError event should be created correctly`() {
        // When
        val event = HomeScreenEvent.MapLoadError
        
        // Then
        assertTrue("Should be MapLoadError event", event is HomeScreenEvent.MapLoadError)
        assertEquals("Should have correct string representation", "MapLoadError", event.toString())
    }

    @Test
    fun `screenConfigurationChanged event should be created correctly`() {
        // Given
        val isNarrowScreen = true
        
        // When
        val event = HomeScreenEvent.ScreenConfigurationChanged(isNarrowScreen)
        
        // Then
        assertTrue("Should be ScreenConfigurationChanged event", event is HomeScreenEvent.ScreenConfigurationChanged)
        assertEquals("Should store isNarrowScreen value", isNarrowScreen, event.isNarrowScreen)
    }

    @Test
    fun `animationPreferencesChanged event should be created correctly`() {
        // Given
        val animationsEnabled = false
        
        // When
        val event = HomeScreenEvent.AnimationPreferencesChanged(animationsEnabled)
        
        // Then
        assertTrue("Should be AnimationPreferencesChanged event", event is HomeScreenEvent.AnimationPreferencesChanged)
        assertEquals("Should store animationsEnabled value", animationsEnabled, event.animationsEnabled)
    }

    @Test
    fun `events should be distinguishable by type`() {
        // Given
        val events = listOf(
            HomeScreenEvent.StartSharing,
            HomeScreenEvent.NavigateToFriends,
            HomeScreenEvent.NavigateToSettings,
            HomeScreenEvent.ShowWhatsNew,
            HomeScreenEvent.DismissWhatsNew,
            HomeScreenEvent.LocationPermissionGranted,
            HomeScreenEvent.LocationPermissionDenied,
            HomeScreenEvent.MapLoadError,
            HomeScreenEvent.ScreenConfigurationChanged(false),
            HomeScreenEvent.AnimationPreferencesChanged(true)
        )
        
        // When & Then
        events.forEachIndexed { index, event ->
            events.forEachIndexed { otherIndex, otherEvent ->
                if (index != otherIndex) {
                    assertNotEquals("Events should be different", event::class, otherEvent::class)
                }
            }
        }
    }

    @Test
    fun `data events should support equality comparison`() {
        // Given
        val event1 = HomeScreenEvent.ScreenConfigurationChanged(true)
        val event2 = HomeScreenEvent.ScreenConfigurationChanged(true)
        val event3 = HomeScreenEvent.ScreenConfigurationChanged(false)
        
        // Then
        assertEquals("Same data events should be equal", event1, event2)
        assertNotEquals("Different data events should not be equal", event1, event3)
        
        // Given
        val animEvent1 = HomeScreenEvent.AnimationPreferencesChanged(true)
        val animEvent2 = HomeScreenEvent.AnimationPreferencesChanged(true)
        val animEvent3 = HomeScreenEvent.AnimationPreferencesChanged(false)
        
        // Then
        assertEquals("Same animation events should be equal", animEvent1, animEvent2)
        assertNotEquals("Different animation events should not be equal", animEvent1, animEvent3)
    }
}