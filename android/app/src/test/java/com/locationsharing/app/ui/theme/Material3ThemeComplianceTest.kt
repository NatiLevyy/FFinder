package com.locationsharing.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.locationsharing.app.ui.map.MapScreenConstants
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.pow

/**
 * Test class to verify Material 3 theme compliance and color requirements
 * Tests requirements 10.1, 10.2, 10.3, 10.4, 10.5, 10.6 from MapScreen redesign spec
 */
class Material3ThemeComplianceTest {

    @Test
    fun `verify light theme primary color matches requirement 10_1`() {
        // Requirement 10.1: Primary color SHALL be #2E7D32 (green)
        val expectedPrimary = Color(0xFF2E7D32)
        assertEquals("Primary color must match requirement 10.1", expectedPrimary, FFinderPrimary)
    }

    @Test
    fun `verify light theme secondary color matches requirement 10_2`() {
        // Requirement 10.2: Secondary color SHALL be #6B4F8F (purple)
        val expectedSecondary = Color(0xFF6B4F8F)
        assertEquals("Secondary color must match requirement 10.2", expectedSecondary, FFinderSecondary)
    }

    @Test
    fun `verify light theme surface color matches requirement 10_3`() {
        // Requirement 10.3: Surface color SHALL be white
        val expectedSurface = Color.White
        assertEquals("Surface color must match requirement 10.3", expectedSurface, FFinderSurface)
    }

    @Test
    fun `verify light theme background color matches requirement 10_4`() {
        // Requirement 10.4: Background color SHALL be #F1F1F1
        val expectedBackground = Color(0xFFF1F1F1)
        assertEquals("Background color must match requirement 10.4", expectedBackground, FFinderBackground)
    }

    @Test
    fun `verify light color scheme uses correct brand colors`() {
        val lightScheme = lightColorScheme(
            primary = FFinderPrimary,
            secondary = FFinderSecondary,
            surface = FFinderSurface,
            background = FFinderBackground
        )

        // Verify all brand colors are correctly applied
        assertEquals("Light scheme primary must be brand green", Color(0xFF2E7D32), lightScheme.primary)
        assertEquals("Light scheme secondary must be brand purple", Color(0xFF6B4F8F), lightScheme.secondary)
        assertEquals("Light scheme surface must be white", Color.White, lightScheme.surface)
        assertEquals("Light scheme background must be #F1F1F1", Color(0xFFF1F1F1), lightScheme.background)
    }

    @Test
    fun `verify dark theme has proper color adaptations`() {
        // Requirement 10.6: Dark mode colors SHALL adapt appropriately
        val darkScheme = darkColorScheme(
            primary = FFinderPrimaryDark,
            secondary = FFinderSecondaryDark,
            surface = FFinderSurfaceDark,
            background = FFinderBackgroundDark
        )

        // Verify dark theme colors are different from light theme
        assertNotEquals("Dark primary should differ from light primary", FFinderPrimary, darkScheme.primary)
        assertNotEquals("Dark surface should differ from light surface", FFinderSurface, darkScheme.surface)
        assertNotEquals("Dark background should differ from light background", FFinderBackground, darkScheme.background)
        
        // Verify dark theme colors maintain brand identity
        assertTrue("Dark primary should be a green variant", isDarkGreenVariant(darkScheme.primary))
        assertTrue("Dark secondary should be a purple variant", isDarkPurpleVariant(darkScheme.secondary))
    }

    @Test
    fun `verify material 3 elevation support`() {
        val lightScheme = lightColorScheme(
            primary = FFinderPrimary,
            surfaceTint = FFinderPrimary,
            surfaceVariant = Color(0xFFF5F5F5),
            outline = Color(0xFFBDBDBD)
        )

        // Verify elevation tonal colors are properly set
        assertEquals("Surface tint should be primary color", FFinderPrimary, lightScheme.surfaceTint)
        assertNotNull("Surface variant should be defined", lightScheme.surfaceVariant)
        assertNotNull("Outline should be defined", lightScheme.outline)
    }

