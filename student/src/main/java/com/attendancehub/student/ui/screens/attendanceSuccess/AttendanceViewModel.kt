package com.attendancehub.student.ui.screens.attendanceSuccess

import com.attendancehub.base.BaseViewModel

class AttendanceViewModel :BaseViewModel<AttendanceState, AttendanceEffect>(
    initialState = AttendanceState()
), AttendanceInteractionListener {

    override fun onDisconnectClick() {
        updateState { copy(isDisconnecting = true) }
        sendEffect(AttendanceEffect.NavigateBack)
    }
}