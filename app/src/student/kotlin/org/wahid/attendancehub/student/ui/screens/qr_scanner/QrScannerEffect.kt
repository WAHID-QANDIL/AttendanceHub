package org.wahid.attendancehub.student.ui.screens.qr_scanner

import kotlinx.serialization.InternalSerializationApi
import org.wahid.attendancehub.models.QRData

@OptIn(InternalSerializationApi::class)
sealed interface QrScannerEffect {
    data class NavigateToConnecting(
        val qrData: QRData
    ) : QrScannerEffect
    data object NavigateBackHome: QrScannerEffect
}