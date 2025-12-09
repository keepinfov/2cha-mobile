package dev.yaul.twocha.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset

/**
 * Material 3 Expression Motion System
 *
 * Optimized animations with spring physics for natural, responsive feel.
 * All animations are 60fps-optimized with reduced durations.
 */

// =============================================================================
// SPRING SPECIFICATIONS
// =============================================================================

object Springs {
    /**
     * Snappy spring - for quick, responsive interactions
     * Use for: toggles, checkboxes, small UI elements
     */
    val snappy = spring<Float>(
        dampingRatio = SpringPhysics.snappyDamping,
        stiffness = SpringPhysics.snappyStiffness
    )

    /**
     * Responsive spring - for standard UI feedback
     * Use for: buttons, cards, selection states
     */
    val responsive = spring<Float>(
        dampingRatio = SpringPhysics.responsiveDamping,
        stiffness = SpringPhysics.responsiveStiffness
    )

    /**
     * Gentle spring - for smooth, subtle animations
     * Use for: page transitions, large elements
     */
    val gentle = spring<Float>(
        dampingRatio = SpringPhysics.gentleDamping,
        stiffness = SpringPhysics.gentleStiffness
    )

    /**
     * Bouncy spring - for playful, emphasized feedback
     * Use for: success states, celebrations
     */
    val bouncy = spring<Float>(
        dampingRatio = SpringPhysics.bouncyDamping,
        stiffness = SpringPhysics.bouncyStiffness
    )

    /**
     * Expressive spring - for emphasized, attention-grabbing animations
     * Use for: connection status changes, important state updates
     */
    val expressive = spring<Float>(
        dampingRatio = SpringPhysics.expressiveDamping,
        stiffness = SpringPhysics.expressiveStiffness
    )
}

// Generic spring specs for different types
object SpringSpecs {
    fun <T> snappy() = spring<T>(
        dampingRatio = SpringPhysics.snappyDamping,
        stiffness = SpringPhysics.snappyStiffness
    )

    fun <T> responsive() = spring<T>(
        dampingRatio = SpringPhysics.responsiveDamping,
        stiffness = SpringPhysics.responsiveStiffness
    )

    fun <T> gentle() = spring<T>(
        dampingRatio = SpringPhysics.gentleDamping,
        stiffness = SpringPhysics.gentleStiffness
    )

    fun <T> bouncy() = spring<T>(
        dampingRatio = SpringPhysics.bouncyDamping,
        stiffness = SpringPhysics.bouncyStiffness
    )

    fun <T> expressive() = spring<T>(
        dampingRatio = SpringPhysics.expressiveDamping,
        stiffness = SpringPhysics.expressiveStiffness
    )
}

// =============================================================================
// TWEEN SPECIFICATIONS (Optimized durations)
// =============================================================================

object Tweens {
    val ultraFast = tween<Float>(Duration.ultraFast, easing = FastOutSlowInEasing)
    val fast = tween<Float>(Duration.fast, easing = FastOutSlowInEasing)
    val medium = tween<Float>(Duration.medium, easing = FastOutSlowInEasing)
    val standard = tween<Float>(Duration.standard, easing = FastOutSlowInEasing)
    val slow = tween<Float>(Duration.slow, easing = FastOutSlowInEasing)

    // Emphasized easing for expressive animations
    val expressiveEnter = tween<Float>(Duration.medium, easing = EmphasizedDecelerateEasing)
    val expressiveExit = tween<Float>(Duration.fast, easing = EmphasizedAccelerateEasing)
}

// Material 3 emphasized easing curves
private val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

// =============================================================================
// ENTER/EXIT TRANSITIONS
// =============================================================================

object Transitions {
    // Fade transitions
    val fadeIn: EnterTransition = fadeIn(
        animationSpec = tween(Duration.fast, easing = EmphasizedDecelerateEasing)
    )

    val fadeOut: ExitTransition = fadeOut(
        animationSpec = tween(Duration.ultraFast, easing = EmphasizedAccelerateEasing)
    )

    // Scale transitions
    val scaleIn: EnterTransition = scaleIn(
        initialScale = 0.92f,
        animationSpec = spring(
            dampingRatio = SpringPhysics.responsiveDamping,
            stiffness = SpringPhysics.responsiveStiffness
        )
    )

