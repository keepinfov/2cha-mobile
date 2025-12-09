package dev.yaul.twocha.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material 3 Expression Color System
 *
 * Vibrant, expressive colors with multiple theme variants.
 * Developer-friendly: Easy to add new themes.
 */

// =============================================================================
// BASE COLORS - Shared across themes
// =============================================================================

// Status colors (consistent across all themes)
val StatusConnected = Color(0xFF34D399)      // Emerald green
val StatusConnecting = Color(0xFFFBBF24)     // Amber
val StatusDisconnected = Color(0xFF9CA3AF)   // Gray
val StatusError = Color(0xFFF87171)          // Red

// Gradient colors for expressive effects
val GradientCyan = Color(0xFF22D3EE)
val GradientBlue = Color(0xFF3B82F6)
val GradientPurple = Color(0xFF8B5CF6)
val GradientPink = Color(0xFFEC4899)
val GradientGreen = Color(0xFF10B981)

// =============================================================================
// THEME COLOR PALETTES
// =============================================================================

/**
 * Color palette interface for easy theme creation
 */
@Immutable
data class TwochaColorPalette(
    // Primary
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,

    // Secondary
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    // Tertiary
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    // Error
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,

    // Background & Surface
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,

    // Surface containers
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,

    // Outline
    val outline: Color,
    val outlineVariant: Color,

    // Inverse
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,

    // Scrim
    val scrim: Color,

    // Extended colors for app-specific use
    val success: Color = StatusConnected,
    val warning: Color = StatusConnecting,
    val info: Color = primary,

    // Gradient accent
    val gradientStart: Color = primary,
    val gradientEnd: Color = secondary
)

// =============================================================================
// DARK THEME - Default (Cyber/Tech)
// =============================================================================

val DarkCyberPalette = TwochaColorPalette(
    // Primary - Cyan blue
    primary = Color(0xFF58A6FF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004880),
    onPrimaryContainer = Color(0xFFD1E4FF),

    // Secondary - Green
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF005234),
    onSecondaryContainer = Color(0xFF9EF6C4),

    // Tertiary - Amber
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF422D00),
    tertiaryContainer = Color(0xFF5F4300),
    onTertiaryContainer = Color(0xFFFFDF9E),

    // Error
    error = Color(0xFFF87171),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    // Background & Surface - Deep dark with blue tint
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFFA3ADB8),

    // Surface containers
    surfaceContainerLowest = Color(0xFF080B0E),
    surfaceContainerLow = Color(0xFF0D1117),
    surfaceContainer = Color(0xFF161B22),
    surfaceContainerHigh = Color(0xFF1C2128),
    surfaceContainerHighest = Color(0xFF282E36),

    // Outline
    outline = Color(0xFF3D444D),
    outlineVariant = Color(0xFF30363D),

    // Inverse
    inverseSurface = Color(0xFFE6EDF3),
    inverseOnSurface = Color(0xFF161B22),
    inversePrimary = Color(0xFF0969DA),

    // Scrim
    scrim = Color(0xFF000000),

    // Gradient
    gradientStart = Color(0xFF58A6FF),
    gradientEnd = Color(0xFF34D399)
)

// =============================================================================
// DARK THEME - Ocean
// =============================================================================

val DarkOceanPalette = TwochaColorPalette(
    // Primary - Teal
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF6FF7E2),

    // Secondary - Blue
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF003062),
    secondaryContainer = Color(0xFF00468A),
    onSecondaryContainer = Color(0xFFD1E4FF),

    // Tertiary - Cyan
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF003641),
    tertiaryContainer = Color(0xFF004E5E),
    onTertiaryContainer = Color(0xFF97F0FF),

    // Error
    error = Color(0xFFF87171),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    // Background - Deep ocean
    background = Color(0xFF0A1628),
    onBackground = Color(0xFFE0F2FE),
    surface = Color(0xFF0F1D32),
    onSurface = Color(0xFFE0F2FE),
    surfaceVariant = Color(0xFF1A2942),
    onSurfaceVariant = Color(0xFF94A3B8),

    // Surface containers
    surfaceContainerLowest = Color(0xFF060E1A),
    surfaceContainerLow = Color(0xFF0A1628),
    surfaceContainer = Color(0xFF0F1D32),
    surfaceContainerHigh = Color(0xFF15253D),
    surfaceContainerHighest = Color(0xFF1E3048),

    // Outline
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),

    // Inverse
    inverseSurface = Color(0xFFE0F2FE),
    inverseOnSurface = Color(0xFF0F1D32),
    inversePrimary = Color(0xFF0D9488),

    scrim = Color(0xFF000000),
    gradientStart = Color(0xFF2DD4BF),
    gradientEnd = Color(0xFF22D3EE)
)

