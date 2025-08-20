package com.locationsharing.app.ui.map.haptic

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for MapHapticFeedbackManager.
 * 
 * Tests all haptic feedback patterns to ensure they work correctly
 * and provide appropriate feedback types for different actions.
 * Validates requirements 3.4, 4.5, 9.6 from the MapScreen redesign specification.
 */
@RunWith(RobolectricTestRunner::class)
class MapHapticFeedbackManagerTest {
    
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
    fun `performPrimaryFABAction should use TextHandleMove feedback`() {
        // When
        hapticManager.performPrimaryFABAction()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `performSecondaryFABAction should use LongPress feedback`() {
        // When
        hapticManager.performSecondaryFABAction()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    
    @Test
    fun `performAppBarAction should use TextHandleMove feedback`() {
        // When
        hapticManager.performAppBarAction()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `performDrawerAction should use TextHandleMove feedback`() {
        // When
        hapticManager.performDrawerAction()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `performFriendItemAction should use TextHandleMove feedback`() {
        // When
        hapticManager.performFriendItemAction()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `performFriendActionButton should use LongPress feedback`() {
        // When
        hapticManager.performFriendActionButton()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    
    @Test
    fun `performStatusSheetAction with important action should use LongPress feedback`() {
        // When
        hapticManager.performStatusSheetAction(isImportantAction = true)
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    
    @Test
    fun `performStatusSheetAction with non-important action should use TextHandleMove feedback`() {
        // When
        hapticManager.performStatusSheetAction(isImportantAction = false)
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `performMarkerAction should use TextHandleMove feedback`() {
        // When
        hapticManager.performMarkerAction()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `performErrorFeedback should use LongPress feedback`() {
        // When
        hapticManager.performErrorFeedback()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    
    @Test
    fun `performSuccessFeedback should use TextHandleMove feedback`() {
        // When
        hapticManager.performSuccessFeedback()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `performLocationAction should use TextHandleMove feedback`() {
        // When
        hapticManager.performLocationAction()
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `haptic feedback should handle exceptions gracefully`() {
        // Given
        every { mockHapticFeedback.performHapticFeedback(any()) } throws RuntimeException("Haptic not available")
        
        // When & Then - should not throw exception
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
        
        // Verify all calls were attempted
        verify(exactly = 12) { mockHapticFeedback.performHapticFeedback(any()) }
    }
    
    @Test
    fun `MapHapticPatterns extension functions should work correctly`() {
        // When
        mockHapticFeedback.apply {
            MapHapticPatterns.run {
                fabPress()
                importantAction()
                lightTouch()
                navigationAction()
                errorState()
            }
        }
        
        // Then
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
        verify(exactly = 3) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        verify(exactly = 2) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    
    @Test
    fun `MapHapticTesting should test all patterns without throwing exceptions`() {
        // When & Then - should not throw exception
        MapHapticTesting.testAllHapticPatterns(hapticManager)
        
        // Verify all patterns were tested
        verify(exactly = 2) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) } // Secondary FAB + Error
        verify(exactly = 10) { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } // All others
    }
    
    @Test
    fun `MapHapticTesting validateAccessibilityCompatibility should return true on success`() {
        // When
        val result = MapHapticTesting.validateAccessibilityCompatibility(hapticManager)
        
        // Then
        assert(result)
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    
    @Test
    fun `MapHapticTesting validateAccessibilityCompatibility should return false on failure`() {
        // Given
        every { mockHapticFeedback.performHapticFeedback(any()) } throws RuntimeException("Accessibility service blocked")
        
        // When
        val result = MapHapticTesting.validateAccessibilityCompatibility(hapticManager)
        
        // Then
        assert(!result)
        verify { mockHapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
}