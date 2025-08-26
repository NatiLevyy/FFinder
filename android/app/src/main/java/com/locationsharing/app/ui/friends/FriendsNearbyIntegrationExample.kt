package com.locationsharing.app.ui.friends

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.maps.android.compose.rememberCameraPositionState
import com.locationsharing.app.ui.friends.components.FriendInfoBottomSheetContent
import com.locationsharing.app.ui.friends.components.FriendsNearbyPanel
import com.locationsharing.app.ui.friends.components.FriendsPanelScaffold
import kotlinx.coroutines.launch

/**
 * Integration example demonstrating the Friends Nearby Panel with friend interaction handlers.
 * 
 * This example shows how to:
 * - Handle friend interactions (Navigate, Ping, Stop Sharing, Message)
 * - Focus camera on friend location
 * - Show user feedback for actions
 * - Handle navigation and message intents
 * 
 * Requirements: 5.1, 5.4, 5.5, 5.6, 5.7, 8.5
 */
@Composable
fun FriendsNearbyIntegrationExample(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cameraPositionState = rememberCameraPositionState()
    
    // ViewModels
    val nearbyViewModel: FriendsNearbyViewModel = viewModel()
    
    // State
    val uiState by nearbyViewModel.uiState.collectAsState()
    val feedbackMessage by nearbyViewModel.feedbackMessage.collectAsState()
    val navigationIntent by nearbyViewModel.navigationIntent.collectAsState()
    val messageIntent by nearbyViewModel.messageIntent.collectAsState()
    
    // Handle feedback messages
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            nearbyViewModel.onEvent(NearbyPanelEvent.ClearFeedback)
        }
    }
    
    // Handle navigation intents
    LaunchedEffect(navigationIntent) {
        navigationIntent?.let { data ->
            try {
                val intent = nearbyViewModel.createNavigationIntent(context, data)
                context.startActivity(intent)
                snackbarHostState.showSnackbar("Opening navigation to ${data.friendName}")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to open navigation: ${e.message}")
            } finally {
                nearbyViewModel.clearNavigationIntent()
            }
        }
    }
    
    // Handle message intents
    LaunchedEffect(messageIntent) {
        messageIntent?.let { data ->
            try {
                val intent = nearbyViewModel.createMessageIntent(data)
                context.startActivity(intent)
                snackbarHostState.showSnackbar("Opening message to ${data.friendName}")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to open messaging: ${e.message}")
            } finally {
                nearbyViewModel.clearMessageIntent()
            }
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        
        FriendsPanelScaffold(
            uiState = uiState,
            onEvent = { event ->
                when (event) {
                    is NearbyPanelEvent.FriendClick -> {
                        // Handle friend click - focus camera and show bottom sheet
                        nearbyViewModel.focusOnFriend(event.friendId, cameraPositionState)
                        nearbyViewModel.onEvent(event)
                    }
                    else -> {
                        // Handle all other events through the ViewModel
                        nearbyViewModel.onEvent(event)
                    }
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            // Map content would go here
            Box(modifier = Modifier.fillMaxSize()) {
                // Placeholder for Google Map
                // In a real implementation, this would be the GoogleMap composable
            }
        }
        
        // Friend Info Bottom Sheet
        uiState.selectedFriend?.let { friend ->
            FriendInfoBottomSheetContent(
                friend = friend,
                onEvent = nearbyViewModel::onEvent
            )
        }
    }
}

/**
 * Example of how to handle friend interactions in a custom implementation
 */
class FriendInteractionHandler(
    private val context: Context,
    private val nearbyViewModel: FriendsNearbyViewModel
) {
    
    /**
     * Handle friend click with camera focus
     * Requirement 5.1: Focus camera on friend location
     */
    suspend fun handleFriendClick(friendId: String, cameraPositionState: com.google.maps.android.compose.CameraPositionState) {
        // Focus camera on friend
        nearbyViewModel.focusOnFriend(friendId, cameraPositionState)
        
        // Select friend for bottom sheet
        nearbyViewModel.onEvent(NearbyPanelEvent.FriendClick(friendId))
    }
    
    /**
     * Handle navigation to friend
     * Requirement 5.4: Open Google Maps navigation
     */
    suspend fun handleNavigation(friendId: String): Result<Unit> {
        return try {
            nearbyViewModel.onEvent(NearbyPanelEvent.Navigate(friendId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Handle ping friend
     * Requirement 5.5: Send ping to friend
     */
    suspend fun handlePing(friendId: String): Result<Unit> {
        return try {
            nearbyViewModel.onEvent(NearbyPanelEvent.Ping(friendId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Handle stop sharing location
     * Requirement 5.6: Stop receiving location from friend
     */
    suspend fun handleStopSharing(friendId: String): Result<Unit> {
        return try {
            nearbyViewModel.onEvent(NearbyPanelEvent.StopSharing(friendId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Handle message friend
     * Requirement 5.7: Open share intent for messaging
     */
    suspend fun handleMessage(friendId: String): Result<Unit> {
        return try {
            nearbyViewModel.onEvent(NearbyPanelEvent.Message(friendId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}