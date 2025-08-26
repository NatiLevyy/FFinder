package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.locationsharing.app.R
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.ui.components.button.ButtonResponseManager
import com.locationsharing.app.ui.components.button.ButtonResponseManagerViewModel

/**
 * Primary Call-to-Action component for the FFinder Home Screen.
 * 
 * Enhanced with ResponsiveButton integration for proper navigation handling,
 * loading states, haptic feedback, and visual feedback. Adapts to narrow screens
 * by showing an icon-only FAB with tooltip.
 * 
 * @param onStartShare Callback invoked when the user taps the CTA button
 * @param navigationManager NavigationManager for handling navigation operations
 * @param modifier Modifier for styling the component
 * @param isNarrowScreen Whether the screen width is narrow (< 360dp)
 * @param loading Whether the button should show loading state
 * @param enabled Whether the button should be enabled
 */
@Composable
fun PrimaryCallToAction(
    onStartShare: () -> Unit,
    navigationManager: NavigationManager? = null,
    modifier: Modifier = Modifier,
    isNarrowScreen: Boolean = false,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    val buttonResponseManager = hiltViewModel<ButtonResponseManagerViewModel>().buttonResponseManager
    val buttonId = remember { "primary_cta_start_sharing" }
    
    // Get button state from manager
    val buttonState by buttonResponseManager.getButtonState(buttonId).collectAsState()
    
    // Update manager state when props change
    if (buttonState.isEnabled != enabled) {
        buttonResponseManager.setButtonEnabled(buttonId, enabled)
    }
    if (buttonState.isLoading != loading) {
        buttonResponseManager.setButtonLoading(buttonId, loading)
    }
    
    val isButtonEnabled = buttonState.canClick && enabled
    val isButtonLoading = buttonState.isLoading || loading
    val showFeedback = buttonState.showFeedback
    
    if (isNarrowScreen) {
        // Icon-only FAB for narrow screens with enhanced responsiveness
        FloatingActionButton(
            onClick = {
                if (isButtonEnabled && !isButtonLoading) {
                    buttonResponseManager.handleButtonClick(buttonId) {
                        // Navigate to map screen after starting sharing
                        navigationManager?.navigateToMap()
                        onStartShare()
                    }
                }
            },
            modifier = modifier
                .size(56.dp) // Meets minimum 48dp touch target
                .semantics {
                    contentDescription = if (isButtonLoading) {
                        "Start Live Sharing, loading"
                    } else {
                        "Start Live Sharing. Tap to begin sharing your location with friends."
                    }
                    role = Role.Button
                },
            containerColor = if (showFeedback) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = if (showFeedback) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                Color.White
            },
            elevation = if (showFeedback) {
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            } else {
                FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            },
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isButtonLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = if (showFeedback) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        Color.White
                    }
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pin_finder_vector),
                    contentDescription = null, // Handled by parent FAB
                    modifier = Modifier.size(24.dp),
                    tint = if (showFeedback) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        Color.White
                    }
                )
            }
        }
    } else {
        // Extended FAB for normal screens with enhanced responsiveness
        ExtendedFloatingActionButton(
            onClick = {
                if (isButtonEnabled && !isButtonLoading) {
                    buttonResponseManager.handleButtonClick(buttonId) {
                        // Navigate to map screen after starting sharing
                        navigationManager?.navigateToMap()
                        onStartShare()
                    }
                }
            },
            modifier = modifier
                .fillMaxWidth(0.8f)
                .semantics {
                    contentDescription = if (isButtonLoading) {
                        "Start Live Sharing button, loading"
                    } else {
                        "Start Live Sharing button. Tap to begin sharing your location with friends."
                    }
                    role = Role.Button
                },
            containerColor = if (showFeedback) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = if (showFeedback) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                Color.White
            },
            elevation = if (showFeedback) {
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            } else {
                FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            },
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isButtonLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (showFeedback) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        Color.White
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pin_finder_vector),
                    contentDescription = null, // Handled by parent FAB
                    modifier = Modifier.size(24.dp),
                    tint = if (showFeedback) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        Color.White
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = "Start Live Sharing",
                style = MaterialTheme.typography.labelLarge,
                color = if (showFeedback) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    Color.White
                }
            )
        }
    }
}