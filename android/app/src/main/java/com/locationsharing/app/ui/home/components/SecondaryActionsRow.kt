package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.ui.components.button.ButtonResponseManager
import com.locationsharing.app.ui.components.button.ButtonResponseManagerViewModel
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Secondary navigation actions row component for the FFinder Home Screen.
 * 
 * Enhanced with ResponsiveButton integration for proper navigation handling,
 * loading states, haptic feedback, and visual feedback. Displays two buttons
 * side-by-side for Friends and Settings navigation.
 * 
 * @param onFriends Callback function invoked when Friends button is tapped
 * @param onSettings Callback function invoked when Settings button is tapped
 * @param navigationManager NavigationManager for handling navigation operations
 * @param modifier Modifier to be applied to the component
 * @param friendsLoading Whether the Friends button should show loading state
 * @param settingsLoading Whether the Settings button should show loading state
 * @param friendsEnabled Whether the Friends button should be enabled
 * @param settingsEnabled Whether the Settings button should be enabled
 */
@Composable
fun SecondaryActionsRow(
    onFriends: () -> Unit,
    onSettings: () -> Unit,
    navigationManager: NavigationManager? = null,
    modifier: Modifier = Modifier,
    friendsLoading: Boolean = false,
    settingsLoading: Boolean = false,
    friendsEnabled: Boolean = true,
    settingsEnabled: Boolean = true
) {
    val buttonResponseManager = hiltViewModel<ButtonResponseManagerViewModel>().buttonResponseManager
    val friendsButtonId = remember { "secondary_friends_button" }
    val settingsButtonId = remember { "secondary_settings_button" }
    
    // Get button states from manager
    val friendsButtonState by buttonResponseManager.getButtonState(friendsButtonId).collectAsState()
    val settingsButtonState by buttonResponseManager.getButtonState(settingsButtonId).collectAsState()
    
    // Update manager states when props change
    if (friendsButtonState.isEnabled != friendsEnabled) {
        buttonResponseManager.setButtonEnabled(friendsButtonId, friendsEnabled)
    }
    if (friendsButtonState.isLoading != friendsLoading) {
        buttonResponseManager.setButtonLoading(friendsButtonId, friendsLoading)
    }
    if (settingsButtonState.isEnabled != settingsEnabled) {
        buttonResponseManager.setButtonEnabled(settingsButtonId, settingsEnabled)
    }
    if (settingsButtonState.isLoading != settingsLoading) {
        buttonResponseManager.setButtonLoading(settingsButtonId, settingsLoading)
    }
    
    val isFriendsEnabled = friendsButtonState.canClick && friendsEnabled
    val isFriendsLoading = friendsButtonState.isLoading || friendsLoading
    val showFriendsFeedback = friendsButtonState.showFeedback
    
    val isSettingsEnabled = settingsButtonState.canClick && settingsEnabled
    val isSettingsLoading = settingsButtonState.isLoading || settingsLoading
    val showSettingsFeedback = settingsButtonState.showFeedback
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Friends Button - 50% width with enhanced responsiveness
        OutlinedButton(
            onClick = {
                if (isFriendsEnabled && !isFriendsLoading) {
                    buttonResponseManager.handleButtonClick(friendsButtonId) {
                        navigationManager?.navigateToFriends()
                        onFriends()
                    }
                }
            },
            modifier = Modifier
                .weight(0.5f)
                .semantics {
                    contentDescription = if (isFriendsLoading) {
                        "Friends button, loading"
                    } else {
                        "Friends button. Navigate to friends list and management."
                    }
                    role = Role.Button
                },
            enabled = isFriendsEnabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (showFriendsFeedback) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                containerColor = if (showFriendsFeedback) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = if (showFriendsFeedback) 2.dp else 1.dp
            )
        ) {
            if (isFriendsLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Text(
                    text = "👥",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Friends",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        
        // Settings Button - 50% width with enhanced responsiveness
        OutlinedButton(
            onClick = {
                if (isSettingsEnabled && !isSettingsLoading) {
                    buttonResponseManager.handleButtonClick(settingsButtonId) {
                        navigationManager?.navigateToSettings()
                        onSettings()
                    }
                }
            },
            modifier = Modifier
                .weight(0.5f)
                .semantics {
                    contentDescription = if (isSettingsLoading) {
                        "Settings button, loading"
                    } else {
                        "Settings button. Navigate to app settings and preferences."
                    }
                    role = Role.Button
                },
            enabled = isSettingsEnabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (showSettingsFeedback) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                containerColor = if (showSettingsFeedback) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = if (showSettingsFeedback) 2.dp else 1.dp
            )
        ) {
            if (isSettingsLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Text(
                    text = "⚙️",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SecondaryActionsRowPreview() {
    FFinderTheme {
        SecondaryActionsRow(
            onFriends = { /* Preview action */ },
            onSettings = { /* Preview action */ },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
fun SecondaryActionsRowDarkPreview() {
    FFinderTheme(darkTheme = true) {
        SecondaryActionsRow(
            onFriends = { /* Preview action */ },
            onSettings = { /* Preview action */ },
            modifier = Modifier.padding(16.dp)
        )
    }
}