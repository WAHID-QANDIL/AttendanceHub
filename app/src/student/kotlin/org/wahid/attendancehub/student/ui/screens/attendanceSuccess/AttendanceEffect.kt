package org.wahid.attendancehub.student.ui.screens.attendanceSuccess

sealed interface AttendanceEffect {
    data object ReturnHome : AttendanceEffect
}