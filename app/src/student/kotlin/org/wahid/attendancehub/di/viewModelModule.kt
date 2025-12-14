package org.wahid.attendancehub.di

import org.wahid.attendancehub.student.ui.screens.permission.PermissionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { PermissionViewModel() }
}