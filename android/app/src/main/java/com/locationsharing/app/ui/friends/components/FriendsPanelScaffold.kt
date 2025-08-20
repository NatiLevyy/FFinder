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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.R
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.theme.FFinderTheme
import com.locationsharing.app.ui.friends.logging.NearbyPanelLogger
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A scaffold component that wraps MapScreen with ModalNavigationDrawer for the Friends Nearby Panel.
 * 
 * Implements requirements:
 * - 1.2: Toggle Modal Navigation Drawer when friends button is tapped
 * - 1.3: Allow tap outside or close button to dismiss drawer
 * - 1.4: Maintain map view state when panel toggles
 * - 3.2: Implement LaunchedEffect to handle programmatic drawer opening when button is clicked
 * - 3.3: Ensure smooth animation when drawer opens via button click vs swipe gesture
 * - 3.4: Maintain existing tap-outside-to-close and swipe-to-close functionality
 * - 6.3: Add proper coroutine scope management for drawer state changes
 * 
 * Features:
 * - 320dp panel width as specified in design
 * - Enhanced drawer state management with smooth animations and conflict prevention
 * - Proper coroutine scope management to prevent animation conflicts during rapid interactions
 * - Tap-outside-to-close functionality (built into ModalNavigationDrawer)
 * - Swipe-to-close functionality with proper state synchronization
 * - Maintains map view state during panel transitions
 * - Accessibility support for drawer navigation
 * - Animation cancellation to prevent conflicts during rapid button taps
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
    val focusManager = LocalFocusManager.current
    
    // Track animation jobs to prevent conflicts during rapid interactions
    var animationJob by remember { mutableStateOf<Job?>(null) }
    
    // Drawer state management - synced with uiState.isPanelOpen
    val drawerState = rememberDrawerState(
        initialValue = if (uiState.isPanelOpen) DrawerValue.Open else DrawerValue.Closed
    )
    
    // Enhanced drawer state synchronization with proper coroutine scope management and accessibility
    LaunchedEffect(uiState.isPanelOpen) {
        // Cancel any ongoing animation to prevent conflicts
        animationJob?.cancel()
        
        animationJob = launch {
            try {
                if (uiState.isPanelOpen && drawerState.isClosed) {
                    NearbyPanelLogger.logPanelStateChange(
                        isOpen = true,
                        friendCount = uiState.friends.size
                    )
                    // Smooth programmatic opening with proper animation
                    drawerState.open()
                    
                    // Enhanced accessibility: Announce panel opening to screen readers
                    if (accessibilityManager != null) {
                        // Small delay to ensure the panel is visible before announcing
                        delay(200)
                        // The announcement will be handled by the live region semantics on the panel
                    }
                } else if (!uiState.isPanelOpen && drawerState.isOpen) {
                    NearbyPanelLogger.logPanelStateChange(
                        isOpen = false,
                        friendCount = uiState.friends.size
                    )
                    // Smooth programmatic closing with proper animation
                    drawerState.close()
                    
                    // Enhanced accessibility: Announce panel closing and manage focus
                    if (accessibilityManager != null) {
                        // Small delay to ensure the panel is closed before announcing
                        delay(200)
                        // Focus management: When panel closes, focus should return to the FAB
                        try {
                            // Clear focus first, then let the system handle focus restoration
                            focusManager.clearFocus()
                        } catch (e: Exception) {
                            // Handle focus management exceptions gracefully
                            NearbyPanelLogger.logPanelStateChange(
                                isOpen = false,
                                friendCount = uiState.friends.size
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle animation cancellation gracefully
                NearbyPanelLogger.logPanelStateChange(
                    isOpen = uiState.isPanelOpen,
                    friendCount = uiState.friends.size
                )
            }
        }
    }
    
    // Enhanced gesture handling with proper state synchronization
    LaunchedEffect(drawerState.targetValue) {
        // Only respond to gesture-initiated state changes
        when (drawerState.targetValue) {
            DrawerValue.Open -> {
                if (!uiState.isPanelOpen) {
                    NearbyPanelLogger.logPanelStateChange(
                        isOpen = true,
                        friendCount = uiState.friends.size
                    )
                    onEvent(NearbyPanelEvent.TogglePanel)
                }
            }
            DrawerValue.Closed -> {
                if (uiState.isPanelOpen) {
                    NearbyPanelLogger.logPanelStateChange(
                        isOpen = false,
                        friendCount = uiState.friends.size
                    )
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
                        contentDescription = when {
                            uiState.isLoading -> "Friends nearby panel, loading friends"
                            uiState.error != null -> "Friends nearby panel, error occurred"
                            uiState.friends.isNotEmpty() -> "Friends nearby panel, ${uiState.friends.size} ${if (uiState.friends.size == 1) "friend" else "friends"} available"
                            else -> "Friends nearby panel, no friends available"
                        }
                        // Live region for announcing panel state changes
                        liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
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
            
            // Enhanced Friends Toggle FAB with responsive behavior positioned in top-right corner
            FriendsToggleFAB(
                onClick = { 
                    // Cancel any ongoing animation to prevent conflicts during rapid taps
                    animationJob?.cancel()
                    
                    // Handle button click with proper coroutine scope management
                    animationJob = scope.launch {
                        try {
                            // Log the button interaction for debugging
                            NearbyPanelLogger.logPanelStateChange(
                                isOpen = !uiState.isPanelOpen,
                                friendCount = uiState.friends.size
                            )
                            onEvent(NearbyPanelEvent.TogglePanel)
                        } catch (e: Exception) {
                            // Handle any exceptions during button click processing
                            NearbyPanelLogger.logPanelStateChange(
                                isOpen = uiState.isPanelOpen,
                                friendCount = uiState.friends.size
                            )
                        }
                    }
                },
                friendCount = uiState.friends.size,
                isExpanded = true, // Let the FAB handle responsive expansion logic internally
                isPanelOpen = uiState.isPanelOpen, // Pass panel state for responsive behavior and content description
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