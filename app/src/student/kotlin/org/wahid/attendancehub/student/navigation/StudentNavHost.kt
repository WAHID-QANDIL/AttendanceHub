@file:OptIn(InternalSerializationApi::class)

package org.wahid.attendancehub.student.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.wahid.attendancehub.student.ui.screens.attendanceSuccess.AttendanceSuccessScreen
import org.wahid.attendancehub.student.ui.screens.ConnectingScreen
import org.wahid.attendancehub.student.ui.screens.ConnectionStep
import org.wahid.attendancehub.student.ui.screens.ManualEntryDialog
import org.wahid.attendancehub.student.ui.screens.permission.PermissionsScreen
import org.wahid.attendancehub.student.ui.screens.qr_scanner.QRScannerScreen
import org.wahid.attendancehub.student.ui.screens.home.StudentNetworkScanScreen
import org.wahid.attendancehub.student.viewmodel.StudentUiState
import org.wahid.attendancehub.student.viewmodel.StudentViewModel
import kotlinx.serialization.InternalSerializationApi

@Composable
fun StudentNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: StudentViewModel = viewModel(),
    hasPermissions: Boolean = false
) {
//    val uiState by viewModel.uiState.collectAsState()
//    val availableNetworks by viewModel.availableNetworks.collectAsState()
//    val firstName by viewModel.firstName.collectAsState()
//    val lastName by viewModel.lastName.collectAsState()
//    val studentId by viewModel.studentId.collectAsState()

    // Navigate based on UI state
//    LaunchedEffect(uiState) {
//        when (val state = uiState) {
//            is StudentUiState.StudentInfo -> {
//                if (navController.currentDestination?.route != StudentScreen.StudentInfo.route) {
//                    navController.navigate(StudentScreen.StudentInfo.route) {
//                        popUpTo(0) { inclusive = true }
//                    }
//                }
//            }
//            is StudentUiState.QRScanning -> {
//                if (navController.currentDestination?.route != StudentScreen.QRScanner.route) {
//                    navController.navigate(StudentScreen.QRScanner.route)
//                }
//            }
//            is StudentUiState.Connecting -> {
//                if (navController.currentDestination?.route != "connecting/${state.networkName}") {
//                    navController.navigate("connecting/${state.networkName}") {
//                        popUpTo(StudentScreen.NetworkScan.route)
//                    }
//                }
//            }
//            is StudentUiState.Success -> {
//                if (navController.currentDestination?.route != StudentScreen.Success.route) {
//                    navController.navigate(StudentScreen.Success.route) {
//                        popUpTo(StudentScreen.NetworkScan.route) { inclusive = true }
//                    }
//                }
//            }
//            is StudentUiState.Error -> {
//                // Navigate back to network scan on error
//                if (navController.currentDestination?.route != StudentScreen.NetworkScan.route) {
//                    navController.navigate(StudentScreen.NetworkScan.route) {
//                        popUpTo(StudentScreen.NetworkScan.route) { inclusive = true }
//                    }
//                }
//            }
//            is StudentUiState.Scanning -> {
//                if (navController.currentDestination?.route == "connecting/{networkName}") {
//                    navController.popBackStack()
//                }
//            }
//            else -> { /* No navigation */ }
//        }
//    }

    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(
            navController = navController,
            startDestination = if (hasPermissions) StudentScreen.StudentInfo.route else StudentScreen.Permissions.route
        ) {
            // Student Info Screen
            composable(StudentScreen.StudentInfo.route) {
                org.wahid.attendancehub.student.ui.screens.StudentInfoScreen(
                    onInfoSaved = { first, last, id ->
                        viewModel.saveStudentInfo(first, last, id)
                        navController.navigate(StudentScreen.NetworkScan.route){
                            popUpTo(StudentScreen.StudentInfo.route) { inclusive = true}
                        }
                    },
                    existingFirstName = firstName,
                    existingLastName = lastName,
                    existingStudentId = studentId
                )
            }

            // Permissions Screen
            composable(StudentScreen.Permissions.route) {
                PermissionsScreen()
            }

            // Network Scan Screen
            composable(StudentScreen.NetworkScan.route) {
            LaunchedEffect(Unit) {
                viewModel.scanNetworks()
            }

            // Show manual entry dialog when state is ManualEntry
            if (uiState is StudentUiState.ManualEntry) {
                ManualEntryDialog(
                    onDismiss = { viewModel.cancelManualEntry() },
                    onConnect = { ssid, password ->
                        viewModel.connectManually(ssid, password)
                    }
                )
            }

            // Show error snackbar when state is Error
            val snackbarHostState = remember { SnackbarHostState() }
            if (uiState is StudentUiState.Error) {
                val errorMessage = (uiState as StudentUiState.Error).message
                LaunchedEffect(errorMessage) {
                    snackbarHostState.showSnackbar(
                        message = errorMessage,
                        duration = SnackbarDuration.Long
                    )
                    // Reset to Idle after showing error
                    viewModel.scanNetworks()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                StudentNetworkScanScreen(
                    availableNetworks = availableNetworks,
                    onNetworkSelected = { network ->
                        viewModel.connectToNetwork(network)
                    },
                    onRefresh = {
                        viewModel.scanNetworks()
                    },
                    onScanQR = {
                        viewModel.startQRScanning()
                    },
                    onManualEntry = {
                        viewModel.startManualEntry()
                    }
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // QR Scanner Screen
        composable(StudentScreen.QRScanner.route) {
            QRScannerScreen(
                onClose = {
                    viewModel.cancelQRScanning()
                    navController.popBackStack()
                },
                navController = navController
            )
        }

        // Connecting Screen
        composable(
            route = "connecting/{networkName}",
            arguments = listOf(navArgument("networkName") { type = NavType.StringType })
        ) { backStackEntry ->
            val networkName = backStackEntry.arguments?.getString("networkName") ?: ""
            val state = uiState as? StudentUiState.Connecting

            ConnectingScreen(
                networkName = networkName,
                currentStep = state?.currentStep ?: ConnectionStep.NETWORK_FOUND
            )
        }

        // Success Screen
        composable(StudentScreen.Success.route) {
            val state = uiState as? StudentUiState.Success

            state?.let {
                AttendanceSuccessScreen(
                    networkName = it.networkName,
                    markedAtTime = it.markedAtTime,
                    navController = navController
                )
            }
        }
    }
    }
}

val LocalNavController = staticCompositionLocalOf<NavController> { //search static
    error("NavController not provided")
}