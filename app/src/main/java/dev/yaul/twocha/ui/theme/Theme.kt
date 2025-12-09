package dev.yaul.twocha.ui.theme

import android.app.Activity
import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 Expressive Theme System
 *
 * Developer-friendly theme with:
 * - Easy theme switching via ThemeStyle enum
 * - Multiple built-in themes (Cyber, Ocean, Violet, OLED, Light, Warm)
 * - Debug mode for inspecting colors
 * - Dynamic color support (Material You)
 * - Composition locals for easy access to design tokens
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
// THEME STATE HOLDER
// =============================================================================

/**
 * Holds the current theme state for easy access throughout the app
 */
class TwochaThemeState(
    initialThemeStyle: ThemeStyle = ThemeStyle.CYBER,
    initialDynamicColor: Boolean = false
) {
    var themeStyle by mutableStateOf(initialThemeStyle)
    var dynamicColor by mutableStateOf(initialDynamicColor)

    val isDark: Boolean
        get() = themeStyle.isDark()

    fun toggleDarkMode() {
        themeStyle = if (isDark) ThemeStyle.LIGHT else ThemeStyle.CYBER
    }

    fun setTheme(style: ThemeStyle) {
        themeStyle = style
    }
}

@Composable
fun rememberTwochaThemeState(
    initialThemeStyle: ThemeStyle = ThemeStyle.CYBER,
    initialDynamicColor: Boolean = false
): TwochaThemeState {
    return remember {
        TwochaThemeState(initialThemeStyle, initialDynamicColor)
    }
}

val LocalThemeState = staticCompositionLocalOf<TwochaThemeState> {
    error("No TwochaThemeState provided")
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

    // Create theme state
    val themeState = remember(themeStyle, dynamicColor) {
        TwochaThemeState(themeStyle, dynamicColor)
    }

    // Configure system bars
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Transparent system bars for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Set icon colors based on theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    // Provide all design tokens via CompositionLocals
    CompositionLocalProvider(
        LocalTwochaColors provides twochaColors,
        LocalThemeState provides themeState,
        LocalExpressiveShapes provides ExpressiveShapes(),
        LocalSpacing provides Spacing,
        LocalElevation provides Elevation,
        LocalRadius provides Radius,
        LocalIconSize provides IconSize,
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
// THEME ACCESSOR OBJECT
// =============================================================================

/**
 * Easy access to theme values from anywhere in the app
 *
 * Usage:
 * ```kotlin
 * val primary = TwochaTheme.colors.primary
 * val cardShape = TwochaTheme.shapes.card
 * val spacing = TwochaTheme.spacing.md
 * ```
 */
object TwochaThemeAccessor {
    val colors: TwochaColorPalette
        @Composable get() = LocalTwochaColors.current

    val shapes: ExpressiveShapes
        @Composable get() = LocalExpressiveShapes.current

    val spacing: Spacing
        @Composable get() = LocalSpacing.current

    val elevation: Elevation
        @Composable get() = LocalElevation.current

    val radius: Radius
        @Composable get() = LocalRadius.current

    val iconSize: IconSize
        @Composable get() = LocalIconSize.current

    val duration: Duration
        @Composable get() = LocalDuration.current

    val themeState: TwochaThemeState
        @Composable get() = LocalThemeState.current
}

// Convenient alias
val Theme: TwochaThemeAccessor = TwochaThemeAccessor

// =============================================================================
// PREVIEW THEMES
// =============================================================================

/**
 * Preview helper for displaying content in all theme variants
 */
@Composable
fun PreviewAllThemes(
    content: @Composable () -> Unit
) {
    ThemeStyle.entries.forEach { style ->
        TwochaTheme(themeStyle = style) {
            content()
        }
    }
}

/**
 * Preview helper for light/dark comparison
 */
@Composable
fun PreviewLightDark(
    content: @Composable () -> Unit
) {
    TwochaTheme(themeStyle = ThemeStyle.LIGHT) {
        content()
    }
    TwochaTheme(themeStyle = ThemeStyle.CYBER) {
        content()
    }
}
