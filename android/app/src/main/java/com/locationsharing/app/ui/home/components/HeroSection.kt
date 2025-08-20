package com.locationsharing.app.ui.home.components

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.R
import com.locationsharing.app.ui.components.animations.LottieAsset
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Hero section composable for the FFinder Home Screen.
 * 
 * Displays the FFinder logo with smooth animations, Travel_Somewhere Lottie backdrop,
 * and a subtitle text. Features:
 * - Travel_Somewhere animation backdrop (match-parent, 0.3 speed, fades behind gradient)
 * - Logo fade-in animation with 1000ms duration using EaseOut easing
 * - Slow zoom animation (1.0 → 1.1 → 1.0 scale over 4 seconds) with infiniteRepeatable
 * - Subtitle text with bodyMedium typography, white color, center alignment
 * - Accessibility support to respect animation preferences
 * 
 * @param modifier Modifier to be applied to the hero section container
 * @param animationsEnabled Whether animations should be played (respects accessibility preferences)
 */
@Composable
fun HeroSection(
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true
) {
    // Check system accessibility preferences
    val shouldAnimate = AccessibilityUtils.shouldEnableAnimations(animationsEnabled)
    
    // Logo zoom animation - slow breathing effect
    val logoScale by animateFloatAsState(
        targetValue = if (shouldAnimate) 1.1f else 1.0f,
        animationSpec = if (shouldAnimate) {
            infiniteRepeatable(
                animation = tween(4000, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(0) // No animation when disabled
        },
        label = "logo_zoom_animation"
    )
    
    // Logo fade-in animation
    val logoAlpha by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = if (shouldAnimate) {
            tween(1000, easing = EaseOut)
        } else {
            tween(0) // Immediate appearance when animations disabled
        },
        label = "logo_fade_in_animation"
    )
    
    Box(
        modifier = modifier.padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Travel_Somewhere animation backdrop (match-parent, 0.3 speed, fades behind gradient)
        if (shouldAnimate) {
            LottieAsset(
                resId = R.raw.travel_somewhere,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .alpha(0.3f), // Fades behind gradient as specified
                iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever,
                speed = 0.3f // 0.3 speed as specified
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // FFinder Logo with animations (96 dp, animated zoom as specified)
            Image(
                painter = painterResource(id = R.drawable.logo_full),
                contentDescription = "FFinder app logo - Find Friends, Share Locations",
                modifier = Modifier
                    .height(96.dp) // 96 dp as specified
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .semantics {
                        heading()
                        contentDescription = "FFinder app logo - Find Friends, Share Locations"
                    },
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtitle text
            Text(
                text = "Share your live location and find friends instantly.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .semantics {
                        contentDescription = "App description: Share your live location and find friends instantly"
                    }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HeroSectionPreview() {
    FFinderTheme {
        BackgroundGradient {
            HeroSection(
                animationsEnabled = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Animations Disabled")
@Composable
fun HeroSectionAnimationsDisabledPreview() {
    FFinderTheme {
        BackgroundGradient {
            HeroSection(
                animationsEnabled = false
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
fun HeroSectionDarkPreview() {
    FFinderTheme(darkTheme = true) {
        BackgroundGradient {
            HeroSection(
                animationsEnabled = true
            )
        }
    }
}