package com.locationsharing.app.ui.map.crossdevice

import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Configuration data for testing MapScreen across different device types and capabilities.
 */
object DeviceTestConfiguration {

    /**
     * Device categories for testing
     */
    enum class DeviceCategory {
        LOW_END,
        MID_RANGE,
        HIGH_END,
        TABLET,
        FOLDABLE
    }

    /**
     * Screen size categories based on Android guidelines
     */
    enum class ScreenSizeCategory {
        COMPACT,
        MEDIUM,
        EXPANDED
    }

    /**
     * Density categories for testing
     */
    enum class DensityCategory {
        LDPI,    // ~120dpi
        MDPI,    // ~160dpi
        HDPI,    // ~240dpi
        XHDPI,   // ~320dpi
        XXHDPI,  // ~480dpi
        XXXHDPI  // ~640dpi
    }

    /**
     * Android API level categories for testing
     */
    enum class ApiLevelCategory {
        API_24_25,  // Android 7.0-7.1
        API_26_27,  // Android 8.0-8.1
        API_28_29,  // Android 9-10
        API_30_31,  // Android 11-12
        API_32_33,  // Android 12L-13
        API_34_PLUS // Android 14+
    }

    /**
     * Test device configurations
     */
    data class DeviceConfig(
        val name: String,
        val category: DeviceCategory,
        val screenWidthDp: Int,
        val screenHeightDp: Int,
        val densityCategory: DensityCategory,
        val densityDpi: Int,
        val apiLevelCategory: ApiLevelCategory,
        val minApiLevel: Int,
        val maxApiLevel: Int,
        val hasLowMemory: Boolean = false,
        val hasSlowCpu: Boolean = false,
        val supportsHardwareAcceleration: Boolean = true
    )

    /**
     * Predefined device configurations for testing
     */
    val testDeviceConfigs = listOf(
        // Low-end phones
        DeviceConfig(
            name = "Low-end Phone (Small)",
            category = DeviceCategory.LOW_END,
            screenWidthDp = 320,
            screenHeightDp = 480,
            densityCategory = DensityCategory.MDPI,
            densityDpi = 160,
            apiLevelCategory = ApiLevelCategory.API_24_25,
            minApiLevel = 24,
            maxApiLevel = 25,
            hasLowMemory = true,
            hasSlowCpu = true,
            supportsHardwareAcceleration = false
        ),
        
        DeviceConfig(
            name = "Low-end Phone (Medium)",
            category = DeviceCategory.LOW_END,
            screenWidthDp = 360,
            screenHeightDp = 640,
            densityCategory = DensityCategory.HDPI,
            densityDpi = 240,
            apiLevelCategory = ApiLevelCategory.API_26_27,
            minApiLevel = 26,
            maxApiLevel = 27,
            hasLowMemory = true,
            hasSlowCpu = true
        ),

        // Mid-range phones
        DeviceConfig(
            name = "Mid-range Phone",
            category = DeviceCategory.MID_RANGE,
            screenWidthDp = 411,
            screenHeightDp = 731,
            densityCategory = DensityCategory.XHDPI,
            densityDpi = 320,
            apiLevelCategory = ApiLevelCategory.API_28_29,
            minApiLevel = 28,
            maxApiLevel = 29
        ),

        DeviceConfig(
            name = "Mid-range Phone (Large)",
            category = DeviceCategory.MID_RANGE,
            screenWidthDp = 428,
            screenHeightDp = 926,
            densityCategory = DensityCategory.XXHDPI,
            densityDpi = 480,
            apiLevelCategory = ApiLevelCategory.API_30_31,
            minApiLevel = 30,
            maxApiLevel = 31
        ),

        // High-end phones
        DeviceConfig(
            name = "High-end Phone",
            category = DeviceCategory.HIGH_END,
            screenWidthDp = 393,
            screenHeightDp = 851,
            densityCategory = DensityCategory.XXXHDPI,
            densityDpi = 640,
            apiLevelCategory = ApiLevelCategory.API_32_33,
            minApiLevel = 32,
            maxApiLevel = 33
        ),

        DeviceConfig(
            name = "High-end Phone (Premium)",
            category = DeviceCategory.HIGH_END,
            screenWidthDp = 412,
            screenHeightDp = 915,
            densityCategory = DensityCategory.XXXHDPI,
            densityDpi = 640,
            apiLevelCategory = ApiLevelCategory.API_34_PLUS,
            minApiLevel = 34,
            maxApiLevel = 99
        ),

        // Tablets
        DeviceConfig(
            name = "Small Tablet",
            category = DeviceCategory.TABLET,
            screenWidthDp = 600,
            screenHeightDp = 800,
            densityCategory = DensityCategory.MDPI,
            densityDpi = 160,
            apiLevelCategory = ApiLevelCategory.API_28_29,
            minApiLevel = 28,
            maxApiLevel = 29
        ),

        DeviceConfig(
            name = "Large Tablet",
            category = DeviceCategory.TABLET,
            screenWidthDp = 1024,
            screenHeightDp = 768,
            densityCategory = DensityCategory.HDPI,
            densityDpi = 240,
            apiLevelCategory = ApiLevelCategory.API_30_31,
            minApiLevel = 30,
            maxApiLevel = 31
        ),

        DeviceConfig(
            name = "Premium Tablet",
            category = DeviceCategory.TABLET,
            screenWidthDp = 1200,
            screenHeightDp = 1600,
            densityCategory = DensityCategory.XHDPI,
            densityDpi = 320,
            apiLevelCategory = ApiLevelCategory.API_32_33,
            minApiLevel = 32,
            maxApiLevel = 33
        ),

        // Foldable devices
        DeviceConfig(
            name = "Foldable (Closed)",
            category = DeviceCategory.FOLDABLE,
            screenWidthDp = 374,
            screenHeightDp = 834,
            densityCategory = DensityCategory.XXHDPI,
            densityDpi = 480,
            apiLevelCategory = ApiLevelCategory.API_30_31,
            minApiLevel = 30,
            maxApiLevel = 31
        ),

        DeviceConfig(
            name = "Foldable (Open)",
            category = DeviceCategory.FOLDABLE,
            screenWidthDp = 768,
            screenHeightDp = 834,
            densityCategory = DensityCategory.XXHDPI,
            densityDpi = 480,
            apiLevelCategory = ApiLevelCategory.API_30_31,
            minApiLevel = 30,
            maxApiLevel = 31
        )
    )

