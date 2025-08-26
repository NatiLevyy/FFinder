package com.locationsharing.app.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Data class representing responsive layout configuration.
 * 
 * @property isNarrowScreen Whether the screen width is narrow (< 360dp)
 * @property screenWidthDp Current screen width in dp
 * @property screenHeightDp Current screen height in dp
 * @property isLandscape Whether the device is in landscape orientation
 */
data class ResponsiveLayoutConfig(
    val isNarrowScreen: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isLandscape: Boolean
)

/**
 * Composable that provides responsive layout configuration based on screen dimensions.
 * 
 * Uses LocalConfiguration to detect screen size changes and provides responsive
 * layout information to child composables. Triggers callbacks when layout
 * configuration changes.
 * 
 * @param onConfigurationChanged Callback invoked when screen configuration changes
 * @param content Content composable that receives the responsive layout configuration
 */
@Composable
fun ResponsiveLayout(
    onConfigurationChanged: ((ResponsiveLayoutConfig) -> Unit)? = null,
    content: @Composable (ResponsiveLayoutConfig) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    var layoutConfig by remember { mutableStateOf(ResponsiveLayoutConfig(false, 0, 0, false)) }
    
    // Calculate responsive layout configuration
    val currentConfig = remember(configuration) {
        val screenWidthDp = configuration.screenWidthDp
        val screenHeightDp = configuration.screenHeightDp
        val isNarrowScreen = screenWidthDp < 360 // Narrow screen threshold
        val isLandscape = screenWidthDp > screenHeightDp
        
        ResponsiveLayoutConfig(
            isNarrowScreen = isNarrowScreen,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            isLandscape = isLandscape
        )
    }
    
    // Update layout config and notify callback when configuration changes
    LaunchedEffect(currentConfig) {
        if (layoutConfig != currentConfig) {
            layoutConfig = currentConfig
            onConfigurationChanged?.invoke(currentConfig)
        }
    }
    
    content(currentConfig)
}

/**
 * Utility composable for detecting narrow screen layouts.
 * 
 * Simplified version of ResponsiveLayout that only provides narrow screen detection.
 * Useful for components that only need to know if they should adapt to narrow screens.
 * 
 * @param onNarrowScreenChanged Callback invoked when narrow screen state changes
 * @param content Content composable that receives the narrow screen boolean
 */
@Composable
fun NarrowScreenDetector(
    onNarrowScreenChanged: ((Boolean) -> Unit)? = null,
    content: @Composable (Boolean) -> Unit
) {
    ResponsiveLayout(
        onConfigurationChanged = { config ->
            onNarrowScreenChanged?.invoke(config.isNarrowScreen)
        }
    ) { config ->
        content(config.isNarrowScreen)
    }
}

/**
 * Extension function to get screen width breakpoints.
 */
fun ResponsiveLayoutConfig.getScreenSizeCategory(): ScreenSizeCategory {
    return when {
        screenWidthDp < 360 -> ScreenSizeCategory.Compact
        screenWidthDp < 600 -> ScreenSizeCategory.Medium
        screenWidthDp < 840 -> ScreenSizeCategory.Expanded
        else -> ScreenSizeCategory.Large
    }
}

/**
 * Screen size categories based on Material Design guidelines.
 */
enum class ScreenSizeCategory {
    Compact,    // < 360dp
    Medium,     // 360dp - 599dp
    Expanded,   // 600dp - 839dp
    Large       // >= 840dp
}