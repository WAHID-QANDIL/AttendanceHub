package org.wahid.attendancehub.student.ui.screens.qr_scanner

import android.app.Application
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.viewModelScope
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import org.wahid.attendancehub.api.AttendanceClient
import org.wahid.attendancehub.base.BaseViewModel
import org.wahid.attendancehub.core.SharedPrefs
import org.wahid.attendancehub.models.QRData
import org.wahid.attendancehub.models.ServerResponse
import org.wahid.attendancehub.models.StudentAttendance
import org.wahid.attendancehub.models.StudentInfo
import org.wahid.attendancehub.models.WifiNetwork
import org.wahid.attendancehub.network.StudentHotspotConnectionManager
import org.wahid.attendancehub.student.ui.screens.ConnectionStep
import java.util.UUID

class QrScannerScreenViewModel(application: Application) :
    BaseViewModel<QrScannerScreenUiState, QrScannerEffect>(initialState = QrScannerScreenUiState.Idle),
    QrScannerScreenInteractionListener {
    private val TAG = "QrScannerScreenViewModel"

    private val hotspotManager = StudentHotspotConnectionManager.getInstance(application)
    private val attendanceClient = AttendanceClient(context = application)

    //    private val wifiScanner = org.wahid.attendancehub.network.WiFiScanner(application)
    private var connectionStartTime: Long = 0
    private val prefs = SharedPrefs.getInstance(context = application)

    private val studentInfo = StudentInfo(
        firstName = prefs.firstName.value,
        lastName = prefs.lastName.value,
        studentId = prefs.studentId.value,
    )


    @OptIn(InternalSerializationApi::class)
    override fun onQrCodeScanned(qrCode: QRData) {
        handleQRCode(qrData = qrCode)
    }


    @OptIn(InternalSerializationApi::class)
    private fun handleQRCode(qrData: QRData) {
        viewModelScope.launch {
            Log.d(TAG, "=== QR Code Handler Started ===")
            Log.d(
                TAG,
                "QR Code scanned - SSID: ${qrData.ssid}, IP: ${qrData.serverIp}, Port: ${qrData.port}"
            )
            Log.d(TAG, "Session ID: ${qrData.sessionId}")
            Log.d(TAG, "Expiry: ${qrData.expiryTimestamp}")

            // Validate QR data
            if (qrData.ssid.isEmpty() || qrData.password.isEmpty()) {
                Log.e(TAG, "QR validation failed - empty SSID or password")
                updateState {
                    QrScannerScreenUiState.Error("Invalid QR code data")
                }
                return@launch
            }

            // Check if session is expired
            qrData.expiryTimestamp?.let { expiry ->
                if (System.currentTimeMillis() > expiry) {
                    Log.e(
                        TAG,
                        "QR session expired - current: ${System.currentTimeMillis()}, expiry: $expiry"
                    )
                    updateState {
                        QrScannerScreenUiState.Error("Session has expired. Ask teacher to generate a new QR code.")
                    }
                    return@launch
                } else {
                    Log.d(
                        TAG,
                        "Session is valid - ${(expiry - System.currentTimeMillis()) / 1000 / 60} minutes remaining"
                    )
                }
            }

            // Create network object from QR data
            Log.d(TAG, "Creating network object from QR data")
            val network = WifiNetwork(
                ssid = qrData.ssid,
                signalStrength = 5,
                isSecured = true,
                isTeacherNetwork = true
            )

            // Connect using QR data
            Log.d(TAG, "Calling connectToNetwork with QR data")


            execute(
                onSuccess = {
                    val markedTime = getCurrentTime()
                    updateState {
                        QrScannerScreenUiState.Connected(
                            networkName = network.ssid,
                            markedAtTime = markedTime
                        )
                    }
                    sendEffect(
                        effect = QrScannerEffect.NavigateToAttendanceSuccess(
                            networkName = network.ssid,
                            markedAtTime = markedTime
                        )
                    )
                },
                onError = { errorMsg ->
                    updateState {
                        QrScannerScreenUiState.Error(
                            errorMsg.message.toString()
                        )
                    }
                }
            ) {
                connectToNetwork(network, qrData)
            }

        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun connectToNetwork(network: WifiNetwork, qrData: QRData? = null) {
        viewModelScope.launch {
            try {

                updateState {
                    QrScannerScreenUiState.Connecting(
                        networkName = network.ssid,
                        currentStep = ConnectionStep.NETWORK_FOUND
                    )
                }

                connectionStartTime = System.currentTimeMillis()

                // Step 1: Network found
                delay(500)

                // Step 2: Authenticating
                updateState {
                    QrScannerScreenUiState.Connecting(
                        networkName = network.ssid,
                        currentStep = ConnectionStep.AUTHENTICATING
                    )
                }

                // Connect to WiFi with timeout
                val password = qrData?.password ?: network.password
                Log.d(TAG, "Attempting to connect to: ${network.ssid}")

                val connectResult = withTimeoutOrNull(35000L) {
                    hotspotManager.connect(network.ssid, password)
                }

                if (connectResult == null) {
                    // Timeout occurred
                    Log.e(TAG, "Connection timeout")

                    updateState {
                        QrScannerScreenUiState.Error(
                            "Connection timeout. Please check if WiFi is enabled and try again."
                        )
                    }

//                    _uiState.value = StudentUiState.Error(
//                        "Connection timeout. Please check if WiFi is enabled and try again."
//                    )
                    return@launch
                }

                if (connectResult.isFailure) {
                    val errorMsg = connectResult.exceptionOrNull()?.message ?: "Failed to connect"
                    Log.e(TAG, "Connection failed: $errorMsg")
                    updateState {
                        QrScannerScreenUiState.Error(
                            "Failed to connect: $errorMsg"
                        )
                    }


//                    _uiState.value = StudentUiState.Error(
//                        "Failed to connect: $errorMsg"
//                    )
                    return@launch
                }

                Log.d(TAG, "Successfully connected to WiFi")

                // Step 3: Wait for network to be fully ready
                // The network binding may succeed but routes may not be established yet
                delay(3000) // Increased from 1000ms to 3000ms

                updateState {
                    QrScannerScreenUiState.Connecting(
                        networkName = network.ssid,
                        currentStep = ConnectionStep.REGISTERING
                    )
                }

//                _uiState.value = StudentUiState.Connecting(
//                    network.ssid,
//                    ConnectionStep.REGISTERING
//                )

                // Get student full name
                val fullName = studentInfo.firstName + " " + studentInfo.lastName

                // Send attendance data with real student info
                val studentData = StudentAttendance(
                    studentId = studentInfo.studentId,
                    name = fullName,
                    timestamp = System.currentTimeMillis().toString(),
                    deviceId = getDeviceId(),
                    sessionId = qrData?.sessionId ?: UUID.randomUUID().toString(),
                    token = qrData?.token
                )

                Log.d(TAG, "Submitting attendance for: $fullName (${studentInfo.studentId})")

                val serverIp = qrData?.serverIp ?: "192.168.49.1"
                val port = qrData?.port ?: 8080

                Log.d(TAG, "Sending attendance to $serverIp:$port")

                // Retry logic for attendance submission
                var sendResult: Result<ServerResponse>? = null
                var attempts = 0
                val maxAttempts = 3

                while (attempts < maxAttempts && (sendResult == null || sendResult.isFailure)) {
                    if (attempts > 0) {
                        Log.d(TAG, "Retry attempt $attempts of $maxAttempts")
                        delay(2000) // Wait 2 seconds between retries
                    }
                    attempts++
                    sendResult = attendanceClient.sendAttendance(serverIp, port, studentData)
                }

                if (sendResult?.isFailure == true) {
                    val errorMsg =
                        sendResult.exceptionOrNull()?.message ?: "Failed to mark attendance"
                    Log.e(TAG, "Attendance submission failed after $attempts attempts: $errorMsg")
                    updateState {
                        QrScannerScreenUiState.Error(
                            "Connected but failed to mark attendance: $errorMsg"
                        )
                    }


//                    _uiState.value = StudentUiState.Error(
//                        "Connected but failed to mark attendance: $errorMsg"
//                    )
                    return@launch
                }

                // Success!
                Log.d(TAG, "Attendance marked successfully")
//                val duration = getDuration()
                val markedTime = getCurrentTime()

                updateState {
                    QrScannerScreenUiState.Connected(
                        networkName = network.ssid,
                        markedAtTime = markedTime
                    )
                }

//                _uiState.value = StudentUiState.Success(
//                    networkName = network.ssid,
//                    markedAtTime = markedTime
//                )

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting", e)

                updateState {
                    QrScannerScreenUiState.Error(
                        e.message ?: "Unknown error occurred"
                    )
                }

//                _uiState.value = StudentUiState.Error(e.message ?: "Unknown error")
            }
        }
    }


    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    @OptIn(InternalSerializationApi::class)
    fun cameraPreviewListenerStub(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        previewView: androidx.camera.view.PreviewView,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        cameraExecutor: java.util.concurrent.Executor,
        onQRCodeScanned: (QRData) -> Unit,

        ) {


//        TODO("Need to update the state")
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

//            onFlashDetected(camera.cameraInfo.hasFlashUnit())
            Log.d(
                "QRScanner",
                "Camera bound successfully, hasFlash=${camera.cameraInfo.hasFlashUnit()}"
            )

        } catch (e: Exception) {
            Log.e("QRScanner", "Camera binding failed", e)
            onError("Failed to start camera: ${e.message}")
        }


    }


    private fun getDeviceId(): String {
        val deviceId = prefs.deviceId
        if (deviceId.value.isBlank()) {
            prefs.addDeviceId(UUID.randomUUID().toString())
        }
        return deviceId.value
    }

    private fun getCurrentTime(): String {
        val formatter = java.text.SimpleDateFormat("h:mm:ss a", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }

    private fun onError(message: String) {
        Log.e(TAG, "Camera error: $message")
        updateState {
            QrScannerScreenUiState.Error(message)
        }
    }


}