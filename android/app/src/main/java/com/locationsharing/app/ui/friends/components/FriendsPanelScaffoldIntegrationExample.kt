package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.theme.FFinderTheme
import com.google.android.gms.maps.model.LatLng

/**
 * Integration example showing how FriendsPanelScaffold would be used in MapScreen.
 * 
 * This demonstrates the proper integration pattern:
 * 1. Wrap the existing MapScreen content with FriendsPanelScaffold
 * 2. Pass the UI state and event handler
 * 3. The scaffold handles the drawer state management and FAB positioning
 * 4. The map content remains unchanged and maintains its state
 */
@Composable
fun FriendsPanelScaffoldIntegrationExample() {
    // Example state management (in real implementation, this would come from ViewModel)
    var uiState by remember {
        mutableStateOf(
            NearbyUiState(
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
            )
        )
    }
    
    // Example event handler (in real implementation, this would delegate to ViewModel)
    val onEvent: (NearbyPanelEvent) -> Unit = { event ->
        when (event) {
            is NearbyPanelEvent.TogglePanel -> {
                uiState = uiState.copy(isPanelOpen = !uiState.isPanelOpen)
            }
            is NearbyPanelEvent.SearchQuery -> {
                uiState = uiState.copy(searchQuery = event.query)
            }
            is NearbyPanelEvent.FriendClick -> {
                uiState = uiState.copy(selectedFriendId = event.friendId)
            }
            else -> {
                // Handle other events
            }
        }
    }
    
    // Integration: Wrap existing MapScreen content with FriendsPanelScaffold
    FriendsPanelScaffold(
        uiState = uiState,
        onEvent = onEvent
    ) {
        // This is where the existing MapScreen content would go
        // The GoogleMap, FABs, and other map-related UI components
        MockMapContent()
    }
}

/**
 * Mock map content to demonstrate the integration.
 * In the real implementation, this would be the actual GoogleMap and related components.
 */
@Composable
private fun MockMapContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Google Map Content\n\n" +
                    "This represents the existing MapScreen content:\n" +
                    "• GoogleMap composable\n" +
                    "• Location markers\n" +
                    "• Current location FAB\n" +
                    "• Other map UI elements\n\n" +
                    "The FriendsPanelScaffold wraps this content\n" +
                    "and adds the friends panel functionality.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsPanelScaffoldIntegrationExamplePreview() {
    FFinderTheme {
        FriendsPanelScaffoldIntegrationExample()
    }
}