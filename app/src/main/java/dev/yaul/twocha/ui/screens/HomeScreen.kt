package dev.yaul.twocha.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.BuildConfig
import dev.yaul.twocha.R
import dev.yaul.twocha.config.VpnConfig
import dev.yaul.twocha.protocol.Constants
import dev.yaul.twocha.ui.components.ShieldConnectButton
import dev.yaul.twocha.ui.theme.ComponentShapes
import dev.yaul.twocha.ui.theme.IconSize
import dev.yaul.twocha.ui.theme.Radius
import dev.yaul.twocha.ui.theme.Spacing
import dev.yaul.twocha.ui.theme.TouchTargets
import dev.yaul.twocha.viewmodel.VpnViewModel
import dev.yaul.twocha.vpn.ConnectionState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: VpnViewModel,
    onNavigateToConfig: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val config by viewModel.config.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importConfig(context, it) }
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.connect()
        }
    }

    val handleToggle: () -> Unit = {
        when (connectionState) {
            ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                val prepareIntent = viewModel.prepareVpn()
                if (prepareIntent != null) {
                    vpnPermissionLauncher.launch(prepareIntent)
                } else {
                    viewModel.connect()
                }
            }
            ConnectionState.CONNECTED -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.disconnect()
            }
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> {
                // Ignore rapid taps while a transition is in progress
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    val hasConfig = config?.isValid() == true

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            TopActions(
                onOpenConfig = onNavigateToConfig,
                onOpenSettings = onNavigateToSettings
            )

            HomeHeader()

            ShieldConnectButton(
                state = connectionState,
                serverAddress = config?.client?.server,
                onToggle = handleToggle,
                modifier = Modifier.fillMaxWidth()
            )

            QuickActions(
                onManualSetup = onNavigateToConfig,
                onImport = {
                    importLauncher.launch(
                        arrayOf("application/toml", "application/json", "text/plain", "*/*")
                    )
                },
                isConfigMissing = hasConfig.not()
            )

            ProtocolCard(config = config, hasConfig = hasConfig, onOpenConfig = onNavigateToConfig)
        }
    }
}

@Composable
private fun HomeHeader() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TopActions(
    onOpenConfig: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = onOpenConfig,
            shape = ComponentShapes.buttonAction
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Text(
                text = stringResource(R.string.home_action_manual),
                modifier = Modifier.padding(start = Spacing.xs)
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(TouchTargets.default)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuickActions(
    onManualSetup: () -> Unit,
    onImport: () -> Unit,
    isConfigMissing: Boolean
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        FilledTonalButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onManualSetup()
            },
            modifier = Modifier.weight(1f),
            shape = ComponentShapes.buttonAction
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null)
            Text(
                text = stringResource(R.string.home_action_manual),
                modifier = Modifier.padding(start = Spacing.xs)
            )
        }

        OutlinedButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onImport()
            },
            modifier = Modifier.weight(1f),
            shape = ComponentShapes.buttonAction,
            enabled = isConfigMissing
        ) {
            Icon(Icons.Rounded.FileCopy, contentDescription = null)
            Text(
                text = stringResource(R.string.home_action_import),
                modifier = Modifier.padding(start = Spacing.xs)
            )
        }
    }
}

@Composable
private fun ProtocolCard(
    config: VpnConfig?,
    hasConfig: Boolean,
    onOpenConfig: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(Radius.xxl)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(Spacing.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(TouchTargets.default)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(IconSize.md)
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                                Text(
                                    text = stringResource(R.string.home_overview_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.home_overview_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Badges row
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        AboutBadge(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = MaterialTheme.colorScheme.primary
                        )
                        AboutBadge(
                            text = "Protocol v${Constants.PROTOCOL_VERSION.toInt()}",
                            color = MaterialTheme.colorScheme.secondary
                        )
                        AboutBadge(
                            text = "UDP",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Technical details section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // App name and encryption info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    AboutInfoItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Shield,
                        label = stringResource(R.string.app_name),
                        value = "VPN Client",
                        iconColor = MaterialTheme.colorScheme.primary
                    )
                    AboutInfoItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Lock,
                        label = "Cipher",
                        value = config?.crypto?.cipher?.name?.replace("_", "-")
                            ?: stringResource(R.string.home_cipher_fallback),
                        iconColor = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                // Config status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasConfig) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(Radius.lg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                imageVector = if (hasConfig) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (hasConfig) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(IconSize.md)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                                Text(
                                    text = if (hasConfig) {
                                        stringResource(R.string.home_config_ready)
                                    } else {
                                        stringResource(R.string.home_config_missing)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasConfig) {
                                        config?.client?.server ?: stringResource(R.string.home_config_ready_subtitle)
                                    } else {
                                        stringResource(R.string.home_config_missing_subtitle)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = onOpenConfig) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = if (hasConfig) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.3f),
            shape = RoundedCornerShape(50)
        )
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        )
    }
}

@Composable
private fun AboutInfoItem(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(IconSize.lgPlus)
                .background(
                    color = iconColor.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(IconSize.sm)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
