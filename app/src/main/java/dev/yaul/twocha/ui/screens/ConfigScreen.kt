package dev.yaul.twocha.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.config.CipherSuite
import dev.yaul.twocha.config.VpnConfig
import dev.yaul.twocha.viewmodel.VpnViewModel
import dev.yaul.twocha.vpn.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected = connectionState == ConnectionState.CONNECTED

    var serverAddress by remember(config) { mutableStateOf(config?.client?.server ?: "") }
    var encryptionKey by remember(config) { mutableStateOf(config?.crypto?.key ?: "") }
    var selectedCipher by remember(config) { mutableStateOf(config?.crypto?.cipher ?: CipherSuite.CHACHA20_POLY1305) }
    var ipv4Address by remember(config) { mutableStateOf(config?.ipv4?.address ?: "10.0.0.2") }
    var ipv4Prefix by remember(config) { mutableStateOf(config?.ipv4?.prefix?.toString() ?: "24") }
    var ipv4RouteAll by remember(config) { mutableStateOf(config?.ipv4?.routeAll ?: false) }
    var ipv6Enabled by remember(config) { mutableStateOf(config?.ipv6?.enable ?: false) }
    var ipv6Address by remember(config) { mutableStateOf(config?.ipv6?.address ?: "fd00:2cha::2") }
    var dnsServers by remember(config) { mutableStateOf(config?.dns?.serversV4?.joinToString(", ") ?: "1.1.1.1, 8.8.8.8") }
    var mtu by remember(config) { mutableStateOf(config?.tun?.mtu?.toString() ?: "1420") }
    var keepalive by remember(config) { mutableStateOf(config?.timeouts?.keepalive?.toString() ?: "25") }

    var showKeyField by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSaveDialog = true },
                        enabled = !isConnected
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning if connected
            if (isConnected) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Disconnect VPN to edit configuration",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Server Section
            ConfigSection(title = "Server") {
                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it },
                    label = { Text("Server Address") },
                    placeholder = { Text("vpn.example.com:51820") },
                    leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnected,
                    singleLine = true
                )
            }

            // Encryption Section
            ConfigSection(title = "Encryption") {
                // Cipher Selection
                var cipherExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = cipherExpanded,
                    onExpandedChange = { if (!isConnected) cipherExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedCipher) {
                            CipherSuite.CHACHA20_POLY1305 -> "ChaCha20-Poly1305"
                            CipherSuite.AES_256_GCM -> "AES-256-GCM"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cipher") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cipherExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isConnected
                    )
                    ExposedDropdownMenu(
                        expanded = cipherExpanded,
                        onDismissRequest = { cipherExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("ChaCha20-Poly1305 (Recommended)") },
                            onClick = {
                                selectedCipher = CipherSuite.CHACHA20_POLY1305
                                cipherExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("AES-256-GCM") },
                            onClick = {
                                selectedCipher = CipherSuite.AES_256_GCM
                                cipherExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Encryption Key
                OutlinedTextField(
                    value = encryptionKey,
                    onValueChange = { encryptionKey = it },
                    label = { Text("Encryption Key") },
                    placeholder = { Text("64 hex characters") },
                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showKeyField = !showKeyField }) {
                            Icon(
                                if (showKeyField) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showKeyField) "Hide" else "Show"
                            )
                        }
                    },
                    visualTransformation = if (showKeyField) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnected,
                    singleLine = true,
                    supportingText = {
                        Text("${encryptionKey.length}/64 characters")
                    },
                    isError = encryptionKey.isNotEmpty() && encryptionKey.length != 64
                )
            }

            // IPv4 Section
            ConfigSection(title = "IPv4") {
                OutlinedTextField(
                    value = ipv4Address,
                    onValueChange = { ipv4Address = it },
                    label = { Text("IP Address") },
                    leadingIcon = { Icon(Icons.Filled.Computer, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnected,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ipv4Prefix,
                    onValueChange = { ipv4Prefix = it.filter { c -> c.isDigit() } },
                    label = { Text("Prefix Length") },
                    placeholder = { Text("24") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnected,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Route All Traffic",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Send all IPv4 traffic through VPN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = ipv4RouteAll,
                        onCheckedChange = { ipv4RouteAll = it },
                        enabled = !isConnected
                    )
                }
            }

            // IPv6 Section
            ConfigSection(title = "IPv6") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Enable IPv6",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Enable IPv6 dual-stack support",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = ipv6Enabled,
                        onCheckedChange = { ipv6Enabled = it },
                        enabled = !isConnected
                    )
                }

                AnimatedVisibility(
                    visible = ipv6Enabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = ipv6Address,
                            onValueChange = { ipv6Address = it },
                            label = { Text("IPv6 Address") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isConnected,
                            singleLine = true
                        )
                    }
                }
            }

            // DNS Section
            ConfigSection(title = "DNS") {
                OutlinedTextField(
                    value = dnsServers,
                    onValueChange = { dnsServers = it },
                    label = { Text("DNS Servers") },
                    placeholder = { Text("1.1.1.1, 8.8.8.8") },
                    leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnected,
                    supportingText = { Text("Separate multiple servers with commas") }
                )
            }

            // Advanced Section
            Card(
                onClick = { showAdvanced = !showAdvanced },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Advanced Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(
                visible = showAdvanced,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ConfigSection(title = "Network") {
                        OutlinedTextField(
                            value = mtu,
                            onValueChange = { mtu = it.filter { c -> c.isDigit() } },
                            label = { Text("MTU") },
                            placeholder = { Text("1420") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isConnected,
                            singleLine = true,
                            supportingText = { Text("Maximum Transmission Unit (1280-1500)") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = keepalive,
                            onValueChange = { keepalive = it.filter { c -> c.isDigit() } },
                            label = { Text("Keepalive Interval") },
                            placeholder = { Text("25") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isConnected,
                            singleLine = true,
                            supportingText = { Text("Seconds between keepalive packets") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon = { Icon(Icons.Filled.Save, contentDescription = null) },
            title = { Text("Save Configuration") },
            text = { Text("Save the current configuration? This will overwrite any existing settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Build and save config
                        val newConfig = VpnConfig(
                            client = dev.yaul.twocha.config.ClientSection(
                                server = serverAddress
                            ),
                            tun = dev.yaul.twocha.config.TunSection(
                                mtu = mtu.toIntOrNull() ?: 1420
                            ),
                            crypto = dev.yaul.twocha.config.CryptoSection(
                                cipher = selectedCipher,
                                key = encryptionKey
                            ),
                            ipv4 = dev.yaul.twocha.config.Ipv4Section(
                                enable = true,
                                address = ipv4Address,
                                prefix = ipv4Prefix.toIntOrNull() ?: 24,
                                routeAll = ipv4RouteAll
                            ),
                            ipv6 = dev.yaul.twocha.config.Ipv6Section(
                                enable = ipv6Enabled,
                                address = if (ipv6Enabled) ipv6Address else null
                            ),
                            dns = dev.yaul.twocha.config.DnsSection(
                                serversV4 = dnsServers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            ),
                            timeouts = dev.yaul.twocha.config.TimeoutsSection(
                                keepalive = keepalive.toLongOrNull() ?: 25
                            )
                        )
                        viewModel.saveConfig(newConfig)
                        showSaveDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ConfigSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}