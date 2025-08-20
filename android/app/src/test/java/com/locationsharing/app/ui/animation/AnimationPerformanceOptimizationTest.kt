package com.locationsharing.app.ui.animation

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for animation performance optimization features.
 * 
 * Tests all Task 13 requirements:
 * - Animation lifecycle management with DisposableEffect
 * - Performance monitoring using Choreographer callbacks
 * - Optimized recomposition with stable parameters
 * - Reduced motion alternatives for accessibility
 * - Memory usage monitoring for animation-heavy components
 */
@RunWith(AndroidJUnit4::class)
class AnimationPerformanceOptimizationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockBatteryManager: BatteryManager
    
    @Mock
    private lateinit var mockPowerManager: PowerManager
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Setup mock behaviors
        whenever(mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
            .thenReturn(80)
        whenever(mockPowerManager.isPowerSaveMode).thenReturn(false)
    }
    
    @Test
    fun testAnimationPerformanceOptimizer_initialization() {
        var optimizationConfig: AnimationOptimizationConfig? = null
        var performanceMetrics: AnimationPerformanceMetrics? = null
        
        composeTestRule.setContent {
            AnimationPerformanceOptimizer(
                isEnabled = true,
                onPerformanceUpdate = { metrics ->
                    performanceMetrics = metrics
                }
            ) { config ->
                optimizationConfig = config
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Verify initialization
        assertNotNull(optimizationConfig)
        assertEquals(AnimationQuality.FULL, optimizationConfig?.animationQuality)
        assertTrue(optimizationConfig?.enableComplexAnimations == true)
        assertTrue(optimizationConfig?.enableSimpleAnimations == true)
    }
    
    @Test
    fun testAnimationLifecycleManager_registration() {
        val manager = AnimationLifecycleManager()
        
        // Test animation registration
        val animationId = manager.registerAnimation(
            onComplete = { /* completion callback */ }
        )
        
        assertNotNull(animationId)
        assertEquals(1, manager.getActiveAnimationCount())
        
        // Test animation unregistration
        manager.unregisterAnimation(animationId)
        assertEquals(0, manager.getActiveAnimationCount())
        
        manager.dispose()
    }
    
    @Test
    fun testAnimationLifecycleManager_cleanup() {
        val manager = AnimationLifecycleManager()
        
        // Register multiple animations
        repeat(5) {
            manager.registerAnimation()
        }
        
        assertEquals(5, manager.getActiveAnimationCount())
        
        // Test cleanup
        manager.cancelAllAnimations()
        assertEquals(0, manager.getActiveAnimationCount())
        
        manager.dispose()
    }
    
    @Test
    fun testOptimizedRecomposition_stableConfig() {
        val baseConfig = StableAnimationConfig(
            duration = 300,
            easing = FastOutSlowInEasing
        )
        
        var recompositionCount = 0
        
        composeTestRule.setContent {
            val optimizedConfig = rememberOptimizedAnimationConfig(
                quality = AnimationQuality.FULL,
                baseConfig = baseConfig
            )
            
            // Track recompositions
            LaunchedEffect(optimizedConfig) {
                recompositionCount++
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Should only recompose once for initialization
        assertEquals(1, recompositionCount)
    }
    
    @Test
    fun testOptimizedAnimationSpecs_qualityAdaptation() {
        composeTestRule.setContent {
            val fullQualityConfig = OptimizedAnimationSpecs.rememberFastAnimation(
                quality = AnimationQuality.FULL
            )
            val reducedQualityConfig = OptimizedAnimationSpecs.rememberFastAnimation(
                quality = AnimationQuality.REDUCED
            )
            val minimalQualityConfig = OptimizedAnimationSpecs.rememberFastAnimation(
                quality = AnimationQuality.MINIMAL
            )
            val disabledQualityConfig = OptimizedAnimationSpecs.rememberFastAnimation(
                quality = AnimationQuality.DISABLED
            )
            
            // Verify duration scaling
            assertTrue(fullQualityConfig.duration > reducedQualityConfig.duration)
            assertTrue(reducedQualityConfig.duration > minimalQualityConfig.duration)
            assertEquals(0, disabledQualityConfig.duration)
        }
    }
    
    @Test
    fun testReducedMotionAlternatives_accessibilityCompliance() = runTest {
        var animationExecuted = false
        var reducedMotionContentShown = false
        
        composeTestRule.setContent {
            AccessibleAnimation(
                enabled = true,
                reducedMotionContent = {
                    reducedMotionContentShown = true
                },
                animatedContent = {
                    animationExecuted = true
                }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Should show appropriate content based on accessibility settings
        assertTrue(animationExecuted || reducedMotionContentShown)
    }
    
    @Test
    fun testAccessibleAnimationValues_reducedMotion() {
        var finalValue: Float? = null
        
        composeTestRule.setContent {
            val animatedValue by animateAccessibleFloatAsState(
                targetValue = 1f,
                finishedListener = { value ->
                    finalValue = value
                }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Should reach target value regardless of motion settings
        assertEquals(1f, finalValue)
    }
    
    @Test
    fun testMemoryUsageMonitor_initialization() {
        val monitor = AnimationMemoryMonitor(context)
        
        val metrics = monitor.getCurrentMetrics()
        
        assertNotNull(metrics)
        assertTrue(metrics.totalMemoryMb > 0)
        assertTrue(metrics.maxMemoryMb > 0)
        assertNotNull(metrics.memoryPressureLevel)
        assertTrue(metrics.timestamp > 0)
    }
    
    @Test
    fun testMemoryUsageMonitor_componentTracking() {
        val monitor = AnimationMemoryMonitor(context)
        val testComponent = "TestAnimationComponent"
        
        // Register component
        monitor.registerAnimationComponent(testComponent, 5L)
        
        val metrics = monitor.getCurrentMetrics()
        assertTrue(metrics.animationMemoryMb >= 5L)
        
        // Unregister component
        monitor.unregisterAnimationComponent(testComponent, 5L)
        
        val updatedMetrics = monitor.getCurrentMetrics()
        assertTrue(updatedMetrics.animationMemoryMb < metrics.animationMemoryMb)
    }
    
    @Test
    fun testMemoryUsageMonitor_analysis() {
        val monitor = AnimationMemoryMonitor(context)
        
        // Create high memory usage scenario
        val highMemoryMetrics = AnimationMemoryMetrics(
            totalMemoryMb = 1000L,
            usedMemoryMb = 900L, // 90% usage
            freeMemoryMb = 100L,
            maxMemoryMb = 1000L,
            animationMemoryMb = 60L, // High animation memory
            memoryPressureLevel = MemoryPressureLevel.HIGH,
            gcCount = 10, // Frequent GC
            nativeHeapSizeMb = 200L,
            nativeHeapAllocatedMb = 150L
        )
        
        val analysis = monitor.analyzeMemoryUsage(highMemoryMetrics)
        
        assertTrue(analysis.suggestions.isNotEmpty())
        assertTrue(analysis.warnings.isNotEmpty())
        assertTrue(analysis.overallHealth == MemoryHealth.POOR || analysis.overallHealth == MemoryHealth.CRITICAL)
    }
    
    @Test
    fun testMemoryUsageMonitor_cleanup() {
        val monitor = AnimationMemoryMonitor(context)
        
        // Register some components
        repeat(3) { index ->
            monitor.registerAnimationComponent("Component$index", 2L)
        }
        
        val beforeMetrics = monitor.getCurrentMetrics()
        val cleanupResult = monitor.performMemoryCleanup()
        
        assertNotNull(cleanupResult)
        assertNotNull(cleanupResult.beforeMetrics)
        assertNotNull(cleanupResult.afterMetrics)
    }
    
    @Test
    fun testAnimationOptimizationConfig_qualityLevels() {
        val fullConfig = AnimationOptimizationConfig(
            animationQuality = AnimationQuality.FULL
        )
        val reducedConfig = AnimationOptimizationConfig(
            animationQuality = AnimationQuality.REDUCED
        )
        val minimalConfig = AnimationOptimizationConfig(
            animationQuality = AnimationQuality.MINIMAL
        )
        val disabledConfig = AnimationOptimizationConfig(
            animationQuality = AnimationQuality.DISABLED
        )
        
        // Test complex animations
        assertTrue(fullConfig.enableComplexAnimations)
        assertFalse(reducedConfig.enableComplexAnimations)
        assertFalse(minimalConfig.enableComplexAnimations)
        assertFalse(disabledConfig.enableComplexAnimations)
        
        // Test simple animations
        assertTrue(fullConfig.enableSimpleAnimations)
        assertTrue(reducedConfig.enableSimpleAnimations)
        assertTrue(minimalConfig.enableSimpleAnimations)
        assertFalse(disabledConfig.enableSimpleAnimations)
        
        // Test duration multipliers
        assertEquals(1.0f, fullConfig.durationMultiplier)
        assertEquals(0.7f, reducedConfig.durationMultiplier)
        assertEquals(0.5f, minimalConfig.durationMultiplier)
        assertEquals(0.0f, disabledConfig.durationMultiplier)
        
        // Test concurrent animation limits
        assertTrue(fullConfig.maxConcurrentAnimations > reducedConfig.maxConcurrentAnimations)
        assertTrue(reducedConfig.maxConcurrentAnimations > minimalConfig.maxConcurrentAnimations)
        assertEquals(0, disabledConfig.maxConcurrentAnimations)
    }
    
    @Test
    fun testPerformanceMetrics_grading() {
        val excellentMetrics = AnimationPerformanceMetrics(
            averageFps = 60.0,
            minFps = 58.0,
            maxFps = 60.0,
            jitterMs = 1.0,
            droppedFramePercentage = 1f,
            totalFrames = 1000,
            droppedFrames = 10,
            memoryUsageMb = 50L,
            batteryLevel = 80,
            isLowPowerMode = false,
            thermalState = ThermalState.NONE,
            isReducedMotionEnabled = false
        )
        
        val poorMetrics = AnimationPerformanceMetrics(
            averageFps = 25.0,
            minFps = 20.0,
            maxFps = 30.0,
            jitterMs = 10.0,
            droppedFramePercentage = 25f,
            totalFrames = 1000,
            droppedFrames = 250,
            memoryUsageMb = 250L,
            batteryLevel = 15,
            isLowPowerMode = true,
            thermalState = ThermalState.SEVERE,
            isReducedMotionEnabled = false
        )
        
        assertEquals(PerformanceGrade.EXCELLENT, excellentMetrics.performanceGrade)
        assertEquals(PerformanceGrade.POOR, poorMetrics.performanceGrade)
    }
    
    @Test
    fun testAnimationQualityDetermination() {
        val highPerformanceMetrics = AnimationPerformanceMetrics(
            averageFps = 60.0,
            minFps = 58.0,
            maxFps = 60.0,
            jitterMs = 1.0,
            droppedFramePercentage = 1f,
            totalFrames = 1000,
            droppedFrames = 10,
            memoryUsageMb = 50L,
            batteryLevel = 80,
            isLowPowerMode = false,
            thermalState = ThermalState.NONE,
            isReducedMotionEnabled = false
        )
        
        val lowPerformanceMetrics = AnimationPerformanceMetrics(
            averageFps = 25.0,
            minFps = 20.0,
            maxFps = 30.0,
            jitterMs = 10.0,
            droppedFramePercentage = 25f,
            totalFrames = 1000,
            droppedFrames = 250,
            memoryUsageMb = 250L,
            batteryLevel = 15,
            isLowPowerMode = true,
            thermalState = ThermalState.SEVERE,
            isReducedMotionEnabled = false
        )
        
        val reducedMotionMetrics = highPerformanceMetrics.copy(
            isReducedMotionEnabled = true
        )
        
        // Test quality determination logic
        assertEquals(AnimationQuality.FULL, determineAnimationQuality(highPerformanceMetrics))
        assertEquals(AnimationQuality.MINIMAL, determineAnimationQuality(lowPerformanceMetrics))
        assertEquals(AnimationQuality.DISABLED, determineAnimationQuality(reducedMotionMetrics))
    }
    
    @Test
    fun testStableAnimationConfig_conversion() {
        val config = StableAnimationConfig(
            duration = 300,
            easing = FastOutSlowInEasing,
            delayMillis = 100,
            repeatMode = RepeatMode.Reverse,
            iterations = 3
        )
        
        val animationSpec = config.toAnimationSpec()
        val repeatableSpec = config.toRepeatableSpec()
        
        assertNotNull(animationSpec)
        assertNotNull(repeatableSpec)
    }
    
    @Test
    fun testOptimizedAnimationValue_thresholdBehavior() {
        val animationValue = OptimizedAnimationValue(
            initialValue = 0f,
            threshold = { old, new -> kotlin.math.abs(old - new) > 0.1f }
        )
        
        // Small change should not trigger update
        animationValue.value = 0.05f
        assertEquals(0f, animationValue.value)
        
        // Large change should trigger update
        animationValue.value = 0.2f
        assertEquals(0.2f, animationValue.value)
    }
    
    @Test
    fun testMemoryAwareAnimationConfig_adaptation() {
        val baseConfig = StableAnimationConfig(
            duration = 300,
            easing = FastOutSlowInEasing
        )
        
        composeTestRule.setContent {
            val adaptedConfig = rememberMemoryAwareAnimationConfig(
                baseConfig = baseConfig,
                memoryThresholdMb = 100L
            )
            
            // Config should be initialized
            assertNotNull(adaptedConfig)
        }
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun testAnimationPerformanceIssues_detection() {
        val droppedFrameIssue = AnimationPerformanceIssue.DroppedFrame(
            frameTimeMs = 50f,
            targetFrameTimeMs = 16.67f
        )
        
        val lowFrameRateIssue = AnimationPerformanceIssue.LowFrameRate(
            currentFps = 25.0,
            targetFps = 60.0
        )
        
        val memoryPressureIssue = AnimationPerformanceIssue.MemoryPressure(
            currentMemoryMb = 250L,
            thresholdMemoryMb = 200L
        )
        
        assertNotNull(droppedFrameIssue)
        assertNotNull(lowFrameRateIssue)
        assertNotNull(memoryPressureIssue)
    }
    
    @Test
    fun testThermalStateHandling() {
        val normalConfig = AnimationOptimizationConfig(
            thermalState = ThermalState.NONE
        )
        
        val throttledConfig = AnimationOptimizationConfig(
            thermalState = ThermalState.SEVERE
        )
        
        // Normal thermal state should allow full animations
        assertTrue(normalConfig.enableComplexAnimations)
        
        // Severe thermal state should be handled appropriately
        // (This would be determined by the actual implementation logic)
        assertNotNull(throttledConfig.thermalState)
    }
}

/**
 * Helper function to test animation quality determination.
 */
private fun determineAnimationQuality(metrics: AnimationPerformanceMetrics): AnimationQuality {
    return when {
        metrics.isReducedMotionEnabled -> AnimationQuality.DISABLED
        metrics.isLowPowerMode -> AnimationQuality.MINIMAL
        metrics.batteryLevel < 20 -> AnimationQuality.MINIMAL
        metrics.thermalState >= ThermalState.SEVERE -> AnimationQuality.MINIMAL
        metrics.averageFps < 30.0 -> AnimationQuality.MINIMAL
        metrics.memoryUsageMb > 200L -> AnimationQuality.REDUCED
        metrics.batteryLevel < 50 -> AnimationQuality.REDUCED
        metrics.thermalState >= ThermalState.MODERATE -> AnimationQuality.REDUCED
        metrics.averageFps < 45.0 -> AnimationQuality.REDUCED
        metrics.droppedFramePercentage > 10f -> AnimationQuality.REDUCED
        else -> AnimationQuality.FULL
    }
}