    val scaleOut: ExitTransition = scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(Duration.fast, easing = EmphasizedAccelerateEasing)
    )

    // Slide transitions
    val slideInFromBottom: EnterTransition = slideInVertically(
        initialOffsetY = { it / 4 },
        animationSpec = spring(
            dampingRatio = SpringPhysics.responsiveDamping,
            stiffness = SpringPhysics.responsiveStiffness
        )
    )

    val slideOutToBottom: ExitTransition = slideOutVertically(
        targetOffsetY = { it / 4 },
        animationSpec = tween(Duration.fast, easing = EmphasizedAccelerateEasing)
    )

    // Expand/Collapse
    val expandVertically: EnterTransition = expandVertically(
        animationSpec = spring(
            dampingRatio = SpringPhysics.responsiveDamping,
            stiffness = SpringPhysics.responsiveStiffness
        )
    )

    val shrinkVertically: ExitTransition = shrinkVertically(
        animationSpec = tween(Duration.medium, easing = EmphasizedAccelerateEasing)
    )

    // Combined expressive transitions
    val expressiveEnter: EnterTransition = fadeIn + scaleIn + slideInFromBottom
    val expressiveExit: ExitTransition = fadeOut + scaleOut + slideOutToBottom

    // Subtle transitions for cards
    val cardEnter: EnterTransition = fadeIn(
        animationSpec = tween(Duration.fast)
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = spring(
            dampingRatio = SpringPhysics.responsiveDamping,
            stiffness = SpringPhysics.responsiveStiffness
        )
    )

    val cardExit: ExitTransition = fadeOut(
        animationSpec = tween(Duration.ultraFast)
    ) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(Duration.fast)
    )
}

// =============================================================================
// ANIMATED STATE HELPERS
// =============================================================================

/**
 * Optimized color animation with spring physics
 */
@Composable
fun animateColorExpressive(
    targetValue: Color,
    label: String = "color"
): androidx.compose.runtime.State<Color> {
    return animateColorAsState(
        targetValue = targetValue,
        animationSpec = tween(Duration.colorTransition, easing = FastOutSlowInEasing),
        label = label
    )
}

/**
 * Optimized float animation with spring physics
 */
@Composable
fun animateFloatExpressive(
    targetValue: Float,
    label: String = "float"
): androidx.compose.runtime.State<Float> {
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = Springs.responsive,
        label = label
    )
}

/**
 * Bouncy float animation for playful interactions
 */
@Composable
fun animateFloatBouncy(
    targetValue: Float,
    label: String = "float"
): androidx.compose.runtime.State<Float> {
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = Springs.bouncy,
        label = label
    )
}

// =============================================================================
// MODIFIER EXTENSIONS FOR ANIMATIONS
// =============================================================================

/**
 * Apply press feedback with scale animation
 */
fun Modifier.pressScale(
    pressed: Boolean,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Springs.snappy,
        label = "pressScale"
    )
    this.scale(scale)
}

/**
 * Apply hover feedback with elevation animation
 */
fun Modifier.hoverElevation(
    hovered: Boolean,
    hoveredElevation: Float = 8f,
    normalElevation: Float = 0f
): Modifier = composed {
    val elevation by animateFloatAsState(
        targetValue = if (hovered) hoveredElevation else normalElevation,
        animationSpec = Springs.responsive,
        label = "hoverElevation"
    )
    this.graphicsLayer {
        shadowElevation = elevation
    }
}

/**
 * Pulse animation modifier for attention states
 */
@Composable
fun rememberPulseAnimation(
    enabled: Boolean,
    minScale: Float = 1f,
    maxScale: Float = 1.08f,
    durationMillis: Int = 800
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return if (enabled) {
        infiniteTransition.animateFloat(
            initialValue = minScale,
            targetValue = maxScale,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        ).value
    } else {
        1f
    }
}

/**
 * Glow animation for connection status
 */
@Composable
fun rememberGlowAnimation(
    enabled: Boolean,
    minAlpha: Float = 0.3f,
    maxAlpha: Float = 0.8f,
    durationMillis: Int = 1200
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    return if (enabled) {
        infiniteTransition.animateFloat(
            initialValue = minAlpha,
            targetValue = maxAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = EmphasizedEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        ).value
    } else {
        minAlpha
    }
}

/**
 * Rotation animation for loading states
 */
@Composable
fun rememberRotationAnimation(
    enabled: Boolean,
    durationMillis: Int = 1000
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    return if (enabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotationAngle"
        ).value
    } else {
        0f
    }
}

// =============================================================================
// PAGE TRANSITION SPECS
// =============================================================================

object PageTransitions {
    val enterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = spring(
                dampingRatio = SpringPhysics.responsiveDamping,
                stiffness = SpringPhysics.responsiveStiffness
            )
        ) + fadeIn(
            animationSpec = tween(Duration.fast)
        )
    }

    val exitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(Duration.medium, easing = EmphasizedAccelerateEasing)
        ) + fadeOut(
            animationSpec = tween(Duration.ultraFast)
        )
    }

    val popEnterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = spring(
                dampingRatio = SpringPhysics.responsiveDamping,
                stiffness = SpringPhysics.responsiveStiffness
            )
        ) + fadeIn(
            animationSpec = tween(Duration.fast)
        )
    }

    val popExitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(Duration.medium, easing = EmphasizedAccelerateEasing)
        ) + fadeOut(
            animationSpec = tween(Duration.ultraFast)
        )
    }
}
