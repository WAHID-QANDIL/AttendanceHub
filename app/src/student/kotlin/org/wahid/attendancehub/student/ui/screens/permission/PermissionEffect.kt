package org.wahid.attendancehub.student.ui.screens.permission

sealed interface PermissionEffect {
    data object NavigateToStudentInfo : PermissionEffect
}