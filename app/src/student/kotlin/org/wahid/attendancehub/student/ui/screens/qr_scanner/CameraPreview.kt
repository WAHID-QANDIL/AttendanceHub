package org.wahid.attendancehub.student.ui.screens.qr_scanner

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.serialization.InternalSerializationApi
import java.util.concurrent.Executor

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(InternalSerializationApi::class)
@Composable
fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: Executor,
    viewModel: QrScannerScreenViewModel
) {
    AndroidView(
        factory = { ctx ->
            Log.d("QRScanner", "AndroidView factory started - initializing camera")
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    viewModel.cameraPreviewListenerStub(
                        cameraProviderFuture,
                        previewView,
                        lifecycleOwner,
                        cameraExecutor,
                        onQRCodeScanned = { qrData ->
                            viewModel.onQrCodeScanned(qrData)
                        }
                    )
                } catch (e: Exception) {
                    Log.e("QRScanner", "Camera provider failed", e)
                    // Error will be handled inside the ViewModel
                }
            }, ContextCompat.getMainExecutor(ctx))

            Log.d("QRScanner", "Returning preview view from factory")
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}