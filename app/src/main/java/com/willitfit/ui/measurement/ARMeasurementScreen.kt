package com.willitfit.ui.measurement

import android.Manifest
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.willitfit.data.model.MeasurementMode
import com.willitfit.data.model.MeasurementStep
import com.willitfit.data.model.Product
import com.willitfit.data.model.SamplingState
import com.willitfit.data.model.Verdict
import com.willitfit.ui.measurement.components.MeasurementGuide
import com.willitfit.ui.measurement.components.SamplingOverlay
import com.willitfit.ui.measurement.components.TrackingQualityBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARMeasurementScreen(
    product: Product,
    mode: MeasurementMode,
    onComplete: (Verdict?) -> Unit,
    onBack: () -> Unit,
    viewModel: MeasurementViewModel = viewModel()
) {
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
        viewModel.initialize(product, mode)
    }

    val currentStep by viewModel.currentStep.collectAsState()
    val samplingState by viewModel.samplingState.collectAsState()
    val trackingState by viewModel.trackingState.collectAsState()
    val trackingConfidence by viewModel.trackingConfidence.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()
    val verdict by viewModel.verdict.collectAsState()

    // Navigate to result when complete
    LaunchedEffect(isComplete) {
        if (isComplete) {
            onComplete(verdict)
        }
    }

    // Lifecycle handling
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (mode) {
                            MeasurementMode.DOOR -> "Measure Door"
                            MeasurementMode.SPACE -> "Measure Space"
                            MeasurementMode.VIRTUAL_PLACEMENT -> "Virtual Placement"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (currentStep !is MeasurementStep.Complete) {
                FloatingActionButton(
                    onClick = { viewModel.resetMeasurement() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasPermission) {
                // AR Scene View
                AndroidView(
                    factory = { context ->
                        ArSceneView(context).apply {
                            viewModel.initializeAR(this)
                            setOnTouchListener { _, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    viewModel.handleTap(event.x, event.y)
                                }
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Tracking quality badge (top-right)
                TrackingQualityBadge(
                    trackingState = trackingState,
                    confidence = trackingConfidence,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )

                // Sampling overlay
                when (val state = samplingState) {
                    is SamplingState.Sampling -> {
                        SamplingOverlay(
                            progress = state.progress,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is SamplingState.Failed -> {
                        SamplingFailedOverlay(
                            reason = state.reason,
                            onRetry = { viewModel.retryCurrentPoint() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    SamplingState.Idle -> { /* No overlay */ }
                }

                // Bottom instruction panel
                MeasurementGuide(
                    step = currentStep,
                    product = product,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // No permission state
                Text(
                    text = "Camera permission is required for AR measurement",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SamplingFailedOverlay(
    reason: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sampling Failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                androidx.compose.material3.Button(onClick = onRetry) {
                    Text("Tap to Retry")
                }
            }
        }
    }
}
