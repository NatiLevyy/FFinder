package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for PrimaryCallToAction component.
 * 
 * Tests the component's integration with ResponsiveLayout and other
 * home screen components to ensure proper behavior in real usage scenarios.
 */
@RunWith(AndroidJUnit4::class)
class PrimaryCallToActionIntegrationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun primaryCallToAction_integratesWithResponsiveLayout() {
        var startShareClicked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                ResponsiveLayout { config ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PrimaryCallToAction(
                            onStartShare = { startShareClicked = true },
                            isNarrowScreen = config.isNarrowScreen
                        )
                    }
                }
            }
        }
        
        // Should display the appropriate FAB based on screen size
        // In test environment, this will typically be normal screen
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertIsDisplayed()
            .performClick()
        
        assertTrue("Callback should be invoked through responsive layout", startShareClicked)
    }
    
    @Test
    fun primaryCallToAction_worksWithHapticFeedback() {
        var callbackInvoked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { callbackInvoked = true },
                    isNarrowScreen = false
                )
            }
        }
        
        // Click should trigger both haptic feedback and callback
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .performClick()
        
        assertTrue("Callback should be invoked with haptic feedback", callbackInvoked)
    }
    
    @Test
    fun primaryCallToAction_maintainsStateAcrossRecomposition() {
        var recomposeCount = 0
        var isNarrow = false
        
        composeTestRule.setContent {
            recomposeCount++
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = isNarrow
                )
            }
        }
        
        // Initial composition
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertIsDisplayed()
        
        // Trigger recomposition by changing narrow screen state
        isNarrow = true
        composeTestRule.setContent {
            recomposeCount++
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = isNarrow
                )
            }
        }
        
        // Should now show icon-only version
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing")
            .assertIsDisplayed()
        
        assertTrue("Component should recompose properly", recomposeCount > 1)
    }
    
    @Test
    fun primaryCallToAction_accessibilityIntegration() {
        composeTestRule.setContent {
            FFinderTheme {
                Column {
                    // Test with other components to ensure accessibility order
                    HeroSection()
                    PrimaryCallToAction(
                        onStartShare = {},
                        isNarrowScreen = false
                    )
                }
            }
        }
        
        // Verify CTA is accessible after hero section
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button")
            .assertIsDisplayed()
    }
    
    @Test
    fun primaryCallToAction_performanceWithFrequentUpdates() {
        var updateCount = 0
        var isNarrow = false
        
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { updateCount++ },
                    isNarrowScreen = isNarrow
                )
            }
        }
        
        // Simulate multiple screen size changes
        repeat(5) { iteration ->
            isNarrow = iteration % 2 == 0
            
            composeTestRule.setContent {
                FFinderTheme {
                    PrimaryCallToAction(
                        onStartShare = { updateCount++ },
                        isNarrowScreen = isNarrow
                    )
                }
            }
            
            // Verify component still works after updates
            if (isNarrow) {
                composeTestRule
                    .onNodeWithContentDescription("Start Live Sharing")
                    .assertIsDisplayed()
            } else {
                composeTestRule
                    .onNodeWithText("Start Live Sharing")
                    .assertIsDisplayed()
            }
        }
    }
}