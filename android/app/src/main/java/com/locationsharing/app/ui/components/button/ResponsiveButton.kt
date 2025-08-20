package com.locationsharing.app.ui.components.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ripple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.delay

/**
 * Enumeration of button types for styling.
 */
enum class ButtonType {
    PRIMARY,
    SECONDARY,
    TERTIARY
}



/**
 * A responsive button component that provides enhanced visual feedback, debouncing,
 * and state management for navigation and other actions.
 * 
 * @param text The text to display on the button
 * @param onClick The action to execute when button is clicked
 * @param modifier Modifier for styling the button
 * @param enabled Whether the button is enabled
 * @param loading Whether the button should show loading state
 * @param buttonType The type of button for styling
 * @param buttonResponseManager The manager for handling button responses
 * @param showRippleEffect Whether to show enhanced ripple effects
 * @param enableHapticFeedback Whether to enable haptic feedback
 */
@Composable
fun ResponsiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    buttonType: ButtonType = ButtonType.PRIMARY,
    buttonResponseManager: ButtonResponseManager = hiltViewModel<ButtonResponseManagerViewModel>().buttonResponseManager,
    showRippleEffect: Boolean = true,
    enableHapticFeedback: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current
    val buttonId = remember { "button_${text.hashCode()}" }
    
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
    
    // Animation states
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed && isButtonEnabled) 0.95f else 1f,
        animationSpec = if (isPressed) {
            FFinderAnimations.MicroInteractions.buttonPress()
        } else {
            FFinderAnimations.MicroInteractions.buttonRelease()
        },
        label = "button_scale"
    )
    
    // Enhanced color animations
    val containerColor by animateColorAsState(
        targetValue = getContainerColor(buttonType, showFeedback, isButtonEnabled),
        animationSpec = FFinderAnimations.MicroInteractions.colorTransition(),
        label = "container_color"
    )
    
    val contentColor by animateColorAsState(
        targetValue = getContentColor(buttonType, showFeedback, isButtonEnabled),
        animationSpec = FFinderAnimations.MicroInteractions.colorTransition(),
        label = "content_color"
    )
    
    // Ripple effect management
    val interactionSource = remember { MutableInteractionSource() }
    val rippleColor = when (buttonType) {
        ButtonType.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        ButtonType.SECONDARY -> MaterialTheme.colorScheme.onSecondary
        ButtonType.TERTIARY -> MaterialTheme.colorScheme.onTertiary
    }
    val rippleAlpha = if (showRippleEffect) 0.32f else 0.16f
    
    // Handle press feedback timing
    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            isPressed = true
            delay(150) // Visual feedback duration
            isPressed = false
        }
    }
    
    Box(
        modifier = modifier.scale(buttonScale)
    ) {
            Button(
                onClick = {
                    if (isButtonEnabled && !isButtonLoading) {
                        if (enableHapticFeedback) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        buttonResponseManager.handleButtonClick(buttonId) {
                            onClick()
                        }
                    }
                },
                enabled = isButtonEnabled,
                modifier = Modifier
                    .semantics {
                        contentDescription = if (isButtonLoading) "$text, loading" else text
                        stateDescription = when {
                            isButtonLoading -> "Loading"
                            !isButtonEnabled -> "Disabled"
                            showFeedback -> "Pressed"
                            else -> "Enabled"
                        }
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (showFeedback) 8.dp else 4.dp,
                    pressedElevation = if (showFeedback) 2.dp else 1.dp,
                    disabledElevation = 0.dp
                ),
                border = if (buttonType == ButtonType.SECONDARY) {
                    BorderStroke(
                        width = 1.dp,
                        color = if (isButtonEnabled) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                } else null,
                interactionSource = interactionSource
            ) {
                if (isButtonLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

@Composable
private fun getContainerColor(
    buttonType: ButtonType,
    showFeedback: Boolean,
    isEnabled: Boolean
): Color = when (buttonType) {
    ButtonType.PRIMARY -> if (showFeedback) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    ButtonType.SECONDARY -> if (showFeedback) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    ButtonType.TERTIARY -> if (showFeedback) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        Color.Transparent
    }
}

@Composable
private fun getContentColor(
    buttonType: ButtonType,
    showFeedback: Boolean,
    isEnabled: Boolean
): Color = when (buttonType) {
    ButtonType.PRIMARY -> if (showFeedback) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    ButtonType.SECONDARY -> if (showFeedback) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    ButtonType.TERTIARY -> if (showFeedback) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
}