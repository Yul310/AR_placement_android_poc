package com.willitfit.ui.measurement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willitfit.data.model.MeasurementStep
import com.willitfit.data.model.Product
import com.willitfit.ui.theme.VerdictPass

@Composable
fun MeasurementGuide(
    step: MeasurementStep,
    product: Product,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(alpha = 0.8f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Product info
        Text(
            text = product.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = product.dimensionsText,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step indicator
        StepIndicator(step = step)

        Spacer(modifier = Modifier.height(12.dp))

        // Instruction
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step is MeasurementStep.Complete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = VerdictPass
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = step.instruction,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (step !is MeasurementStep.Complete) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the screen to measure",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StepIndicator(step: MeasurementStep) {
    val (currentIndex, totalSteps) = when (step) {
        is MeasurementStep.DoorBottomLeft -> 1 to 3
        is MeasurementStep.DoorBottomRight -> 2 to 3
        is MeasurementStep.DoorTopLeft -> 3 to 3
        is MeasurementStep.SpaceWidthLeft -> 1 to 6
        is MeasurementStep.SpaceWidthRight -> 2 to 6
        is MeasurementStep.SpaceDepthFront -> 3 to 6
        is MeasurementStep.SpaceDepthBack -> 4 to 6
        is MeasurementStep.SpaceHeightFloor -> 5 to 6
        is MeasurementStep.SpaceHeightCeiling -> 6 to 6
        is MeasurementStep.Complete -> 0 to 0
    }

    if (totalSteps > 0) {
        Row {
            repeat(totalSteps) { index ->
                val isComplete = index < currentIndex
                val isCurrent = index == currentIndex - 1

                DotIndicator(
                    isComplete = isComplete,
                    isCurrent = isCurrent
                )

                if (index < totalSteps - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DotIndicator(
    isComplete: Boolean,
    isCurrent: Boolean
) {
    val color = when {
        isComplete -> VerdictPass
        isCurrent -> Color.White
        else -> Color.White.copy(alpha = 0.3f)
    }

    val size = if (isCurrent) 12.dp else 8.dp

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .width(size)
            .height(size)
    ) {
        drawCircle(color = color)
    }
}
