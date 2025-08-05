package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Integration example showing how FriendsToggleFAB would be positioned in MapScreen.
 * 
 * This demonstrates the FAB positioned in the top-right corner with 16dp padding
 * as specified in the task requirements.
 */
@Composable
fun FriendsToggleFABIntegrationExample() {
    var isPanelOpen by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Simulated map content
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            // Map content would go here
        }
        
        // Friends Toggle FAB positioned in top-right corner
        FriendsToggleFAB(
            isOpen = isPanelOpen,
            onClick = { isPanelOpen = !isPanelOpen },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp) // 16dp padding as specified in requirements
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsToggleFABIntegrationPreview() {
    FFinderTheme {
        FriendsToggleFABIntegrationExample()
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
fun FriendsToggleFABIntegrationPreviewDark() {
    FFinderTheme(darkTheme = true) {
        FriendsToggleFABIntegrationExample()
    }
}