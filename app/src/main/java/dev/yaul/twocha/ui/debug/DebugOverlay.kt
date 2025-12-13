package dev.yaul.twocha.ui.debug

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.ui.theme.*
import kotlin.math.roundToInt

/**
 * Debug Overlay for Development
 *
 * Features:
 * - Live color palette viewer
 * - Theme switcher
 * - Animation speed controls
 * - Component inspector
 * - Drag-to-position FAB
 *
 * Enable via ThemeConfig.debugMode = true
 */

@Composable
fun DebugOverlay(
    enabled: Boolean = ThemeConfig.debugMode,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (enabled) {
            DebugFab()
        }
    }
}

@Composable
private fun DebugFab() {
    var showPanel by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md)
    ) {
        // Draggable FAB
        FloatingActionButton(
            onClick = { showPanel = !showPanel },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Icon(
                imageVector = if (showPanel) Icons.Rounded.Close else Icons.Rounded.BugReport,
                contentDescription = "Debug"
            )
        }

        // Debug Panel
        AnimatedVisibility(
            visible = showPanel,
            enter = Transitions.expressiveEnter,
            exit = Transitions.expressiveExit,
            modifier = Modifier.align(Alignment.Center)
        ) {
            DebugPanel(onDismiss = { showPanel = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebugPanel(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Colors", "Theme", "Animations")

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.7f),
        shape = ComponentShapes.dialog,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            TopAppBar(
                title = { Text("Debug Panel") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.md)
            ) {
                when (selectedTab) {
                    0 -> ColorPaletteTab()
                    1 -> ThemeSwitcherTab()
                    2 -> AnimationControlsTab()
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteTab() {
    val colors = LocalTwochaColors.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        item {
            Text(
                text = "Tap color to copy hex",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        items(getColorList(colors)) { (name, color) ->
            ColorRow(
                name = name,
                color = color,
                onCopy = {
                    scope.launch { clipboard.setClipEntry(color.toHexString().toClipEntry()) }
                }
            )
        }
    }
}

@Composable
private fun ColorRow(
    name: String,
    color: Color,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(onClick = onCopy)
            .padding(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color swatch
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(Radius.xs))
                .background(color)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(Radius.xs)
                )
        )

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Name
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Hex value
        Text(
            text = color.toHexString(),
            style = TextStyles.code,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeSwitcherTab() {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = "Select Theme",
            style = MaterialTheme.typography.titleMedium
        )

        ThemeStyle.entries.forEach { style ->
            ThemeOption(
                style = style,
                isSelected = false, // TODO: Would need actual state management
                onSelect = {
                    // TODO: Would need to update theme state
                }
            )
        }
    }
}

@Composable
private fun ThemeOption(
    style: ThemeStyle,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val palette = getColorPalette(style)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onSelect),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme preview swatches
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                ColorSwatch(palette.primary)
                ColorSwatch(palette.secondary)
                ColorSwatch(palette.tertiary)
                ColorSwatch(palette.background)
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // Theme name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = style.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (style.isDark()) "Dark" else "Light",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            )
    )
}

@Composable
private fun AnimationControlsTab() {
    var animationScale by remember { mutableFloatStateOf(ThemeConfig.animationScale) }
    var useSpringAnimations by remember { mutableStateOf(ThemeConfig.useSpringAnimations) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = "Animation Settings",
            style = MaterialTheme.typography.titleMedium
        )

        // Animation speed slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Animation Speed")
                Text(
                    text = "${(animationScale * 100).toInt()}%",
                    style = TextStyles.code
                )
            }

            Slider(
                value = animationScale,
                onValueChange = {
                    animationScale = it
                    ThemeConfig.animationScale = it
                },
                valueRange = 0.25f..2f,
                steps = 6
            )
        }

        // Spring animations toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use Spring Animations")
            Switch(
                checked = useSpringAnimations,
                onCheckedChange = {
                    useSpringAnimations = it
                    ThemeConfig.useSpringAnimations = it
                }
            )
        }

        HorizontalDivider()

        // Quick presets
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.titleSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            AssistChip(
                onClick = {
                    animationScale = 0.5f
                    ThemeConfig.animationScale = 0.5f
                },
                label = { Text("Fast") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.FastForward,
                        contentDescription = null,
                        Modifier.size(18.dp)
                    )
                }
            )

            AssistChip(
                onClick = {
                    animationScale = 1f
                    ThemeConfig.animationScale = 1f
                },
                label = { Text("Normal") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        Modifier.size(18.dp)
                    )
                }
            )

            AssistChip(
                onClick = {
                    animationScale = 2f
                    ThemeConfig.animationScale = 2f
                },
                label = { Text("Slow") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.SlowMotionVideo,
                        contentDescription = null,
                        Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

// =============================================================================
// UTILITIES
// =============================================================================

private fun getColorList(colors: TwochaColorPalette): List<Pair<String, Color>> {
    return listOf(
        "Primary" to colors.primary,
        "On Primary" to colors.onPrimary,
        "Primary Container" to colors.primaryContainer,
        "On Primary Container" to colors.onPrimaryContainer,
        "Secondary" to colors.secondary,
        "On Secondary" to colors.onSecondary,
        "Secondary Container" to colors.secondaryContainer,
        "Tertiary" to colors.tertiary,
        "Error" to colors.error,
        "Background" to colors.background,
        "On Background" to colors.onBackground,
        "Surface" to colors.surface,
        "On Surface" to colors.onSurface,
        "Surface Variant" to colors.surfaceVariant,
        "Outline" to colors.outline,
        "Success" to colors.success,
        "Warning" to colors.warning
    )
}

private fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("#%08X", argb)
}
