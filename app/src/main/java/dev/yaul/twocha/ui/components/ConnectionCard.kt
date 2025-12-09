package dev.yaul.twocha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.R
import dev.yaul.twocha.ui.theme.*
import dev.yaul.twocha.vpn.ConnectionState

@Composable
fun ConnectionCard(
    state: ConnectionState,
    serverAddress: String?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = state == ConnectionState.CONNECTED
    val isConnecting = state == ConnectionState.CONNECTING || state == ConnectionState.DISCONNECTING

    // Animated status color
    val statusColor by animateColorAsState(
        targetValue = when (state) {
            ConnectionState.CONNECTED -> StatusConnected
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> StatusConnecting
            ConnectionState.ERROR -> StatusError
            else -> StatusDisconnected
        },
        animationSpec = tween(300),
        label = "statusColor"
    )

    // Pulsing animation for connecting state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status indicator with pulse animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (isConnecting) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.3f),
                                statusColor.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (state) {
                            ConnectionState.CONNECTED -> Icons.Default.Lock
                            ConnectionState.CONNECTING -> Icons.Default.Sync
                            ConnectionState.DISCONNECTING -> Icons.Default.SyncDisabled
                            ConnectionState.ERROR -> Icons.Default.Error
                            else -> Icons.Default.LockOpen
                        },
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status text
            Text(
                text = when (state) {
                    ConnectionState.CONNECTED -> stringResource(R.string.state_connected)
                    ConnectionState.CONNECTING -> stringResource(R.string.state_connecting)
                    ConnectionState.DISCONNECTING -> stringResource(R.string.state_disconnecting)
                    ConnectionState.ERROR -> stringResource(R.string.state_error)
                    else -> stringResource(R.string.state_disconnected)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )

            // Server address
            if (serverAddress != null && (isConnected || isConnecting)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = serverAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Connect/Disconnect button
            AnimatedConnectButton(
                isConnected = isConnected,
                isLoading = isConnecting,
                onClick = onToggle
            )
        }
    }
}

@Composable
fun AnimatedConnectButton(
    isConnected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isConnected) Error else Primary,
        animationSpec = tween(300),
        label = "buttonColor"
    )

    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (isConnected) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected)
                    stringResource(R.string.btn_disconnect)
                else
                    stringResource(R.string.btn_connect),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}