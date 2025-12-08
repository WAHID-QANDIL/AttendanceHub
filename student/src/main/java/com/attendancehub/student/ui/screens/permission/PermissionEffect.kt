package com.attendancehub.student.ui.screens.permission

sealed interface PermissionEffect {
    data object NavigateToStudentInfo : PermissionEffect
}