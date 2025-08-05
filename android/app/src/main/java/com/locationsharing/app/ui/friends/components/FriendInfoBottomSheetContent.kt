package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.locationsharing.app.R
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Friend Info Bottom Sheet Content component for the Friends Nearby Panel.
 * 
 * Displays detailed information about a selected friend including avatar, name, distance,
 * last updated timestamp, and action buttons for various interactions.
 * 
 * Features:
 * - Friend avatar (48dp) with placeholder support
 * - Friend name, distance, and last updated timestamp
 * - Action buttons: Navigate (🚗), Ping (📍), Stop Sharing (🔕), Message (💬)
 * - All action buttons have 48dp touch targets and accessibility descriptions
 * - Material 3 styling and proper spacing
 * - Comprehensive accessibility support
 * 
 * Requirements: 5.2, 5.3, 7.1, 7.4
 * 
 * @param friend The friend to display information for
 * @param onEvent Callback for handling user interactions
 * @param modifier Modifier for styling and layout
 */
@Composable
fun FriendInfoBottomSheetContent(
    friend: NearbyFriend,
    onEvent: (NearbyPanelEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header with avatar and friend info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Friend Avatar (48dp)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(friend.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${friend.displayName} avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                fallback = null, // Will show background color if no image
                error = null // Will show background color on error
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Friend name and details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${friend.formattedDistance} away • Updated ${formatTimestamp(friend.lastUpdated)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Buttons Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                ActionButton(
                    icon = Icons.Default.Directions,
                    label = "Navigate",
                    accessibilityLabel = "Navigate to ${friend.displayName}",
                    onClick = { onEvent(NearbyPanelEvent.Navigate(friend.id)) }
                )
            }
            item {
                ActionButton(
                    icon = Icons.Default.LocationOn,
                    label = "Ping",
                    accessibilityLabel = "Send ping to ${friend.displayName}",
                    onClick = { onEvent(NearbyPanelEvent.Ping(friend.id)) }
                )
            }
            item {
                ActionButton(
                    icon = Icons.Default.LocationOff,
                    label = "Stop",
                    accessibilityLabel = "Stop sharing location with ${friend.displayName}",
                    onClick = { onEvent(NearbyPanelEvent.StopSharing(friend.id)) }
                )
            }
            item {
                ActionButton(
                    icon = Icons.Default.Message,
                    label = "Message",
                    accessibilityLabel = "Send message to ${friend.displayName}",
                    onClick = { onEvent(NearbyPanelEvent.Message(friend.id)) }
                )
            }
        }
    }
}

/**
 * Action button component for friend interactions.
 * 
 * Features:
 * - 48dp minimum touch target for accessibility
 * - Circular background with Material 3 colors
 * - Icon and label layout
 * - Comprehensive accessibility descriptions
 * 
 * @param icon The icon to display
 * @param label The label text below the icon
 * @param onClick Callback when the button is clicked
 */
@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    accessibilityLabel: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .padding(4.dp) // Extra padding to ensure 48dp touch target
            .semantics {
                contentDescription = accessibilityLabel
            }
    ) {
        // Icon container with circular background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp) // Visual size of the icon container
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Content description is on the parent
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Label text
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Formats a timestamp into a human-readable relative time string.
 * 
 * @param timestamp The timestamp in milliseconds
 * @return Formatted time string (e.g., "2 min ago", "1 hour ago")
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now" // Less than 1 minute
        diff < 3600_000 -> "${diff / 60_000} min ago" // Less than 1 hour
        diff < 86400_000 -> "${diff / 3600_000} hour${if (diff / 3600_000 > 1) "s" else ""} ago" // Less than 1 day
        else -> {
            // More than 1 day, show actual date
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}