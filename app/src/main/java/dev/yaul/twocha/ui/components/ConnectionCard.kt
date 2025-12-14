package dev.yaul.twocha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.R
import dev.yaul.twocha.ui.theme.*
import dev.yaul.twocha.vpn.ConnectionState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Connection Card
 *
 * Features:
 * - Spring-based animations for natural feel
 * - Animated gradient ring during connection
 * - Pulsing glow effect for status indication
 * - Morphing button shape
 */
@Composable
fun ShieldConnectButton(
    state: ConnectionState,
    serverAddress: String?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = state == ConnectionState.CONNECTED
    val isConnecting = state == ConnectionState.CONNECTING || state == ConnectionState.DISCONNECTING

    val statusColor by animateColorAsState(
        targetValue = when (state) {
            ConnectionState.CONNECTED -> StatusConnected
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> StatusConnecting
            ConnectionState.ERROR -> StatusError
            else -> StatusDisconnected
        },
        animationSpec = spring(
            dampingRatio = SpringPhysics.responsiveDamping,
            stiffness = SpringPhysics.responsiveStiffness
        ),
        label = "shieldStatusColor"
    )

    val glowAlpha = rememberGlowAnimation(
        enabled = isConnecting,
        minAlpha = 0.2f,
        maxAlpha = 0.6f,
        durationMillis = 1000
    )

    val pulseScale = rememberPulseAnimation(
        enabled = isConnected,
        minScale = 1f,
        maxScale = 1.03f,
        durationMillis = 2000
    )

    val ringRotation = rememberRotationAnimation(
        enabled = isConnecting,
        durationMillis = 3000
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = Springs.responsive,
        label = "shieldPressScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = SpringPhysics.gentleDamping,
                    stiffness = SpringPhysics.gentleStiffness
                )
            )
            .clip(ComponentShapes.connectionCard)
            .clickable(
                enabled = !isConnecting,
                interactionSource = interactionSource,
                indication = null
            ) { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = ComponentShapes.connectionCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpressiveStatusIndicator(
                state = state,
                statusColor = statusColor,
                glowAlpha = glowAlpha,
                pulseScale = pulseScale,
                ringRotation = ringRotation
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = when (state) {
                    ConnectionState.CONNECTED -> stringResource(R.string.state_connected)
                    ConnectionState.CONNECTING -> stringResource(R.string.state_connecting)
                    ConnectionState.DISCONNECTING -> stringResource(R.string.state_disconnecting)
                    ConnectionState.ERROR -> stringResource(R.string.state_error)
                    else -> stringResource(R.string.state_disconnected)
                },
                style = TextStyles.connectionStatus,
                color = statusColor
            )

            if (serverAddress != null && (isConnected || isConnecting)) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(IconSize.lgPlus)
                                .background(
                                    color = statusColor.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.sm),
                                tint = statusColor
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = serverAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(
    state: ConnectionState,
    serverAddress: String?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = state == ConnectionState.CONNECTED
    val isConnecting = state == ConnectionState.CONNECTING || state == ConnectionState.DISCONNECTING
    val isError = state == ConnectionState.ERROR

    // Animated status color with spring physics
    val statusColor by animateColorAsState(
        targetValue = when (state) {
            ConnectionState.CONNECTED -> StatusConnected
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> StatusConnecting
            ConnectionState.ERROR -> StatusError
            else -> StatusDisconnected
        },
        animationSpec = spring(
            dampingRatio = SpringPhysics.responsiveDamping,
            stiffness = SpringPhysics.responsiveStiffness
        ),
        label = "statusColor"
    )

    // Glow animation for connecting state
    val glowAlpha = rememberGlowAnimation(
        enabled = isConnecting,
        minAlpha = 0.2f,
        maxAlpha = 0.6f,
        durationMillis = 1000
    )

    // Pulse animation for connected state
    val pulseScale = rememberPulseAnimation(
        enabled = isConnected,
        minScale = 1f,
        maxScale = 1.03f,
        durationMillis = 2000
    )

    // Ring rotation for connecting
    val ringRotation = rememberRotationAnimation(
        enabled = isConnecting,
        durationMillis = 3000
    )

    // Card scale animation on state change
    val cardScale by animateFloatAsState(
        targetValue = if (isConnecting) 0.98f else 1f,
        animationSpec = Springs.responsive,
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = ComponentShapes.connectionCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status indicator with animations
            ExpressiveStatusIndicator(
                state = state,
                statusColor = statusColor,
                glowAlpha = glowAlpha,
                pulseScale = pulseScale,
                ringRotation = ringRotation
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Status text with animation
            Text(
                text = when (state) {
                    ConnectionState.CONNECTED -> stringResource(R.string.state_connected)
                    ConnectionState.CONNECTING -> stringResource(R.string.state_connecting)
                    ConnectionState.DISCONNECTING -> stringResource(R.string.state_disconnecting)
                    ConnectionState.ERROR -> stringResource(R.string.state_error)
                    else -> stringResource(R.string.state_disconnected)
                },
                style = TextStyles.connectionStatus,
                color = statusColor
            )

            // Server address
            if (serverAddress != null && (isConnected || isConnecting)) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.md)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(IconSize.lgPlus)
                                .background(
                                    color = statusColor.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.sm),
                                tint = statusColor
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = serverAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Connect/Disconnect button with morphing animation
            ExpressiveConnectButton(
                isConnected = isConnected,
                isLoading = isConnecting,
                isError = isError,
                onClick = onToggle
            )
        }
    }
}

