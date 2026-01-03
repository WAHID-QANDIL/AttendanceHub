package org.wahid.attendancehub.student.ui.screens.qr_scanner

import kotlinx.serialization.InternalSerializationApi
import org.wahid.attendancehub.models.QRData


interface QrScannerScreenInteractionListener {
    @OptIn(InternalSerializationApi::class)
    fun onQrCodeScanned(qrCode: QRData)
}