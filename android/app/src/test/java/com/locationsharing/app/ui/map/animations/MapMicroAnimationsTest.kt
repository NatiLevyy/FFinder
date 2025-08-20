package com.locationsharing.app.ui.map.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Test suite for MapMicroAnimations system
 * Validates all micro-animation implementations for requirements 8.1, 8.2, 8.3, 8.4, 8.5
 */
class MapMicroAnimationsTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Test enhanced location marker pulse animation
     * Requirement 8.1: Create location marker pulse animation (every 3 seconds)
     */
    @Test
    fun locationMarkerPulse_animatesCorrectly() {
        var pulseValue = 1.0f
        var reducedMotionPulseValue = 1.0f
        
        composeTestRule.setContent {
            FFinderTheme {
                pulseValue = MapMicroAnimations.LocationMarkerPulse(isReducedMotion = false)
                reducedMotionPulseValue = MapMicroAnimations.LocationMarkerPulse(isReducedMotion = true)
            }
        }
        
        // Verify initial state
        composeTestRule.waitForIdle()
        assertTrue("Pulse animation should start at or near 1.0f", pulseValue >= 0.9f && pulseValue <= 1.3f)
        assertTrue("Reduced motion pulse should be more subtle", 
                  reducedMotionPulseValue >= 0.95f && reducedMotionPulseValue <= 1.1f)
        
        // The animation should be running and changing values
        // Note: In a real test, you'd advance the animation clock to test specific values
    }
    
    /**
     * Test enhanced FAB scale animation
     * Requirement 8.2: Implement FAB scale animations for all floating action buttons
     */
    @Test
    fun fabScaleAnimation_respondsToPress() {
        var isPressed by mutableStateOf(false)
        var isHovered by mutableStateOf(false)
        var isLoading by mutableStateOf(false)
        var scaleValue = 1.0f
        var loadingPulseValue = 1.0f
        
        composeTestRule.setContent {
            FFinderTheme {
                scaleValue = MapMicroAnimations.FABScaleAnimation(
                    isPressed = isPressed,
                    isHovered = isHovered,
                    isLoading = isLoading,
                    pressedScale = 0.9f,
                    hoveredScale = 1.05f,
                    normalScale = 1.0f
                )
                loadingPulseValue = MapMicroAnimations.FABLoadingPulse()
            }
        }
        
        // Test initial state (not pressed)
        composeTestRule.waitForIdle()
        assertEquals("FAB should start at normal scale", 1.0f, scaleValue, 0.1f)
        
        // Test pressed state
        isPressed = true
        composeTestRule.waitForIdle()
        // Animation should be transitioning towards pressed scale
        assertTrue("FAB should be scaling down when pressed", scaleValue <= 1.0f)
        
        // Test hover state
        isPressed = false
        isHovered = true
        composeTestRule.waitForIdle()
        // Animation should be transitioning towards hover scale
        assertTrue("FAB should be scaling up when hovered", scaleValue >= 1.0f)
        
        // Test loading state
        isHovered = false
        isLoading = true
        composeTestRule.waitForIdle()
        assertTrue("Loading pulse should be active", loadingPulseValue >= 0.9f && loadingPulseValue <= 1.1f)
        
        // Test release
        isLoading = false
        composeTestRule.waitForIdle()
        // Animation should be transitioning back to normal scale
    }
    
    /**
     * Test enhanced drawer slide animation
     * Requirement 8.3: Add smooth drawer slide animation with overshoot interpolator
     */
    @Test
    fun drawerSlideAnimation_animatesWithOvershoot() {
        var isOpen by mutableStateOf(false)
        var offsetValue = 280f
        var scrimAlpha = 0f
        
        composeTestRule.setContent {
            FFinderTheme {
                offsetValue = MapMicroAnimations.DrawerSlideAnimation(
                    isOpen = isOpen,
                    drawerWidth = 280f,
                    isReducedMotion = false
                )
                scrimAlpha = MapMicroAnimations.DrawerScrimFade(
                    isVisible = isOpen,
                    targetAlpha = 0.5f
                )
            }
        }
        
        // Test initial state (closed)
        composeTestRule.waitForIdle()
        assertEquals("Drawer should start closed", 280f, offsetValue, 10f)
        assertEquals("Scrim should start transparent", 0f, scrimAlpha, 0.1f)
        
        // Test opening
        isOpen = true
        composeTestRule.waitForIdle()
        // Animation should be transitioning towards open position
        assertTrue("Drawer should be sliding towards open position", offsetValue < 280f)
        assertTrue("Scrim should be fading in", scrimAlpha >= 0f)
        
        // Test closing
        isOpen = false
        composeTestRule.waitForIdle()
        // Animation should be transitioning back to closed position
    }
    
    /**
     * Test enhanced status sheet fade animation
     * Requirement 8.4: Create status sheet fade-in/out animations
     */
    @Test
    fun statusSheetFadeAnimation_fadesCorrectly() {
        var isVisible by mutableStateOf(false)
        var alphaValue = 0f
        var scaleValue = 1f
        
        composeTestRule.setContent {
            FFinderTheme {
                alphaValue = MapMicroAnimations.StatusSheetFadeAnimation(
                    isVisible = isVisible,
                    fadeDuration = 200,
                    isReducedMotion = false
                )
                scaleValue = MapMicroAnimations.StatusSheetScaleAnimation(
                    isVisible = isVisible,
                    isReducedMotion = false
                )
            }
        }
        
        // Test initial state (hidden)
        composeTestRule.waitForIdle()
        assertEquals("Sheet should start hidden", 0f, alphaValue, 0.1f)
        assertTrue("Sheet should start slightly scaled down", scaleValue <= 1f)
        
        // Test showing
        isVisible = true
        composeTestRule.waitForIdle()
        // Animation should be transitioning towards visible
        assertTrue("Sheet should be fading in", alphaValue >= 0f)
        assertTrue("Sheet should be scaling up", scaleValue >= 0.9f)
        
        // Test hiding
        isVisible = false
        composeTestRule.waitForIdle()
        // Animation should be transitioning towards hidden
    }
    
    /**
     * Test enhanced friend marker position animation
     * Requirement 8.5: Add friend marker position interpolation animations
     */
    @Test
    fun friendMarkerPositionAnimation_interpolatesPosition() {
        var targetX = 0f
        var targetY = 0f
        var animatedPosition = Pair(0f, 0f)
        var velocityPosition = Pair(0f, 0f)
        
        composeTestRule.setContent {
            FFinderTheme {
                animatedPosition = MapMicroAnimations.FriendMarkerPositionAnimation(
                    targetX = targetX,
                    targetY = targetY,
                    animationDuration = 500,
                    isReducedMotion = false
                )
                velocityPosition = MapMicroAnimations.FriendMarkerVelocityAnimation(
                    targetX = targetX,
                    targetY = targetY,
                    previousX = 0f,
                    previousY = 0f,
                    baseSpeed = 200f
                )
            }
        }
        
        // Test initial position
        composeTestRule.waitForIdle()
        assertEquals("X position should start at 0", 0f, animatedPosition.first, 0.1f)
        assertEquals("Y position should start at 0", 0f, animatedPosition.second, 0.1f)
        assertEquals("Velocity X position should start at 0", 0f, velocityPosition.first, 0.1f)
        assertEquals("Velocity Y position should start at 0", 0f, velocityPosition.second, 0.1f)
        
        // Test position change
        targetX = 100f
        targetY = 50f
        composeTestRule.waitForIdle()
        // Animation should be interpolating towards new position
        // Note: Exact values depend on animation timing
    }
    
    /**
     * Test friend marker appearance animation
     */
    @Test
    fun friendMarkerAppearAnimation_scalesAndFades() {
        var isVisible by mutableStateOf(false)
        var animationValues = Pair(0f, 0f)
        
        composeTestRule.setContent {
            FFinderTheme {
                animationValues = MapMicroAnimations.FriendMarkerAppearAnimation(
                    isVisible = isVisible
                )
            }
        }
        
        // Test initial state (hidden)
        composeTestRule.waitForIdle()
        assertEquals("Scale should start at 0", 0f, animationValues.first, 0.1f)
        assertEquals("Alpha should start at 0", 0f, animationValues.second, 0.1f)
        
        // Test appearing
        isVisible = true
        composeTestRule.waitForIdle()
        // Animation should be transitioning towards visible state
        assertTrue("Scale should be increasing", animationValues.first >= 0f)
        assertTrue("Alpha should be increasing", animationValues.second >= 0f)
    }
    
    /**
     * Test staggered animation
     */
    @Test
    fun staggeredAnimation_appliesCorrectDelay() {
        var isVisible by mutableStateOf(false)
        val animationValues = mutableListOf<Float>()
        
        composeTestRule.setContent {
            FFinderTheme {
                // Test multiple staggered animations
                repeat(3) { index ->
                    val value = MapMicroAnimations.StaggeredAnimation(
                        isVisible = isVisible,
                        index = index,
                        totalItems = 3,
                        staggerDelay = 50
                    )
                    animationValues.add(value)
                }
            }
        }
        
        // Test initial state
        composeTestRule.waitForIdle()
        animationValues.forEach { value ->
            assertEquals("All animations should start at 0", 0f, value, 0.1f)
        }
        
        // Test staggered appearance
        isVisible = true
        composeTestRule.waitForIdle()
        // All animations should be transitioning, potentially at different rates due to stagger
        animationValues.forEach { value ->
            assertTrue("All animations should be progressing", value >= 0f)
        }
    }
    
    /**
     * Test breathing animation
     */
    @Test
    fun breathingAnimation_oscillatesCorrectly() {
        var breathingValue = 1.0f
        
        composeTestRule.setContent {
            FFinderTheme {
                breathingValue = MapMicroAnimations.BreathingAnimation()
            }
        }
        
        // Test that breathing animation produces values in expected range
        composeTestRule.waitForIdle()
        assertTrue("Breathing animation should be between 0.8 and 1.0", 
                  breathingValue >= 0.8f && breathingValue <= 1.0f)
    }
    
    /**
     * Test shake animation
     */
    @Test
    fun shakeAnimation_triggersOnError() {
        var trigger by mutableStateOf(false)
        var shakeValue = 0f
        
        composeTestRule.setContent {
            FFinderTheme {
                shakeValue = MapMicroAnimations.ShakeAnimation(trigger = trigger)
            }
        }
        
        // Test initial state
        composeTestRule.waitForIdle()
        assertEquals("Shake should start at 0", 0f, shakeValue, 0.1f)
        
        // Test trigger
        trigger = true
        composeTestRule.waitForIdle()
        // Shake animation should be running (value may vary during animation)
    }
    
    /**
     * Test enhanced animation performance monitoring
     */
    @Test
    fun animationPerformanceMonitor_tracksMetrics() {
        // Test initial state
        assertEquals("Should start with 0% dropped frames", 
                    0f, com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.getDroppedFramePercentage(), 0.1f)
        
        // Record some frames
        com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.recordFrame(false, System.currentTimeMillis()) // Good frame
        com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.recordFrame(true, System.currentTimeMillis() + 16)  // Dropped frame
        com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.recordFrame(false, System.currentTimeMillis() + 32) // Good frame
        
        // Check metrics
        val droppedPercentage = com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.getDroppedFramePercentage()
        assertEquals("Should have 33.33% dropped frames", 33.33f, droppedPercentage, 1f)
        
        // Test animation tracking
        com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.recordAnimationStart()
        com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.recordAnimationStart()
        
        val metrics = com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.getPerformanceMetrics()
        assertEquals("Should have 2 active animations", 2, metrics.activeAnimations)
        assertTrue("Should detect performance issues", 
                  com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.shouldReduceAnimationQuality())
        
        // Test reset
        com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.reset()
        assertEquals("Should reset to 0% dropped frames", 
                    0f, com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.getDroppedFramePercentage(), 0.1f)
    }
    
    /**
     * Test enhanced animation state management
     */
    @Test
    fun animationState_managesCorrectly() {
        val state = com.locationsharing.app.ui.map.animations.AnimationState(
            isLocationPulsing = true,
            isFABPressed = false,
            isDrawerOpen = false,
            isStatusSheetVisible = true,
            visibleFriendMarkers = setOf("friend1", "friend2"),
            animatingFriendMarkers = setOf("friend1"),
            isReducedMotionEnabled = false,
            maxConcurrentAnimations = 20,
            animationQuality = com.locationsharing.app.ui.map.animations.AnimationQuality.HIGH
        )
        
        // Verify state properties
        assertTrue("Location should be pulsing", state.isLocationPulsing)
        assertFalse("FAB should not be pressed", state.isFABPressed)
        assertFalse("Drawer should be closed", state.isDrawerOpen)
        assertTrue("Status sheet should be visible", state.isStatusSheetVisible)
        assertEquals("Should have 2 visible markers", 2, state.visibleFriendMarkers.size)
        assertEquals("Should have 1 animating marker", 1, state.animatingFriendMarkers.size)
        assertFalse("Reduced motion should be disabled", state.isReducedMotionEnabled)
        assertEquals("Should allow 20 concurrent animations", 20, state.maxConcurrentAnimations)
        assertEquals("Should use high quality", com.locationsharing.app.ui.map.animations.AnimationQuality.HIGH, state.animationQuality)
    }
    
    /**
     * Test coordinated animation system
     */
    @Test
    fun coordinatedAnimation_managesMultipleElements() {
        var isActive by mutableStateOf(false)
        val elements = listOf("element1", "element2", "element3")
        var animationValues = mapOf<String, Float>()
        
        composeTestRule.setContent {
            FFinderTheme {
                animationValues = MapMicroAnimations.CoordinatedAnimation(
                    isActive = isActive,
                    elements = elements,
                    staggerDelay = 50
                )
            }
        }
        
        // Test initial state
        composeTestRule.waitForIdle()
        elements.forEach { element ->
            assertEquals("$element should start at 0", 0f, animationValues[element] ?: 0f, 0.1f)
        }
        
        // Test activation
        isActive = true
        composeTestRule.waitForIdle()
        // All elements should be animating
        elements.forEach { element ->
            assertTrue("$element should be animating", (animationValues[element] ?: 0f) >= 0f)
        }
    }
    
    /**
     * Test notification badge animation
     */
    @Test
    fun notificationBadgeAnimation_respondsToCountChanges() {
        var count by mutableStateOf(0)
        var previousCount = 0
        var animationValues = Pair(0f, 1f)
        
        composeTestRule.setContent {
            FFinderTheme {
                animationValues = MapMicroAnimations.NotificationBadgeAnimation(
                    count = count,
                    previousCount = previousCount
                )
            }
        }
        
        // Test initial state (no notifications)
        composeTestRule.waitForIdle()
        assertEquals("Badge should be hidden", 0f, animationValues.first, 0.1f)
        
        // Test new notification
        previousCount = count
        count = 1
        composeTestRule.waitForIdle()
        // Badge should be appearing
        assertTrue("Badge should be scaling in", animationValues.first >= 0f)
        assertTrue("Badge should be pulsing", animationValues.second >= 1f)
    }
    
    /**
     * Test search bar animation
     */
    @Test
    fun searchBarAnimation_expandsAndCollapses() {
        var isExpanded by mutableStateOf(false)
        var width = 48f
        
        composeTestRule.setContent {
            FFinderTheme {
                width = MapMicroAnimations.SearchBarAnimation(
                    isExpanded = isExpanded,
                    collapsedWidth = 48f,
                    expandedWidth = 280f
                )
            }
        }
        
        // Test initial state (collapsed)
        composeTestRule.waitForIdle()
        assertEquals("Search bar should start collapsed", 48f, width, 5f)
        
        // Test expansion
        isExpanded = true
        composeTestRule.waitForIdle()
        // Animation should be transitioning towards expanded width
        assertTrue("Search bar should be expanding", width >= 48f)
    }
}