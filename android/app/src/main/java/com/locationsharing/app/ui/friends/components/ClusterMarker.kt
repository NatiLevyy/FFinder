package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.locationsharing.app.data.friends.Friend
import androidx.compose.ui.unit.sp
import com.locationsharing.app.ui.theme.FFinderAnimations

/**
 * Cluster marker component for displaying grouped friends on the map
 * Features branded styling, online friend indicators, and smooth animations
 */
@Composable
fun ClusterMarker(
    cluster: FriendCluster,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalFriends = cluster.friends.size
    val onlineFriends = cluster.onlineFriendsCount
    val hasOnlineFriends = onlineFriends > 0
    
    // Pulsing animation for clusters with online friends
    val infiniteTransition = rememberInfiniteTransition(label = "cluster_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (hasOnlineFriends) 1.1f else 1.0f,
        animationSpec = if (hasOnlineFriends) {
            FFinderAnimations.Loading.breathing()
        } else {
            infiniteRepeatable(FFinderAnimations.Accessibility.reducedMotion())
        },
        label = "cluster_pulse_scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (hasOnlineFriends) 0.4f else 0.8f,
        animationSpec = if (hasOnlineFriends) {
            FFinderAnimations.Loading.breathing()
        } else {
            infiniteRepeatable(FFinderAnimations.Accessibility.reducedMotion())
        },
        label = "cluster_pulse_alpha"
    )
    
    Box(
        modifier = modifier
            .size(getClusterSize(totalFriends))
            .clickable { onClick() }
            .semantics {
                contentDescription = "$totalFriends friends clustered here" +
                        if (hasOnlineFriends) ", $onlineFriends online" else ""
            },
        contentAlignment = Alignment.Center
    ) {
        // Pulsing ring for clusters with online friends
        if (hasOnlineFriends) {
            Box(
                modifier = Modifier
                    .size(getClusterSize(totalFriends) + 8.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        // Main cluster circle with gradient
        Box(
            modifier = Modifier
                .size(getClusterSize(totalFriends))
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            getClusterColor(hasOnlineFriends),
                            getClusterColor(hasOnlineFriends).copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 3.dp,
                    color = Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Cluster count text
            Text(
                text = totalFriends.toString(),
                color = Color.White,
                fontSize = getClusterTextSize(totalFriends),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        // Online friends indicator
        if (hasOnlineFriends && onlineFriends < totalFriends) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Main indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.Center)
                        .background(
                            color = Color(0xFF4CAF50),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = onlineFriends.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Friend avatars preview (for small clusters)
        if (totalFriends <= 3) {
            FriendAvatarsPreview(
                friends = cluster.friends,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Preview of friend avatars for small clusters
 */
@Composable
private fun FriendAvatarsPreview(
    friends: List<Friend>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy((-8).dp)
    ) {
        friends.take(3).forEachIndexed { index, friend ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .background(
                        color = friend.getDisplayColor(),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Get cluster size based on number of friends
 */
private fun getClusterSize(friendCount: Int) = when {
    friendCount < 5 -> 48.dp
    friendCount < 10 -> 56.dp
    friendCount < 20 -> 64.dp
    else -> 72.dp
}

/**
 * Get cluster text size based on number of friends
 */
private fun getClusterTextSize(friendCount: Int) = when {
    friendCount < 10 -> 16.sp
    friendCount < 100 -> 14.sp
    else -> 12.sp
}

/**
 * Get cluster color based on online status
 */
private fun getClusterColor(hasOnlineFriends: Boolean) = if (hasOnlineFriends) {
    Color(0xFF2E86AB) // FFinder Ocean Blue
} else {
    Color(0xFF757575) // Muted gray for offline clusters
}