@Composable
private fun ExpressiveStatusIndicator(
    state: ConnectionState,
    statusColor: Color,
    glowAlpha: Float,
    pulseScale: Float,
    ringRotation: Float
) {
    val isConnecting = state == ConnectionState.CONNECTING || state == ConnectionState.DISCONNECTING
    val isConnected = state == ConnectionState.CONNECTED

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(if (isConnected) pulseScale else 1f),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .drawBehind {
                    // Gradient glow effect
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                statusColor.copy(alpha = glowAlpha * 0.5f),
                                statusColor.copy(alpha = glowAlpha * 0.2f),
                                Color.Transparent
                            ),
                            radius = size.minDimension / 2
                        )
                    )
                }
        )

        // Animated ring for connecting state
        if (isConnecting) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer { rotationZ = ringRotation }
                    .drawBehind {
                        val strokeWidth = 3.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2

                        // Draw arc segments
                        for (i in 0 until 3) {
                            val startAngle = i * 120f
                            drawArc(
                                color = statusColor.copy(alpha = 0.8f - (i * 0.2f)),
                                startAngle = startAngle,
                                sweepAngle = 60f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = androidx.compose.ui.geometry.Size(
                                    radius * 2,
                                    radius * 2
                                )
                            )
                        }
                    }
            )
        }

        // Inner circle with gradient
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.25f),
                            statusColor.copy(alpha = 0.1f)
                        )
                    )
                )
                .then(
                    if (isConnected) {
                        Modifier.border(
                            width = 2.dp,
                            color = statusColor.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // Center icon
            Icon(
                imageVector = when (state) {
                    ConnectionState.CONNECTED -> Icons.Rounded.Shield
                    ConnectionState.CONNECTING -> Icons.Rounded.Sync
                    ConnectionState.DISCONNECTING -> Icons.Rounded.SyncDisabled
                    ConnectionState.ERROR -> Icons.Rounded.ErrorOutline
                    else -> Icons.Rounded.Shield
                },
                contentDescription = null,
                modifier = Modifier
                    .size(IconSize.xl)
                    .then(
                        if (isConnecting) {
                            Modifier.graphicsLayer { rotationZ = ringRotation }
                        } else Modifier
                    ),
                tint = statusColor
            )
        }
    }
}

@Composable
fun ExpressiveConnectButton(
    isConnected: Boolean,
    isLoading: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Button color with spring animation
    val buttonColor by animateColorAsState(
        targetValue = when {
            isError -> StatusError
            isConnected -> StatusError.copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = spring(
            dampingRatio = SpringPhysics.responsiveDamping,
            stiffness = SpringPhysics.responsiveStiffness
        ),
        label = "buttonColor"
    )

    // Button corner radius morphing
    val cornerRadius by animateDpAsState(
        targetValue = if (isLoading) Radius.full else Radius.xxl,
        animationSpec = spring(
            dampingRatio = SpringPhysics.responsiveDamping,
            stiffness = SpringPhysics.responsiveStiffness
        ),
        label = "cornerRadius"
    )

    // Button width morphing
    val buttonWidth by animateFloatAsState(
        targetValue = if (isLoading) 0.65f else 1f,
        animationSpec = Springs.responsive,
        label = "buttonWidth"
    )

    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth(buttonWidth)
            .height(ButtonSize.largeHeight)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            disabledContainerColor = buttonColor.copy(alpha = Opacity.medium)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize.md),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isConnected)
                        Icons.Rounded.PowerSettingsNew
                    else
                        Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.md)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = if (isConnected)
                        stringResource(R.string.btn_disconnect)
                    else
                        stringResource(R.string.btn_connect),
                    style = TextStyles.button
                )
            }
        }
    }
}

// Preview helper
@Composable
fun AnimatedConnectButton(
    isConnected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExpressiveConnectButton(
        isConnected = isConnected,
        isLoading = isLoading,
        isError = false,
        onClick = onClick,
        modifier = modifier
    )
}
