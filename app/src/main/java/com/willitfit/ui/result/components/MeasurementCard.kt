package com.willitfit.ui.result.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willitfit.data.model.ConfidenceLevel
import com.willitfit.data.model.MeasurementResult
import com.willitfit.ui.theme.ConfidenceHigh
import com.willitfit.ui.theme.ConfidenceLow
import com.willitfit.ui.theme.ConfidenceMedium

@Composable
fun MeasurementCard(
    label: String,
    result: MeasurementResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.formattedValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = result.formattedUncertainty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ConfidenceBadge(level = result.confidenceLevel)
        }
    }
}

@Composable
private fun ConfidenceBadge(
    level: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, text) = when (level) {
        ConfidenceLevel.HIGH -> ConfidenceHigh to "High"
        ConfidenceLevel.MEDIUM -> ConfidenceMedium to "Medium"
        ConfidenceLevel.LOW -> ConfidenceLow to "Low"
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
