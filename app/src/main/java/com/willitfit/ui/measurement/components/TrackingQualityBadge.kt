package com.willitfit.ui.measurement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.TrackingState
import com.willitfit.ui.theme.ConfidenceHigh
import com.willitfit.ui.theme.ConfidenceLow
import com.willitfit.ui.theme.ConfidenceMedium

@Composable
fun TrackingQualityBadge(
    trackingState: TrackingState,
    confidence: Double,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, icon, text) = when {
        trackingState != TrackingState.TRACKING -> {
            BadgeData(
                backgroundColor = ConfidenceLow.copy(alpha = 0.9f),
                textColor = Color.White,
                icon = Icons.Default.Warning,
                text = "Not Tracking"
            )
        }
        confidence >= 0.8 -> {
            BadgeData(
                backgroundColor = ConfidenceHigh.copy(alpha = 0.9f),
                textColor = Color.White,
                icon = Icons.Default.CheckCircle,
                text = "High"
            )
        }
        confidence >= 0.6 -> {
            BadgeData(
                backgroundColor = ConfidenceMedium.copy(alpha = 0.9f),
                textColor = Color.White,
                icon = Icons.Default.CheckCircle,
                text = "Medium"
            )
        }
        else -> {
            BadgeData(
                backgroundColor = ConfidenceLow.copy(alpha = 0.9f),
                textColor = Color.White,
                icon = Icons.Default.Warning,
                text = "Low"
            )
        }
    }

    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class BadgeData(
    val backgroundColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String
)
