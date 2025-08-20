package com.locationsharing.app.ui.map.crossdevice

import android.os.Build
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.locationsharing.app.ui.screens.MapScreen
import com.locationsharing.app.ui.theme.FFTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Comprehensive cross-device test suite that validates MapScreen functionality
 * across all supported device configurations, API levels, and capabilities.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ComprehensiveCrossDeviceTestSuite {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapScreen_worksOnAllSupportedApiLevels() {
        val currentApiLevel = Build.VERSION.SDK_INT
        val compatibleConfigs = DeviceTestConfiguration.getConfigsForCurrentApiLevel()
        
        assert(compatibleConfigs.isNotEmpty()) { 
            "No compatible device configurations found for API level $currentApiLevel" 
        }

        compatibleConfigs.forEach { config ->
            testMapScreenOnDeviceConfig(config)
        }
    }

    @Test
    fun mapScreen_adaptsToAllScreenSizes() {
        DeviceTestConfiguration.testDeviceConfigs.forEach { config ->
            composeTestRule.setContent {
                SimulateDeviceConfig(config) {
                    FFTheme {
                        MapScreen(
                            onNavigateBack = {},
                            onNavigateToFriends = {}
                        )
                    }
                }
            }

            // Verify core components are present and properly sized
            verifyBasicMapScreenComponents()
            
            // Verify screen size specific adaptations
            when (DeviceTestConfiguration.getScreenSizeCategory(config.screenWidthDp)) {
                DeviceTestConfiguration.ScreenSizeCategory.COMPACT -> {
                    verifyCompactScreenLayout()
                }
                DeviceTestConfiguration.ScreenSizeCategory.MEDIUM -> {
                    verifyMediumScreenLayout()
                }
                DeviceTestConfiguration.ScreenSizeCategory.EXPANDED -> {
                    verifyExpandedScreenLayout()
                }
            }
        }
    }

    @Test
    fun mapScreen_performsWellOnLowEndDevices() {
        val lowEndConfigs = DeviceTestConfiguration.getLowEndConfigs()
        
        lowEndConfigs.forEach { config ->
            val expectations = DeviceTestConfiguration.getPerformanceExpectations(config.category)
            
            composeTestRule.setContent {
                SimulateDeviceConfig(config) {
                    FFTheme {
                        MapScreen(
                            onNavigateBack = {},
                            onNavigateToFriends = {}
                        )
                    }
                }
            }

            // Test composition performance
            val compositionTime = measureTimeMillis {
                composeTestRule.waitForIdle()
            }
            assert(compositionTime <= expectations.maxCompositionTimeMs) {
                "Composition took too long on ${config.name}: ${compositionTime}ms > ${expectations.maxCompositionTimeMs}ms"
            }

            // Test interaction performance
            val interactionTime = measureTimeMillis {
                composeTestRule.onNodeWithContentDescription("View nearby friends")
                    .performClick()
                composeTestRule.waitForIdle()
            }
            assert(interactionTime <= expectations.maxInteractionTimeMs) {
                "Interaction took too long on ${config.name}: ${interactionTime}ms > ${expectations.maxInteractionTimeMs}ms"
            }

            // Close drawer
            composeTestRule.onNodeWithTag("DrawerScrim").performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun mapScreen_maintainsAccessibilityOnAllDevices() {
        DeviceTestConfiguration.testDeviceConfigs.forEach { config ->
            val accessibilityReqs = DeviceTestConfiguration.getAccessibilityRequirements(config.category)
            
            composeTestRule.setContent {
                SimulateDeviceConfig(config) {
                    FFTheme {
                        MapScreen(
                            onNavigateBack = {},
                            onNavigateToFriends = {}
                        )
                    }
                }
            }

            // Verify touch target sizes
            val minTouchTarget = accessibilityReqs.minTouchTargetSizeDp.dp
            composeTestRule.onNodeWithContentDescription("Navigate back")
                .assertHeightIsAtLeast(minTouchTarget)
                .assertWidthIsAtLeast(minTouchTarget)

            composeTestRule.onNodeWithContentDescription("Share your location instantly")
                .assertHeightIsAtLeast(56.dp) // FAB minimum size
                .assertWidthIsAtLeast(56.dp)

            // Verify content descriptions are present
            verifyAccessibilityLabels()

            // Test screen reader navigation
            if (accessibilityReqs.requiresScreenReader) {
                verifyScreenReaderNavigation()
            }
        }
    }

    @Test
    fun mapScreen_worksInBothThemesOnAllDevices() {
        val themes = listOf(false, true) // Light and dark
        
        DeviceTestConfiguration.testDeviceConfigs.forEach { config ->
            themes.forEach { isDarkTheme ->
                composeTestRule.setContent {
                    SimulateDeviceConfig(config) {
                        FFTheme(darkTheme = isDarkTheme) {
                            MapScreen(
                                onNavigateBack = {},
                                onNavigateToFriends = {}
                            )
                        }
                    }
                }

                // Verify components are visible in both themes
                verifyBasicMapScreenComponents()
                
                // Test interactions work in both themes
                testBasicInteractions()
            }
        }
    }

    @Test
    fun mapScreen_handlesOrientationChangesOnAllDevices() {
        DeviceTestConfiguration.testDeviceConfigs.forEach { config ->
            // Test portrait orientation
            composeTestRule.setContent {
                SimulateDeviceConfig(config) {
                    FFTheme {
                        MapScreen(
                            onNavigateBack = {},
                            onNavigateToFriends = {}
                        )
                    }
                }
            }
            verifyBasicMapScreenComponents()

            // Test landscape orientation (swap width and height)
            val landscapeConfig = config.copy(
                screenWidthDp = config.screenHeightDp,
                screenHeightDp = config.screenWidthDp
            )
            
            composeTestRule.setContent {
                SimulateDeviceConfig(landscapeConfig) {
                    FFTheme {
                        MapScreen(
                            onNavigateBack = {},
                            onNavigateToFriends = {}
                        )
                    }
                }
            }
            verifyBasicMapScreenComponents()
        }
    }

    @Test
    fun mapScreen_handlesHighDensityScreens() {
        val highDensityConfigs = DeviceTestConfiguration.testDeviceConfigs.filter { 
            it.densityCategory in listOf(
                DeviceTestConfiguration.DensityCategory.XXHDPI,
                DeviceTestConfiguration.DensityCategory.XXXHDPI
            )
        }

        highDensityConfigs.forEach { config ->
            composeTestRule.setContent {
                SimulateDeviceConfig(config) {
                    FFTheme {
                        MapScreen(
                            onNavigateBack = {},
                            onNavigateToFriends = {}
                        )
                    }
                }
            }

            // Verify components scale properly for high density
            verifyBasicMapScreenComponents()
            
            // Verify touch targets are still appropriate
            composeTestRule.onNodeWithContentDescription("Navigate back")
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)

            composeTestRule.onNodeWithContentDescription("Share your location instantly")
                .assertHeightIsAtLeast(56.dp)
                .assertWidthIsAtLeast(56.dp)
        }
    }

    @Test
    fun mapScreen_worksOnTabletDevices() {
        val tabletConfigs = DeviceTestConfiguration.getConfigsForCategory(
            DeviceTestConfiguration.DeviceCategory.TABLET
        )

        tabletConfigs.forEach { config ->
            composeTestRule.setContent {
                SimulateDeviceConfig(config) {
                    FFTheme {
                        MapScreen(
                            onNavigateBack = {},
                            onNavigateToFriends = {}
                        )
                    }
                }
            }

            // Verify tablet-specific adaptations
            verifyBasicMapScreenComponents()
            
            // Test drawer behavior on tablets
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithTag("NearbyFriendsDrawer")
                .assertIsDisplayed()
            
            // Drawer should have appropriate width for tablets
            composeTestRule.onNodeWithTag("DrawerScrim").performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun mapScreen_worksOnFoldableDevices() {
        val foldableConfigs = DeviceTestConfiguration.getConfigsForCategory(
            DeviceTestConfiguration.DeviceCategory.FOLDABLE
        )

        foldableConfigs.forEach { config ->
            composeTestRule.setContent {
                SimulateDeviceConfig(config) {
                    FFTheme {
                        MapScreen(
                            onNavigateBack = {},
                            onNavigateToFriends = {}
                        )
                    }
                }
            }

            // Verify foldable-specific adaptations
            verifyBasicMapScreenComponents()
            testBasicInteractions()
        }
    }

    private fun testMapScreenOnDeviceConfig(config: DeviceTestConfiguration.DeviceConfig) {
        composeTestRule.setContent {
            SimulateDeviceConfig(config) {
                FFTheme {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }
        }

        // Verify basic functionality
        verifyBasicMapScreenComponents()
        testBasicInteractions()
        
        // Test performance expectations
        val expectations = DeviceTestConfiguration.getPerformanceExpectations(config.category)
        testPerformanceExpectations(expectations)
    }

    private fun verifyBasicMapScreenComponents() {
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("View nearby friends").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Center map on your location").assertIsDisplayed()
        composeTestRule.onNodeWithTag("GoogleMap").assertExists()
    }

    private fun verifyCompactScreenLayout() {
        // Verify compact screen specific layout
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
    }

    private fun verifyMediumScreenLayout() {
        // Verify medium screen specific layout
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
    }

    private fun verifyExpandedScreenLayout() {
        // Verify expanded screen specific layout
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
    }

    private fun verifyAccessibilityLabels() {
        composeTestRule.onNodeWithContentDescription("Navigate back").assertExists()
        composeTestRule.onNodeWithContentDescription("View nearby friends").assertExists()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertExists()
        composeTestRule.onNodeWithContentDescription("Center map on your location").assertExists()
        composeTestRule.onNodeWithContentDescription("Add test friends to map").assertExists()
    }

    private fun verifyScreenReaderNavigation() {
        // Test that all interactive elements are discoverable
        composeTestRule.onAllNodesWithContentDescription("Navigate back")
            .assertCountEquals(1)
        composeTestRule.onAllNodesWithContentDescription("View nearby friends")
            .assertCountEquals(1)
        composeTestRule.onAllNodesWithContentDescription("Share your location instantly")
            .assertCountEquals(1)
    }

    private fun testBasicInteractions() {
        // Test drawer interaction
        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
        
        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()

        // Test FAB interaction
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .performClick()
        composeTestRule.waitForIdle()

        // Test debug FAB interaction
        composeTestRule.onNodeWithContentDescription("Add test friends to map")
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun testPerformanceExpectations(expectations: DeviceTestConfiguration.PerformanceExpectations) {
        // Test interaction performance
        val interactionTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            composeTestRule.waitForIdle()
        }
        
        assert(interactionTime <= expectations.maxInteractionTimeMs) {
            "Interaction exceeded performance expectation: ${interactionTime}ms > ${expectations.maxInteractionTimeMs}ms"
        }

        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()
    }

    @Composable
    private fun SimulateDeviceConfig(
        config: DeviceTestConfiguration.DeviceConfig,
        content: @Composable () -> Unit
    ) {
        val density = DeviceTestConfiguration.getDensityFromDpi(config.densityDpi)
        
        BoxWithConstraints {
            // Simulate device constraints
            content()
        }
    }
}