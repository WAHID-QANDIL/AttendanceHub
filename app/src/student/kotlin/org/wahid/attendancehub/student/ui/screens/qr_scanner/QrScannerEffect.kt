package org.wahid.attendancehub.student.ui.screens.qr_scanner

sealed interface QrScannerEffect {
    data class NavigateToAttendanceSuccess(
        val networkName: String,
        val markedAtTime: String
    ) : QrScannerEffect
}