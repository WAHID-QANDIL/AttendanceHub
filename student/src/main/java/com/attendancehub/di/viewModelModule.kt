package com.attendancehub.di

import com.attendancehub.student.ui.screens.attendanceSuccess.AttendanceViewModel
import com.attendancehub.student.ui.screens.connection.ConnectionViewModel
import com.attendancehub.student.ui.screens.permission.PermissionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { PermissionViewModel() }
    viewModel { AttendanceViewModel() }
    viewModel { ConnectionViewModel() }
}