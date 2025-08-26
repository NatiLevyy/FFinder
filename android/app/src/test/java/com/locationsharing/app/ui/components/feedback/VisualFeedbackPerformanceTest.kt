package com.locationsharing.app.ui.components.feedback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class VisualFeedbackPerformanceTest {
    
    private lateinit var visualFeedbackManager: VisualFeedbackManagerImpl
    private lateinit var hapticFeedbackManager: HapticFeedbackManagerImpl
    
    @BeforeEach
    fun setUp() {
        visualFeedbackManager = VisualFeedbackManagerImpl()
        hapticFeedbackManager = HapticFeedbackManagerImpl()
    }
    
    @Test
    fun `visual feedback should trigger within acceptable time`() = runTest {
        val maxAcceptableTime = 50L // 50ms
        
        val executionTime = measureTimeMillis {
            visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS)
        }
        
        assertTrue(
            executionTime < maxAcceptableTime,
            "Visual feedback took ${executionTime}ms, expected less than ${maxAcceptableTime}ms"
        )
    }
    
    @Test
    fun `haptic feedback should trigger within acceptable time`() = runTest {
        val maxAcceptableTime = 50L // 50ms
        
        val executionTime = measureTimeMillis {
            hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.BUTTON_PRESS)
        }
        
        assertTrue(
            executionTime < maxAcceptableTime,
            "Haptic feedback took ${executionTime}ms, expected less than ${maxAcceptableTime}ms"
        )
    }
    
    @Test
    fun `multiple concurrent feedback operations should not block`() = runTest {
        val numberOfOperations = 100
        val maxAcceptableTime = 500L // 500ms for 100 operations
        
        val executionTime = measureTimeMillis {
            val jobs = (1..numberOfOperations).map { index ->
                launch {
                    visualFeedbackManager.triggerFeedback(
                        type = FeedbackType.values()[index % FeedbackType.values().size],
                        duration = 10L // Short duration to avoid overlap
                    )
                }
            }
            jobs.forEach { it.join() }
        }
        
        assertTrue(
            executionTime < maxAcceptableTime,
            "Concurrent operations took ${executionTime}ms, expected less than ${maxAcceptableTime}ms"
        )
    }
    
    @Test
    fun `feedback state updates should be efficient`() = runTest {
        val numberOfUpdates = 1000
        val maxAcceptableTime = 100L // 100ms for 1000 updates
        
        val executionTime = measureTimeMillis {
            repeat(numberOfUpdates) {
                visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS, duration = 1L)
                visualFeedbackManager.clearFeedback()
            }
        }
        
        assertTrue(
            executionTime < maxAcceptableTime,
            "State updates took ${executionTime}ms, expected less than ${maxAcceptableTime}ms"
        )
    }
    
    @Test
    fun `memory usage should remain stable during feedback operations`() = runTest {
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Perform many feedback operations
        repeat(1000) {
            visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS, duration = 1L)
            hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.BUTTON_PRESS)
            visualFeedbackManager.clearFeedback()
            hapticFeedbackManager.clearFeedback()
        }
        
        // Force garbage collection
        System.gc()
        kotlinx.coroutines.delay(100)
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val maxAcceptableIncrease = 1024 * 1024 // 1MB
        
        assertTrue(
            memoryIncrease < maxAcceptableIncrease,
            "Memory increased by ${memoryIncrease} bytes, expected less than ${maxAcceptableIncrease} bytes"
        )
    }
    
    @Test
    fun `feedback clearing should be immediate`() = runTest {
        val maxAcceptableTime = 10L // 10ms
        
        // Activate feedback
        visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS)
        assertTrue(visualFeedbackManager.feedbackState.value.isActive)
        
        // Measure clear time
        val executionTime = measureTimeMillis {
            visualFeedbackManager.clearFeedback()
        }
        
        assertFalse(visualFeedbackManager.feedbackState.value.isActive)
        assertTrue(
            executionTime < maxAcceptableTime,
            "Feedback clearing took ${executionTime}ms, expected less than ${maxAcceptableTime}ms"
        )
    }
    
    @Test
    fun `rapid feedback triggers should not cause performance degradation`() = runTest {
        val numberOfRapidTriggers = 50
        val maxAcceptableTimePerTrigger = 20L // 20ms per trigger
        
        val times = mutableListOf<Long>()
        
        repeat(numberOfRapidTriggers) {
            val time = measureTimeMillis {
                visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS, duration = 1L)
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        assertTrue(
            averageTime < maxAcceptableTimePerTrigger,
            "Average trigger time was ${averageTime}ms, expected less than ${maxAcceptableTimePerTrigger}ms"
        )
        
        assertTrue(
            maxTime < maxAcceptableTimePerTrigger * 2,
            "Maximum trigger time was ${maxTime}ms, expected less than ${maxAcceptableTimePerTrigger * 2}ms"
        )
    }
    
    @Test
    fun `feedback state flow should not cause memory leaks`() = runTest {
        val initialCollectors = mutableListOf<kotlinx.coroutines.flow.StateFlow<VisualFeedbackState>>()
        
        // Create multiple collectors
        repeat(100) {
            initialCollectors.add(visualFeedbackManager.feedbackState)
        }
        
        // Trigger feedback operations
        repeat(100) {
            visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS, duration = 1L)
        }
        
        // Clear references
        initialCollectors.clear()
        System.gc()
        kotlinx.coroutines.delay(100)
        
        // Verify the manager still works correctly
        visualFeedbackManager.triggerFeedback(FeedbackType.NAVIGATION_SUCCESS)
        assertTrue(visualFeedbackManager.feedbackState.value.isActive)
        assertEquals(FeedbackType.NAVIGATION_SUCCESS, visualFeedbackManager.feedbackState.value.feedbackType)
    }
    
    @Test
    fun `haptic feedback disabled state should not impact performance`() = runTest {
        val maxAcceptableTime = 10L // 10ms
        
        // Disable haptic feedback
        hapticFeedbackManager.setHapticEnabled(false)
        
        val executionTime = measureTimeMillis {
            repeat(100) {
                hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.BUTTON_PRESS)
            }
        }
        
        assertTrue(
            executionTime < maxAcceptableTime,
            "Disabled haptic feedback took ${executionTime}ms, expected less than ${maxAcceptableTime}ms"
        )
    }
    
    @Test
    fun `feedback system should handle edge cases efficiently`() = runTest {
        val maxAcceptableTime = 50L // 50ms
        
        val executionTime = measureTimeMillis {
            // Test null/empty scenarios
            visualFeedbackManager.clearFeedback() // Clear when already clear
            hapticFeedbackManager.clearFeedback() // Clear when already clear
            
            // Test rapid enable/disable
            repeat(10) {
                hapticFeedbackManager.setHapticEnabled(false)
                hapticFeedbackManager.setHapticEnabled(true)
            }
            
            // Test extreme values
            visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS, intensity = 0f, duration = 0L)
            visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS, intensity = Float.MAX_VALUE, duration = Long.MAX_VALUE)
        }
        
        assertTrue(
            executionTime < maxAcceptableTime,
            "Edge case handling took ${executionTime}ms, expected less than ${maxAcceptableTime}ms"
        )
    }
}