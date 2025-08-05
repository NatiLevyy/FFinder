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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// FFinder Brand Color Schemes
private val FFinderDarkColorScheme =
    darkColorScheme(
        primary = FFinderPrimaryDark,
        primaryContainer = FFinderPrimaryVariantDark,
        secondary = FFinderSecondaryDark,
        secondaryContainer = FFinderSecondaryVariant,
        tertiary = FFinderAccent,
        background = FFinderBackgroundDark,
        surface = FFinderSurfaceDark,
        error = FFinderError,
        onPrimary = FFinderOnPrimaryDark,
        onSecondary = FFinderOnSecondaryDark,
        onTertiary = FFinderOnSecondary,
        onBackground = FFinderOnBackgroundDark,
        onSurface = FFinderOnSurfaceDark,
        onError = FFinderOnError,
    )

private val FFinderLightColorScheme =
    lightColorScheme(
        primary = FFinderPrimary,
        primaryContainer = FFinderPrimaryVariant,
        secondary = FFinderSecondary,
        secondaryContainer = FFinderSecondaryVariant,
        tertiary = FFinderAccent,
        background = FFinderBackground,
        surface = FFinderSurface,
        error = FFinderError,
        onPrimary = FFinderOnPrimary,
        onSecondary = FFinderOnSecondary,
        onTertiary = FFinderOnSecondary,
        onBackground = FFinderOnBackground,
        onSurface = FFinderOnSurface,
        onError = FFinderOnError,
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
        )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
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
