package com.attendancehub.student.ui.screens.student_info

sealed interface StudentInfoEffect {
    object NavigateToPermissionScreen : StudentInfoEffect
}