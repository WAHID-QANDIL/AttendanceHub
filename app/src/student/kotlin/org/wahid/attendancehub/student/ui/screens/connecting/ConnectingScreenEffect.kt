package org.wahid.attendancehub.student.ui.screens.connecting

sealed interface ConnectingScreenEffect {
    data class NavigateToSuccess(
        val networkName: String,
        val markedAtTime: String
    ) : ConnectingScreenEffect

    data object NavigateBack : ConnectingScreenEffect
}

