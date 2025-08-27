package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieConstants
import com.locationsharing.app.R
import com.locationsharing.app.ui.components.LottieAsset
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Map animations section component that displays a Lottie animation
 * as a replacement for the Map Preview card.
 * 
 * This component shows a continuously looping Lottie animation:
 * - location_animation.json (displayed directly on screen background)
 * 
 * Features:
 * - No visual container - seamlessly integrated into screen background
 * - Full width animation display
 * - Accessibility support with contentDescription
 * - Test tag for UI testing
 * - Clean animation without background styling
 * 
 * @param modifier Modifier for styling and layout customization
 * @param animationsEnabled Whether animations should loop (for accessibility)
 */
@Composable
fun MapAnimationsSection(
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true
) {
    // Location animation displayed directly on screen background with controlled height
    LottieAsset(
        resId = R.raw.location_animation,
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag("map_animations_section")
            .semantics {
                contentDescription = "Location animations"
            },
        iterations = if (animationsEnabled) LottieConstants.IterateForever else 1,
        speed = if (animationsEnabled) 1f else 0f
    )
}

@Preview(showBackground = true)
@Composable
fun MapAnimationsSectionPreview() {
    FFinderTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MapAnimationsSection(
                animationsEnabled = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Animations Disabled")
@Composable
fun MapAnimationsSectionAccessibilityPreview() {
    FFinderTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MapAnimationsSection(
                animationsEnabled = false
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
fun MapAnimationsSectionDarkPreview() {
    FFinderTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MapAnimationsSection(
                animationsEnabled = true
            )
        }
    }
}