// =============================================================================
// DARK THEME - Purple/Violet
// =============================================================================

val DarkVioletPalette = TwochaColorPalette(
    // Primary - Violet
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),

    // Secondary - Pink
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF4A1942),
    secondaryContainer = Color(0xFF652D5A),
    onSecondaryContainer = Color(0xFFFFD8EC),

    // Tertiary - Blue
    tertiary = Color(0xFF818CF8),
    onTertiary = Color(0xFF252F72),
    tertiaryContainer = Color(0xFF3C468A),
    onTertiaryContainer = Color(0xFFDFE0FF),

    // Error
    error = Color(0xFFF87171),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    // Background - Deep purple
    background = Color(0xFF0F0A1A),
    onBackground = Color(0xFFF3E8FF),
    surface = Color(0xFF1A1225),
    onSurface = Color(0xFFF3E8FF),
    surfaceVariant = Color(0xFF251B32),
    onSurfaceVariant = Color(0xFFA78BDA),

    // Surface containers
    surfaceContainerLowest = Color(0xFF080612),
    surfaceContainerLow = Color(0xFF0F0A1A),
    surfaceContainer = Color(0xFF1A1225),
    surfaceContainerHigh = Color(0xFF211830),
    surfaceContainerHighest = Color(0xFF2A1F3B),

    // Outline
    outline = Color(0xFF433566),
    outlineVariant = Color(0xFF322648),

    // Inverse
    inverseSurface = Color(0xFFF3E8FF),
    inverseOnSurface = Color(0xFF1A1225),
    inversePrimary = Color(0xFF6D28D9),

    scrim = Color(0xFF000000),
    gradientStart = Color(0xFFA78BFA),
    gradientEnd = Color(0xFFF472B6)
)

// =============================================================================
// LIGHT THEME - Default
// =============================================================================

val LightDefaultPalette = TwochaColorPalette(
    // Primary - Blue
    primary = Color(0xFF0969DA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3D),

    // Secondary - Green
    secondary = Color(0xFF059669),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF002114),

    // Tertiary - Amber
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF331900),

    // Error
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF410002),

    // Background & Surface
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1E293B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),

    // Surface containers
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),

    // Outline
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),

    // Inverse
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF93C5FD),

    scrim = Color(0xFF000000),
    gradientStart = Color(0xFF0969DA),
    gradientEnd = Color(0xFF059669)
)

// =============================================================================
// LIGHT THEME - Warm
// =============================================================================

val LightWarmPalette = TwochaColorPalette(
    // Primary - Orange
    primary = Color(0xFFEA580C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF431407),

    // Secondary - Rose
    secondary = Color(0xFFE11D48),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF4C0519),

    // Tertiary - Amber
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),

    // Error
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF410002),

    // Background & Surface - Warm white
    background = Color(0xFFFFFBEB),
    onBackground = Color(0xFF292524),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF292524),
    surfaceVariant = Color(0xFFFEF3C7),
    onSurfaceVariant = Color(0xFF57534E),

    // Surface containers
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFBEB),
    surfaceContainer = Color(0xFFFEF3C7),
    surfaceContainerHigh = Color(0xFFFDE68A),
    surfaceContainerHighest = Color(0xFFFCD34D),

    // Outline
    outline = Color(0xFFD6D3D1),
    outlineVariant = Color(0xFFE7E5E4),

    // Inverse
    inverseSurface = Color(0xFF292524),
    inverseOnSurface = Color(0xFFFFFBEB),
    inversePrimary = Color(0xFFFDBA74),

    scrim = Color(0xFF000000),
    gradientStart = Color(0xFFEA580C),
    gradientEnd = Color(0xFFE11D48)
)

// =============================================================================
// PURE DARK (OLED)
// =============================================================================

val DarkOledPalette = DarkCyberPalette.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF1A1A1A)
)

// =============================================================================
// THEME ENUM FOR EASY SELECTION
// =============================================================================

enum class ThemeStyle {
    CYBER,      // Default dark cyber theme
    OCEAN,      // Ocean blue theme
    VIOLET,     // Purple/violet theme
    OLED,       // Pure black OLED theme
    LIGHT,      // Default light theme
    WARM        // Warm light theme
}

fun getColorPalette(style: ThemeStyle): TwochaColorPalette {
    return when (style) {
        ThemeStyle.CYBER -> DarkCyberPalette
        ThemeStyle.OCEAN -> DarkOceanPalette
        ThemeStyle.VIOLET -> DarkVioletPalette
        ThemeStyle.OLED -> DarkOledPalette
        ThemeStyle.LIGHT -> LightDefaultPalette
        ThemeStyle.WARM -> LightWarmPalette
    }
}

fun ThemeStyle.isDark(): Boolean = this != ThemeStyle.LIGHT && this != ThemeStyle.WARM

