package dev.yaul.twocha.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.BuildConfig
import dev.yaul.twocha.protocol.Constants
import dev.yaul.twocha.ui.theme.IconSize
import dev.yaul.twocha.ui.theme.Spacing
import dev.yaul.twocha.ui.theme.ThemeStyle
import dev.yaul.twocha.ui.theme.getColorPalette
import dev.yaul.twocha.ui.theme.isDark
import dev.yaul.twocha.viewmodel.VpnViewModel

/**
 * Settings Screen - Reimagined Material 3 layout
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Settings", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Personalize your secure connection",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            SettingsHero()

            SettingsGroupCard(
                title = "Appearance",
                subtitle = "Dial in a look that matches your vibe"
            ) {
                SettingRow(
                    icon = Icons.Rounded.Palette,
                    title = "Theme presets",
                    subtitle = "Try expressive palettes for 2cha",
                    supporting = {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            ThemeStyle.entries.take(3).forEach { style ->
                                ColorSwatch(getColorPalette(style).primary)
                            }
                        }
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showThemeDialog = true
                    }
                )

                SettingSwitchRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "Dark mode",
                    subtitle = "Prefer deeper contrast across the app",
                    checked = settings.darkMode,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setDarkMode(it)
                    }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingSwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Dynamic color",
                        subtitle = "Match 2cha to your wallpaper colors",
                        checked = settings.dynamicColor,
                        onCheckedChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setDynamicColor(it)
                        }
                    )
                }
            }

            SettingsGroupCard(
                title = "Connection",
                subtitle = "Stay online with confidence"
            ) {
                SettingSwitchRow(
                    icon = Icons.Rounded.Bolt,
                    title = "Auto-connect",
                    subtitle = "Start protection as soon as 2cha opens",
                    checked = settings.autoConnect,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setAutoConnect(it)
                    }
                )

                SettingSwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Connection alerts",
                    subtitle = "Status updates while you browse",
                    checked = settings.showNotifications,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setShowNotifications(it)
                    }
                )

                SettingSwitchRow(
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = "Keep alive on battery saver",
                    subtitle = "Maintain tunnels when power saving kicks in",
                    checked = settings.keepAliveOnBattery,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setKeepAliveOnBattery(it)
                    }
                )
            }

            SettingsGroupCard(
                title = "Data & backups",
                subtitle = "Manage and safeguard your configuration"
            ) {
                SettingRow(
                    icon = Icons.Rounded.FileUpload,
                    title = "Export configuration",
                    subtitle = "Save your setup as a portable file",
                    trailing = {
                        AssistChip(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.exportConfig(context)
                        }, label = { Text("Save") })
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.exportConfig(context)
                    }
                )

                SettingRow(
                    icon = Icons.Rounded.FileDownload,
                    title = "Import configuration",
                    subtitle = "Load settings from a saved file",
                    trailing = {
                        AssistChip(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.importConfig(context)
                        }, label = { Text("Browse") })
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.importConfig(context)
                    }
                )

                SettingRow(
                    icon = Icons.Rounded.DeleteForever,
                    title = "Reset configuration",
                    subtitle = "Start fresh with default preferences",
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showResetDialog = true
                    }
                )
            }

            SettingsGroupCard(
                title = "About",
                subtitle = "Transparency and ways to reach us"
            ) {
                SettingRow(
                    icon = Icons.Rounded.Info,
                    title = "About 2cha",
                    subtitle = "Version notes, licenses, and tech",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAboutDialog = true
                    }
                )

                SettingRow(
                    icon = Icons.Rounded.Code,
                    title = "Source code",
                    subtitle = "Inspect the project on GitHub",
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

                SettingRow(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = "Mobile app source",
                    subtitle = "See the Android client on GitHub",
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/keepinfov/2cha-mobile")
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

                SettingRow(
                    icon = Icons.Rounded.BugReport,
                    title = "Report an issue",
                    subtitle = "Tell us about bugs or feature ideas",
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

            Spacer(modifier = Modifier.height(Spacing.lg))

            Text(
                text = "2cha VPN v${BuildConfig.VERSION_NAME} • Protocol v${Constants.PROTOCOL_VERSION.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .padding(bottom = Spacing.lg)
            )
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { style ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setThemeStyle(style)
                showThemeDialog = false
            }
        )
    }

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
                    AboutItem("Version", BuildConfig.VERSION_NAME)
                    AboutItem("Protocol", "v${Constants.PROTOCOL_VERSION.toInt()}")
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

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.DeleteForever,
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
private fun SettingsHero() {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(Spacing.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(IconSize.lg)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "You're protected",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Text(
                    text = "Tune how 2cha looks, behaves, and keeps your tunnels healthy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SettingsBadge(text = "Secure", color = MaterialTheme.colorScheme.primary)
                    SettingsBadge(text = "Optimized", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun SettingsBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.4f),
            shape = RoundedCornerShape(50)
        )
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailingIcon: ImageVector? = null,
    supporting: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    ListItem(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        headlineContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = titleColor, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                supporting?.invoke()
            }
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint)
                }
            }
        },
        trailingContent = trailing ?: trailingIcon?.let {
            {
                Icon(
                    it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSize.md)
                )
            }
        }
    )
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        },
        onClick = { onCheckedChange(!checked) }
    )
}

@Composable
private fun ThemeSelectionDialog(
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeStyle) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose theme style") },
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
                Text("Close")
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
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.md)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    ColorSwatch(palette.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ColorSwatch(palette.surface)
                        ColorSwatch(palette.tertiary)
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = style.name.lowercase().replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleSmall
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
