package com.locationsharing.app.ui.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Location error handler component for displaying location-related errors
 * Implements requirements 7.4, 7.5 for location error handling and user feedback
 */
@Composable
fun LocationErrorHandler(
    error: String?,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showAsSnackbar: Boolean = false
) {
    if (error == null) return
    
    if (showAsSnackbar) {
        LocationErrorSnackbar(
            error = error,
            onRetry = onRetry,
            onDismiss = onDismiss,
            modifier = modifier
        )
    } else {
        LocationErrorCard(
            error = error,
            onRetry = onRetry,
            onOpenSettings = onOpenSettings,
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
}

/**
 * Location error displayed as a card overlay
 */
@Composable
private fun LocationErrorCard(
    error: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errorType = determineErrorType(error)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics {
                contentDescription = "Location error: $error"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error icon
            Icon(
                imageVector = errorType.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Error title
            Text(
                text = errorType.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Error message
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dismiss button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        contentDescription = "Dismiss error message"
                    }
                ) {
                    Text("Dismiss")
                }
                
                // Settings button (for permission errors)
                if (errorType.showSettingsButton) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.semantics {
                            contentDescription = "Open device settings"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Settings")
                    }
                }
                
                // Retry button
                Button(
                    onClick = onRetry,
                    modifier = Modifier.semantics {
                        contentDescription = "Retry location request"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * Location error displayed as a snackbar
 */
@Composable
private fun LocationErrorSnackbar(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(error) {
        snackbarHostState.showSnackbar(
            message = error,
            actionLabel = "Retry",
            withDismissAction = true
        )
    }
    
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { snackbarData ->
        Snackbar(
            snackbarData = snackbarData,
            actionOnNewLine = false,
            modifier = Modifier.semantics {
                contentDescription = "Location error: $error"
            }
        )
    }
}

/**
 * Determine error type based on error message
 */
private fun determineErrorType(error: String): LocationErrorType {
    return when {
        error.contains("permission", ignoreCase = true) -> LocationErrorType.PERMISSION_DENIED
        error.contains("location services", ignoreCase = true) || 
        error.contains("GPS", ignoreCase = true) -> LocationErrorType.LOCATION_SERVICES_DISABLED
        error.contains("network", ignoreCase = true) || 
        error.contains("connection", ignoreCase = true) -> LocationErrorType.NETWORK_ERROR
        error.contains("timeout", ignoreCase = true) -> LocationErrorType.TIMEOUT
        else -> LocationErrorType.GENERAL_ERROR
    }
}

/**
 * Location error types with associated UI properties
 */
private enum class LocationErrorType(
    val title: String,
    val icon: ImageVector,
    val showSettingsButton: Boolean
) {
    PERMISSION_DENIED(
        title = "Location Permission Required",
        icon = Icons.Default.LocationOff,
        showSettingsButton = true
    ),
    LOCATION_SERVICES_DISABLED(
        title = "Location Services Disabled",
        icon = Icons.Default.LocationOff,
        showSettingsButton = true
    ),
    NETWORK_ERROR(
        title = "Network Error",
        icon = Icons.Default.Error,
        showSettingsButton = false
    ),
    TIMEOUT(
        title = "Location Request Timeout",
        icon = Icons.Default.Error,
        showSettingsButton = false
    ),
    GENERAL_ERROR(
        title = "Location Error",
        icon = Icons.Default.Error,
        showSettingsButton = false
    )
}

/**
 * Composable for handling multiple location error states
 */
@Composable
fun LocationErrorStates(
    locationError: String?,
    locationSharingError: String?,
    onRetryLocation: () -> Unit,
    onRetryLocationSharing: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissLocationError: () -> Unit,
    onDismissLocationSharingError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Location error
        LocationErrorHandler(
            error = locationError,
            onRetry = onRetryLocation,
            onOpenSettings = onOpenSettings,
            onDismiss = onDismissLocationError,
            showAsSnackbar = false
        )
        
        // Location sharing error (as snackbar to avoid stacking)
        LocationErrorHandler(
            error = locationSharingError,
            onRetry = onRetryLocationSharing,
            onOpenSettings = onOpenSettings,
            onDismiss = onDismissLocationSharingError,
            showAsSnackbar = true
        )
    }
}

/**
 * Preview for location error card
 */
@Preview(showBackground = true)
@Composable
fun LocationErrorCardPreview() {
    FFinderTheme {
        LocationErrorCard(
            error = "Location permission is required to show your position on the map and share your location with friends. Please grant location permission in settings.",
            onRetry = {},
            onOpenSettings = {},
            onDismiss = {}
        )
    }
}

/**
 * Preview for location services error
 */
@Preview(showBackground = true)
@Composable
fun LocationServicesErrorPreview() {
    FFinderTheme {
        LocationErrorCard(
            error = "Location services are disabled. Please enable GPS or network location in your device settings.",
            onRetry = {},
            onOpenSettings = {},
            onDismiss = {}
        )
    }
}

/**
 * Preview for network error
 */
@Preview(showBackground = true)
@Composable
fun NetworkErrorPreview() {
    FFinderTheme {
        LocationErrorCard(
            error = "Unable to connect to location services. Please check your internet connection and try again.",
            onRetry = {},
            onOpenSettings = {},
            onDismiss = {}
        )
    }
}