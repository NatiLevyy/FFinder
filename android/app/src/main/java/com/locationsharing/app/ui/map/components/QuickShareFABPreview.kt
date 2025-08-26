package com.locationsharing.app.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Preview composables for QuickShareFAB component design validation
 * Demonstrates all states and variations for design review
 */

@Preview(name = "QuickShare FAB - Default State", showBackground = true)
@Composable
fun QuickShareFABDefaultPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            QuickShareFAB(
                onClick = { }
            )
        }
    }
}

@Preview(name = "QuickShare FAB - Active Sharing", showBackground = true)
@Composable
fun QuickShareFABActivePreview() {
    FFinderTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            QuickShareFAB(
                onClick = { },
                isLocationSharingActive = true
            )
        }
    }
}

@Preview(name = "QuickShare FAB - Pressed State", showBackground = true)
@Composable
fun QuickShareFABPressedPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            QuickShareFAB(
                onClick = { },
                isPressed = true
            )
        }
    }
}

@Preview(name = "QuickShare FAB - Disabled State", showBackground = true)
@Composable
fun QuickShareFABDisabledPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            QuickShareFAB(
                onClick = { },
                enabled = false
            )
        }
    }
}

@Preview(name = "QuickShare FAB - Dark Theme", showBackground = true)
@Composable
fun QuickShareFABDarkPreview() {
    FFinderTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            QuickShareFAB(
                onClick = { }
            )
        }
    }
}

@Preview(name = "QuickShare FAB - All States", showBackground = true)
@Composable
fun QuickShareFABAllStatesPreview() {
    FFinderTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Default state
            QuickShareFAB(
                onClick = { }
            )
            
            // Active sharing state
            QuickShareFAB(
                onClick = { },
                isLocationSharingActive = true
            )
            
            // Pressed state
            QuickShareFAB(
                onClick = { },
                isPressed = true
            )
            
            // Active + Pressed state
            QuickShareFAB(
                onClick = { },
                isLocationSharingActive = true,
                isPressed = true
            )
        }
    }
}

@Preview(name = "QuickShare FAB - Positioned Layout", showBackground = true)
@Composable
fun QuickShareFABPositionedPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Simulate map background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // QuickShare FAB positioned as it would be in MapScreen
            QuickShareFAB(
                onClick = { },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        bottom = 16.dp,
                        end = 16.dp
                    )
            )
        }
    }
}

@Preview(name = "QuickShare FAB - Interactive Demo", showBackground = true)
@Composable
fun QuickShareFABInteractivePreview() {
    var isLocationSharingActive by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    
    FFinderTheme {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            QuickShareFAB(
                onClick = { 
                    isLocationSharingActive = !isLocationSharingActive
                },
                isLocationSharingActive = isLocationSharingActive,
                isPressed = isPressed
            )
        }
    }
}

@Preview(name = "QuickShare FAB - Material 3 Colors", showBackground = true)
@Composable
fun QuickShareFABMaterial3ColorsPreview() {
    FFinderTheme {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primary color (inactive)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QuickShareFAB(
                    onClick = { },
                    isLocationSharingActive = false
                )
                androidx.compose.material3.Text(
                    text = "Primary",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Secondary color (active)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QuickShareFAB(
                    onClick = { },
                    isLocationSharingActive = true
                )
                androidx.compose.material3.Text(
                    text = "Secondary",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}