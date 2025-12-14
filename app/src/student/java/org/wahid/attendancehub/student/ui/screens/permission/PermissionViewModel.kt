package org.wahid.attendancehub.student.ui.screens.permission

import org.wahid.attendancehub.base.BaseViewModel

class PermissionViewModel : BaseViewModel<PermissionState, PermissionEffect>(
    initialState = PermissionState()
), PermissionInteractionListener {

    override fun onGrantPermissionsEnabled() {
        updateState { copy(permissionsGranted = true) }
        sendEffect(PermissionEffect.NavigateToStudentInfo)
    }
}