package com.attendancehub.student.ui.ui_state

import com.attendancehub.student.ui.model.ConnectionStep
import com.attendancehub.student.ui.screens.WifiNetwork

sealed class StudentUiState {
    object Idle : StudentUiState()
    object StudentInfo : StudentUiState()
    object QRScanning : StudentUiState()
    object ManualEntry : StudentUiState()
    data class Scanning(val networks: List<WifiNetwork>) : StudentUiState()
    data class Connecting(
        val networkName: String,
        val currentStep: ConnectionStep,
    ) : StudentUiState()
    data class Success(
        val networkName: String,
        val connectedDuration: String,
        val markedAtTime: String,
    ) : StudentUiState()
    data class Error(val message: String) : StudentUiState()
}