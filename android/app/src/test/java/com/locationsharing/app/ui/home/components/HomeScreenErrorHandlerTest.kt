package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for HomeScreenErrorHandler component.
 * 
 * Tests error handling scenarios including:
 * - Error display and formatting
 * - User-friendly error messages
 * - Recovery action suggestions
 * - Error severity handling
 * - Callback invocation
 * - Accessibility support
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenErrorHandlerTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun homeScreenErrorHandler_displaysLocationPermissionError() {
        val error = HomeScreenError.LocationPermissionDenied(isPermanent = false)
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = error,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        
        // Verify error title and message
        composeTestRule
            .onNodeWithText("Location Access Required")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Location access is needed to show your area on the map and enable location sharing.")
            .assertIsDisplayed()
        
        // Verify retry button is present (not permanent denial)
        composeTestRule
            .onNodeWithText("Retry")
            .assertIsDisplayed()
        
        // Verify recovery action
        composeTestRule
            .onNodeWithText("Tap Retry to grant permission")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreenErrorHandler_displaysPermanentLocationPermissionError() {
        val error = HomeScreenError.LocationPermissionDenied(isPermanent = true)
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = error,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        
        // Verify permanent denial message
        composeTestRule
            .onNodeWithText("Location access is permanently denied. Please enable it in Settings to see your area on the map.")
            .assertIsDisplayed()
        
        // Verify no retry button for permanent denial
        composeTestRule
            .onNodeWithText("Retry")
            .assertDoesNotExist()
        
        // Verify settings guidance
        composeTestRule
            .onNodeWithText("Go to Settings > Apps > FFinder > Permissions")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreenErrorHandler_displaysMapLoadError() {
        val error = HomeScreenError.MapLoadFailed("Network timeout")
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = error,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        
        // Verify error details
        composeTestRule
            .onNodeWithText("Map Unavailable")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Unable to load the map preview. Please check your internet connection and try again.")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Check your internet connection")
            .assertIsDisplayed()
        
        // Verify retry button is present
        composeTestRule
            .onNodeWithText("Retry")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreenErrorHandler_displaysNetworkError() {
        val error = HomeScreenError.NetworkError(isConnected = false)
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = error,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        
        // Verify network error details
        composeTestRule
            .onNodeWithText("Connection Issue")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("No internet connection. Please check your network settings.")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Check your Wi-Fi or mobile data connection")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreenErrorHandler_displaysPerformanceIssue() {
        val error = HomeScreenError.PerformanceIssue(
            currentFps = 35.2,
            issue = "Low frame rate detected"
        )
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = error,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        
        // Verify performance issue details
        composeTestRule
            .onNodeWithText("Performance Issue")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Animations may appear choppy. Consider reducing animation quality in settings.")
            .assertIsDisplayed()
        
        // Verify no retry button for performance issues
        composeTestRule
            .onNodeWithText("Retry")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithText("Reduce animation quality in Settings")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreenErrorHandler_invokesRetryCallback() {
        var retryInvoked = false
        val error = HomeScreenError.MapLoadFailed("Network error")
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = error,
                    onRetry = { retryInvoked = true },
                    onDismiss = {}
                )
            }
        }
        
        // Tap retry button
        composeTestRule
            .onNodeWithText("Retry")
            .performClick()
        
        // Verify callback was invoked
        assert(retryInvoked)
    }
    
    @Test
    fun homeScreenErrorHandler_invokesDismissCallback() {
        var dismissInvoked = false
        val error = HomeScreenError.MapLoadFailed("Network error")
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = error,
                    onRetry = {},
                    onDismiss = { dismissInvoked = true }
                )
            }
        }
        
        // Tap dismiss button (close icon)
        composeTestRule
            .onNodeWithContentDescription("Dismiss error")
            .performClick()
        
        // Verify callback was invoked
        assert(dismissInvoked)
    }
    
    @Test
    fun homeScreenErrorHandler_showsCorrectSeverityColors() {
        // Test critical error
        val criticalError = HomeScreenError.NetworkError(isConnected = false)
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = criticalError,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        
        // Verify critical error is displayed
        composeTestRule
            .onNodeWithContentDescription("Critical error")
            .assertIsDisplayed()
        
        // Test warning error
        val warningError = HomeScreenError.LocationPermissionDenied()
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = warningError,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        
        // Verify warning error is displayed
        composeTestRule
            .onNodeWithContentDescription("Warning error")
            .assertIsDisplayed()
        
        // Test info error
        val infoError = HomeScreenError.PerformanceIssue(45.0, "Minor performance issue")
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = infoError,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        
        // Verify info error is displayed
        composeTestRule
            .onNodeWithContentDescription("Info error")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreenErrorHandler_hasProperAccessibilitySupport() {
        val error = HomeScreenError.LocationPermissionDenied()
        
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenErrorHandler(
                    error = error,
                    onRetry = {},
                    onDismiss = {}
                )
            }
        }
        
        // Verify accessibility content description
        composeTestRule
            .onNodeWithContentDescription(
                "Error: ${error.userMessage}. ${error.recoveryAction}"
            )
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreenErrorUtils_createsCorrectErrorFromException() {
        // Test SecurityException -> LocationPermissionDenied
        val securityException = SecurityException("Location permission denied")
        val permissionError = HomeScreenErrorUtils.createErrorFromException(securityException)
        assert(permissionError is HomeScreenError.LocationPermissionDenied)
        
        // Test UnknownHostException -> NetworkError
        val networkException = java.net.UnknownHostException("No internet")
        val networkError = HomeScreenErrorUtils.createErrorFromException(networkException)
        assert(networkError is HomeScreenError.NetworkError)
        assert((networkError as HomeScreenError.NetworkError).isConnected == false)
        
        // Test SocketTimeoutException -> LocationTimeout
        val timeoutException = java.net.SocketTimeoutException("Request timeout")
        val timeoutError = HomeScreenErrorUtils.createErrorFromException(timeoutException)
        assert(timeoutError is HomeScreenError.LocationTimeout)
        
        // Test unknown exception -> UnknownError
        val unknownException = RuntimeException("Unknown error")
        val unknownError = HomeScreenErrorUtils.createErrorFromException(unknownException)
        assert(unknownError is HomeScreenError.UnknownError)
        assert((unknownError as HomeScreenError.UnknownError).exception == unknownException)
    }
    
    @Test
    fun homeScreenErrorUtils_determinesDisplayPriority() {
        val criticalError = HomeScreenError.NetworkError(isConnected = false)
        val warningError = HomeScreenError.LocationPermissionDenied()
        val infoError = HomeScreenError.PerformanceIssue(45.0, "Minor issue")
        
        // Critical and warning errors should show immediately
        assert(HomeScreenErrorUtils.shouldShowImmediately(criticalError))
        assert(HomeScreenErrorUtils.shouldShowImmediately(warningError))
        
        // Info errors should not show immediately
        assert(!HomeScreenErrorUtils.shouldShowImmediately(infoError))
    }
    
    @Test
    fun homeScreenErrorUtils_providesCorrectDisplayDuration() {
        val criticalError = HomeScreenError.NetworkError(isConnected = false)
        val warningError = HomeScreenError.LocationPermissionDenied()
        val infoError = HomeScreenError.PerformanceIssue(45.0, "Minor issue")
        
        // Critical errors should show until dismissed (0ms)
        assert(HomeScreenErrorUtils.getDisplayDurationMs(criticalError) == 0L)
        
        // Warning errors should show for 8 seconds
        assert(HomeScreenErrorUtils.getDisplayDurationMs(warningError) == 8000L)
        
        // Info errors should show for 5 seconds
        assert(HomeScreenErrorUtils.getDisplayDurationMs(infoError) == 5000L)
    }
    
    @Test
    fun homeScreenErrorUtils_formatsForLogging() {
        val error = HomeScreenError.MapLoadFailed("Network timeout")
        val logFormat = HomeScreenErrorUtils.formatForLogging(error)
        
        assert(logFormat.contains("[Warning]"))
        assert(logFormat.contains("Map Unavailable"))
        assert(logFormat.contains("Map load failed: Network timeout"))
    }
    
    @Test
    fun homeScreenErrorUtils_providesRecoverySuggestions() {
        // Test location permission suggestions
        val permissionError = HomeScreenError.LocationPermissionDenied(isPermanent = true)
        val permissionSuggestions = HomeScreenErrorUtils.getRecoverySuggestions(permissionError)
        
        assert(permissionSuggestions.contains("Open device Settings"))
        assert(permissionSuggestions.contains("Navigate to Apps > FFinder > Permissions"))
        assert(permissionSuggestions.contains("Enable Location permission"))
        
        // Test network error suggestions
        val networkError = HomeScreenError.NetworkError(isConnected = false)
        val networkSuggestions = HomeScreenErrorUtils.getRecoverySuggestions(networkError)
        
        assert(networkSuggestions.contains("Check Wi-Fi connection"))
        assert(networkSuggestions.contains("Check mobile data"))
        assert(networkSuggestions.contains("Try moving to an area with better signal"))
        
        // Test performance issue suggestions
        val performanceError = HomeScreenError.PerformanceIssue(35.0, "Low FPS")
        val performanceSuggestions = HomeScreenErrorUtils.getRecoverySuggestions(performanceError)
        
        assert(performanceSuggestions.contains("Reduce animation quality in Settings"))
        assert(performanceSuggestions.contains("Close other apps to free memory"))
        assert(performanceSuggestions.contains("Restart the device if needed"))
    }
    
    @Test
    fun rememberHomeScreenErrorState_managesStateCorrectly() {
        var capturedErrorState: HomeScreenErrorState? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                capturedErrorState = rememberHomeScreenErrorState()
            }
        }
        
        composeTestRule.waitForIdle()
        
        capturedErrorState?.let { errorState ->
            // Initial state should have no error
            assert(!errorState.hasError)
            assert(errorState.errorCount == 0)
            assert(!errorState.hasCriticalError)
            
            // Test showing an error
            val testError = HomeScreenError.MapLoadFailed("Test error")
            errorState.showError(testError)
            
            // State should now have error
            assert(errorState.hasError)
            assert(errorState.currentError == testError)
            assert(errorState.errorCount == 1)
            
            // Test dismissing error
            errorState.dismissError()
            
            // Error should be dismissed but history preserved
            assert(!errorState.hasError)
            assert(errorState.errorCount == 1) // History still contains the error
            
            // Test clearing history
            errorState.clearHistory()
            assert(errorState.errorCount == 0)
        }
    }
    
    @Test
    fun errorSeverity_enumValuesAreCorrect() {
        val severities = ErrorSeverity.values()
        
        assert(severities.contains(ErrorSeverity.Info))
        assert(severities.contains(ErrorSeverity.Warning))
        assert(severities.contains(ErrorSeverity.Critical))
        assert(severities.size == 3)
    }
}