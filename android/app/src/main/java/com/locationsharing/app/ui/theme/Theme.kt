package com.locationsharing.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// FFinder Brand Color Schemes
private val FFinderDarkColorScheme =
    darkColorScheme(
        primary = FFinderPrimaryDark,                // Lighter green for dark theme
        primaryContainer = FFinderPrimaryVariantDark, // Green variant for dark theme
        secondary = FFinderSecondaryDark,            // Lighter purple for dark theme
        secondaryContainer = FFinderSecondaryVariant, // Purple variant
        tertiary = FFinderAccent,                    // Light green accent
        background = FFinderBackgroundDark,
        surface = FFinderSurfaceDark,
        error = FFinderError,
        onPrimary = FFinderOnPrimaryDark,
        onSecondary = FFinderOnPrimaryDark,          // Dark text on light purple
        onTertiary = FFinderOnSecondary,
        onBackground = FFinderOnBackgroundDark,
        onSurface = FFinderOnSurfaceDark,
        onError = FFinderOnError,
        // Material 3 elevation and surface handling for dark theme
        surfaceVariant = Color(0xFF424242),          // Dark surface variant
        onSurfaceVariant = Color(0xFFBDBDBD),        // Text on dark surface variant
        outline = Color(0xFF616161),                 // Dark outline color
        outlineVariant = Color(0xFF424242),          // Dark outline variant
        // Elevation tonal colors for Material 3 dark theme
        surfaceTint = FFinderPrimaryDark,            // Primary color for surface tinting
        inverseSurface = Color(0xFFF5F5F5),          // Inverse surface for contrast
        inverseOnSurface = Color(0xFF2E2E2E),        // Text on inverse surface
        inversePrimary = Color(0xFF2E7D32),          // Inverse primary color
    )

private val FFinderLightColorScheme =
    lightColorScheme(
        primary = FFinderPrimary,                    // #2E7D32 (brand green) - requirement 10.1
        secondary = FFinderSecondary,                // #6B4F8F (brand purple) - requirement 10.2
        surface = FFinderSurface,                    // White surface - requirement 10.3
        onSurface = Color(0xFF212121),               // Dark text on surface
        background = FFinderBackground,              // #F1F1F1 background - requirement 10.4
        primaryContainer = FFinderPrimaryVariant,    // Darker green variant
        secondaryContainer = FFinderSecondaryVariant, // Darker purple variant
        tertiary = FFinderAccent,                    // Light green accent
        error = FFinderError,
        onPrimary = Color.White,                     // White text on green primary
        onSecondary = Color.White,                   // White text on purple secondary
        onTertiary = FFinderOnSecondary,
        onBackground = Color(0xFF212121),            // Dark text on light background
        onError = FFinderOnError,
        // Material 3 elevation and surface handling
        surfaceVariant = Color(0xFFF5F5F5),          // Light surface variant
        onSurfaceVariant = Color(0xFF757575),        // Text on surface variant
        outline = Color(0xFFBDBDBD),                 // Outline color
        outlineVariant = Color(0xFFE0E0E0),          // Light outline variant
        // Elevation tonal colors for Material 3
        surfaceTint = FFinderPrimary,                // Primary color for surface tinting
        inverseSurface = Color(0xFF2E2E2E),          // Inverse surface for contrast
        inverseOnSurface = Color(0xFFF5F5F5),        // Text on inverse surface
        inversePrimary = Color(0xFF81C784),          // Inverse primary color
    )

// Legacy color schemes for compatibility
private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
    )

/**
 * Extended color palette for FFinder brand
 */
data class FFinderExtendedColors(
    val warning: androidx.compose.ui.graphics.Color,
    val success: androidx.compose.ui.graphics.Color,
    val info: androidx.compose.ui.graphics.Color,
    val pulseActive: androidx.compose.ui.graphics.Color,
    val loadingOverlay: androidx.compose.ui.graphics.Color,
    val transitionOverlay: androidx.compose.ui.graphics.Color,
    // Gradient colors for Home Screen background
    val gradientTop: androidx.compose.ui.graphics.Color,
    val gradientBottom: androidx.compose.ui.graphics.Color,
)

private val LocalFFinderExtendedColors =
    staticCompositionLocalOf {
        FFinderExtendedColors(
            warning = FFinderWarning,
            success = FFinderSuccess,
            info = FFinderInfo,
            pulseActive = FFinderPulseActive,
            loadingOverlay = FFinderLoadingOverlay,
            transitionOverlay = FFinderTransitionOverlay,
            gradientTop = FFinderGradientTop,
            gradientBottom = FFinderGradientBottom,
        )
    }

/**
 * FFinder theme with brand colors, typography, and animation specifications
 */
@Composable
fun FFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to maintain brand consistency
    useLegacyColors: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            useLegacyColors -> {
                when {
                    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        val context = LocalContext.current
                        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    }
                    darkTheme -> DarkColorScheme
                    else -> LightColorScheme
                }
            }
            darkTheme -> FFinderDarkColorScheme
            else -> FFinderLightColorScheme
        }

    val extendedColors =
        FFinderExtendedColors(
            warning = FFinderWarning,
            success = FFinderSuccess,
            info = FFinderInfo,
            pulseActive = FFinderPulseActive,
            loadingOverlay = FFinderLoadingOverlay,
            transitionOverlay = FFinderTransitionOverlay,
            gradientTop = if (darkTheme) FFinderGradientTopDark else FFinderGradientTop,
            gradientBottom = if (darkTheme) FFinderGradientBottomDark else FFinderGradientBottom,
        )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalFFinderExtendedColors provides extendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FFinderTypography,
            content = content,
        )
    }
}

/**
 * Legacy theme for backward compatibility
 */
@Composable
fun FFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    FFinderTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        useLegacyColors = true,
        content = content,
    )
}

/**
 * Access to extended FFinder colors
 */
object FFinderTheme {
    val extendedColors: FFinderExtendedColors
        @Composable
        get() = LocalFFinderExtendedColors.current
}
