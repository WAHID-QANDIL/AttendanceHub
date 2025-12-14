package com.attendancehub.student.ui.screens.student_info

import com.attendancehub.base.BaseViewModel
import com.attendancehub.utils.PrefsManager

class StudentInfoViewModel(
    private val prefsManager: PrefsManager
) : BaseViewModel<StudentInfoState, StudentInfoEffect>(
    initialState = StudentInfoState(),
), StudentInfoInteractionListener{

    override fun onContinueToScannerClick(
        firstName: String,
        lastName: String,
        studentId: String
    ) {
        updateState { copy(firstName = firstName, lastName = lastName, studentID = studentId) }
        prefsManager.saveStudentInfo(studentId,firstName,lastName)
        sendEffect(StudentInfoEffect.NavigateToPermissionScreen)
    }
}