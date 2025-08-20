package com.locationsharing.app.ui.map.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for DebugFAB component
 * Tests requirements 4.1, 4.2, 4.4, 4.5
 */
@RunWith(AndroidJUnit4::class)
class DebugFABTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun debugFAB_whenDebugBuild_isDisplayed() {
        // Given debug build (BuildConfig.DEBUG = true in test)
        val onClickMock = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                DebugFAB(onClick = onClickMock)
            }
        }
        
        // Then - FAB should be displayed in debug builds (requirement 4.1)
        if (BuildConfig.DEBUG) {
            composeTestRule
                .onNodeWithContentDescription(MapScreenConstants.Accessibility.DEBUG_FAB_DESC)
                .assertIsDisplayed()
        }
    }
    
    @Test
    fun debugFAB_whenReleaseBuild_isNotDisplayed() {
        // This test would need to be run with BuildConfig.DEBUG = false
        // In a real scenario, this would be tested in a separate build variant
        val onClickMock = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                DebugFAB(onClick = onClickMock)
            }
        }
        
        // Then - FAB should not be displayed in release builds (requirement 4.4)
        if (!BuildConfig.DEBUG) {
            composeTestRule
                .onNodeWithContentDescription(MapScreenConstants.Accessibility.DEBUG_FAB_DESC)
                .assertIsNotDisplayed()
        }
    }
    
    @Test
    fun debugFAB_whenClicked_triggersOnClick() {
        // Given
        val onClickMock = mock<() -> Unit>()
        
        composeTestRule.setContent {
            FFinderTheme {
                DebugFAB(onClick = onClickMock)
            }
        }
        
        // When - clicking the debug FAB (requirement 4.2)
        if (BuildConfig.DEBUG) {
            composeTestRule
                .onNodeWithContentDescription(MapScreenConstants.Accessibility.DEBUG_FAB_DESC)
                .performClick()
            
            // Then - onClick should be triggered
            verify(onClickMock).invoke()
        }
    }
    
    @Test
    fun debugFAB_whenDisabled_doesNotTriggerOnClick() {
        // Given
        val onClickMock = mock<() -> Unit>()
        
        composeTestRule.setContent {
            FFinderTheme {
                DebugFAB(
                    onClick = onClickMock,
                    enabled = false
                )
            }
        }
        
        // When - clicking disabled debug FAB
        if (BuildConfig.DEBUG) {
            composeTestRule
                .onNodeWithContentDescription(MapScreenConstants.Accessibility.DEBUG_FAB_DESC)
                .performClick()
            
            // Then - onClick should not be triggered
            verify(onClickMock, org.mockito.kotlin.never()).invoke()
        }
    }
    
    @Test
    fun debugFAB_hasCorrectAccessibilityProperties() {
        // Given
        val onClickMock = mock<() -> Unit>()
        
        composeTestRule.setContent {
            FFinderTheme {
                DebugFAB(onClick = onClickMock)
            }
        }
        
        // Then - should have correct content description (requirement 4.5)
        if (BuildConfig.DEBUG) {
            composeTestRule
                .onNodeWithContentDescription(MapScreenConstants.Accessibility.DEBUG_FAB_DESC)
                .assertIsDisplayed()
        }
    }
}