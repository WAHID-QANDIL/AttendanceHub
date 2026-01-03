package com.attendancehub.student.ui.screens.attendanceSuccess

data class AttendanceState(
    val isDisconnecting: Boolean = false,
    val networkName : String = "",
    val markedAt : String = ""
)