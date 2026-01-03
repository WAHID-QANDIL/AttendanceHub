package com.attendancehub.student.ui.screens.connection

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendancehub.composables.Logo
import com.attendancehub.student.R
import com.attendancehub.student.navigation.LocalNavController
import com.attendancehub.student.navigation.StudentScreen
import com.attendancehub.student.ui.screens.connection.composable.ConnectionStepItem
import com.attendancehub.student.ui.model.ConnectionStep
import com.attendancehub.student.ui.ui_effect.StudentEffect
import com.attendancehub.utils.ObserveAsEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun ConnectingScreen(
    networkName: String,
    currentStep: ConnectionStep = ConnectionStep.NETWORK_FOUND,
    viewModel: ConnectionViewModel = koinViewModel()
) {
    val navController = LocalNavController.current

    ObserveAsEffect(viewModel.effect) { effect ->
        when (effect) {
            is ConnectionEffect.NavigateToSuccessScreen -> {
                if (navController.currentDestination?.route != StudentScreen.Success.route) {
                    navController.navigate(StudentScreen.Success.route) {
                        popUpTo(StudentScreen.NetworkScan.route) { inclusive = true }
                    }
                }
            }
        }
    }

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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Logo(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = Color(0xFF9C27B0).copy(alpha = 0.1f),
            icon = Icons.Default.Wifi,
            iconTint = Color(0xFF9C27B0),
            iconModifier = Modifier
                .size(60.dp)
                .rotate(rotation),
        )

        Text(
            text = stringResource(R.string.connecting_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp)
        )

        Text(
            text = stringResource(R.string.establishing_connection_fmt, networkName),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        // Info message for Authenticating step
        if (currentStep == ConnectionStep.AUTHENTICATING) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
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
                    Text(
                        text = stringResource(R.string.authenticating_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        // Connection Steps
        ConnectionStepItem(
            icon = Icons.Default.Wifi,
            text = stringResource(R.string.step_network_found),
            isCompleted = currentStep.ordinal >= ConnectionStep.NETWORK_FOUND.ordinal,
            isActive = currentStep == ConnectionStep.NETWORK_FOUND,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ConnectionStepItem(
            icon = Icons.Default.Lock,
            text = stringResource(R.string.step_authenticating),
            isCompleted = currentStep.ordinal >= ConnectionStep.AUTHENTICATING.ordinal,
            isActive = currentStep == ConnectionStep.AUTHENTICATING,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ConnectionStepItem(
            icon = Icons.Default.PersonAdd,
            text = stringResource(R.string.step_registering),
            isCompleted = currentStep.ordinal >= ConnectionStep.REGISTERING.ordinal,
            isActive = currentStep == ConnectionStep.REGISTERING,
        )
    }
}

@Preview
@Composable
fun ConnectingScreenPreview() {
    ConnectingScreen(
        networkName = "Example Network",
        currentStep = ConnectionStep.AUTHENTICATING,
    )
}