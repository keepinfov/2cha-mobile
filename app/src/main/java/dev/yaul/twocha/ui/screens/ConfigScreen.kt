package dev.yaul.twocha.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.yaul.twocha.config.CipherSuite
import dev.yaul.twocha.config.VpnConfig
import dev.yaul.twocha.ui.theme.IconSize
import dev.yaul.twocha.ui.theme.Radius
import dev.yaul.twocha.ui.theme.Spacing
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
    var showSaveSheet by remember { mutableStateOf(false) }
    val saveSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text("Configuration", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Craft your perfect tunnel setup",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSaveSheet = true }, enabled = !isConnected) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save configuration")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            ConfigHero(connectionState = connectionState)

            if (isConnected) {
                ConfigWarning()
            }

            ConfigGroupCard(
                title = "Server & keys",
                subtitle = "Secure handshake details"
            ) {
                ConfigTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it },
                    label = "Server address",
                    placeholder = "vpn.example.com:51820",
                    leadingIcon = { Icon(Icons.Rounded.Shield, contentDescription = null) },
                    enabled = !isConnected
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                CipherDropdown(
                    selectedCipher = selectedCipher,
                    onCipherSelected = { selectedCipher = it },
                    enabled = !isConnected
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                ConfigTextField(
                    value = encryptionKey,
                    onValueChange = { encryptionKey = it },
                    label = "Encryption key",
                    placeholder = "64 hex characters",
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showKeyField = !showKeyField }) {
                            Icon(
                                imageVector = if (showKeyField) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showKeyField) "Hide" else "Show"
                            )
                        }
                    },
                    visualTransformation = if (showKeyField) VisualTransformation.None else PasswordVisualTransformation(),
                    supportingText = "${encryptionKey.length}/64 characters",
                    isError = encryptionKey.isNotEmpty() && encryptionKey.length != 64,
                    enabled = !isConnected
                )
            }

            ConfigGroupCard(
                title = "IPv4",
                subtitle = "Route everyday traffic"
            ) {
                ConfigTextField(
                    value = ipv4Address,
                    onValueChange = { ipv4Address = it },
                    label = "IP address",
                    leadingIcon = { Icon(Icons.Rounded.SettingsEthernet, contentDescription = null) },
                    enabled = !isConnected
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                ConfigTextField(
                    value = ipv4Prefix,
                    onValueChange = { ipv4Prefix = it.filter(Char::isDigit) },
                    label = "Prefix length",
                    placeholder = "24",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Rounded.SettingsInputHdmi, contentDescription = null) },
                    enabled = !isConnected
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                ConfigSwitchRow(
                    title = "Route all traffic",
                    subtitle = "Send every IPv4 packet through 2cha",
                    checked = ipv4RouteAll,
                    onCheckedChange = { ipv4RouteAll = it },
                    enabled = !isConnected,
                    icon = Icons.AutoMirrored.Rounded.AltRoute
                )
            }

            ConfigGroupCard(
                title = "IPv6",
                subtitle = "Dual-stack when you need it"
            ) {
                ConfigSwitchRow(
                    title = "Enable IPv6",
                    subtitle = "Add an IPv6 address for the tunnel",
                    checked = ipv6Enabled,
                    onCheckedChange = { ipv6Enabled = it },
                    enabled = !isConnected,
                    icon = Icons.Rounded.CloudDone
                )

                AnimatedVisibility(
                    visible = ipv6Enabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        ConfigTextField(
                            value = ipv6Address,
                            onValueChange = { ipv6Address = it },
                            label = "IPv6 address",
                            leadingIcon = { Icon(Icons.Rounded.SettingsEthernet, contentDescription = null) },
                            enabled = !isConnected
                        )
                    }
                }
            }

            ConfigGroupCard(
                title = "DNS",
                subtitle = "Choose resolvers for your tunnel"
            ) {
                ConfigTextField(
                    value = dnsServers,
                    onValueChange = { dnsServers = it },
                    label = "DNS servers",
                    placeholder = "1.1.1.1, 8.8.8.8",
                    leadingIcon = { Icon(Icons.Rounded.Dns, contentDescription = null) },
                    supportingText = "Separate multiple servers with commas",
                    enabled = !isConnected
                )
            }

            ConfigGroupCard(
                title = "Advanced",
                subtitle = "Fine-tune performance"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
                    ) {
                        Text(
                            "Network tweaks",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Adjust MTU and keepalive for tricky networks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = { showAdvanced = !showAdvanced },
                        label = {
                            Text(
                                if (showAdvanced) "Hide" else "Show",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(Radius.sm)
                    )
                }

                AnimatedVisibility(
                    visible = showAdvanced,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        ConfigTextField(
                            value = mtu,
                            onValueChange = { mtu = it.filter(Char::isDigit) },
                            label = "MTU",
                            placeholder = "1420",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Rounded.Memory, contentDescription = null) },
                            supportingText = "Recommended: 1280 - 1500",
                            enabled = !isConnected
                        )

                        ConfigTextField(
                            value = keepalive,
                            onValueChange = { keepalive = it.filter(Char::isDigit) },
                            label = "Keepalive interval",
                            placeholder = "25",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                            supportingText = "Seconds between keepalive packets",
                            enabled = !isConnected
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }

    if (showSaveSheet) {
        SaveConfigBottomSheet(
            sheetState = saveSheetState,
            onDismiss = { showSaveSheet = false },
            onSave = {
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
                showSaveSheet = false
                onNavigateBack()
            },
            serverAddress = serverAddress,
            cipher = selectedCipher,
            keyLength = encryptionKey.length
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveConfigBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    serverAddress: String,
    cipher: CipherSuite,
    keyLength: Int
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = Radius.xxl, topEnd = Radius.xxl)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(Radius.lg)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(IconSize.lg)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(
                        "Save Configuration",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Review your settings before saving",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.lg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    SaveSummaryRow(
                        icon = Icons.Rounded.Shield,
                        label = "Server",
                        value = serverAddress.ifEmpty { "Not set" }
                    )
                    SaveSummaryRow(
                        icon = Icons.Rounded.Lock,
                        label = "Cipher",
                        value = when (cipher) {
                            CipherSuite.CHACHA20_POLY1305 -> "ChaCha20-Poly1305"
                            CipherSuite.AES_256_GCM -> "AES-256-GCM"
                        }
                    )
                    SaveSummaryRow(
                        icon = Icons.Rounded.Key,
                        label = "Key",
                        value = if (keyLength == 64) "Valid (64 chars)" else "Invalid ($keyLength/64)",
                        isError = keyLength != 64
                    )
                }
            }

            // Warning if key invalid
            if (keyLength != 64) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.md),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Encryption key should be exactly 64 hex characters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Radius.md)
                ) {
                    Text("Cancel")
                }
                FilledTonalButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Radius.md)
                ) {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun SaveSummaryRow(
    icon: ImageVector,
    label: String,
    value: String,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.sm)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ConfigHero(connectionState: ConnectionState) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        )
    )

    val isConnected = connectionState == ConnectionState.CONNECTED
    val statusColor = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val statusText = when (connectionState) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.CONNECTING -> "Connecting"
        ConnectionState.DISCONNECTED -> "Ready to connect"
        ConnectionState.DISCONNECTING -> "Disconnecting"
        ConnectionState.ERROR -> "Error"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.xxl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(Spacing.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(statusColor.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(IconSize.lg)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text(
                            text = "Configuration",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ConfigBadge(text = "Private", color = MaterialTheme.colorScheme.primary)
                    ConfigBadge(text = "Curated", color = MaterialTheme.colorScheme.secondary)
                }

                Text(
                    text = "Adjust keys, routes, and DNS with a layout inspired by Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConfigBadge(text: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(50)
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
private fun ConfigWarning() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(Radius.xl)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.errorContainer),
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            },
            headlineContent = {
                Text(
                    text = "Disconnect to edit",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            },
            supportingContent = {
                Text(
                    text = "VPN must be disconnected before changes can be saved.",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        )
    }
}

@Composable
private fun ConfigGroupCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun ConfigTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        shape = RoundedCornerShape(Radius.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    )
}

