package org.wahid.attendancehub.student.ui.screens.attendanceSuccess

import org.wahid.attendancehub.base.BaseViewModel

class AttendanceViewModel :BaseViewModel<AttendanceState, AttendanceEffect>(
    initialState = AttendanceState()
), AttendanceInteractionListener {

    override fun onReturnHome() {
        updateState { copy(isReturningHome = true) }
        sendEffect(AttendanceEffect.NavigateBack)
    }
}