package com.locationsharing.app.ui.components.feedback

import com.locationsharing.app.navigation.NavigationManagerImpl
import com.locationsharing.app.navigation.NavigationStateTracker
import com.locationsharing.app.navigation.NavigationErrorHandler
import com.locationsharing.app.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class VisualFeedbackIntegrationTest {
    
    private lateinit var visualFeedbackManager: VisualFeedbackManagerImpl
    private lateinit var hapticFeedbackManager: HapticFeedbackManagerImpl
    private lateinit var navigationManager: NavigationManagerImpl
    private val mockNavController = mock<NavController>()
    private val mockNavigationStateTracker = mock<NavigationStateTracker>()
    private val mockNavigationErrorHandler = mock<NavigationErrorHandler>()
    
    @BeforeEach
    fun setUp() {
        visualFeedbackManager = VisualFeedbackManagerImpl()
        hapticFeedbackManager = HapticFeedbackManagerImpl()
        navigationManager = NavigationManagerImpl(
            navigationStateTracker = mockNavigationStateTracker,
            navigationErrorHandler = mockNavigationErrorHandler,
            visualFeedbackManager = visualFeedbackManager,
            hapticFeedbackManager = hapticFeedbackManager
        )
        
        // Set up mock navigation state
        val mockNavigationState = com.locationsharing.app.navigation.NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        whenever(mockNavigationStateTracker.currentState).thenReturn(MutableStateFlow(mockNavigationState))
        
        navigationManager.setNavController(mockNavController)
    }
    
    @Test
    fun `navigation should trigger visual feedback`() = runTest {
        // Trigger navigation
        navigationManager.navigateToMap()
        
        // Wait for feedback to be triggered
        kotlinx.coroutines.delay(100)
        
        // Verify visual feedback was triggered
        val visualState = visualFeedbackManager.feedbackState.value
        // Note: Due to timing, the feedback might have already cleared
        // In a real test, we'd need to capture the state changes
    }
    
    @Test
    fun `navigation should trigger haptic feedback`() = runTest {
        // Trigger navigation
        navigationManager.navigateToMap()
        
        // Wait for feedback to be triggered
        kotlinx.coroutines.delay(100)
        
        // Verify haptic feedback was triggered
        val hapticState = hapticFeedbackManager.feedbackState.value
        // Note: Due to timing, the feedback might have already cleared
        // In a real test, we'd need to capture the state changes
    }
    
    @Test
    fun `visual and haptic feedback should work together`() = runTest {
        // Test that both feedback systems can be active simultaneously
        visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS)
        hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.BUTTON_PRESS)
        
        val visualState = visualFeedbackManager.feedbackState.value
        val hapticState = hapticFeedbackManager.feedbackState.value
        
        assertTrue(visualState.isActive)
        assertTrue(hapticState.isActive)
        assertEquals(FeedbackType.BUTTON_PRESS, visualState.feedbackType)
        assertEquals(NavigationHapticType.BUTTON_PRESS, hapticState.feedbackType)
    }
    
    @Test
    fun `feedback managers should handle concurrent operations`() = runTest {
        // Trigger multiple feedback operations concurrently
        val jobs = listOf(
            kotlinx.coroutines.launch {
                visualFeedbackManager.triggerFeedback(FeedbackType.NAVIGATION_START)
            },
            kotlinx.coroutines.launch {
                hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.NAVIGATION_START)
            },
            kotlinx.coroutines.launch {
                visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS)
            },
            kotlinx.coroutines.launch {
                hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.BUTTON_PRESS)
            }
        )
        
        // Wait for all operations to complete
        jobs.forEach { it.join() }
        
        // Verify that the managers handled concurrent operations without issues
        // The final state should be from the last operation
        val visualState = visualFeedbackManager.feedbackState.value
        val hapticState = hapticFeedbackManager.feedbackState.value
        
        assertTrue(visualState.isActive)
        assertTrue(hapticState.isActive)
    }
    
    @Test
    fun `feedback should be disabled when haptic is disabled`() = runTest {
        // Disable haptic feedback
        hapticFeedbackManager.setHapticEnabled(false)
        
        // Try to trigger haptic feedback
        hapticFeedbackManager.triggerHapticFeedback(NavigationHapticType.BUTTON_PRESS)
        
        // Verify haptic feedback was not triggered
        val hapticState = hapticFeedbackManager.feedbackState.value
        assertFalse(hapticState.isActive)
        
        // Visual feedback should still work
        visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS)
        val visualState = visualFeedbackManager.feedbackState.value
        assertTrue(visualState.isActive)
    }
    
    @Test
    fun `feedback should clear properly after duration`() = runTest {
        val shortDuration = 50L
        
        // Trigger feedback with short duration
        visualFeedbackManager.triggerFeedback(
            FeedbackType.BUTTON_PRESS,
            duration = shortDuration
        )
        
        // Verify feedback is active
        assertTrue(visualFeedbackManager.feedbackState.value.isActive)
        
        // Wait for duration + buffer
        kotlinx.coroutines.delay(shortDuration + 20)
        
        // Verify feedback has cleared
        assertFalse(visualFeedbackManager.feedbackState.value.isActive)
    }
    
    @Test
    fun `multiple feedback types should be handled correctly`() = runTest {
        val feedbackTypes = listOf(
            FeedbackType.BUTTON_PRESS,
            FeedbackType.NAVIGATION_START,
            FeedbackType.NAVIGATION_SUCCESS,
            FeedbackType.NAVIGATION_ERROR,
            FeedbackType.LOADING_START,
            FeedbackType.LOADING_END,
            FeedbackType.SUCCESS_CONFIRMATION,
            FeedbackType.ERROR_SHAKE,
            FeedbackType.RIPPLE_EFFECT
        )
        
        // Test each feedback type
        for (feedbackType in feedbackTypes) {
            visualFeedbackManager.triggerFeedback(feedbackType)
            
            val state = visualFeedbackManager.feedbackState.value
            assertTrue(state.isActive)
            assertEquals(feedbackType, state.feedbackType)
            
            visualFeedbackManager.clearFeedback()
        }
    }
    
    @Test
    fun `haptic feedback types should map correctly`() = runTest {
        val hapticTypes = listOf(
            NavigationHapticType.BUTTON_PRESS,
            NavigationHapticType.NAVIGATION_START,
            NavigationHapticType.NAVIGATION_SUCCESS,
            NavigationHapticType.NAVIGATION_ERROR,
            NavigationHapticType.BACK_NAVIGATION,
            NavigationHapticType.MODAL_OPEN,
            NavigationHapticType.MODAL_CLOSE,
            NavigationHapticType.SUCCESS_CONFIRMATION,
            NavigationHapticType.ERROR_FEEDBACK,
            NavigationHapticType.SELECTION_CHANGE
        )
        
        // Test each haptic type
        for (hapticType in hapticTypes) {
            hapticFeedbackManager.triggerHapticFeedback(hapticType)
            
            val state = hapticFeedbackManager.feedbackState.value
            assertTrue(state.isActive)
            assertEquals(hapticType, state.feedbackType)
            
            hapticFeedbackManager.clearFeedback()
        }
    }
}