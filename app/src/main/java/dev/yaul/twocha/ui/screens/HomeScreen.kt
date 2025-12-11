package dev.yaul.twocha.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.R
import dev.yaul.twocha.config.VpnConfig
import dev.yaul.twocha.protocol.Constants
import dev.yaul.twocha.ui.components.ShieldConnectButton
import dev.yaul.twocha.ui.theme.Spacing
import dev.yaul.twocha.viewmodel.VpnViewModel
import dev.yaul.twocha.vpn.ConnectionState

@Composable
fun HomeScreen(
    viewModel: VpnViewModel,
    onNavigateToConfig: () -> Unit,
    onNavigateToSettings: () -> Unit?,
    onNavigateToLogs: () -> Unit?
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val config by viewModel.config.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.connect()
        }
    }

    val handleToggle = {
        if (connectionState == ConnectionState.DISCONNECTED) {
            val prepareIntent = viewModel.prepareVpn()
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                viewModel.connect()
            }
        } else {
            viewModel.disconnect()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

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
        HomeContent(
            paddingValues = paddingValues,
            connectionState = connectionState,
            config = config,
            hasConfig = hasConfig,
            onToggle = handleToggle,
            onConfigClick = onNavigateToConfig
        )
    }
}

@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    connectionState: ConnectionState,
    config: VpnConfig?,
    hasConfig: Boolean,
    onToggle: () -> Unit,
    onConfigClick: () -> Unit
) {
    val serverAddress = config?.client?.server
    val tunName = config?.tun?.name ?: "tun0"
    val mtuValue = config?.tun?.mtu ?: Constants.DEFAULT_MTU
    val cipherName = config?.crypto?.cipher?.name?.replace("_", "-") ?: "CHACHA20-POLY1305"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
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

                    IconButton(
                        onClick = onConfigClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Icon(imageVector = Icons.Rounded.Settings, contentDescription = null)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    ConnectionStatusPill(state = connectionState)

                    ShieldConnectButton(
                        state = connectionState,
                        serverAddress = serverAddress,
                        onToggle = onToggle,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FlowingInfoRow(
                        items = listOf(
                            stringResource(R.string.home_protocol_version_label, Constants.PROTOCOL_VERSION.toInt()),
                            stringResource(R.string.home_cipher_label, cipherName),
                            stringResource(R.string.home_tun_label, tunName),
                            stringResource(R.string.home_mtu_label, mtuValue)
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
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

                        Button(
                            onClick = onToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(text = if (connectionState == ConnectionState.CONNECTED) {
                                stringResource(R.string.btn_disconnect)
                            } else {
                                stringResource(R.string.btn_connect)
                            })
                        }
                    }

                    InfoCardRow(
                        label = stringResource(R.string.stats_server),
                        value = serverAddress ?: stringResource(R.string.home_config_missing),
                        icon = Icons.Rounded.Info
                    )

                    InfoCardRow(
                        label = stringResource(R.string.home_protocol_label),
                        value = stringResource(R.string.home_protocol_value, Constants.PROTOCOL_VERSION),
                        icon = Icons.Rounded.CheckCircle
                    )

                    InfoCardRow(
                        label = stringResource(R.string.home_config_status_label),
                        value = if (hasConfig) stringResource(R.string.home_config_ready) else stringResource(R.string.home_config_missing),
                        icon = Icons.Rounded.Settings
                    )

                    TextButton(
                        onClick = onConfigClick,
                        enabled = hasConfig.not(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (hasConfig) {
                                stringResource(R.string.home_config_ready_subtitle)
                            } else {
                                stringResource(R.string.home_load_config)
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusPill(state: ConnectionState) {
    val (statusText, color) = when (state) {
        ConnectionState.CONNECTED -> stringResource(R.string.state_connected) to MaterialTheme.colorScheme.primary
        ConnectionState.CONNECTING -> stringResource(R.string.state_connecting) to MaterialTheme.colorScheme.secondary
        ConnectionState.DISCONNECTING -> stringResource(R.string.state_disconnecting) to MaterialTheme.colorScheme.tertiary
        ConnectionState.ERROR -> stringResource(R.string.state_error) to MaterialTheme.colorScheme.error
        else -> stringResource(R.string.state_disconnected) to MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.08f),
        contentColor = color,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = statusText, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FlowingInfoRow(items: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items.forEach { text ->
            InfoChip(text = text)
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
        )
    }
}

@Composable
private fun InfoCardRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
