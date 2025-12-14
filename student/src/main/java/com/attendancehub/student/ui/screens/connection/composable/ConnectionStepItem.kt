package com.attendancehub.student.ui.screens.connection.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionStepItem(
    icon: ImageVector,
    text: String,
    isCompleted: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = when {
                isCompleted -> Color(0xFF4CAF50)
                isActive -> Color(0xFF9C27B0)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isActive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isActive || isCompleted) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isCompleted -> Color(0xFF4CAF50)
                isActive -> Color(0xFF9C27B0)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}