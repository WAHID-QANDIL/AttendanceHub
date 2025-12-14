package com.attendancehub.student.ui.screens.connection

sealed interface ConnectionEffect {
    object NavigateToSuccessScreen: ConnectionEffect
}