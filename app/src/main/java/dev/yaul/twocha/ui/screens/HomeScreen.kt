package dev.yaul.twocha.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.yaul.twocha.R
import dev.yaul.twocha.ui.components.ShieldConnectButton
import dev.yaul.twocha.ui.theme.Spacing
import dev.yaul.twocha.viewmodel.VpnViewModel
import dev.yaul.twocha.vpn.ConnectionState

@Composable
fun HomeScreen(
    viewModel: VpnViewModel,
    onNavigateToConfig: () -> Unit
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
            serverAddress = config?.client?.server,
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
    serverAddress: String?,
    hasConfig: Boolean,
    onToggle: () -> Unit,
    onConfigClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Text(
            text = "2cha",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ShieldConnectButton(
            state = connectionState,
            serverAddress = serverAddress,
            onToggle = onToggle,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Button(
            onClick = onConfigClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = hasConfig.not()
        ) {
            Text(
                text = if (hasConfig) {
                    stringResource(R.string.home_config_ready)
                } else {
                    stringResource(R.string.home_load_config)
                }
            )
        }
    }
}
