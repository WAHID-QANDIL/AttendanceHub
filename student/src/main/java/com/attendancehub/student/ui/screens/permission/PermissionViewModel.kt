package com.attendancehub.student.ui.screens.permission

import BaseViewModel
import com.attendancehub.student.ui.screens.permission.composables.PermissionEffect
import com.attendancehub.student.ui.screens.permission.composables.PermissionInteractionListener
import com.attendancehub.student.ui.screens.permission.composables.PermissionState

class PermissionViewModel : BaseViewModel<PermissionState, PermissionEffect>(
    initialState = PermissionState()
), PermissionInteractionListener {

    override fun onGrantPermissionsEnabled() {
        updateState { copy(permissionsGranted = true) }
        sendEffect(PermissionEffect.NavigateToStudentInfo)
    }
}