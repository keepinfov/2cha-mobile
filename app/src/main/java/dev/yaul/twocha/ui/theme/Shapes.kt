package dev.yaul.twocha.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Material 3 Expression Shapes
 *
 * Modern, expressive shapes including morphing containers
 * and dynamic corner treatments.
 */

// =============================================================================
// STANDARD MATERIAL 3 SHAPES
// =============================================================================

val TwochaShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xxl)
)

// =============================================================================
// COMPONENT-SPECIFIC SHAPES
// =============================================================================

object ComponentShapes {
    // Cards
    val card = RoundedCornerShape(Radius.xl)
    val cardSmall = RoundedCornerShape(Radius.md)
    val cardLarge = RoundedCornerShape(Radius.xxl)

    // Buttons
    val button = RoundedCornerShape(Radius.md)
    val buttonPill = RoundedCornerShape(Radius.full)
    val fabStandard = RoundedCornerShape(Radius.lg)
    val fabLarge = RoundedCornerShape(Radius.xxl)

    // Status indicator
    val statusIndicator = CircleShape
    val statusBadge = RoundedCornerShape(Radius.xs)

    // Input fields
    val textField = RoundedCornerShape(Radius.sm)
    val searchBar = RoundedCornerShape(Radius.full)

    // Dialogs and sheets
    val dialog = RoundedCornerShape(Radius.xxl)
    val bottomSheet = RoundedCornerShape(
        topStart = Radius.xxl,
        topEnd = Radius.xxl,
        bottomStart = Radius.none,
        bottomEnd = Radius.none
    )

    // Navigation
    val navigationBar = RoundedCornerShape(
        topStart = Radius.lg,
        topEnd = Radius.lg,
        bottomStart = Radius.none,
        bottomEnd = Radius.none
    )
    val navigationIndicator = RoundedCornerShape(Radius.md)

    // Chips and badges
    val chip = RoundedCornerShape(Radius.sm)
    val badge = RoundedCornerShape(Radius.xs)

    // Connection card specific
    val connectionCard = RoundedCornerShape(Radius.xxl)
    val connectionButton = RoundedCornerShape(Radius.lg)
    val statsCard = RoundedCornerShape(Radius.xl)
}

// =============================================================================
// EXPRESSIVE SHAPES (Squircle / Superellipse)
// =============================================================================

/**
 * Squircle shape for more organic, Google-style corners
 * Similar to iOS but commonly used in Material Expression
 */
class SquircleShape(
    private val cornerRadius: Dp = 24.dp,
    private val smoothness: Float = 0.6f // 0 = rounded rect, 1 = superellipse
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val actualRadius = min(radiusPx, min(size.width, size.height) / 2f)

        return Outline.Generic(
            path = createSquirclePath(size, actualRadius, smoothness)
        )
    }

    private fun createSquirclePath(size: Size, radius: Float, smoothness: Float): Path {
        val path = Path()
        val width = size.width
        val height = size.height
        val controlOffset = radius * (1 - smoothness) * 0.55f

        path.moveTo(radius, 0f)

        // Top edge
        path.lineTo(width - radius, 0f)
        // Top right corner
        path.cubicTo(
            width - controlOffset, 0f,
            width, controlOffset,
            width, radius
        )

        // Right edge
        path.lineTo(width, height - radius)
        // Bottom right corner
        path.cubicTo(
            width, height - controlOffset,
            width - controlOffset, height,
            width - radius, height
        )

        // Bottom edge
        path.lineTo(radius, height)
        // Bottom left corner
        path.cubicTo(
            controlOffset, height,
            0f, height - controlOffset,
            0f, height - radius
        )

        // Left edge
        path.lineTo(0f, radius)
        // Top left corner
        path.cubicTo(
            0f, controlOffset,
            controlOffset, 0f,
            radius, 0f
        )

        path.close()
        return path
    }
}

// Convenient squircle shapes
object SquircleShapes {
    val small = SquircleShape(cornerRadius = Radius.sm)
    val medium = SquircleShape(cornerRadius = Radius.md)
    val large = SquircleShape(cornerRadius = Radius.lg)
    val extraLarge = SquircleShape(cornerRadius = Radius.xxl)
    val card = SquircleShape(cornerRadius = Radius.xl)
    val button = SquircleShape(cornerRadius = Radius.md)
}

// =============================================================================
// MORPHING SHAPE (For animated shape transitions)
// =============================================================================

/**
 * Shape that can morph between two states
 * Useful for connection button animations
 */
class MorphingShape(
    private val progress: Float, // 0 = start shape, 1 = end shape
    private val startRadius: Dp = 16.dp,
    private val endRadius: Dp = 50.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val startRadiusPx = with(density) { startRadius.toPx() }
        val endRadiusPx = with(density) { endRadius.toPx() }
        val currentRadius = startRadiusPx + (endRadiusPx - startRadiusPx) * progress

        return Outline.Rounded(
            roundRect = androidx.compose.ui.geometry.RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = currentRadius,
                radiusY = currentRadius
            )
        )
    }
}

/**
 * Create an animated morphing shape
 */
@Composable
fun rememberMorphingShape(
    progress: Float,
    startRadius: Dp = Radius.lg,
    endRadius: Dp = Radius.full
): Shape {
    return MorphingShape(
        progress = progress,
        startRadius = startRadius,
        endRadius = endRadius
    )
}

// =============================================================================
// CUT CORNER SHAPES (Material Expression style)
// =============================================================================

/**
 * Shape with cut corners - modern Material Expression style
 */
class CutCornerShape(
    private val cutSize: Dp = 16.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cutSizePx = with(density) { cutSize.toPx() }
        val path = Path().apply {
            moveTo(cutSizePx, 0f)
            lineTo(size.width - cutSizePx, 0f)
            lineTo(size.width, cutSizePx)
            lineTo(size.width, size.height - cutSizePx)
            lineTo(size.width - cutSizePx, size.height)
            lineTo(cutSizePx, size.height)
            lineTo(0f, size.height - cutSizePx)
            lineTo(0f, cutSizePx)
            close()
        }
        return Outline.Generic(path)
    }
}

object CutCornerShapes {
    val small = CutCornerShape(cutSize = 8.dp)
    val medium = CutCornerShape(cutSize = 12.dp)
    val large = CutCornerShape(cutSize = 16.dp)
}

// =============================================================================
// COMPOSITION LOCAL FOR SHAPES
// =============================================================================

@Immutable
data class ExpressiveShapes(
    val card: Shape = ComponentShapes.card,
    val cardSmall: Shape = ComponentShapes.cardSmall,
    val cardLarge: Shape = ComponentShapes.cardLarge,
    val button: Shape = ComponentShapes.button,
    val buttonPill: Shape = ComponentShapes.buttonPill,
    val fab: Shape = ComponentShapes.fabStandard,
    val dialog: Shape = ComponentShapes.dialog,
    val bottomSheet: Shape = ComponentShapes.bottomSheet,
    val chip: Shape = ComponentShapes.chip,
    val statusIndicator: Shape = ComponentShapes.statusIndicator,
    val connectionCard: Shape = ComponentShapes.connectionCard,
    val connectionButton: Shape = ComponentShapes.connectionButton,
    val statsCard: Shape = ComponentShapes.statsCard
)

val LocalExpressiveShapes = staticCompositionLocalOf { ExpressiveShapes() }
