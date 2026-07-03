package dev.yaul.twocha.ui.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.yaul.twocha.config.ConfigParser
import dev.yaul.twocha.ui.scan.QrImageAnalyzer
import dev.yaul.twocha.ui.theme.IconSize
import dev.yaul.twocha.ui.theme.Radius
import dev.yaul.twocha.ui.theme.Spacing
import dev.yaul.twocha.viewmodel.VpnViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * QR config import: CameraX preview + [QrImageAnalyzer] (ZXing) in a fully
 * themed Compose screen. A decoded payload is parse-validated against the
 * app's config schema before being handed to [VpnViewModel.importConfig];
 * invalid codes show a snackbar and scanning resumes.
 *
 * The QR is produced by the server wizard (`2cha setup` / `2cha init server`)
 * and never contains a private key — this device keeps its own identity, so
 * its public key (Config screen) must be authorized on the server.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanConfigScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text("Scan config", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Point at the QR from 2cha setup",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (cameraPermission.status.isGranted) {
                ScannerView(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onImported = onNavigateBack
                )
            } else {
                CameraPermissionRationale(onRequest = { cameraPermission.launchPermissionRequest() })
            }
        }
    }
}

@Composable
private fun ScannerView(
    viewModel: VpnViewModel,
    snackbarHostState: SnackbarHostState,
    onImported: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val analyzer = remember {
        QrImageAnalyzer { text ->
            // Called from the analysis executor; hop onto the UI scope.
            scope.launch {
                val parsed = runCatching { ConfigParser.parseJson(text.trim()) }.getOrNull()
                val errors = parsed?.validate()
                if (parsed != null && errors.orEmpty().isEmpty()) {
                    viewModel.importConfig(text.trim())
                    onImported()
                } else {
                    snackbarHostState.showSnackbar(
                        errors?.firstOrNull()?.let { "Invalid config: $it" }
                            ?: "Not a 2cha config QR"
                    )
                    // resume after the operator moves the camera
                }
            }
        }
    }

    // Resume scanning once the snackbar for a rejected code is on screen
    LaunchedEffect(snackbarHostState.currentSnackbarData) {
        if (snackbarHostState.currentSnackbarData != null) {
            analyzer.reset()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, analyzer) }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Themed scan frame + hint, drawn by us so it matches the app style
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(Radius.lg)
                    )
            )
            Surface(
                shape = RoundedCornerShape(Radius.full),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "Server side: sudo 2cha setup shows this QR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = Spacing.md,
                        vertical = Spacing.xs
                    )
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionRationale(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.QrCodeScanner,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(IconSize.xl)
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "Camera access needed",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "The camera is only used to read the config QR code — nothing is recorded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Button(onClick = onRequest) {
            Text("Grant camera access")
        }
    }
}
