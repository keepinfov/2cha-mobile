package dev.yaul.twocha.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.yaul.twocha.viewmodel.LogItem
import dev.yaul.twocha.viewmodel.LogLevel
import dev.yaul.twocha.viewmodel.VpnViewModel
import kotlinx.coroutines.launch

/**
 * Get UI color for a log level
 */
private fun LogLevel.getColor(): Color = when (this) {
    LogLevel.DEBUG -> Color(0xFF7EE787)    // Green
    LogLevel.INFO -> Color(0xFF58A6FF)     // Blue
    LogLevel.WARN -> Color(0xFFD29922)     // Orange
    LogLevel.ERROR -> Color(0xFFFF7B72)    // Red
    LogLevel.VERBOSE -> Color(0xFF8B949E)  // Gray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit
) {
    val logs by viewModel.logs.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var filterLevel by remember { mutableStateOf<LogLevel?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size, autoScroll) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val filteredLogs = remember(logs, filterLevel) {
        if (filterLevel == null) logs
        else logs.filter { it.level == filterLevel }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Filter button
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Badge(
                                modifier = Modifier.offset(x = 8.dp, y = (-8).dp),
                                containerColor = if (filterLevel != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Transparent
                            ) {
                                if (filterLevel != null) {
                                    Text("1", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Logs") },
                                onClick = {
                                    filterLevel = null
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (filterLevel == null) {
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                    }
                                }
                            )
                            HorizontalDivider()
                            LogLevel.entries.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            level.name,
                                            color = level.getColor()
                                        )
                                    },
                                    onClick = {
                                        filterLevel = level
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        if (filterLevel == level) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Copy all logs
                    IconButton(
                        onClick = {
                            val text = filteredLogs.joinToString("\n") { log ->
                                "${log.timestamp} [${log.level}] ${log.message}"
                            }
                            clipboardManager.setText(AnnotatedString(text))
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy All")
                    }

                    // Clear logs
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto-scroll toggle
                SmallFloatingActionButton(
                    onClick = { autoScroll = !autoScroll },
                    containerColor = if (autoScroll)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        if (autoScroll) Icons.Filled.VerticalAlignBottom else Icons.Filled.PauseCircle,
                        contentDescription = if (autoScroll) "Auto-scroll on" else "Auto-scroll off"
                    )
                }

                // Scroll to bottom
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (filteredLogs.isNotEmpty()) {
                                listState.animateScrollToItem(filteredLogs.size - 1)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Scroll to bottom")
                }
            }
        }
    ) { paddingValues ->
        if (filteredLogs.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        if (filterLevel != null) "No ${filterLevel?.name?.lowercase()} logs"
                        else "No logs yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Logs will appear here when you connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = filteredLogs,
                    key = { it.id }
                ) { log ->
                    LogEntryCard(
                        log = log,
                        onCopy = {
                            clipboardManager.setText(
                                AnnotatedString("${log.timestamp} [${log.level}] ${log.message}")
                            )
                        }
                    )
                }

                // Spacer for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogEntryCard(
    log: LogItem,
    onCopy: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val color = log.level.getColor()

    Card(
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        log.level.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Timestamp
                Text(
                    log.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message
            Text(
                log.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )

            // Expanded actions
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCopy,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
