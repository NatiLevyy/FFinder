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
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.google.android.gms.maps.model.LatLng

/**
 * A composable that displays a nearby friend item with avatar, name, distance, and online status.
 * 
 * Implements requirements:
 * - 2.3: Display friend information with proper formatting
 * - 7.1: Comprehensive accessibility descriptions for screen readers
 * - 7.2: Proper Material 3 styling and typography
 * 
 * @param friend The nearby friend data to display
 * @param onClick Callback when the item is clicked
 * @param modifier Modifier for styling the component
 */
@Composable
fun NearbyFriendItem(
    friend: NearbyFriend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Create comprehensive accessibility description
    val accessibilityDescription = remember(friend) {
        val statusText = if (friend.isOnline) "online" else "offline"
        "Friend ${friend.displayName}, ${friend.formattedDistance} away, $statusText"
    }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
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
            // Online friend with avatar
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
                onClick = {}
            )
            
            // Offline friend without avatar
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
                onClick = {}
            )
            
            // Friend with very long name
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
        }
    }
}