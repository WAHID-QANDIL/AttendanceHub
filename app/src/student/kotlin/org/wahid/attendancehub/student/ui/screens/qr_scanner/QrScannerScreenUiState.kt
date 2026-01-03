package org.wahid.attendancehub.student.ui.screens.qr_scanner

sealed class QrScannerScreenUiState {
    data class Error(val message: String) : QrScannerScreenUiState()
    data object Idle : QrScannerScreenUiState()
    data object ActiveScan : QrScannerScreenUiState()
    data object Validating : QrScannerScreenUiState()
}