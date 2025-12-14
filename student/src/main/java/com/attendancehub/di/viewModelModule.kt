package com.attendancehub.di

import com.attendancehub.student.ui.screens.attendanceSuccess.AttendanceViewModel
import com.attendancehub.student.ui.screens.connection.ConnectionViewModel
import com.attendancehub.student.ui.screens.permission.PermissionViewModel
import com.attendancehub.student.ui.screens.student_info.StudentInfoViewModel
import com.attendancehub.utils.PrefsManager
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { PrefsManager(get()) }

    viewModel { PermissionViewModel() }
    viewModel { AttendanceViewModel() }
    viewModel { ConnectionViewModel() }
    viewModel { StudentInfoViewModel(get()) }
}