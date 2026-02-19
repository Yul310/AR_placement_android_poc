package com.willitfit.ui.result

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willitfit.data.model.DoorMeasurement
import com.willitfit.data.model.MeasurementMode
import com.willitfit.data.model.MeasurementResult
import com.willitfit.data.model.Product
import com.willitfit.data.model.SpaceMeasurement
import com.willitfit.data.model.Verdict
import com.willitfit.ui.result.components.MeasurementCard
import com.willitfit.ui.result.components.VerdictCard
import com.willitfit.ui.theme.VerdictFail
import com.willitfit.ui.theme.VerdictNotSure
import com.willitfit.ui.theme.VerdictPass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    product: Product,
    mode: MeasurementMode,
    verdict: Verdict?,
    doorMeasurement: DoorMeasurement?,
    spaceMeasurement: SpaceMeasurement?,
    onMeasureAgain: () -> Unit,
    onDone: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Verdict card
            verdict?.let {
                VerdictCard(verdict = it)
            } ?: run {
                Text(
                    text = "Measurement Complete",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Product info
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = product.dimensionsText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Measurements
            Text(
                text = "Measurements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (mode) {
                MeasurementMode.DOOR -> {
                    doorMeasurement?.let { door ->
                        door.width?.let { MeasurementCard(label = "Width", result = it) }
                        Spacer(modifier = Modifier.height(8.dp))
                        door.height?.let { MeasurementCard(label = "Height", result = it) }
                    }
                }
                MeasurementMode.SPACE -> {
                    spaceMeasurement?.let { space ->
                        space.width?.let { MeasurementCard(label = "Width", result = it) }
                        Spacer(modifier = Modifier.height(8.dp))
                        space.depth?.let { MeasurementCard(label = "Depth", result = it) }
                        Spacer(modifier = Modifier.height(8.dp))
                        space.height?.let { MeasurementCard(label = "Height", result = it) }
                    }
                }
                MeasurementMode.VIRTUAL_PLACEMENT -> {
                    // No measurements for virtual placement
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onMeasureAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Measure Again")
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
