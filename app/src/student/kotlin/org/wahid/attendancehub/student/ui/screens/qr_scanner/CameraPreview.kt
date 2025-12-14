package org.wahid.attendancehub.student.ui.screens.qr_scanner

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.wahid.attendancehub.models.QRData
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import java.util.concurrent.Executor

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(InternalSerializationApi::class)
@Composable
fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: Executor,
    onQRCodeScanned: (QRData) -> Unit,
    onFlashDetected: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit,
    isScanning: (Boolean) -> Unit
) {
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
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

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
                                                barcode.rawValue?.let { qrContent ->
                                                    try {
                                                        Log.d(
                                                            "QRScanner",
                                                            "QR Code detected (type=${barcode.valueType}): $qrContent"
                                                        )

                                                        val qrData =
                                                            Json.decodeFromString<QRData>(qrContent)

                                                        Log.d(
                                                            "QRScanner",
                                                            "QR parsed - SSID: ${qrData.ssid}, Password: ${qrData.password}"
                                                        )

                                                        // Validate QR data
                                                        if (qrData.ssid.isNotEmpty() && qrData.password.isNotEmpty()) {
                                                            Log.d(
                                                                "QRScanner",
                                                                "QR validated successfully, calling onQRCodeScanned"
                                                            )

                                                            isScanning(false) // stop scanning
                                                            onSuccess("QR Code detected! Connecting...")

                                                            onQRCodeScanned(qrData)

                                                        } else {
                                                            Log.w(
                                                                "QRScanner",
                                                                "QR data validation failed - SSID or password empty"
                                                            )
                                                            onError("Invalid QR data - missing network info")
                                                        }

                                                    } catch (e: Exception) {
                                                        Log.e(
                                                            "QRScanner",
                                                            "Failed to parse QR code: ${e.message}",
                                                            e
                                                        )

                                                        if (qrContent.trim().startsWith("{")) {
                                                            onError("Invalid QR code format: ${e.message}")
                                                        } else {
                                                            Log.d(
                                                                "QRScanner",
                                                                "Ignoring non-JSON QR code"
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            Log.e("QRScanner", "Failed to scan", it)
                                            onError("Scan failed: ${it.message}")
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

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

                        onFlashDetected(camera.cameraInfo.hasFlashUnit())
                        Log.d(
                            "QRScanner",
                            "Camera bound successfully, hasFlash=${camera.cameraInfo.hasFlashUnit()}"
                        )

                    } catch (e: Exception) {
                        Log.e("QRScanner", "Camera binding failed", e)
                        onError("Failed to start camera: ${e.message}")
                    }

                } catch (e: Exception) {
                    Log.e("QRScanner", "Camera provider failed", e)
                    onError("Camera initialization failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            Log.d("QRScanner", "Returning preview view from factory")
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}