package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Background gradient composable for the FFinder Home Screen.
 * 
 * Creates a vertical gradient from mint at the top to white at the bottom,
 * providing optimal contrast for dark logo and headline while maintaining
 * the fresh mint-white palette.
 * 
 * @param modifier Modifier to be applied to the background container
 * @param content The content to be displayed over the gradient background
 */
@Composable
fun BackgroundGradient(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val extendedColors = FFinderTheme.extendedColors
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientTop,    // Mint (top)
                        extendedColors.gradientBottom  // White (bottom)
                    ),
                    startY = 0f,          // mint at the very top
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun BackgroundGradientPreview() {
    FFinderTheme {
        BackgroundGradient {
            // Empty content for preview
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
fun BackgroundGradientDarkPreview() {
    FFinderTheme(darkTheme = true) {
        BackgroundGradient {
            // Empty content for preview
        }
    }
}