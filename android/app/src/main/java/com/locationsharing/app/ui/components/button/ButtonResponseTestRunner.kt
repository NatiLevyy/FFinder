package com.locationsharing.app.ui.components.button

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Simple test runner to verify button response functionality works correctly.
 * This is a temporary class for validation purposes.
 */
class ButtonResponseTestRunner {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val buttonAnalytics = object : com.locationsharing.app.navigation.ButtonAnalytics {
        override fun trackButtonClick(buttonId: String, buttonType: String, responseTime: Long) {
            // Mock implementation for testing
        }
        override fun trackButtonInteractionError(buttonId: String, errorType: String) {
            // Mock implementation for testing
        }
        override fun trackButtonLoadingDuration(buttonId: String, loadingDuration: Long) {
            // Mock implementation for testing
        }
        override fun trackButtonAccessibilityUsage(buttonId: String, accessibilityFeature: String) {
            // Mock implementation for testing
        }
        override fun trackButtonDoubleClickPrevention(buttonId: String, preventionCount: Int) {
            // Mock implementation for testing
        }
    }
    private val buttonResponseManager = ButtonResponseManagerImpl(buttonAnalytics)
    
    fun runTests() {
        scope.launch {
            Log.d("ButtonResponseTest", "Starting button response tests...")
            
            testBasicButtonClick()
            testDebouncing()
            testButtonStates()
            testMultipleButtons()
            
            Log.d("ButtonResponseTest", "All tests completed successfully!")
        }
    }
    
    private suspend fun testBasicButtonClick() {
        Log.d("ButtonResponseTest", "Testing basic button click...")
        
        val buttonId = "test_button"
        var actionExecuted = false
        
        buttonResponseManager.handleButtonClick(buttonId) {
            actionExecuted = true
        }
        
        val state = buttonResponseManager.getButtonState(buttonId).value
        
        assert(actionExecuted) { "Action should have been executed" }
        assert(state.showFeedback) { "Feedback should be shown" }
        
        // Wait for feedback to disappear
        delay(250)
        
        val finalState = buttonResponseManager.getButtonState(buttonId).value
        assert(!finalState.showFeedback) { "Feedback should have disappeared" }
        
        Log.d("ButtonResponseTest", "✓ Basic button click test passed")
    }
    
    private suspend fun testDebouncing() {
        Log.d("ButtonResponseTest", "Testing debouncing...")
        
        val buttonId = "debounce_test"
        var clickCount = 0
        
        // Click twice rapidly
        buttonResponseManager.handleButtonClick(buttonId) { clickCount++ }
        buttonResponseManager.handleButtonClick(buttonId) { clickCount++ }
        
        assert(clickCount == 1) { "Only first click should execute due to debouncing" }
        
        // Wait for debounce period and click again
        delay(600)
        buttonResponseManager.handleButtonClick(buttonId) { clickCount++ }
        
        assert(clickCount == 2) { "Second click should execute after debounce period" }
        
        Log.d("ButtonResponseTest", "✓ Debouncing test passed")
    }
    
    private suspend fun testButtonStates() {
        Log.d("ButtonResponseTest", "Testing button states...")
        
        val buttonId = "state_test"
        var actionExecuted = false
        
        // Test disabled button
        buttonResponseManager.setButtonEnabled(buttonId, false)
        buttonResponseManager.handleButtonClick(buttonId) { actionExecuted = true }
        
        assert(!actionExecuted) { "Action should not execute when button is disabled" }
        
        // Test loading button
        buttonResponseManager.setButtonEnabled(buttonId, true)
        buttonResponseManager.setButtonLoading(buttonId, true)
        buttonResponseManager.handleButtonClick(buttonId) { actionExecuted = true }
        
        assert(!actionExecuted) { "Action should not execute when button is loading" }
        
        // Test enabled button
        buttonResponseManager.setButtonLoading(buttonId, false)
        buttonResponseManager.handleButtonClick(buttonId) { actionExecuted = true }
        
        assert(actionExecuted) { "Action should execute when button is enabled and not loading" }
        
        Log.d("ButtonResponseTest", "✓ Button states test passed")
    }
    
    private suspend fun testMultipleButtons() {
        Log.d("ButtonResponseTest", "Testing multiple buttons...")
        
        val button1Id = "button1"
        val button2Id = "button2"
        var button1Clicked = false
        var button2Clicked = false
        
        // Set different states
        buttonResponseManager.setButtonEnabled(button1Id, false)
        buttonResponseManager.setButtonLoading(button2Id, true)
        
        // Try to click both
        buttonResponseManager.handleButtonClick(button1Id) { button1Clicked = true }
        buttonResponseManager.handleButtonClick(button2Id) { button2Clicked = true }
        
        assert(!button1Clicked) { "Button 1 should not click when disabled" }
        assert(!button2Clicked) { "Button 2 should not click when loading" }
        
        // Enable both and click
        buttonResponseManager.setButtonEnabled(button1Id, true)
        buttonResponseManager.setButtonLoading(button2Id, false)
        
        buttonResponseManager.handleButtonClick(button1Id) { button1Clicked = true }
        buttonResponseManager.handleButtonClick(button2Id) { button2Clicked = true }
        
        assert(button1Clicked) { "Button 1 should click when enabled" }
        assert(button2Clicked) { "Button 2 should click when not loading" }
        
        Log.d("ButtonResponseTest", "✓ Multiple buttons test passed")
    }
}