package com.locationsharing.app.ui.map.haptic

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis

/**
 * Performance tests for MapHapticFeedbackManager.
 * 
 * Tests that haptic feedback operations are performant and don't block
 * the main thread or cause performance issues during rapid interactions.
 * Validates performance requirements from the MapScreen redesign specification.
 */
@RunWith(RobolectricTestRunner::class)
class MapHapticFeedbackPerformanceTest {
    
    @MockK
    private lateinit var mockHapticFeedback: HapticFeedback
    
    private lateinit var hapticManager: MapHapticFeedbackManager
    
    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockHapticFeedback.performHapticFeedback(any()) } returns Unit
        hapticManager = MapHapticFeedbackManager(mockHapticFeedback)
    }
    
    @Test
    fun `haptic feedback should be performant for single interactions`() {
        // When & Then - Each haptic feedback call should complete quickly
        val executionTime = measureTimeMillis {
            hapticManager.performPrimaryFABAction()
        }
        
        // Haptic feedback should complete in less than 10ms
        assert(executionTime < 10) { "Haptic feedback took ${executionTime}ms, expected < 10ms" }
        
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `haptic feedback should handle rapid successive calls efficiently`() {
        // When - Simulate rapid user interactions
        val executionTime = measureTimeMillis {
            repeat(100) {
                hapticManager.performPrimaryFABAction()
            }
        }
        
        // Then - 100 calls should complete in less than 100ms (1ms per call average)
        assert(executionTime < 100) { "100 haptic feedback calls took ${executionTime}ms, expected < 100ms" }
        
        verify(exactly = 100) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `haptic feedback should not block main thread`() = runBlocking {
        // Given
        var mainThreadBlocked = false
        
        // When - Perform haptic feedback on main thread
        withContext(Dispatchers.Main) {
            val startTime = System.currentTimeMillis()
            hapticManager.performPrimaryFABAction()
            val endTime = System.currentTimeMillis()
            
            // If main thread was blocked, this would take longer
            mainThreadBlocked = (endTime - startTime) > 5
        }
        
        // Then - Main thread should not be blocked
        assert(!mainThreadBlocked) { "Haptic feedback blocked main thread" }
        
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `haptic feedback should handle concurrent calls efficiently`() = runBlocking {
        // When - Simulate concurrent haptic feedback calls
        val executionTime = measureTimeMillis {
            val jobs = (1..50).map {
                launch {
                    hapticManager.performPrimaryFABAction()
                    hapticManager.performSecondaryFABAction()
                }
            }
            jobs.forEach { it.join() }
        }
        
        // Then - Concurrent calls should complete efficiently
        assert(executionTime < 200) { "Concurrent haptic feedback calls took ${executionTime}ms, expected < 200ms" }
        
        verify(exactly = 50) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        verify(exactly = 50) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    
    @Test
    fun `haptic feedback should handle exceptions without performance impact`() {
        // Given - Mock haptic feedback to throw exceptions
        every { mockHapticFeedback.performHapticFeedback(any()) } throws RuntimeException("Haptic not available")
        
        // When - Measure performance with exceptions
        val executionTime = measureTimeMillis {
            repeat(50) {
                hapticManager.performPrimaryFABAction()
                hapticManager.performSecondaryFABAction()
            }
        }
        
        // Then - Exception handling should not significantly impact performance
        assert(executionTime < 100) { "Exception handling took ${executionTime}ms, expected < 100ms" }
        
        verify(exactly = 100) { mockHapticFeedback.performHapticFeedback(any()) }
    }
    
    @Test
    fun `haptic feedback should be memory efficient`() {
        // Given - Create multiple manager instances
        val managers = (1..100).map { MapHapticFeedbackManager(mockHapticFeedback) }
        
        // When - Use all managers
        val executionTime = measureTimeMillis {
            managers.forEach { manager ->
                manager.performPrimaryFABAction()
            }
        }
        
        // Then - Memory usage should be efficient
        assert(executionTime < 50) { "Memory-intensive operations took ${executionTime}ms, expected < 50ms" }
        
        verify(exactly = 100) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `haptic feedback should handle different feedback types efficiently`() {
        // When - Test all feedback types for performance
        val executionTime = measureTimeMillis {
            hapticManager.performPrimaryFABAction()
            hapticManager.performSecondaryFABAction()
            hapticManager.performAppBarAction()
            hapticManager.performDrawerAction()
            hapticManager.performFriendItemAction()
            hapticManager.performFriendActionButton()
            hapticManager.performStatusSheetAction(true)
            hapticManager.performStatusSheetAction(false)
            hapticManager.performMarkerAction()
            hapticManager.performErrorFeedback()
            hapticManager.performSuccessFeedback()
            hapticManager.performLocationAction()
        }
        
        // Then - All feedback types should execute quickly
        assert(executionTime < 20) { "All feedback types took ${executionTime}ms, expected < 20ms" }
        
        // Verify correct feedback types were used
        verify(exactly = 8) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        verify(exactly = 4) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    
    @Test
    fun `haptic feedback testing utilities should be performant`() {
        // When - Test the testing utilities performance
        val executionTime = measureTimeMillis {
            MapHapticTesting.testAllHapticPatterns(hapticManager)
        }
        
        // Then - Testing utilities should execute quickly
        assert(executionTime < 50) { "Testing utilities took ${executionTime}ms, expected < 50ms" }
        
        // Verify all patterns were tested
        verify(exactly = 10) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        verify(exactly = 2) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    
    @Test
    fun `haptic feedback validation should be performant`() {
        // When - Test validation performance
        val executionTime = measureTimeMillis {
            repeat(10) {
                MapHapticTesting.validateAccessibilityCompatibility(hapticManager)
            }
        }
        
        // Then - Validation should be quick
        assert(executionTime < 20) { "Validation took ${executionTime}ms, expected < 20ms" }
        
        verify(exactly = 10) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `haptic feedback should handle high-frequency interactions`() {
        // When - Simulate high-frequency user interactions (like rapid scrolling)
        val executionTime = measureTimeMillis {
            repeat(1000) { index ->
                when (index % 4) {
                    0 -> hapticManager.performPrimaryFABAction()
                    1 -> hapticManager.performFriendItemAction()
                    2 -> hapticManager.performDrawerAction()
                    3 -> hapticManager.performMarkerAction()
                }
            }
        }
        
        // Then - High-frequency interactions should be handled efficiently
        assert(executionTime < 500) { "High-frequency interactions took ${executionTime}ms, expected < 500ms" }
        
        verify(exactly = 1000) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `haptic feedback should maintain performance under stress`() {
        // When - Stress test with mixed operations
        val executionTime = measureTimeMillis {
            repeat(200) { index ->
                when (index % 6) {
                    0 -> hapticManager.performPrimaryFABAction()
                    1 -> hapticManager.performSecondaryFABAction()
                    2 -> hapticManager.performFriendActionButton()
                    3 -> hapticManager.performErrorFeedback()
                    4 -> hapticManager.performStatusSheetAction(true)
                    5 -> hapticManager.performLocationAction()
                }
            }
        }
        
        // Then - Performance should remain consistent under stress
        assert(executionTime < 300) { "Stress test took ${executionTime}ms, expected < 300ms" }
        
        // Verify mixed feedback types were used correctly
        verify(exactly = 134) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        verify(exactly = 66) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
}