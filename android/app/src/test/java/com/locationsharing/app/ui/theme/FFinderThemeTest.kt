package com.locationsharing.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Test class for FFinder theme colors and branding
 * Verifies that the brand colors are correctly applied according to the Home Screen Redesign requirements
 */
class FFinderThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `fFinderTheme should use correct brand colors for light theme`() {
        var actualPrimary: Color? = null
        var actualSecondary: Color? = null
        var actualGradientTop: Color? = null
        var actualGradientBottom: Color? = null

        composeTestRule.setContent {
            FFinderTheme(darkTheme = false) {
                actualPrimary = MaterialTheme.colorScheme.primary
                actualSecondary = MaterialTheme.colorScheme.secondary
                actualGradientTop = FFinderTheme.extendedColors.gradientTop
                actualGradientBottom = FFinderTheme.extendedColors.gradientBottom
            }
        }

        // Verify brand colors according to requirements
        assertEquals("Primary should be brand green #2E7D32", Color(0xFF2E7D32), actualPrimary)
        assertEquals("Secondary should be brand purple #6B4F8F", Color(0xFF6B4F8F), actualSecondary)
        assertEquals("Gradient top should be purple #6B4F8F", Color(0xFF6B4F8F), actualGradientTop)
        assertEquals("Gradient bottom should be green #2E7D32", Color(0xFF2E7D32), actualGradientBottom)
    }

    @Test
    fun `fFinderTheme should use correct brand colors for dark theme`() {
        var actualPrimary: Color? = null
        var actualSecondary: Color? = null
        var actualGradientTop: Color? = null
        var actualGradientBottom: Color? = null

        composeTestRule.setContent {
            FFinderTheme(darkTheme = true) {
                actualPrimary = MaterialTheme.colorScheme.primary
                actualSecondary = MaterialTheme.colorScheme.secondary
                actualGradientTop = FFinderTheme.extendedColors.gradientTop
                actualGradientBottom = FFinderTheme.extendedColors.gradientBottom
            }
        }

        // Verify dark theme colors
        assertEquals("Primary dark should be lighter green", Color(0xFF4CAF50), actualPrimary)
        assertEquals("Secondary dark should be lighter purple", Color(0xFF9C7BB8), actualSecondary)
        assertEquals("Gradient top dark should be darker purple", Color(0xFF4A3A5C), actualGradientTop)
        assertEquals("Gradient bottom dark should be darker green", Color(0xFF1B5E20), actualGradientBottom)
    }

    @Test
    fun `extended colors should be available through FFinderTheme`() {
        var extendedColors: FFinderExtendedColors? = null

        composeTestRule.setContent {
            FFinderTheme {
                extendedColors = FFinderTheme.extendedColors
            }
        }

        assertNotNull("Extended colors should be available", extendedColors)
        assertEquals("Warning color should be orange", Color(0xFFFF9800), extendedColors?.warning)
        assertEquals("Success color should be green", Color(0xFF4CAF50), extendedColors?.success)
        assertEquals("Info color should be blue", Color(0xFF2196F3), extendedColors?.info)
    }

    @Test
    fun `brand color constants should match requirements`() {
        // Test the color constants directly
        assertEquals("Primary green should be #2E7D32", Color(0xFF2E7D32), FFinderPrimary)
        assertEquals("Secondary purple should be #6B4F8F", Color(0xFF6B4F8F), FFinderSecondary)
        assertEquals("Gradient top should be purple", Color(0xFF6B4F8F), FFinderGradientTop)
        assertEquals("Gradient bottom should be green", Color(0xFF2E7D32), FFinderGradientBottom)
    }
}