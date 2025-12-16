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
import androidx.compose.material.icons.filled.Edit
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.InternalSerializationApi
import org.koin.androidx.compose.koinViewModel
import org.wahid.attendancehub.models.QRData
import org.wahid.attendancehub.student.navigation.StudentScreen
import org.wahid.attendancehub.student.ui.screens.core.StudentInfoBottomSheet
import org.wahid.attendancehub.utils.ObserveAsEffect
import java.util.concurrent.Executors


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QRScannerScreen(
    navController: NavController,
    qrScannerScreenViewModel: QrScannerScreenViewModel = koinViewModel<QrScannerScreenViewModel>()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Student info management
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { org.wahid.attendancehub.core.SharedPrefs.getInstance(context) }
    val firestName by remember { sharedPrefs.firstName }.collectAsStateWithLifecycle()
    val lastName by remember { sharedPrefs.lastName }.collectAsStateWithLifecycle()
    val studentId by remember { sharedPrefs.studentId }.collectAsStateWithLifecycle()

    var showStudentInfoSheet by remember { mutableStateOf(false) }
    var pendingQrData by remember { mutableStateOf<QRData?>(null) }

    DisposableEffect(Unit) {
        Log.d("QRScanner", "QRScannerScreen DisposableEffect started")
        onDispose {
            Log.d("QRScanner", "QRScannerScreen disposing, shutting down camera executor")
            cameraExecutor.shutdown()
        }
    }

    val state = qrScannerScreenViewModel.state.collectAsStateWithLifecycle()

    // Handle navigation effects
    ObserveAsEffect(qrScannerScreenViewModel.effect) { effect ->
        when (effect) {
            is QrScannerEffect.NavigateToConnecting -> {
                // Check if student info exists
                if (firestName.isBlank() || lastName.isBlank() || studentId.isBlank()) {
                    // Show bottom sheet to collect info
                    pendingQrData = effect.qrData
                    showStudentInfoSheet = true
                } else {
                    // Info exists, proceed with connection
                    navController.navigate(
                        StudentScreen.Connecting.createRouteWithQrData(effect.qrData)
                    ) {
                        popUpTo(StudentScreen.QRScanner.route) { inclusive = true }
                    }
                }
            }
            is QrScannerEffect.NavigateBackHome -> {
                navController.navigate(StudentScreen.NetworkScan.route) {
                    popUpTo(StudentScreen.NetworkScan.route) { inclusive = true }
                }
            }
        }
    }

    // Student Info Bottom Sheet
    if (showStudentInfoSheet) {
        StudentInfoBottomSheet(
            onDismiss = {
                showStudentInfoSheet = false
                pendingQrData = null
            },
            onInfoSaved = { firstName, lastName, id ->
                val deviceId = sharedPrefs.deviceId.value
                sharedPrefs.saveStudentInfo(firstName, lastName, id, deviceId)
                showStudentInfoSheet = false

                // Proceed with connection after saving info
                pendingQrData?.let { qrData ->
                    navController.navigate(
                        StudentScreen.Connecting.createRouteWithQrData(qrData)
                    ) {
                        popUpTo(StudentScreen.QRScanner.route) { inclusive = true }
                    }
                }
                pendingQrData = null
            },
            existingFirstName = firestName,
            existingLastName = lastName,
            existingStudentId = studentId
        )
    }

    when(val stateValue = state.value){
        QrScannerScreenUiState.ActiveScan -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                   lifecycleOwner = lifecycleOwner,
                    cameraExecutor = cameraExecutor,
                    viewModel = qrScannerScreenViewModel,
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
                            onClick = {
                                cameraExecutor.shutdown()
                                qrScannerScreenViewModel.navigateHome()
                            },
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

                        // Edit student info button
                        if (firestName.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    showStudentInfoSheet = true
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit student info"
                                )
                            }
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

        QrScannerScreenUiState.Validating -> {
            // Show validating indicator
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
                            text = "Validating QR Code...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        is QrScannerScreenUiState.Error -> {
            val error = stateValue.message

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
                        onClick = {
                            cameraExecutor.shutdown()
                            qrScannerScreenViewModel.navigateHome()
                        },
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
        navController = rememberNavController()
    )
}