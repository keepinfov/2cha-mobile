package dev.yaul.twocha.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.R
import dev.yaul.twocha.ui.theme.*
import dev.yaul.twocha.vpn.VpnStats
import java.util.concurrent.TimeUnit

/**
 * Material 3 Expressive Stats Card
 *
 * Features:
 * - Spring-based enter/exit animations
 * - Animated stat counters
 * - Gradient accent backgrounds
 * - Optimized for quick rendering
 */
@Composable
fun StatsCard(
    stats: VpnStats,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = Transitions.expandVertically + Transitions.fadeIn,
        exit = Transitions.shrinkVertically + Transitions.fadeOut
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = ComponentShapes.statsCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.sm),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Stats grid - 2x2 layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // Download
                    ExpressiveStatItem(
                        icon = Icons.Rounded.ArrowDownward,
                        label = stringResource(R.string.stats_download),
                        value = formatBytes(stats.bytesReceived),
                        iconTint = StatusConnected,
                        modifier = Modifier.weight(1f)
                    )

                    // Upload
                    ExpressiveStatItem(
                        icon = Icons.Rounded.ArrowUpward,
                        label = stringResource(R.string.stats_upload),
                        value = formatBytes(stats.bytesSent),
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // Duration
                    ExpressiveStatItem(
                        icon = Icons.Rounded.Schedule,
                        label = stringResource(R.string.stats_duration),
                        value = formatDuration(stats.duration),
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )

                    // Packets
                    ExpressiveStatItem(
                        icon = Icons.Rounded.SwapVert,
                        label = stringResource(R.string.stats_packets),
                        value = formatPackets(stats.packetsReceived + stats.packetsSent),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        shape = ComponentShapes.cardSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with subtle background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(ComponentShapes.cardSmall)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                iconTint.copy(alpha = 0.15f),
                                iconTint.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.sm),
                    tint = iconTint
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            Column {
                Text(
                    text = label,
                    style = TextStyles.statsLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = TextStyles.statsValue,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Compact stats row for inline display
 */
@Composable
fun CompactStatsRow(
    stats: VpnStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactStatChip(
            icon = Icons.Rounded.ArrowDownward,
            value = formatBytesCompact(stats.bytesReceived),
            tint = StatusConnected
        )

        CompactStatChip(
            icon = Icons.Rounded.ArrowUpward,
            value = formatBytesCompact(stats.bytesSent),
            tint = MaterialTheme.colorScheme.primary
        )

        CompactStatChip(
            icon = Icons.Rounded.Schedule,
            value = formatDurationCompact(stats.duration),
            tint = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun CompactStatChip(
    icon: ImageVector,
    value: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = Spacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(IconSize.xs),
            tint = tint
        )
        Spacer(modifier = Modifier.width(Spacing.xxs))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// =============================================================================
// FORMATTING UTILITIES
// =============================================================================

/**
 * Format bytes to human-readable string with appropriate units
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}

/**
 * Compact byte formatting for small displays
 */
private fun formatBytesCompact(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1fG", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1fM", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.0fK", bytes / 1_000.0)
        else -> "${bytes}B"
    }
}

/**
 * Format duration to human-readable string
 */
private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Compact duration formatting
 */
private fun formatDurationCompact(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

    return when {
        hours > 0 -> String.format("%dh%dm", hours, minutes)
        else -> String.format("%dm", minutes)
    }
}

/**
 * Format packet count with K/M suffix for large numbers
 */
private fun formatPackets(packets: Long): String {
    return when {
        packets >= 1_000_000 -> String.format("%.1fM", packets / 1_000_000.0)
        packets >= 1_000 -> String.format("%.1fK", packets / 1_000.0)
        else -> packets.toString()
    }
}
