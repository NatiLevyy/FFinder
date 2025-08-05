package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.R
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.theme.FFinderTheme
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

/**
 * A scaffold component that wraps MapScreen with ModalNavigationDrawer for the Friends Nearby Panel.
 * 
 * Implements requirements:
 * - 1.2: Toggle Modal Navigation Drawer when friends button is tapped
 * - 1.3: Allow tap outside or close button to dismiss drawer
 * - 1.4: Maintain map view state when panel toggles
 * 
 * Features:
 * - 320dp panel width as specified in design
 * - Proper drawer state management with smooth animations
 * - Tap-outside-to-close functionality (built into ModalNavigationDrawer)
 * - Maintains map view state during panel transitions
 * - Accessibility support for drawer navigation
 * 
 * @param uiState The current UI state containing panel visibility and friends data
 * @param onEvent Callback for handling user events
 * @param modifier Modifier for styling the scaffold
 * @param content The main content (typically MapScreen) to be wrapped
 */
@Composable
fun FriendsPanelScaffold(
    uiState: NearbyUiState,
    onEvent: (NearbyPanelEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val accessibilityManager = LocalAccessibilityManager.current
    
    // Drawer state management - synced with uiState.isPanelOpen
    val drawerState = rememberDrawerState(
        initialValue = if (uiState.isPanelOpen) DrawerValue.Open else DrawerValue.Closed
    )
    
    // Sync drawer state with UI state and announce state changes
    LaunchedEffect(uiState.isPanelOpen) {
        if (uiState.isPanelOpen && drawerState.isClosed) {
            drawerState.open()
            // The announcement will be handled by the drawer's built-in accessibility
        } else if (!uiState.isPanelOpen && drawerState.isOpen) {
            drawerState.close()
            // The announcement will be handled by the drawer's built-in accessibility
        }
    }
    
    // Handle drawer state changes from gestures (tap outside, swipe)
    LaunchedEffect(drawerState.targetValue) {
        when (drawerState.targetValue) {
            DrawerValue.Open -> {
                if (!uiState.isPanelOpen) {
                    onEvent(NearbyPanelEvent.TogglePanel)
                }
            }
            DrawerValue.Closed -> {
                if (uiState.isPanelOpen) {
                    onEvent(NearbyPanelEvent.TogglePanel)
                }
            }
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FriendsNearbyPanel(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier
                    .width(320.dp) // Specified panel width
                    .semantics {
                        contentDescription = "Friends nearby panel"
                    }
            )
        },
        modifier = modifier.semantics {
            contentDescription = "Map screen with friends panel"
        },
        gesturesEnabled = true // Enable swipe and tap-outside gestures
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main content (MapScreen) - maintains its state during panel transitions
            content()
            
            // Friends Toggle FAB positioned in top-right corner
            FriendsToggleFAB(
                isOpen = uiState.isPanelOpen,
                onClick = { 
                    scope.launch {
                        onEvent(NearbyPanelEvent.TogglePanel)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPanelScaffoldClosedPreview() {
    FFinderTheme {
        FriendsPanelScaffold(
            uiState = NearbyUiState(
                isPanelOpen = false,
                isLoading = false,
                friends = listOf(
                    NearbyFriend(
                        id = "1",
                        displayName = "Alice Johnson",
                        avatarUrl = null,
                        distance = 150.0,
                        isOnline = true,
                        lastUpdated = System.currentTimeMillis(),
                        latLng = LatLng(37.7749, -122.4194)
                    )
                )
            ),
            onEvent = {}
        ) {
            // Mock map content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "Map Content Here",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPanelScaffoldOpenPreview() {
    FFinderTheme {
        FriendsPanelScaffold(
            uiState = NearbyUiState(
                isPanelOpen = true,
                isLoading = false,
                friends = listOf(
                    NearbyFriend(
                        id = "1",
                        displayName = "Alice Johnson",
                        avatarUrl = null,
                        distance = 150.0,
                        isOnline = true,
                        lastUpdated = System.currentTimeMillis(),
                        latLng = LatLng(37.7749, -122.4194)
                    ),
                    NearbyFriend(
                        id = "2",
                        displayName = "Bob Smith",
                        avatarUrl = null,
                        distance = 1200.0,
                        isOnline = false,
                        lastUpdated = System.currentTimeMillis() - 300000,
                        latLng = LatLng(37.7849, -122.4094)
                    )
                )
            ),
            onEvent = {}
        ) {
            // Mock map content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "Map Content Here",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}