package org.wahid.attendancehub.student.ui.screens.home

sealed interface NetworkScanEffect {
    data object NavigateToQRScanner : NetworkScanEffect
    data object NavigateToManualEntry : NetworkScanEffect
    data class NavigateToConnecting(val ssid: String, val password: String) : NetworkScanEffect
}

