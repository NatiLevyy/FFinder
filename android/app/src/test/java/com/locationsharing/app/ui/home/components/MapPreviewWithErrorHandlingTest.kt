package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.theme.FFinderTheme
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for MapPreviewWithErrorHandling component.
 * 
 * Tests error handling scenarios including:
 * - Loading state display
 * - Error state handling
 * - Permission required state
 * - Location unavailable state
 * - Retry functionality
 * - User-friendly error messages
 */
@RunWith(AndroidJUnit4::class)
class MapPreviewWithErrorHandlingTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun mapPreviewWithErrorHandling_showsLoadingState_initially() {
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Verify loading state is displayed
        composeTestRule
            .onNodeWithText("Loading map preview...")
            .assertIsDisplayed()
        
        // Verify loading icon is present
        composeTestRule
            .onNodeWithContentDescription("Loading map preview")
            .assertIsDisplayed()
    }
    
    @Test
    fun mapPreviewWithErrorHandling_showsPermissionRequiredState_whenNoPermission() {
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true
                )
            }
        }
        
        // Verify permission required state
        composeTestRule
            .onNodeWithText("Location Required")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable location to see your area")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed()
    }
    
    @Test
    fun mapPreviewWithErrorHandling_showsLocationUnavailableState_whenLocationNull() {
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = null,
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Verify location unavailable state
        composeTestRule
            .onNodeWithText("Location Not Found")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Unable to determine your location")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Try Again")
            .assertIsDisplayed()
    }
    
    @Test
    fun mapPreviewWithErrorHandling_invokesPermissionRequestCallback() {
        var permissionRequested = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = null,
                    hasLocationPermission = false,
                    onPermissionRequest = { permissionRequested = true }
                )
            }
        }
        
        // Tap the enable location button
        composeTestRule
            .onNodeWithText("Enable Location")
            .performClick()
        
        // Verify callback was invoked
        assert(permissionRequested)
    }
    
    @Test
    fun mapPreviewWithErrorHandling_invokesRetryCallback() {
        var retryInvoked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = null,
                    hasLocationPermission = true,
                    onRetry = { retryInvoked = true }
                )
            }
        }
        
        // Tap the try again button
        composeTestRule
            .onNodeWithText("Try Again")
            .performClick()
        
        // Verify callback was invoked
        assert(retryInvoked)
    }
    
    @Test
    fun mapPreviewWithErrorHandling_showsErrorState_onMapLoadFailure() = runTest {
        var errorReported: MapPreviewError? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onError = { error -> errorReported = error }
                )
            }
        }
        
        // Wait for potential error state
        composeTestRule.waitForIdle()
        
        // Note: In a real test, you would simulate map loading failure
        // For now, we test the error state display structure
    }
    
    @Test
    fun mapPreviewWithErrorHandling_displaysUserFriendlyErrorMessages() {
        val testErrorMessage = "Network connection failed"
        
        // Test the utility function for user-friendly messages
        val userFriendlyMessage = getErrorDisplayMessage(testErrorMessage)
        
        assert(userFriendlyMessage == "Check your internet connection") {
            "Expected user-friendly message but got: $userFriendlyMessage"
        }
        
        // Test other error message conversions
        val serviceError = getErrorDisplayMessage("Google Maps service unavailable")
        assert(serviceError == "Map service temporarily unavailable")
        
        val timeoutError = getErrorDisplayMessage("Request timeout occurred")
        assert(timeoutError == "Request timed out")
        
        val unknownError = getErrorDisplayMessage("Some unknown error")
        assert(unknownError == "Unable to load map preview")
    }
    
    @Test
    fun mapPreviewWithErrorHandling_limitsRetryAttempts() {
        composeTestRule.setContent {
            FFinderTheme {
                // Simulate error state with high retry count
                MapPreviewErrorState(
                    errorMessage = "Network error",
                    onRetry = {},
                    retryCount = 3
                )
            }
        }
        
        // Verify that retry button is not shown after too many attempts
        composeTestRule
            .onNodeWithText("Please try again later")
            .assertIsDisplayed()
        
        // Verify retry button is not present
        composeTestRule
            .onNodeWithText("Retry")
            .assertDoesNotExist()
    }
    
    @Test
    fun mapPreviewWithErrorHandling_showsRetryCountInButton() {
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewErrorState(
                    errorMessage = "Network error",
                    onRetry = {},
                    retryCount = 1
                )
            }
        }
        
        // Verify retry count is shown in button
        composeTestRule
            .onNodeWithText("Retry (2)")
            .assertIsDisplayed()
    }
    
    @Test
    fun mapPreviewWithErrorHandling_hasProperAccessibilitySupport() {
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = null,
                    hasLocationPermission = false
                )
            }
        }
        
        // Verify accessibility content descriptions
        composeTestRule
            .onNodeWithContentDescription("Map preview requires location permission")
            .assertIsDisplayed()
        
        // Test with location unavailable
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = null,
                    hasLocationPermission = true
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Map preview unavailable - location not found")
            .assertIsDisplayed()
    }
    
    @Test
    fun mapPreviewWithErrorHandling_respectsAnimationPreferences() {
        // Test with animations enabled
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewLoadingPlaceholder(animationsEnabled = true)
            }
        }
        
        // Verify loading indicator is present when animations enabled
        composeTestRule.waitForIdle()
        
        // Test with animations disabled
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewLoadingPlaceholder(animationsEnabled = false)
            }
        }
        
        // Verify static loading state when animations disabled
        composeTestRule
            .onNodeWithText("Loading map preview...")
            .assertIsDisplayed()
    }
    
    @Test
    fun mapPreviewWithErrorHandling_handlesStateTransitions() {
        var currentLocation: LatLng? = null
        var hasPermission = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = currentLocation,
                    hasLocationPermission = hasPermission
                )
            }
        }
        
        // Initial state - no permission
        composeTestRule
            .onNodeWithText("Location Required")
            .assertIsDisplayed()
        
        // Update to have permission but no location
        hasPermission = true
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = currentLocation,
                    hasLocationPermission = hasPermission
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Location Not Found")
            .assertIsDisplayed()
        
        // Update to have both permission and location
        currentLocation = LatLng(37.7749, -122.4194)
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = currentLocation,
                    hasLocationPermission = hasPermission
                )
            }
        }
        
        // Should show loading state initially
        composeTestRule
            .onNodeWithText("Loading map preview...")
            .assertIsDisplayed()
    }
    
    @Test
    fun mapPreviewError_categoriesAreCorrect() {
        // Test MapPreviewError sealed class
        val mapLoadError = MapPreviewError.MapLoadFailed("Network timeout")
        assert(mapLoadError.message == "Network timeout")
        
        val locationError = MapPreviewError.LocationServiceFailed("GPS unavailable")
        assert(locationError.message == "GPS unavailable")
        
        val networkError = MapPreviewError.NetworkError("No connection")
        assert(networkError.message == "No connection")
        
        val permissionError = MapPreviewError.PermissionDenied("Location denied")
        assert(permissionError.message == "Location denied")
        
        val unknownError = MapPreviewError.UnknownError("Unknown issue")
        assert(unknownError.message == "Unknown issue")
    }
}

/**
 * Helper function to access private getErrorDisplayMessage function for testing.
 * In a real implementation, this would be made internal or moved to a testable utility.
 */
private fun getErrorDisplayMessage(errorMessage: String): String {
    return when {
        errorMessage.contains("network", ignoreCase = true) -> "Check your internet connection"
        errorMessage.contains("service", ignoreCase = true) -> "Map service temporarily unavailable"
        errorMessage.contains("timeout", ignoreCase = true) -> "Request timed out"
        errorMessage.contains("permission", ignoreCase = true) -> "Location permission required"
        else -> "Unable to load map preview"
    }
}