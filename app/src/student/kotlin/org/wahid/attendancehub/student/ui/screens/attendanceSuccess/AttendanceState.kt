package org.wahid.attendancehub.student.ui.screens.attendanceSuccess

sealed class AttendanceState{
    data object Idle : AttendanceState()
    class IsReturningHome: AttendanceState()
    data class NetworkDetails(val networkName : String, val markedAt : String): AttendanceState()
}