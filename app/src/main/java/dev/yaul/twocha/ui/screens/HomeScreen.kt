package dev.yaul.twocha.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.yaul.twocha.ui.theme.Spacing
import dev.yaul.twocha.viewmodel.VpnViewModel
import dev.yaul.twocha.vpn.ConnectionState
import java.io.BufferedReader
import java.io.InputStreamReader
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
    val coroutineScope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching { readTextFromUri(context, uri) }
                    .onSuccess { viewModel.importConfig(it) }
                    .onFailure {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.error_invalid_config),
                            duration = SnackbarDuration.Short
                        )
                    }
            }
        }
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
                .animateContentSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            HomeHeader(onNavigateToSettings = onNavigateToSettings)

            ShieldConnectButton(
                state = connectionState,
                serverAddress = config?.client?.server,
                onToggle = handleToggle,
                modifier = Modifier.fillMaxWidth()
            )

            QuickActions(
                onManualSetup = onNavigateToConfig,
                onImport = { importLauncher.launch("application/toml") },
                isConfigMissing = hasConfig.not()
            )

            ProtocolCard(config = config, hasConfig = hasConfig, onOpenConfig = onNavigateToConfig)
        }
    }
}

@Composable
private fun HomeHeader(onNavigateToSettings: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
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

            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
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
            shape = RoundedCornerShape(18.dp)
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
            shape = RoundedCornerShape(18.dp),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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
                IconButton(onClick = onOpenConfig) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.home_protocol_version_label,
                        Constants.PROTOCOL_VERSION.toInt()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.home_cipher_label,
                        config?.crypto?.cipher?.name?.replace("_", "-")
                            ?: stringResource(R.string.home_cipher_fallback)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.home_transport_udp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.home_version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = if (hasConfig) {
                                stringResource(R.string.home_config_ready)
                            } else {
                                stringResource(R.string.home_config_missing)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasConfig) {
                                config?.client?.server ?: stringResource(R.string.home_config_ready_subtitle)
                            } else {
                                stringResource(R.string.home_config_missing_subtitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onOpenConfig) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                }
            }
        }
    }
}

private fun readTextFromUri(context: android.content.Context, uri: Uri): String {
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            return reader.readText()
        }
    }
    error("Unable to read file")
}
