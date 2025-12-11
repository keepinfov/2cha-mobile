package dev.yaul.twocha.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.ui.theme.*
import dev.yaul.twocha.viewmodel.VpnViewModel

/**
 * Settings Screen - Material 3 Expressive Design
 *
 * Features:
 * - Theme selection with live preview
 * - Expressive animations
 * - Clean, organized sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val settings by viewModel.settings.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateBack()
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Appearance Section
        SettingsSection(title = "Appearance") {
            // Theme selector
            SettingsItem(
                icon = Icons.Rounded.Palette,
                title = "App Theme",
                subtitle = "Choose your preferred theme",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showThemeDialog = true
                }
            )

            SettingsSwitch(
                icon = Icons.Rounded.DarkMode,
                title = "Dark Mode",
                subtitle = "Use dark theme throughout the app",
                checked = settings.darkMode,
                onCheckedChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setDarkMode(it)
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsSwitch(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Dynamic Color",
                    subtitle = "Use Material You dynamic colors",
                    checked = settings.dynamicColor,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setDynamicColor(it)
                    }
                )
            }
        }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

            // Connection Section
            SettingsSection(title = "Connection") {
            SettingsSwitch(
                icon = Icons.Rounded.PlayArrow,
                title = "Auto-Connect",
                subtitle = "Connect automatically when app starts",
                checked = settings.autoConnect,
                onCheckedChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setAutoConnect(it)
                }
            )

            SettingsSwitch(
                icon = Icons.Rounded.Notifications,
                title = "Show Notifications",
                subtitle = "Show connection status notifications",
                checked = settings.showNotifications,
                onCheckedChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setShowNotifications(it)
                }
            )

            SettingsSwitch(
                icon = Icons.Rounded.BatteryChargingFull,
                title = "Keep Alive on Battery",
                subtitle = "Maintain connection even on battery saver",
                checked = settings.keepAliveOnBattery,
                onCheckedChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setKeepAliveOnBattery(it)
                }
            )
        }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

            // Data Section
            SettingsSection(title = "Data") {
            SettingsItem(
                icon = Icons.Rounded.FileUpload,
                title = "Export Configuration",
                subtitle = "Save configuration to file",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.exportConfig(context)
                }
            )

            SettingsItem(
                icon = Icons.Rounded.FileDownload,
                title = "Import Configuration",
                subtitle = "Load configuration from file",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.importConfig(context)
                }
            )

            SettingsItem(
                icon = Icons.Rounded.DeleteForever,
                title = "Reset Configuration",
                subtitle = "Reset all settings to defaults",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showResetDialog = true
                },
                tint = MaterialTheme.colorScheme.error
            )
        }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

            // About Section
            SettingsSection(title = "About") {
            SettingsItem(
                icon = Icons.Rounded.Info,
                title = "About 2cha",
                subtitle = "Version, licenses, and more",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showAboutDialog = true
                }
            )

            SettingsItem(
                icon = Icons.Rounded.Code,
                title = "Source Code",
                subtitle = "View on GitHub",
                trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://github.com/keepinfov/2cha")
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            Toast.makeText(
                                context,
                                context.getString(dev.yaul.twocha.R.string.settings_link_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            )

            SettingsItem(
                icon = Icons.Rounded.BugReport,
                title = "Report Issue",
                subtitle = "Report bugs or request features",
                trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://github.com/keepinfov/2cha/issues")
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            Toast.makeText(
                                context,
                                context.getString(dev.yaul.twocha.R.string.settings_link_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            )
        }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Version info at bottom
            Text(
                text = "2cha VPN v0.6.3 • Protocol v3",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { style ->
                // Theme would be applied via ViewModel
                showThemeDialog = false
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("2cha VPN") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    AboutItem("Version", "0.6.3")
                    AboutItem("Protocol", "v3")
                    AboutItem("Encryption", "ChaCha20-Poly1305 / AES-256-GCM")
                    AboutItem("License", "MIT")

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Text(
                        "High-performance VPN utility with IPv4/IPv6 dual-stack support.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Reset Configuration") },
            text = {
                Text("This will reset all settings to their default values. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.resetConfig()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeStyle) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column(
                modifier = Modifier.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                ThemeStyle.entries.forEach { style ->
                    ThemeOptionRow(
                        style = style,
                        onClick = { onThemeSelected(style) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ThemeOptionRow(
    style: ThemeStyle,
    onClick: () -> Unit
) {
    val palette = getColorPalette(style)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ComponentShapes.cardSmall)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color swatches
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
                    text = style.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (style.isDark()) "Dark theme" else "Light theme",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
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
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = {
            Text(title, color = tint)
        },
        supportingContent = {
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = tint)
        },
        trailingContent = trailingIcon?.let {
            {
                Icon(
                    it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSize.sm)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Icon(icon, contentDescription = null)
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
private fun AboutItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
