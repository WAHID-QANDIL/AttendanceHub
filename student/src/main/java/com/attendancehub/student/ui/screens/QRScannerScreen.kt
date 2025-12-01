package com.attendancehub.student.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.attendancehub.models.QRData
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.serialization.json.Json
import java.util.concurrent.Executors

@Composable
fun QRScannerScreen(
    onQRCodeScanned: (QRData) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasFlash by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        Log.d("QRScanner", "QRScannerScreen DisposableEffect started")
        onDispose {
            Log.d("QRScanner", "QRScannerScreen disposing, shutting down camera executor")
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                Log.d("QRScanner", "AndroidView factory started - initializing camera")
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    try {
                        Log.d("QRScanner", "Camera provider listener triggered")
                        val cameraProvider = cameraProviderFuture.get()
                        Log.d("QRScanner", "Camera provider obtained successfully")

                        Log.d("QRScanner", "Camera provider obtained successfully")

                        // Preview
                        Log.d("QRScanner", "Building camera preview")
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        Log.d("QRScanner", "Preview configured")

                        // Image Analysis for QR scanning
                        Log.d("QRScanner", "Building image analyzer for QR detection")
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (!isScanning) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }

                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )

                                        val scanner = BarcodeScanning.getClient()
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    // Accept ANY barcode type - just get the raw value
                                                    // ML Kit may detect JSON QR codes as TYPE_URL (9) instead of TYPE_TEXT
                                                    barcode.rawValue?.let { qrContent ->
                                                        try {
                                                            Log.d("QRScanner", "QR Code detected (type=${barcode.valueType}): $qrContent")

                                                            // Try to parse as JSON QRData
                                                            val qrData = Json.decodeFromString<QRData>(qrContent)

                                                            Log.d("QRScanner", "QR parsed - SSID: ${qrData.ssid}, Password: ${qrData.password}")

                                                            // Validate QR data
                                                            if (qrData.ssid.isNotEmpty() && qrData.password.isNotEmpty()) {
                                                                Log.d("QRScanner", "QR validated successfully, calling onQRCodeScanned")
                                                                isScanning = false
                                                                successMessage = "QR Code detected! Connecting..."
                                                                onQRCodeScanned(qrData)
                                                            } else {
                                                                Log.w("QRScanner", "QR data validation failed - SSID or password empty")
                                                                errorMessage = "Invalid QR data - missing network info"
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("QRScanner", "Failed to parse QR code: ${e.message}", e)
                                                            // Only show error if we can't parse as JSON (ignore non-JSON QR codes)
                                                            if (qrContent.trim().startsWith("{")) {
                                                                errorMessage = "Invalid QR code format: ${e.message}"
                                                            } else {
                                                                Log.d("QRScanner", "Ignoring non-JSON QR code")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnFailureListener {
                                                Log.e("QRScanner", "Failed to scan", it)
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        // Camera selector
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        Log.d("QRScanner", "Camera selector configured for back camera")

                        try {
                            Log.d("QRScanner", "Unbinding all camera use cases")
                            cameraProvider.unbindAll()

                            Log.d("QRScanner", "Binding camera lifecycle with preview and analyzer")
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer
                            )

                            hasFlash = camera.cameraInfo.hasFlashUnit()
                            Log.d("QRScanner", "Camera bound successfully, hasFlash=$hasFlash")

                        } catch (e: Exception) {
                            Log.e("QRScanner", "Camera binding failed", e)
                            errorMessage = "Failed to start camera: ${e.message}"
                        }

                    } catch (e: Exception) {
                        Log.e("QRScanner", "Camera provider failed", e)
                        errorMessage = "Camera initialization failed: ${e.message}"
                    }
                }, ContextCompat.getMainExecutor(ctx))

                Log.d("QRScanner", "Returning preview view from factory")
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Bar
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

                if (hasFlash) {
                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Flash"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Instructions Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan Teacher's QR Code",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Position the QR code within the frame to connect automatically",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Scanning frame overlay (optional - visual guide)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(250.dp)
                    .aspectRatio(1f),
                color = Color.Transparent,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(4.dp, Color.White.copy(alpha = 0.8f))
            ) {}
        }

        // Error Message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .padding(horizontal = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Success Message
        successMessage?.let { success ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .padding(horizontal = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = success,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
