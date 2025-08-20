package com.locationsharing.app.ui.components.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Example usage of ResponsiveButton components demonstrating different button types
 * and states for the navigation button fix implementation.
 */
@Composable
fun ResponsiveButtonExample() {
    var clickCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isEnabled by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Responsive Button Examples",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Click count: $clickCount",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Primary button
        ResponsiveButton(
            text = "Primary Button",
            onClick = { clickCount++ },
            buttonType = ButtonType.PRIMARY
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Secondary button
        ResponsiveButton(
            text = "Secondary Button",
            onClick = { clickCount++ },
            buttonType = ButtonType.SECONDARY
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tertiary button
        ResponsiveButton(
            text = "Tertiary Button",
            onClick = { clickCount++ },
            buttonType = ButtonType.TERTIARY
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Loading button
        ResponsiveButton(
            text = "Loading Button",
            onClick = { 
                isLoading = true
                // Simulate async operation
                CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
                    isLoading = false
                    clickCount++
                }
            },
            loading = isLoading,
            buttonType = ButtonType.PRIMARY
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Disabled button
        ResponsiveButton(
            text = "Disabled Button",
            onClick = { clickCount++ },
            enabled = isEnabled,
            buttonType = ButtonType.SECONDARY
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Toggle button state
        ResponsiveButton(
            text = if (isEnabled) "Disable Button" else "Enable Button",
            onClick = { isEnabled = !isEnabled },
            buttonType = ButtonType.TERTIARY
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResponsiveButtonExamplePreview() {
    MaterialTheme {
        Surface {
            ResponsiveButtonExample()
        }
    }
}