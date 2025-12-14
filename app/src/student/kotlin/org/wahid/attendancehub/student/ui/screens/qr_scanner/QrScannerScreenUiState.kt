package org.wahid.attendancehub.student.ui.screens.qr_scanner

import org.wahid.attendancehub.student.ui.screens.ConnectionStep

sealed class QrScannerScreenUiState {
    data class Connecting(
        val networkName: String,
        val currentStep: ConnectionStep,
    ) : QrScannerScreenUiState()

    data class Connected(
        val networkName: String,
        val markedAtTime: String,
    ) : QrScannerScreenUiState()

    data class Error(val message: String) : QrScannerScreenUiState()
    data object Idle : QrScannerScreenUiState()
    data object ActiveScan : QrScannerScreenUiState()
}