    /**
     * Get screen size category based on width
     */
    fun getScreenSizeCategory(widthDp: Int): ScreenSizeCategory {
        return when {
            widthDp < 600 -> ScreenSizeCategory.COMPACT
            widthDp < 840 -> ScreenSizeCategory.MEDIUM
            else -> ScreenSizeCategory.EXPANDED
        }
    }

    /**
     * Get density from DPI value
     */
    fun getDensityFromDpi(dpi: Int): Density {
        return Density(dpi / 160f)
    }

    /**
     * Check if current API level matches category
     */
    fun isApiLevelInCategory(apiLevel: Int, category: ApiLevelCategory): Boolean {
        return when (category) {
            ApiLevelCategory.API_24_25 -> apiLevel in 24..25
            ApiLevelCategory.API_26_27 -> apiLevel in 26..27
            ApiLevelCategory.API_28_29 -> apiLevel in 28..29
            ApiLevelCategory.API_30_31 -> apiLevel in 30..31
            ApiLevelCategory.API_32_33 -> apiLevel in 32..33
            ApiLevelCategory.API_34_PLUS -> apiLevel >= 34
        }
    }

    /**
     * Get device configs for specific category
     */
    fun getConfigsForCategory(category: DeviceCategory): List<DeviceConfig> {
        return testDeviceConfigs.filter { it.category == category }
    }

    /**
     * Get device configs for current API level
     */
    fun getConfigsForCurrentApiLevel(): List<DeviceConfig> {
        val currentApiLevel = Build.VERSION.SDK_INT
        return testDeviceConfigs.filter { config ->
            currentApiLevel >= config.minApiLevel && currentApiLevel <= config.maxApiLevel
        }
    }

    /**
     * Get low-end device configs for performance testing
     */
    fun getLowEndConfigs(): List<DeviceConfig> {
        return testDeviceConfigs.filter { it.hasLowMemory || it.hasSlowCpu }
    }

    /**
     * Get high-end device configs for feature testing
     */
    fun getHighEndConfigs(): List<DeviceConfig> {
        return testDeviceConfigs.filter { 
            it.category == DeviceCategory.HIGH_END && 
            it.supportsHardwareAcceleration 
        }
    }

    /**
     * Performance expectations based on device category
     */
    data class PerformanceExpectations(
        val maxCompositionTimeMs: Long,
        val maxInteractionTimeMs: Long,
        val maxAnimationTimeMs: Long,
        val maxMemoryUsageMb: Int
    )

    /**
     * Get performance expectations for device category
     */
    fun getPerformanceExpectations(category: DeviceCategory): PerformanceExpectations {
        return when (category) {
            DeviceCategory.LOW_END -> PerformanceExpectations(
                maxCompositionTimeMs = 1500,
                maxInteractionTimeMs = 800,
                maxAnimationTimeMs = 500,
                maxMemoryUsageMb = 128
            )
            DeviceCategory.MID_RANGE -> PerformanceExpectations(
                maxCompositionTimeMs = 1000,
                maxInteractionTimeMs = 500,
                maxAnimationTimeMs = 350,
                maxMemoryUsageMb = 256
            )
            DeviceCategory.HIGH_END -> PerformanceExpectations(
                maxCompositionTimeMs = 500,
                maxInteractionTimeMs = 300,
                maxAnimationTimeMs = 200,
                maxMemoryUsageMb = 512
            )
            DeviceCategory.TABLET -> PerformanceExpectations(
                maxCompositionTimeMs = 800,
                maxInteractionTimeMs = 400,
                maxAnimationTimeMs = 300,
                maxMemoryUsageMb = 384
            )
            DeviceCategory.FOLDABLE -> PerformanceExpectations(
                maxCompositionTimeMs = 600,
                maxInteractionTimeMs = 350,
                maxAnimationTimeMs = 250,
                maxMemoryUsageMb = 384
            )
        }
    }

    /**
     * Accessibility requirements for different device types
     */
    data class AccessibilityRequirements(
        val minTouchTargetSizeDp: Int,
        val requiresHighContrast: Boolean,
        val supportsLargeFonts: Boolean,
        val requiresScreenReader: Boolean
    )

    /**
     * Get accessibility requirements for device category
     */
    fun getAccessibilityRequirements(category: DeviceCategory): AccessibilityRequirements {
        return when (category) {
            DeviceCategory.LOW_END -> AccessibilityRequirements(
                minTouchTargetSizeDp = 48,
                requiresHighContrast = true,
                supportsLargeFonts = true,
                requiresScreenReader = true
            )
            else -> AccessibilityRequirements(
                minTouchTargetSizeDp = 48,
                requiresHighContrast = false,
                supportsLargeFonts = true,
                requiresScreenReader = true
            )
        }
    }
}