package dev.yaul.twocha.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.viewmodel.VpnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsSwitch(
                    icon = Icons.Filled.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme throughout the app",
                    checked = settings.darkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsSwitch(
                        icon = Icons.Filled.Palette,
                        title = "Dynamic Color",
                        subtitle = "Use Material You dynamic colors",
                        checked = settings.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Connection Section
            SettingsSection(title = "Connection") {
                SettingsSwitch(
                    icon = Icons.Filled.PlayArrow,
                    title = "Auto-Connect",
                    subtitle = "Connect automatically when app starts",
                    checked = settings.autoConnect,
                    onCheckedChange = { viewModel.setAutoConnect(it) }
                )

                SettingsSwitch(
                    icon = Icons.Filled.Notifications,
                    title = "Show Notifications",
                    subtitle = "Show connection status notifications",
                    checked = settings.showNotifications,
                    onCheckedChange = { viewModel.setShowNotifications(it) }
                )

                SettingsSwitch(
                    icon = Icons.Filled.BatteryChargingFull,
                    title = "Keep Alive on Battery",
                    subtitle = "Maintain connection even on battery saver",
                    checked = settings.keepAliveOnBattery,
                    onCheckedChange = { viewModel.setKeepAliveOnBattery(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Data Section
            SettingsSection(title = "Data") {
                SettingsItem(
                    icon = Icons.Filled.ImportExport,
                    title = "Export Configuration",
                    subtitle = "Save configuration to file",
                    onClick = { viewModel.exportConfig(context) }
                )

                SettingsItem(
                    icon = Icons.Filled.FileOpen,
                    title = "Import Configuration",
                    subtitle = "Load configuration from file",
                    onClick = { viewModel.importConfig(context) }
                )

                SettingsItem(
                    icon = Icons.Filled.DeleteForever,
                    title = "Reset Configuration",
                    subtitle = "Reset all settings to defaults",
                    onClick = { showResetDialog = true },
                    tint = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "About 2cha",
                    subtitle = "Version, licenses, and more",
                    onClick = { showAboutDialog = true }
                )

                SettingsItem(
                    icon = Icons.Filled.Code,
                    title = "Source Code",
                    subtitle = "View on GitHub",
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/keepinfov/2cha")
                        }
                        context.startActivity(intent)
                    }
                )

                SettingsItem(
                    icon = Icons.Filled.BugReport,
                    title = "Report Issue",
                    subtitle = "Report bugs or request features",
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/keepinfov/2cha/issues")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Version info at bottom
            Text(
                text = "2cha VPN v0.6.3 • Protocol v3",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("2cha VPN") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AboutItem("Version", "0.6.3")
                    AboutItem("Protocol", "v3")
                    AboutItem("Encryption", "ChaCha20-Poly1305 / AES-256-GCM")
                    AboutItem("License", "MIT")

                    Spacer(modifier = Modifier.height(8.dp))

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
                    Icons.Filled.Warning,
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
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
                    modifier = Modifier.size(20.dp)
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