package com.attendancehub.student.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ConnectingScreen(
    networkName: String,
    currentStep: ConnectionStep = ConnectionStep.NETWORK_FOUND
) {
    // Rotation animation for loading icon
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        // Animated WiFi Icon
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = Color(0xFF9C27B0).copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .rotate(rotation),
                    tint = Color(0xFF9C27B0)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Connecting...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Establishing connection to\n$networkName",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Info message for Authenticating step
        if (currentStep == ConnectionStep.AUTHENTICATING) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Please approve the WiFi connection request when the system dialog appears.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Connection Steps
        ConnectionStepItem(
            icon = Icons.Default.Wifi,
            text = "Network found",
            isCompleted = currentStep.ordinal >= ConnectionStep.NETWORK_FOUND.ordinal,
            isActive = currentStep == ConnectionStep.NETWORK_FOUND
        )

        Spacer(modifier = Modifier.height(16.dp))

        ConnectionStepItem(
            icon = Icons.Default.Lock,
            text = "Authenticating...",
            isCompleted = currentStep.ordinal >= ConnectionStep.AUTHENTICATING.ordinal,
            isActive = currentStep == ConnectionStep.AUTHENTICATING
        )

        Spacer(modifier = Modifier.height(16.dp))

        ConnectionStepItem(
            icon = Icons.Default.PersonAdd,
            text = "Registering attendance",
            isCompleted = currentStep.ordinal >= ConnectionStep.REGISTERING.ordinal,
            isActive = currentStep == ConnectionStep.REGISTERING
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ConnectionStepItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isActive || isCompleted) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isCompleted -> Color(0xFF4CAF50)
                isActive -> Color(0xFF9C27B0)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

enum class ConnectionStep {
    NETWORK_FOUND,
    AUTHENTICATING,
    REGISTERING
}

