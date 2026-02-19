package com.willitfit.ui.placement

import android.Manifest
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.willitfit.data.model.Product
import com.willitfit.ui.measurement.components.TrackingQualityBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualPlacementScreen(
    product: Product,
    onBack: () -> Unit,
    viewModel: PlacementViewModel = viewModel()
) {
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
        viewModel.initialize(product)
    }

    val trackingState by viewModel.trackingState.collectAsState()
    val hasPlacedProduct by viewModel.hasPlacedProduct.collectAsState()
    val isOnWall by viewModel.isOnWall.collectAsState()

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
                title = { Text("Virtual Placement") },
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

                // Tracking badge
                TrackingQualityBadge(
                    trackingState = trackingState,
                    confidence = if (trackingState == TrackingState.TRACKING) 1.0 else 0.0,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )

                // Bottom panel
                BottomPanel(
                    product = product,
                    hasPlacedProduct = hasPlacedProduct,
                    isOnWall = isOnWall,
                    onRotateLeft = { viewModel.rotateLeft() },
                    onRotateRight = { viewModel.rotateRight() },
                    onRemove = { viewModel.removeProduct() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                Text(
                    text = "Camera permission is required for AR placement",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BottomPanel(
    product: Product,
    hasPlacedProduct: Boolean,
    isOnWall: Boolean,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = product.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = product.dimensionsText,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (hasPlacedProduct) {
                if (isOnWall) {
                    Text(
                        text = "Mounted on wall",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRotateLeft,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rotate Left",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rotate", color = Color.White)
                    }

                    Button(
                        onClick = onRemove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap to move the product",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = if (product.canMountOnWall) {
                        "Tap on floor or wall to place"
                    } else {
                        "Tap on floor to place"
                    },
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
