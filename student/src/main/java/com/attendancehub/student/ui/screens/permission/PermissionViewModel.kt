package com.attendancehub.student.ui.screens.permission

import BaseViewModel

class PermissionViewModel : BaseViewModel<PermissionState, PermissionEffect>(
    initialState = PermissionState()
), PermissionInteractionListener {

    override fun onGrantPermissionsEnabled() {
        updateState { copy(permissionsGranted = true) }
        sendEffect(PermissionEffect.NavigateToStudentInfo)
    }
}