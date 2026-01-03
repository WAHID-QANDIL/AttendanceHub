package org.wahid.attendancehub.student.ui.screens.connecting

sealed class ConnectingScreenUiState {
    data class Connecting(
        val networkName: String,
        val currentStep: ConnectionStep
    ) : ConnectingScreenUiState()

    data class Success(
        val networkName: String,
        val markedAtTime: String
    ) : ConnectingScreenUiState()

    data class Error(val message: String) : ConnectingScreenUiState()

    data object Idle : ConnectingScreenUiState()
}

