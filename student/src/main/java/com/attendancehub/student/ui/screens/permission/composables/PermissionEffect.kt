package com.attendancehub.student.ui.screens.permission.composables

sealed interface PermissionEffect {
    data object NavigateToStudentInfo : PermissionEffect
}