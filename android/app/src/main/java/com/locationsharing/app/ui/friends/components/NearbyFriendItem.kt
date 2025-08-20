package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.locationsharing.app.R
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.theme.FFinderTheme
import com.locationsharing.app.ui.friends.logging.NearbyPanelLogger
import com.locationsharing.app.ui.map.haptic.rememberMapHapticFeedbackManager
import com.google.android.gms.maps.model.LatLng

/**
 * A composable that displays a nearby friend item with avatar, name, distance, status, and action buttons.
 * 
 * Implements requirements:
 * - 6.5: Display friend information with avatar, name, distance, status, and action button
 * - 9.1: Comprehensive accessibility descriptions for screen readers
 * - 9.6: Appropriate semantic feedback for interactions
 * 
 * @param friend The nearby friend data to display
 * @param onClick Callback when the item is clicked
 * @param onMessageClick Callback when the message action button is clicked
 * @param onMoreClick Callback when the more actions button is clicked
 * @param modifier Modifier for styling the component
 */
@Composable
fun NearbyFriendItem(
    friend: NearbyFriend,
    onClick: () -> Unit,
    onMessageClick: ((NearbyFriend) -> Unit)? = null,
    onMoreClick: ((NearbyFriend) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = rememberMapHapticFeedbackManager()
    
    // Create comprehensive accessibility description
    val accessibilityDescription = remember(friend) {
        val statusText = if (friend.isOnline) "online" else "offline"
        val actionsText = if (onMessageClick != null || onMoreClick != null) {
            ", with action buttons available"
        } else ""
        "Friend ${friend.displayName}, ${friend.formattedDistance} away, $statusText$actionsText"
    }
    
    Card(
        onClick = {
            hapticManager.performFriendItemAction() // Enhanced haptic feedback for friend selection
            NearbyPanelLogger.logFriendInteraction(
                action = "click",
                friendId = friend.id,
                friendName = friend.displayName
            )
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(if (onMessageClick != null || onMoreClick != null) 80.dp else 72.dp)
            .semantics {
                contentDescription = accessibilityDescription
                role = Role.Button
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with placeholder support
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (friend.avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(friend.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${friend.displayName} avatar",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_person_placeholder),
                        error = painterResource(R.drawable.ic_person_placeholder)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "${friend.displayName} avatar placeholder",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Name and Distance
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = friend.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = friend.formattedDistance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            // Online/Offline Status and Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Online/Offline Status
                Icon(
                    imageVector = if (friend.isOnline) Icons.Filled.Circle else Icons.Outlined.Circle,
                    contentDescription = if (friend.isOnline) "Online" else "Offline",
                    tint = if (friend.isOnline) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(12.dp)
                )
                
                // Action Buttons
                if (onMessageClick != null || onMoreClick != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Message Action Button
                    if (onMessageClick != null) {
                        IconButton(
                            onClick = {
                                hapticManager.performFriendActionButton() // Enhanced haptic feedback for action button
                                NearbyPanelLogger.logFriendInteraction(
                                    action = "message",
                                    friendId = friend.id,
                                    friendName = friend.displayName
                                )
                                onMessageClick(friend)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .semantics {
                                    contentDescription = "Send message to ${friend.displayName}"
                                    role = Role.Button
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Message,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // More Actions Button
                    if (onMoreClick != null) {
                        IconButton(
                            onClick = {
                                hapticManager.performFriendActionButton() // Enhanced haptic feedback for action button
                                NearbyPanelLogger.logFriendInteraction(
                                    action = "more_actions",
                                    friendId = friend.id,
                                    friendName = friend.displayName
                                )
                                onMoreClick(friend)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .semantics {
                                    contentDescription = "More actions for ${friend.displayName}"
                                    role = Role.Button
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NearbyFriendItemPreview() {
    FFinderTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Online friend with avatar and action buttons
            NearbyFriendItem(
                friend = NearbyFriend(
                    id = "1",
                    displayName = "Alice Johnson",
                    avatarUrl = "https://example.com/avatar1.jpg",
                    distance = 150.0,
                    isOnline = true,
                    lastUpdated = System.currentTimeMillis(),
                    latLng = LatLng(37.7749, -122.4194)
                ),
                onClick = {},
                onMessageClick = { },
                onMoreClick = { }
            )
            
            // Offline friend without avatar, with message button only
            NearbyFriendItem(
                friend = NearbyFriend(
                    id = "2",
                    displayName = "Bob Smith",
                    avatarUrl = null,
                    distance = 1200.0,
                    isOnline = false,
                    lastUpdated = System.currentTimeMillis() - 300000,
                    latLng = LatLng(37.7849, -122.4094)
                ),
                onClick = {},
                onMessageClick = { }
            )
            
            // Friend with very long name, no action buttons
            NearbyFriendItem(
                friend = NearbyFriend(
                    id = "3",
                    displayName = "Christopher Alexander Thompson-Williams",
                    avatarUrl = null,
                    distance = 2500.0,
                    isOnline = true,
                    lastUpdated = System.currentTimeMillis() - 60000,
                    latLng = LatLng(37.7949, -122.3994)
                ),
                onClick = {}
            )
            
            // Friend with more actions button only
            NearbyFriendItem(
                friend = NearbyFriend(
                    id = "4",
                    displayName = "Diana Wilson",
                    avatarUrl = null,
                    distance = 800.0,
                    isOnline = true,
                    lastUpdated = System.currentTimeMillis() - 30000,
                    latLng = LatLng(37.7649, -122.4294)
                ),
                onClick = {},
                onMoreClick = { }
            )
        }
    }
}