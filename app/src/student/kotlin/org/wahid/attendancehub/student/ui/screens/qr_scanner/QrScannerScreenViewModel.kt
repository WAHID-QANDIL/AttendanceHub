package org.wahid.attendancehub.student.ui.screens.qr_scanner

import android.app.Application
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import org.wahid.attendancehub.base.BaseViewModel
import org.wahid.attendancehub.models.QRData
import java.util.concurrent.Executor

@OptIn(InternalSerializationApi::class)
class QrScannerScreenViewModel(application: Application) :
    BaseViewModel<QrScannerScreenUiState, QrScannerEffect>(initialState = QrScannerScreenUiState.ActiveScan),
    QrScannerScreenInteractionListener {
    private val TAG = "QrScannerScreenViewModel"

    @OptIn(InternalSerializationApi::class)
    override fun onQrCodeScanned(qrCode: QRData) {
        validateAndNavigate(qrCode)
    }

    fun cancelQrScanning() {
        updateState {
            QrScannerScreenUiState.Idle
        }
    }

    fun navigateHome() {
        sendEffect(
            QrScannerEffect.NavigateBackHome
        )
    }

    private fun validateAndNavigate(qrData: QRData) {
        Log.d(TAG, "=== QR Code Validation Started ===")
        Log.d(TAG, "QR Code scanned - SSID: ${qrData.ssid}, IP: ${qrData.serverIp}, Port: ${qrData.port}")
        Log.d(TAG, "Session ID: ${qrData.sessionId}")
        Log.d(TAG, "Expiry: ${qrData.expiryTimestamp}")

        // Update state to validating
        updateState {
            QrScannerScreenUiState.Validating
        }

        // Validate QR data
        if (qrData.ssid.isEmpty() || qrData.password.isEmpty()) {
            Log.e(TAG, "QR validation failed - empty SSID or password")
            updateState {
                QrScannerScreenUiState.Error("Invalid QR code data")
            }
            return
        }

        // Check if session is expired
        qrData.expiryTimestamp?.let { expiry ->
            if (System.currentTimeMillis() > expiry) {
                Log.e(TAG, "QR session expired - current: ${System.currentTimeMillis()}, expiry: $expiry")
                updateState {
                    QrScannerScreenUiState.Error("Session has expired. Ask teacher to generate a new QR code.")
                }
                return
            } else {
                Log.d(TAG, "Session is valid - ${(expiry - System.currentTimeMillis()) / 1000 / 60} minutes remaining")
            }
        }

        // QR is valid, navigate to connecting screen
        Log.d(TAG, "QR validated successfully, navigating to connecting screen")
        sendEffect(
            QrScannerEffect.NavigateToConnecting(qrData)
        )
    }


    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    @OptIn(InternalSerializationApi::class)
    fun cameraPreviewListenerStub(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        cameraExecutor: Executor,
        onQRCodeScanned: (QRData) -> Unit,

        ) {


        Log.d("QRScanner", "Camera provider listener triggered")
        val cameraProvider = cameraProviderFuture.get()
        Log.d("QRScanner", "Camera provider obtained successfully")

        // Preview
        Log.d("QRScanner", "Building camera preview")
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
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
                                        execute(
                                            onError = {
                                                Log.e(
                                                    "QRScanner",
                                                    "Error handling scanned QR code: ${it.message}"
                                                )
                                                onError("Invalid QR code: ${it.message}")
                                            }
                                        ) {
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
                                            if (qrContent.trim().startsWith("{")) {
                                                throw Exception("Invalid QR code format")
                                            } else {
                                                Log.d(
                                                    "QRScanner",
                                                    "Ignoring non-JSON QR code"
                                                )
                                            }
                                            if (qrData.ssid.isNotEmpty() && qrData.password.isNotEmpty()) {
                                                Log.d(
                                                    "QRScanner",
                                                    "QR validated successfully, calling onQRCodeScanned"
                                                )
                                                onQRCodeScanned(qrData)
                                            } else {
                                                throw Exception("SSID or password empty")
                                            }
                                        }

                                    }
                                }
                            }.addOnFailureListener {
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

            Log.d(
                "QRScanner",
                "Camera bound successfully, hasFlash=${camera.cameraInfo.hasFlashUnit()}"
            )

        } catch (e: Exception) {
            Log.e("QRScanner", "Camera binding failed", e)
            onError("Failed to start camera: ${e.message}")
        }
    }

    private fun onError(message: String) {
        Log.e(TAG, "Camera error: $message")
        updateState {
            QrScannerScreenUiState.Error(message)
        }
    }


}