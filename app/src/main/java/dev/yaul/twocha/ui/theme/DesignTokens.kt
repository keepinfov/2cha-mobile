package dev.yaul.twocha.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expression Design Tokens
 *
 * Central location for all design system values.
 * Developer-friendly: Change values here to update entire app.
 */

// =============================================================================
// SPACING TOKENS
// =============================================================================

object Spacing {
    val none: Dp = 0.dp
    val xxxs: Dp = 2.dp
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
    val huge: Dp = 64.dp
}

// =============================================================================
// ELEVATION TOKENS
// =============================================================================

object Elevation {
    val none: Dp = 0.dp
    val level1: Dp = 1.dp
    val level2: Dp = 3.dp
    val level3: Dp = 6.dp
    val level4: Dp = 8.dp
    val level5: Dp = 12.dp
}

// =============================================================================
// CORNER RADIUS TOKENS
// =============================================================================

object Radius {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 28.dp
    val full: Dp = 1000.dp  // For pill shapes
}

// =============================================================================
// SIZE TOKENS
// =============================================================================

object IconSize {
    val xs: Dp = 16.dp
    val sm: Dp = 20.dp
    val md: Dp = 24.dp
    val lg: Dp = 32.dp
    val xl: Dp = 40.dp
    val xxl: Dp = 48.dp
    val display: Dp = 64.dp
    val hero: Dp = 120.dp
}

object ButtonSize {
    val minHeight: Dp = 40.dp
    val standardHeight: Dp = 48.dp
    val largeHeight: Dp = 56.dp
    val fabSize: Dp = 56.dp
    val fabExtendedMinWidth: Dp = 80.dp
}

// =============================================================================
// ANIMATION DURATION TOKENS (in milliseconds)
// =============================================================================

object Duration {
    // Optimized for 60fps - reduced from standard Material durations
    const val instant: Int = 0
    const val ultraFast: Int = 50
    const val fast: Int = 100
    const val medium: Int = 200
    const val standard: Int = 300
    const val slow: Int = 400
    const val slower: Int = 500

    // Specific use cases
    const val colorTransition: Int = 150
    const val scaleTransition: Int = 200
    const val expandCollapse: Int = 250
    const val pageTransition: Int = 300
    const val complexAnimation: Int = 400
}

// =============================================================================
// SPRING PHYSICS TOKENS
// =============================================================================

object SpringPhysics {
    // Snappy - for quick interactions
    const val snappyStiffness: Float = 400f
    const val snappyDamping: Float = 0.75f

    // Responsive - for standard UI responses
    const val responsiveStiffness: Float = 300f
    const val responsiveDamping: Float = 0.85f

    // Gentle - for smooth, natural movement
    const val gentleStiffness: Float = 200f
    const val gentleDamping: Float = 0.9f

    // Bouncy - for playful interactions
    const val bouncyStiffness: Float = 350f
    const val bouncyDamping: Float = 0.6f

    // Expressive - for emphasized animations
    const val expressiveStiffness: Float = 250f
    const val expressiveDamping: Float = 0.7f
}

// =============================================================================
// OPACITY TOKENS
// =============================================================================

object Opacity {
    const val disabled: Float = 0.38f
    const val medium: Float = 0.60f
    const val high: Float = 0.87f
    const val full: Float = 1.0f

    // Surface overlays
    const val surfaceOverlay1: Float = 0.05f
    const val surfaceOverlay2: Float = 0.08f
    const val surfaceOverlay3: Float = 0.11f
    const val surfaceOverlay4: Float = 0.12f
    const val surfaceOverlay5: Float = 0.14f

    // State overlays
    const val stateHover: Float = 0.08f
    const val stateFocus: Float = 0.12f
    const val statePressed: Float = 0.12f
    const val stateDragged: Float = 0.16f
}

// =============================================================================
// CONTENT PADDING TOKENS
// =============================================================================

@Immutable
data class ContentPadding(
    val horizontal: Dp,
    val vertical: Dp
)

object ContentPaddings {
    val card = ContentPadding(horizontal = Spacing.md, vertical = Spacing.md)
    val cardLarge = ContentPadding(horizontal = Spacing.xl, vertical = Spacing.lg)
    val list = ContentPadding(horizontal = Spacing.md, vertical = Spacing.sm)
    val dialog = ContentPadding(horizontal = Spacing.xl, vertical = Spacing.lg)
    val screen = ContentPadding(horizontal = Spacing.md, vertical = Spacing.xs)
    val button = ContentPadding(horizontal = Spacing.lg, vertical = Spacing.sm)
}

// =============================================================================
// COMPOSITION LOCALS FOR EASY ACCESS
// =============================================================================

val LocalSpacing = staticCompositionLocalOf { Spacing }
val LocalElevation = staticCompositionLocalOf { Elevation }
val LocalRadius = staticCompositionLocalOf { Radius }
val LocalIconSize = staticCompositionLocalOf { IconSize }
val LocalDuration = staticCompositionLocalOf { Duration }
