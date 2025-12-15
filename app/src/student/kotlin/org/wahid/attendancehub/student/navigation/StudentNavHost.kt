@file:OptIn(InternalSerializationApi::class)

package org.wahid.attendancehub.student.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel
import org.wahid.attendancehub.models.QRData
import org.wahid.attendancehub.student.ui.screens.attendanceSuccess.AttendanceSuccessScreen
import org.wahid.attendancehub.student.ui.screens.connecting.ConnectingScreen
import org.wahid.attendancehub.student.ui.screens.connecting.ConnectingScreenViewModel
import org.wahid.attendancehub.student.ui.screens.connecting.ConnectingScreenEffect
import org.wahid.attendancehub.student.ui.screens.connecting.ConnectingScreenUiState
import org.wahid.attendancehub.student.ui.screens.connecting.ConnectionStep
import org.wahid.attendancehub.student.ui.screens.permission.PermissionsScreen
import org.wahid.attendancehub.student.ui.screens.qr_scanner.QRScannerScreen
import org.wahid.attendancehub.utils.ObserveAsEffect

@Composable
fun StudentNavHost(
    navController: NavHostController = rememberNavController(),
    hasPermissions: Boolean = false
) {
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(
            navController = navController,
            startDestination = if (hasPermissions) StudentScreen.NetworkScan.route else StudentScreen.Permissions.route
        ) {
            // Permissions Screen
            composable(StudentScreen.Permissions.route) {
                PermissionsScreen()
            }

            // Network Scan Screen (Home)
            composable(StudentScreen.NetworkScan.route) {
                org.wahid.attendancehub.student.ui.screens.home.StudentNetworkScanScreen(
                    navController = navController
                )
            }

            // QR Scanner Screen
            composable(StudentScreen.QRScanner.route) {
                QRScannerScreen(
                    navController = navController
                )
            }

            // Connecting Screen - receives QR data and handles connection
            composable(
                route = "connecting/{qrDataJson}",
                arguments = listOf(navArgument("qrDataJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val qrDataJson = backStackEntry.arguments?.getString("qrDataJson") ?: ""
                val decodedJson = Uri.decode(qrDataJson)
                val qrData = Json.decodeFromString<QRData>(decodedJson)

                val viewModel = koinViewModel<ConnectingScreenViewModel>()
                val state by viewModel.state.collectAsStateWithLifecycle()

                // Start connection when screen is displayed
                LaunchedEffect(qrData) {
                    viewModel.connectToNetwork(qrData)
                }

                // Handle navigation effects
                ObserveAsEffect(viewModel.effect) { effect ->
                    when (effect) {
                        is ConnectingScreenEffect.NavigateToSuccess -> {
                            navController.navigate(
                                StudentScreen.Success.createRoute(
                                    networkName = effect.networkName,
                                    markedAtTime = effect.markedAtTime
                                )
                            ) {
                                popUpTo(StudentScreen.QRScanner.route) { inclusive = true }
                            }
                        }
                        is ConnectingScreenEffect.NavigateBack -> {
                            navController.popBackStack()
                        }
                    }
                }

                // Render connecting screen based on state
                when (val currentState = state) {
                    is ConnectingScreenUiState.Connecting -> {
                        ConnectingScreen(
                            networkName = currentState.networkName,
                            currentStep = currentState.currentStep
                        )
                    }
                    is ConnectingScreenUiState.Error -> {
                        // Show error screen or navigate back
                        // For now, just show connecting with error (you can create an error screen)
                        ConnectingScreen(
                            networkName = qrData.ssid,
                            currentStep = ConnectionStep.NETWORK_FOUND
                        )
                    }
                    else -> {
                        // Idle or Success state - show connecting
                        ConnectingScreen(
                            networkName = qrData.ssid,
                            currentStep = ConnectionStep.NETWORK_FOUND
                        )
                    }
                }
            }

            // Success Screen
            composable(
                route = "success/{networkName}/{markedAtTime}",
                arguments = listOf(
                    navArgument("networkName") { type = NavType.StringType },
                    navArgument("markedAtTime") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val networkName = backStackEntry.arguments?.getString("networkName") ?: ""
                val markedAtTime = backStackEntry.arguments?.getString("markedAtTime") ?: ""

                AttendanceSuccessScreen(
                    networkName = Uri.decode(networkName),
                    markedAtTime = Uri.decode(markedAtTime),
                    navController = navController
                )
            }
        }
    }
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("NavController not provided")
}