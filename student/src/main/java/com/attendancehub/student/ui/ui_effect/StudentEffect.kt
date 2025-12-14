package com.attendancehub.student.ui.ui_effect

sealed class StudentEffect{
    data class navigateTo(val route: String) : StudentEffect()
    data class showToast(val message: String) : StudentEffect()
}