package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Integration example showing how FriendsNearbyPanel would be used within a ModalNavigationDrawer.
 * This demonstrates the component working with the overall app architecture.
 */
@Composable
fun FriendsNearbyPanelIntegrationExample(
    uiState: NearbyUiState,
    onEvent: (NearbyPanelEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(
        initialValue = if (uiState.isPanelOpen) DrawerValue.Open else DrawerValue.Closed
    )
    
    // Sync drawer state with UI state
    LaunchedEffect(uiState.isPanelOpen) {
        if (uiState.isPanelOpen) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FriendsNearbyPanel(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.width(320.dp)
            )
        },
        modifier = modifier
    ) {
        // Main content would go here (e.g., MapScreen)
        // This is just a placeholder for the integration example
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsNearbyPanelIntegrationExamplePreview() {
    FFinderTheme {
        FriendsNearbyPanelIntegrationExample(
            uiState = NearbyUiState(
                isPanelOpen = true,
                isLoading = false,
                searchQuery = "",
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
            onEvent = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}