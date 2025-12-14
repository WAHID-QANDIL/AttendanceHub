@file:OptIn(InternalSerializationApi::class)

package org.wahid.attendancehub.student.ui.screens.qr_scanner

import org.wahid.attendancehub.R
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.InternalSerializationApi
import org.koin.androidx.compose.koinViewModel
import org.wahid.attendancehub.student.ui.screens.attendanceSuccess.AttendanceSuccessScreen
import org.wahid.attendancehub.utils.ObserveAsEffect
import java.util.concurrent.Executors


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QRScannerScreen(
    onClose: () -> Unit,
    navController: NavController,
    qrScannerScreenViewModel: QrScannerScreenViewModel = koinViewModel<QrScannerScreenViewModel>()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        Log.d("QRScanner", "QRScannerScreen DisposableEffect started")
        onDispose {
            Log.d("QRScanner", "QRScannerScreen disposing, shutting down camera executor")
            cameraExecutor.shutdown()
        }
    }

    val state = qrScannerScreenViewModel.state.collectAsState()

    // Handle navigation effects
    ObserveAsEffect(qrScannerScreenViewModel.effect) { effect ->
        when (effect) {
            is QrScannerEffect.NavigateToAttendanceSuccess -> {
                // Navigate to the success screen - composable directly or through navigation
                // Since we're using Jetpack Compose navigation, we can show the success screen directly
                // by updating a local state, or navigate using navController
                // For now, we'll just close and let the parent ViewModel handle the navigation
                // In a more complete implementation, we could navigate directly here
                onClose()
            }
        }
    }

    when(state.value){
        QrScannerScreenUiState.ActiveScan -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                   lifecycleOwner = lifecycleOwner,
                    cameraExecutor = cameraExecutor,
                    viewModel = qrScannerScreenViewModel,
//                    onQRCodeScanned,
//                    onFlashDetected = { hasFlash = it },
//                    onError = { qrScannerScreenViewModel. = it },
//                    onSuccess = { successMessage = it },
//                    isScanning = { isScanning = it }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.5f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .padding(20.dp)
                            .padding(vertical = 16.dp),

                        ) {
                        // Scanning frame overlay (optional - visual guide)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(250.dp)
                                    .aspectRatio(1f),
                                color = Color.Transparent,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(
                                    4.dp,
                                    Color.Green.copy(alpha = 0.8f)
                                )
                            ) {}
                        }
                        Text(
                            text = stringResource(R.string.scan_qr_code),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.align_the_qr_code_within_the_frame_to_scan_your_attendance),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        is QrScannerScreenUiState.Connected -> {
            val connected = state.value as QrScannerScreenUiState.Connected

            // Show the AttendanceSuccessScreen directly
            AttendanceSuccessScreen(
                networkName = connected.networkName,
                markedAtTime = connected.markedAtTime,
                navController = navController
            )
        }

        is QrScannerScreenUiState.Connecting -> {
            val connecting = state.value as QrScannerScreenUiState.Connecting

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.connecting),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = connecting.networkName,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = connecting.currentStep.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is QrScannerScreenUiState.Error -> {
            val error = (state.value as QrScannerScreenUiState.Error).message

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.error),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = stringResource(R.string.close))
                    }
                }
            }
        }

        QrScannerScreenUiState.Idle -> {
            // Initial idle state - could show a loading indicator
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun QRScannerScreenPreview() {
    QRScannerScreen(
        onClose = {},
        navController = rememberNavController()
    )
}