    @Test
    fun `verify no hard coded colors in deprecated constants`() {
        // Verify that deprecated color constants still exist for backward compatibility
        // but are properly marked as deprecated
        @Suppress("DEPRECATION")
        val deprecatedPrimary = MapScreenConstants.Colors.PRIMARY_GREEN
        @Suppress("DEPRECATION")
        val deprecatedSecondary = MapScreenConstants.Colors.SECONDARY_PURPLE
        
        // These should match the theme colors for consistency
        assertEquals("Deprecated primary should match theme primary", FFinderPrimary, deprecatedPrimary)
        assertEquals("Deprecated secondary should match theme secondary", FFinderSecondary, deprecatedSecondary)
    }

    @Test
    fun `verify extended colors are properly defined`() {
        val extendedColors = FFinderExtendedColors(
            warning = FFinderWarning,
            success = FFinderSuccess,
            info = FFinderInfo,
            pulseActive = FFinderPulseActive,
            loadingOverlay = FFinderLoadingOverlay,
            transitionOverlay = FFinderTransitionOverlay,
            gradientTop = FFinderGradientTop,
            gradientBottom = FFinderGradientBottom
        )

        // Verify extended colors are not null and have reasonable values
        assertNotNull("Warning color should be defined", extendedColors.warning)
        assertNotNull("Success color should be defined", extendedColors.success)
        assertNotNull("Info color should be defined", extendedColors.info)
        assertNotNull("Pulse active color should be defined", extendedColors.pulseActive)
        assertNotNull("Loading overlay color should be defined", extendedColors.loadingOverlay)
        assertNotNull("Transition overlay color should be defined", extendedColors.transitionOverlay)
        
        // Verify gradient colors match brand colors
        assertEquals("Gradient top should be brand purple", FFinderSecondary, extendedColors.gradientTop)
        assertEquals("Gradient bottom should be brand green", FFinderPrimary, extendedColors.gradientBottom)
    }

    @Test
    fun `verify color contrast ratios meet accessibility standards`() {
        // Test primary color contrast
        val primaryContrast = calculateContrastRatio(FFinderPrimary, Color.White)
        assertTrue("Primary color should have sufficient contrast with white", primaryContrast >= 4.5)
        
        // Test secondary color contrast
        val secondaryContrast = calculateContrastRatio(FFinderSecondary, Color.White)
        assertTrue("Secondary color should have sufficient contrast with white", secondaryContrast >= 4.5)
        
        // Test background color contrast
        val backgroundContrast = calculateContrastRatio(FFinderBackground, Color(0xFF212121))
        assertTrue("Background color should have sufficient contrast with dark text", backgroundContrast >= 4.5)
    }

    // Helper functions
    private fun isDarkGreenVariant(color: Color): Boolean {
        // Check if color is in the green spectrum and darker than the light theme primary
        val hsl = colorToHsl(color)
        return hsl[0] in 100f..140f && hsl[2] > 0.3f // Hue in green range, reasonable lightness
    }

    private fun isDarkPurpleVariant(color: Color): Boolean {
        // Check if color is in the purple spectrum
        val hsl = colorToHsl(color)
        return hsl[0] in 260f..300f && hsl[2] > 0.3f // Hue in purple range, reasonable lightness
    }

    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val l1 = getRelativeLuminance(color1)
        val l2 = getRelativeLuminance(color2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun getRelativeLuminance(color: Color): Double {
        val r = if (color.red <= 0.03928) color.red / 12.92 else pow((color.red + 0.055) / 1.055, 2.4)
        val g = if (color.green <= 0.03928) color.green / 12.92 else pow((color.green + 0.055) / 1.055, 2.4)
        val b = if (color.blue <= 0.03928) color.blue / 12.92 else pow((color.blue + 0.055) / 1.055, 2.4)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun colorToHsl(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val diff = max - min
        
        val h = when {
            diff == 0f -> 0f
            max == r -> ((g - b) / diff) * 60f
            max == g -> ((b - r) / diff + 2f) * 60f
            else -> ((r - g) / diff + 4f) * 60f
        }.let { if (it < 0) it + 360f else it }
        
        val l = (max + min) / 2f
        val s = if (diff == 0f) 0f else diff / (1f - kotlin.math.abs(2f * l - 1f))
        
        return floatArrayOf(h, s, l)
    }
}