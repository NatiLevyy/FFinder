package com.locationsharing.app.ui.components.feedback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HapticFeedbackManagerTest {
    
    private lateinit var hapticFeedbackManager: HapticFeedbackManagerImpl
    
    @BeforeEach
    fun setUp() {
        hapticFeedbackManager = HapticFeedbackManagerImpl()
    }
    
    @Test
    fun `initial state should be inactive`() {
        val initialState = hapticFeedbackManager.feedbackState.value
        
        assertFalse(initialState.isActive)
        assertNull(initialState.feedbackType)
        assertEquals(1.0f, initialState.intensity)
    }
    
    @Test
    fun `haptic should be enabled by default`() {
        assertTrue(hapticFeedbackManager.isHapticEnabled())
    }
    
    @Test
    fun `setHapticEnabled should update enabled state`() {
        hapticFeedbackManager.setHapticEnabled(false)
        assertFalse(hapticFeedbackManager.isHapticEnabled())
        
        hapticFeedbackManager.setHapticEnabled(true)
        assertTrue(hapticFeedbackManager.isHapticEnabled())
    }
    
    @Test
    fun `triggerHapticFeedback should activate feedback when enabled`() = runTest {
        val feedbackType = NavigationHapticType.BUTTON_PRESS
        val intensity = 0.8f
        
        hapticFeedbackManager.triggerHapticFeedback(feedbackType, intensity)
        
        val state = hapticFeedbackManager.feedbackState.value
        assertTrue(state.isActive)
        assertEquals(feedbackType, state.feedbackType)
        assertEquals(intensity, state.intensity)
    }
    
    @Test
    fun `triggerHapticFeedback should not activate when disabled`() = runTest {
        hapticFeedbackManager.setHapticEnabled(false)
        
        hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.BUTTON_PRESS)
        
        val state = hapticFeedbackManager.feedbackState.value
        assertFalse(state.isActive)
        assertNull(state.feedbackType)
    }
    
    @Test
    fun `feedback should auto-clear after delay`() = runTest {
        hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.NAVIGATION_START)
        
        // Initially active
        assertTrue(hapticFeedbackManager.feedbackState.value.isActive)
        
        // Wait for auto-clear (100ms + buffer)
        kotlinx.coroutines.delay(150)
        
        // Should be cleared
        assertFalse(hapticFeedbackManager.feedbackState.value.isActive)
        assertNull(hapticFeedbackManager.feedbackState.value.feedbackType)
    }
    
    @Test
    fun `clearFeedback should reset state`() = runTest {
        // Activate feedback
        hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.SUCCESS_CONFIRMATION)
        assertTrue(hapticFeedbackManager.feedbackState.value.isActive)
        
        // Clear feedback
        hapticFeedbackManager.clearFeedback()
        
        val state = hapticFeedbackManager.feedbackState.value
        assertFalse(state.isActive)
        assertNull(state.feedbackType)
        assertEquals(1.0f, state.intensity)
    }
    
    @Test
    fun `extension functions should trigger correct feedback types`() = runTest {
        // Test button press
        hapticFeedbackManager.triggerButtonPress()
        assertEquals(NavigationHapticType.BUTTON_PRESS, hapticFeedbackManager.feedbackState.value.feedbackType)
        
        hapticFeedbackManager.clearFeedback()
        
        // Test navigation start
        hapticFeedbackManager.triggerNavigationStart()
        assertEquals(NavigationHapticType.NAVIGATION_START, hapticFeedbackManager.feedbackState.value.feedbackType)
        
        hapticFeedbackManager.clearFeedback()
        
        // Test navigation success
        hapticFeedbackManager.triggerNavigationSuccess()
        assertEquals(NavigationHapticType.NAVIGATION_SUCCESS, hapticFeedbackManager.feedbackState.value.feedbackType)
        
        hapticFeedbackManager.clearFeedback()
        
        // Test navigation error
        hapticFeedbackManager.triggerNavigationError()
        assertEquals(NavigationHapticType.NAVIGATION_ERROR, hapticFeedbackManager.feedbackState.value.feedbackType)
        
        hapticFeedbackManager.clearFeedback()
        
        // Test back navigation
        hapticFeedbackManager.triggerBackNavigation()
        assertEquals(NavigationHapticType.BACK_NAVIGATION, hapticFeedbackManager.feedbackState.value.feedbackType)
        
        hapticFeedbackManager.clearFeedback()
        
        // Test modal open
        hapticFeedbackManager.triggerModalOpen()
        assertEquals(NavigationHapticType.MODAL_OPEN, hapticFeedbackManager.feedbackState.value.feedbackType)
        
        hapticFeedbackManager.clearFeedback()
        
        // Test modal close
        hapticFeedbackManager.triggerModalClose()
        assertEquals(NavigationHapticType.MODAL_CLOSE, hapticFeedbackManager.feedbackState.value.feedbackType)
        
        hapticFeedbackManager.clearFeedback()
        
        // Test success confirmation
        hapticFeedbackManager.triggerSuccessConfirmation()
        assertEquals(NavigationHapticType.SUCCESS_CONFIRMATION, hapticFeedbackManager.feedbackState.value.feedbackType)
        
        hapticFeedbackManager.clearFeedback()
        
        // Test error feedback
        hapticFeedbackManager.triggerErrorFeedback()
        assertEquals(NavigationHapticType.ERROR_FEEDBACK, hapticFeedbackManager.feedbackState.value.feedbackType)
        
        hapticFeedbackManager.clearFeedback()
        
        // Test selection change
        hapticFeedbackManager.triggerSelectionChange()
        assertEquals(NavigationHapticType.SELECTION_CHANGE, hapticFeedbackManager.feedbackState.value.feedbackType)
    }
}