// =============================================================================
// COMPOSITION LOCAL
// =============================================================================

val LocalTwochaColors = staticCompositionLocalOf { DarkCyberPalette }

// =============================================================================
// LEGACY COLOR REFERENCES (For backwards compatibility)
// =============================================================================

// Primary colors
val Primary = DarkCyberPalette.primary
val OnPrimary = DarkCyberPalette.onPrimary
val PrimaryContainer = DarkCyberPalette.primaryContainer
val OnPrimaryContainer = DarkCyberPalette.onPrimaryContainer

// Secondary colors
val Secondary = DarkCyberPalette.secondary
val OnSecondary = DarkCyberPalette.onSecondary
val SecondaryContainer = DarkCyberPalette.secondaryContainer
val OnSecondaryContainer = DarkCyberPalette.onSecondaryContainer

// Tertiary colors
val Tertiary = DarkCyberPalette.tertiary
val OnTertiary = DarkCyberPalette.onTertiary
val TertiaryContainer = DarkCyberPalette.tertiaryContainer
val OnTertiaryContainer = DarkCyberPalette.onTertiaryContainer

// Error colors
val Error = DarkCyberPalette.error
val OnError = DarkCyberPalette.onError
val ErrorContainer = DarkCyberPalette.errorContainer
val OnErrorContainer = DarkCyberPalette.onErrorContainer

// Background/Surface
val Background = DarkCyberPalette.background
val OnBackground = DarkCyberPalette.onBackground
val Surface = DarkCyberPalette.surface
val OnSurface = DarkCyberPalette.onSurface
val SurfaceVariant = DarkCyberPalette.surfaceVariant
val OnSurfaceVariant = DarkCyberPalette.onSurfaceVariant

// Surface containers
val SurfaceContainerLowest = DarkCyberPalette.surfaceContainerLowest
val SurfaceContainerLow = DarkCyberPalette.surfaceContainerLow
val SurfaceContainer = DarkCyberPalette.surfaceContainer
val SurfaceContainerHigh = DarkCyberPalette.surfaceContainerHigh
val SurfaceContainerHighest = DarkCyberPalette.surfaceContainerHighest

// Outline
val Outline = DarkCyberPalette.outline
val OutlineVariant = DarkCyberPalette.outlineVariant

// Inverse
val InverseSurface = DarkCyberPalette.inverseSurface
val InverseOnSurface = DarkCyberPalette.inverseOnSurface
val InversePrimary = DarkCyberPalette.inversePrimary

// Scrim
val Scrim = DarkCyberPalette.scrim

// Gradients
val GradientStart = DarkCyberPalette.gradientStart
val GradientMiddle = DarkCyberPalette.secondary
val GradientEnd = DarkCyberPalette.gradientEnd

// Light theme legacy
val md_theme_light_primary = LightDefaultPalette.primary
val md_theme_light_onPrimary = LightDefaultPalette.onPrimary
val md_theme_light_primaryContainer = LightDefaultPalette.primaryContainer
val md_theme_light_onPrimaryContainer = LightDefaultPalette.onPrimaryContainer
val md_theme_light_secondary = LightDefaultPalette.secondary
val md_theme_light_onSecondary = LightDefaultPalette.onSecondary
val md_theme_light_secondaryContainer = LightDefaultPalette.secondaryContainer
val md_theme_light_onSecondaryContainer = LightDefaultPalette.onSecondaryContainer
val md_theme_light_tertiary = LightDefaultPalette.tertiary
val md_theme_light_onTertiary = LightDefaultPalette.onTertiary
val md_theme_light_tertiaryContainer = LightDefaultPalette.tertiaryContainer
val md_theme_light_onTertiaryContainer = LightDefaultPalette.onTertiaryContainer
val md_theme_light_error = LightDefaultPalette.error
val md_theme_light_errorContainer = LightDefaultPalette.errorContainer
val md_theme_light_onError = LightDefaultPalette.onError
val md_theme_light_onErrorContainer = LightDefaultPalette.onErrorContainer
val md_theme_light_background = LightDefaultPalette.background
val md_theme_light_onBackground = LightDefaultPalette.onBackground
val md_theme_light_surface = LightDefaultPalette.surface
val md_theme_light_onSurface = LightDefaultPalette.onSurface
val md_theme_light_surfaceVariant = LightDefaultPalette.surfaceVariant
val md_theme_light_onSurfaceVariant = LightDefaultPalette.onSurfaceVariant
val md_theme_light_outline = LightDefaultPalette.outline
val md_theme_light_inverseOnSurface = LightDefaultPalette.inverseOnSurface
val md_theme_light_inverseSurface = LightDefaultPalette.inverseSurface
val md_theme_light_inversePrimary = LightDefaultPalette.inversePrimary
