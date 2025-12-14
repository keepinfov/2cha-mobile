package dev.yaul.twocha.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Material 3 Expressive Theme System
 *
 * Features:
 * - Easy theme switching via ThemeStyle enum
 * - Multiple built-in themes (Cyber, Ocean, Violet, OLED, Light, Warm)
 * - Dynamic color support (Material You on Android 12+)
 * - Automatic edge-to-edge system bars configuration
 */

// =============================================================================
// THEME CONFIGURATION
// =============================================================================

/**
 * Global theme configuration
 * Developers can modify these values to customize the app's appearance
 */
object ThemeConfig {
    // Set to true to enable debug overlay
    var debugMode: Boolean = false

    // Default animation speed multiplier (1.0 = normal, 0.5 = faster, 2.0 = slower)
    var animationScale: Float = 1.0f

    // Enable/disable spring animations (fallback to tween if false)
    var useSpringAnimations: Boolean = true
}

// =============================================================================
// COLOR SCHEME BUILDERS
// =============================================================================

private fun TwochaColorPalette.toColorScheme(): ColorScheme {
    return if (this == LightDefaultPalette || this == LightWarmPalette) {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest
        )
    } else {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest
        )
    }
}

// =============================================================================
// MAIN THEME COMPOSABLE
// =============================================================================

/**
 * Main theme composable with Material 3 Expressive styling
 *
 * @param themeStyle The theme style to use (CYBER, OCEAN, VIOLET, OLED, LIGHT, WARM)
 * @param dynamicColor Whether to use Material You dynamic colors (Android 12+)
 * @param content The content to display with this theme
 *
 * Usage:
 * ```kotlin
 * TwochaTheme(themeStyle = ThemeStyle.CYBER) {
 *     // Your app content
 * }
 * ```
 */
@Composable
fun TwochaTheme(
    themeStyle: ThemeStyle = ThemeStyle.CYBER,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = themeStyle.isDark()

    // Get color scheme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getColorPalette(themeStyle).toColorScheme()
    }

    // Get custom color palette for extended colors
    val twochaColors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Use default palette with dynamic colors mixed in
        if (isDark) DarkCyberPalette else LightDefaultPalette
    } else {
        getColorPalette(themeStyle)
    }

    // Configure system bars appearance
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.findActivity()?.window ?: return@SideEffect
            // Edge-to-edge is already configured in MainActivity
            // Just update the system bars appearance based on theme
            val insetsController = WindowInsetsControllerCompat(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    // Provide all design tokens via CompositionLocals
    CompositionLocalProvider(
        LocalTwochaColors provides twochaColors,
        LocalExpressiveShapes provides ExpressiveShapes(),
        LocalSpacing provides Spacing,
        LocalElevation provides Elevation,
        LocalRadius provides Radius,
        LocalIconSize provides IconSize,
        LocalTouchTargets provides TouchTargets,
        LocalDuration provides Duration
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ExpressiveTypography,
            shapes = TwochaShapes,
            content = content
        )
    }
}

/**
 * Legacy theme composable for backwards compatibility
 */
@Composable
fun TwochaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeStyle = if (darkTheme) ThemeStyle.CYBER else ThemeStyle.LIGHT
    TwochaTheme(
        themeStyle = themeStyle,
        dynamicColor = dynamicColor,
        content = content
    )
}

// =============================================================================
// HELPER EXTENSIONS
// =============================================================================

private fun View.findActivity(): Activity? = context.findActivity()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
