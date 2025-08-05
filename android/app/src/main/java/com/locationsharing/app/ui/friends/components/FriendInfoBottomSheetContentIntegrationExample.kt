package com.locationsharing.app.ui.friends.components

import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Integration example demonstrating the FriendInfoBottomSheetContent component
 * within a BottomSheetScaffold as specified in the task requirements.
 * 
 * This example shows how the component integrates with Material 3 BottomSheetScaffold
 * and handles user interactions properly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendInfoBottomSheetContentIntegrationExample() {
    val scaffoldState = rememberBottomSheetScaffoldState()
    var lastEvent by remember { mutableStateOf<NearbyPanelEvent?>(null) }
    
    // Sample friend data for demonstration
    val sampleFriend = NearbyFriend(
        id = "friend123",
        displayName = "Alice Johnson",
        avatarUrl = "https://example.com/avatar.jpg",
        distance = 150.0, // 150 meters
        isOnline = true,
        lastUpdated = System.currentTimeMillis() - 120_000, // 2 minutes ago
        latLng = LatLng(37.7749, -122.4194),
        location = Location("test").apply {
            latitude = 37.7749
            longitude = -122.4194
        }
    )
    
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            FriendInfoBottomSheetContent(
                friend = sampleFriend,
                onEvent = { event ->
                    lastEvent = event
                    // In a real implementation, this would be handled by the ViewModel
                }
            )
        },
        sheetPeekHeight = 200.dp,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Friend Info Bottom Sheet Demo") }
                )
            },
            modifier = Modifier.padding(paddingValues)
        ) { innerPadding ->
            // Main content area
            Text(
                text = buildString {
                    append("Friend Info Bottom Sheet Integration Example\n\n")
                    append("Pull up the bottom sheet to see the friend info component.\n\n")
                    lastEvent?.let { event ->
                        append("Last event: ${event::class.simpleName}")
                        when (event) {
                            is NearbyPanelEvent.Navigate -> append(" - Navigate to ${event.friendId}")
                            is NearbyPanelEvent.Ping -> append(" - Ping ${event.friendId}")
                            is NearbyPanelEvent.StopSharing -> append(" - Stop sharing with ${event.friendId}")
                            is NearbyPanelEvent.Message -> append(" - Message ${event.friendId}")
                            else -> {}
                        }
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendInfoBottomSheetContentIntegrationExamplePreview() {
    FFinderTheme {
        FriendInfoBottomSheetContentIntegrationExample()
    }
}