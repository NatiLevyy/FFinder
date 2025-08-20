package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locationsharing.app.R
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Comprehensive error handling component for the FFinder Home Screen.
 * 
 * This component provides centralized error handling with user-friendly messages,
 * recovery actions, and proper accessibility support for all home screen errors.
 * 
 * Features:
 * - User-friendly error messages
 * - Contextual recovery actions
 * - Accessibility-compliant error announcements
 * - Error categorization and prioritization
 * - Automatic error recovery suggestions
 * 
 * @param error The error to display
 * @param onRetry Callback for retry actions
 * @param onDismiss Callback for dismissing the error
 * @param modifier Modifier for styling
 */
@Composable
fun HomeScreenErrorHandler(
    error: HomeScreenError,
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hapticManager = rememberHapticFeedbackManager()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Error: ${error.userMessage}. ${error.recoveryAction ?: ""}"
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (error.severity) {
                ErrorSeverity.Critical -> MaterialTheme.colorScheme.errorContainer
                ErrorSeverity.Warning -> MaterialTheme.colorScheme.secondaryContainer
                ErrorSeverity.Info -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Error icon
            Icon(
                painter = painterResource(
                    id = when (error.severity) {
                        ErrorSeverity.Critical -> R.drawable.ic_pin_finder_optimized // Use as error icon
        ErrorSeverity.Warning -> R.drawable.ic_pin_finder_optimized
        ErrorSeverity.Info -> R.drawable.ic_pin_finder_optimized
                    }
                ),
                contentDescription = "${error.severity.name} error",
                modifier = Modifier.size(24.dp),
                tint = when (error.severity) {
                    ErrorSeverity.Critical -> MaterialTheme.colorScheme.onErrorContainer
                    ErrorSeverity.Warning -> MaterialTheme.colorScheme.onSecondaryContainer
                    ErrorSeverity.Info -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Error content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Error title
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = when (error.severity) {
                        ErrorSeverity.Critical -> MaterialTheme.colorScheme.onErrorContainer
                        ErrorSeverity.Warning -> MaterialTheme.colorScheme.onSecondaryContainer
                        ErrorSeverity.Info -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Error message
                Text(
                    text = error.userMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (error.severity) {
                        ErrorSeverity.Critical -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        ErrorSeverity.Warning -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        ErrorSeverity.Info -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    fontSize = 12.sp
                )
                
                // Recovery action text
                error.recoveryAction?.let { action ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = action,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (error.severity) {
                            ErrorSeverity.Critical -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            ErrorSeverity.Warning -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            ErrorSeverity.Info -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Action buttons
            if (error.canRetry) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        hapticManager.performSecondaryAction()
                        onRetry()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = when (error.severity) {
                            ErrorSeverity.Critical -> MaterialTheme.colorScheme.onErrorContainer
                            ErrorSeverity.Warning -> MaterialTheme.colorScheme.onSecondaryContainer
                            ErrorSeverity.Info -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (error.canDismiss) {
                IconButton(
                    onClick = {
                        hapticManager.performSecondaryAction()
                        onDismiss()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pin_finder_optimized), // Use as close icon
                        contentDescription = "Dismiss error",
                        modifier = Modifier.size(16.dp),
                        tint = when (error.severity) {
                            ErrorSeverity.Critical -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            ErrorSeverity.Warning -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            ErrorSeverity.Info -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Sealed class representing different types of home screen errors.
 */
sealed class HomeScreenError(
    val title: String,
    val userMessage: String,
    val technicalMessage: String,
    val severity: ErrorSeverity,
    val canRetry: Boolean = true,
    val canDismiss: Boolean = true,
    val recoveryAction: String? = null
) {
    
    // Location-related errors
    data class LocationPermissionDenied(
        val isPermanent: Boolean = false
    ) : HomeScreenError(
        title = "Location Access Required",
        userMessage = if (isPermanent) {
            "Location access is permanently denied. Please enable it in Settings to see your area on the map."
        } else {
            "Location access is needed to show your area on the map and enable location sharing."
        },
        technicalMessage = "Location permission denied",
        severity = ErrorSeverity.Warning,
        canRetry = !isPermanent,
        recoveryAction = if (isPermanent) "Go to Settings > Apps > FFinder > Permissions" else "Tap Retry to grant permission"
    )
    
    data class LocationServiceUnavailable(
        val reason: String = "Unknown"
    ) : HomeScreenError(
        title = "Location Service Unavailable",
        userMessage = "Unable to access your location. Please check that location services are enabled.",
        technicalMessage = "Location service unavailable: $reason",
        severity = ErrorSeverity.Warning,
        recoveryAction = "Check Settings > Location Services"
    )
    
    data class LocationTimeout(
        val timeoutMs: Long
    ) : HomeScreenError(
        title = "Location Timeout",
        userMessage = "Taking longer than expected to find your location. Please try again.",
        technicalMessage = "Location request timed out after ${timeoutMs}ms",
        severity = ErrorSeverity.Info,
        recoveryAction = "Ensure you're in an area with good GPS signal"
    )
    
    // Map-related errors
    data class MapLoadFailed(
        val reason: String = "Unknown"
    ) : HomeScreenError(
        title = "Map Unavailable",
        userMessage = "Unable to load the map preview. Please check your internet connection and try again.",
        technicalMessage = "Map load failed: $reason",
        severity = ErrorSeverity.Warning,
        recoveryAction = "Check your internet connection"
    )
    
    data class MapServiceError(
        val errorCode: Int? = null
    ) : HomeScreenError(
        title = "Map Service Error",
        userMessage = "Google Maps service is temporarily unavailable. Please try again later.",
        technicalMessage = "Map service error${errorCode?.let { " (code: $it)" } ?: ""}",
        severity = ErrorSeverity.Warning,
        recoveryAction = "Try again in a few moments"
    )
    
    // Network-related errors
    data class NetworkError(
        val isConnected: Boolean
    ) : HomeScreenError(
        title = "Connection Issue",
        userMessage = if (isConnected) {
            "Poor internet connection. Some features may not work properly."
        } else {
            "No internet connection. Please check your network settings."
        },
        technicalMessage = "Network error - connected: $isConnected",
        severity = if (isConnected) ErrorSeverity.Warning else ErrorSeverity.Critical,
        recoveryAction = "Check your Wi-Fi or mobile data connection"
    )
    
    // Performance-related errors
    data class PerformanceIssue(
        val currentFps: Double,
        val issue: String
    ) : HomeScreenError(
        title = "Performance Issue",
        userMessage = "Animations may appear choppy. Consider reducing animation quality in settings.",
        technicalMessage = "Performance issue: $issue (FPS: $currentFps)",
        severity = ErrorSeverity.Info,
        canRetry = false,
        recoveryAction = "Reduce animation quality in Settings"
    )
    
    // General errors
    data class UnknownError(
        val exception: Throwable? = null
    ) : HomeScreenError(
        title = "Something Went Wrong",
        userMessage = "An unexpected error occurred. Please try again.",
        technicalMessage = exception?.message ?: "Unknown error",
        severity = ErrorSeverity.Critical,
        recoveryAction = "Restart the app if the problem persists"
    )
    
    data class FeatureUnavailable(
        val featureName: String,
        val reason: String
    ) : HomeScreenError(
        title = "Feature Unavailable",
        userMessage = "$featureName is currently unavailable. $reason",
        technicalMessage = "Feature unavailable: $featureName - $reason",
        severity = ErrorSeverity.Info,
        canRetry = false,
        canDismiss = true
    )
}

/**
 * Enum representing error severity levels.
 */
enum class ErrorSeverity {
    Info,       // Informational, doesn't block functionality
    Warning,    // May impact functionality but app remains usable
    Critical    // Blocks core functionality
}

/**
 * Error handler utility for managing home screen errors.
 */
object HomeScreenErrorUtils {
    
    /**
     * Creates appropriate error based on exception type.
     */
    fun createErrorFromException(exception: Throwable): HomeScreenError {
        return when (exception) {
            is SecurityException -> HomeScreenError.LocationPermissionDenied()
            is java.net.UnknownHostException -> HomeScreenError.NetworkError(isConnected = false)
            is java.net.SocketTimeoutException -> HomeScreenError.LocationTimeout(timeoutMs = 10000)
            else -> HomeScreenError.UnknownError(exception)
        }
    }
    
    /**
     * Determines if an error should be shown immediately or queued.
     */
    fun shouldShowImmediately(error: HomeScreenError): Boolean {
        return when (error.severity) {
            ErrorSeverity.Critical -> true
            ErrorSeverity.Warning -> true
            ErrorSeverity.Info -> false
        }
    }
    
    /**
     * Gets the display duration for an error based on its severity.
     */
    fun getDisplayDurationMs(error: HomeScreenError): Long {
        return when (error.severity) {
            ErrorSeverity.Critical -> 0L // Show until dismissed
            ErrorSeverity.Warning -> 8000L // 8 seconds
            ErrorSeverity.Info -> 5000L // 5 seconds
        }
    }
    
    /**
     * Formats error for logging purposes.
     */
    fun formatForLogging(error: HomeScreenError): String {
        return "[${error.severity}] ${error.title}: ${error.technicalMessage}"
    }
    
    /**
     * Gets user-friendly recovery suggestions.
     */
    fun getRecoverySuggestions(error: HomeScreenError): List<String> {
        return when (error) {
            is HomeScreenError.LocationPermissionDenied -> {
                if (error.isPermanent) {
                    listOf(
                        "Open device Settings",
                        "Navigate to Apps > FFinder > Permissions",
                        "Enable Location permission"
                    )
                } else {
                    listOf("Tap 'Enable Location' to grant permission")
                }
            }
            
            is HomeScreenError.NetworkError -> {
                listOf(
                    "Check Wi-Fi connection",
                    "Check mobile data",
                    "Try moving to an area with better signal"
                )
            }
            
            is HomeScreenError.MapLoadFailed -> {
                listOf(
                    "Check internet connection",
                    "Wait a moment and try again",
                    "Restart the app if problem persists"
                )
            }
            
            is HomeScreenError.PerformanceIssue -> {
                listOf(
                    "Reduce animation quality in Settings",
                    "Close other apps to free memory",
                    "Restart the device if needed"
                )
            }
            
            else -> {
                listOf(
                    "Try again in a moment",
                    "Restart the app if problem persists"
                )
            }
        }
    }
}

/**
 * Composable that manages error state for the entire home screen.
 */
@Composable
fun rememberHomeScreenErrorState(): HomeScreenErrorState {
    var currentError by remember { mutableStateOf<HomeScreenError?>(null) }
    var errorHistory by remember { mutableStateOf<List<HomeScreenError>>(emptyList()) }
    
    return remember {
        HomeScreenErrorState(
            currentError = currentError,
            errorHistory = errorHistory,
            showError = { error ->
                currentError = error
                errorHistory = errorHistory + error
            },
            dismissError = {
                currentError = null
            },
            clearHistory = {
                errorHistory = emptyList()
            }
        )
    }
}

/**
 * Data class representing the error state for the home screen.
 */
data class HomeScreenErrorState(
    val currentError: HomeScreenError?,
    val errorHistory: List<HomeScreenError>,
    val showError: (HomeScreenError) -> Unit,
    val dismissError: () -> Unit,
    val clearHistory: () -> Unit
) {
    val hasError: Boolean
        get() = currentError != null
    
    val errorCount: Int
        get() = errorHistory.size
    
    val hasCriticalError: Boolean
        get() = currentError?.severity == ErrorSeverity.Critical
}

@Preview(showBackground = true)
@Composable
fun HomeScreenErrorHandlerPreview() {
    FFinderTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location permission error
            HomeScreenErrorHandler(
                error = HomeScreenError.LocationPermissionDenied(isPermanent = false),
                onRetry = {},
                onDismiss = {}
            )
            
            // Map load error
            HomeScreenErrorHandler(
                error = HomeScreenError.MapLoadFailed("Network timeout"),
                onRetry = {},
                onDismiss = {}
            )
            
            // Network error
            HomeScreenErrorHandler(
                error = HomeScreenError.NetworkError(isConnected = false),
                onRetry = {},
                onDismiss = {}
            )
            
            // Performance issue
            HomeScreenErrorHandler(
                error = HomeScreenError.PerformanceIssue(
                    currentFps = 35.2,
                    issue = "Low frame rate detected"
                ),
                onRetry = {},
                onDismiss = {}
            )
        }
    }
}