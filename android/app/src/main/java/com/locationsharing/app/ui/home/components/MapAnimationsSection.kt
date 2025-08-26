package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
 * - location_animation.json (fills the entire container)
 * 
 * Features:
 * - Rounded rectangle container with Material 3 surfaceVariant background
 * - 220dp height with appropriate padding
 * - Accessibility support with contentDescription
 * - Test tag for UI testing
 * - Clean single animation display
 * 
 * @param modifier Modifier for styling and layout customization
 * @param animationsEnabled Whether animations should loop (for accessibility)
 */
@Composable
fun MapAnimationsSection(
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .testTag("map_animations_section")
            .semantics {
                contentDescription = "Location animations"
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Location animation - fills the entire container
            LottieAsset(
                resId = R.raw.location_animation,
                modifier = Modifier.fillMaxSize(),
                iterations = if (animationsEnabled) LottieConstants.IterateForever else 1,
                speed = if (animationsEnabled) 1f else 0f
            )
        }
    }
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