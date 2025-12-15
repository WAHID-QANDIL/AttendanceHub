package org.wahid.attendancehub.di

import org.wahid.attendancehub.student.ui.screens.permission.PermissionViewModel
import org.koin.dsl.*
import org.koin.core.module.dsl.viewModelOf
import org.wahid.attendancehub.student.ui.screens.attendanceSuccess.AttendanceViewModel
import org.wahid.attendancehub.student.ui.screens.home.NetworkScanViewModel
import org.wahid.attendancehub.student.ui.screens.qr_scanner.QrScannerScreenViewModel

val viewModelModule = module {
    viewModelOf(constructor = ::PermissionViewModel )
    viewModelOf(constructor = ::AttendanceViewModel )
    viewModelOf(constructor = ::QrScannerScreenViewModel )
    viewModelOf(constructor = ::NetworkScanViewModel )
}