@Composable
private fun ConfigSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    icon: ImageVector
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CipherDropdown(
    selectedCipher: CipherSuite,
    onCipherSelected: (CipherSuite) -> Unit,
    enabled: Boolean
) {
    var cipherExpanded by remember { mutableStateOf(false) }

    val cipherText = when (selectedCipher) {
        CipherSuite.CHACHA20_POLY1305 -> "ChaCha20-Poly1305"
        CipherSuite.AES_256_GCM -> "AES-256-GCM"
    }

    ExposedDropdownMenuBox(
        expanded = cipherExpanded,
        onExpandedChange = { if (enabled) cipherExpanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = cipherText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Cipher") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cipherExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(Radius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        )
        ExposedDropdownMenu(
            expanded = cipherExpanded,
            onDismissRequest = { cipherExpanded = false },
            shape = RoundedCornerShape(Radius.md)
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ChaCha20-Poly1305", maxLines = 1)
                            Text(
                                "Fast and modern",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        if (selectedCipher == CipherSuite.CHACHA20_POLY1305) {
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                onClick = {
                    onCipherSelected(CipherSuite.CHACHA20_POLY1305)
                    cipherExpanded = false
                }
            )

            DropdownMenuItem(
                text = {
                    Column {
                        Text("AES-256-GCM", maxLines = 1)
                        Text(
                            "Hardware accelerated on many devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                onClick = {
                    onCipherSelected(CipherSuite.AES_256_GCM)
                    cipherExpanded = false
                }
            )
        }
    }
}
