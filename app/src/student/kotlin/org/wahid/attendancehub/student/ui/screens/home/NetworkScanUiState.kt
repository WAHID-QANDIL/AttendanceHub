package org.wahid.attendancehub.student.ui.screens.home

sealed class NetworkScanUiState {
    data object Idle : NetworkScanUiState()
    data object Scanning : NetworkScanUiState()
    data class ShowStudentInfoSheet(val pendingAction: PendingAction) : NetworkScanUiState()
}

sealed class PendingAction {
    data object ScanQR : PendingAction()
    data object ManualEntry : PendingAction()
    data class ConnectToNetwork(val ssid: String, val password: String = "") : PendingAction()
}

