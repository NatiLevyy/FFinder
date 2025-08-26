package com.locationsharing.app.ui.map.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.ui.map.MapScreenConstants
import kotlinx.coroutines.delay

/**
 * Debug Snackbar component for showing debug messages
 * Only visible in debug builds according to requirement 4.3
 */
@Composable
fun DebugSnackbar(
    message: String?,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // Only render in debug builds
    if (!BuildConfig.DEBUG) return
    
    // Show snackbar when message is not null
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                withDismissAction = true
            )
            // Auto-dismiss after 3 seconds (requirement 4.3)
            delay(MapScreenConstants.Debug.DEBUG_SNACKBAR_DURATION.toLong())
            onDismiss()
        }
    }
    
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(16.dp)
    ) { snackbarData ->
        Snackbar(
            snackbarData = snackbarData,
            containerColor = MapScreenConstants.Debug.DEBUG_FAB_COLOR,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            actionColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}