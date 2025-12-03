package org.wahid.attendancehub.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.wahid.attendancehub.ui.screens.HotspotActiveScreen
import org.wahid.attendancehub.ui.screens.PermissionsScreen
import org.wahid.attendancehub.ui.screens.TeacherHomeScreen
import org.wahid.attendancehub.ui.viewmodel.TeacherUiState
import org.wahid.attendancehub.ui.viewmodel.TeacherViewModel

@Composable
fun TeacherNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: TeacherViewModel = viewModel(),
    hasPermissions: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate based on UI state
    when (uiState) {
        is TeacherUiState.HotspotActive -> {
            if (navController.currentDestination?.route != TeacherScreen.HotspotActive.route) {
                navController.navigate(TeacherScreen.HotspotActive.route) {
                    popUpTo(TeacherScreen.Home.route) { inclusive = true }
                }
            }
        }
        is TeacherUiState.Idle -> {
            if (navController.currentDestination?.route == TeacherScreen.HotspotActive.route) {
                navController.navigate(TeacherScreen.Home.route) {
                    popUpTo(TeacherScreen.Home.route) { inclusive = true }
                }
            }
        }
        else -> { /* No navigation change */ }
    }

    NavHost(
        navController = navController,
        startDestination = if (hasPermissions) TeacherScreen.Home.route else TeacherScreen.Permissions.route
    ) {
        // Permissions Screen
        composable(TeacherScreen.Permissions.route) {
            PermissionsScreen(
                onGrantPermissions = {
                    // After permissions are granted, navigate to home
                    navController.navigate(TeacherScreen.Home.route) {
                        popUpTo(TeacherScreen.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        // Home/Welcome Screen
        composable(TeacherScreen.Home.route) {
            TeacherHomeScreen(
                onEnableHotspot = {
                    viewModel.startHotspot()
                    // Navigation handled by state observer above
                },
                todaySessionsCount = 3,
                lastSessionTime = "2 hours ago"
            )
        }

        // Hotspot Active Screen
        composable(TeacherScreen.HotspotActive.route) {
            val currentUiState by viewModel.uiState.collectAsState()
            val connectedStudents by viewModel.connectedStudents.collectAsState()

            (currentUiState as? TeacherUiState.HotspotActive)?.let {
                // Log when connected students list changes
                LaunchedEffect(connectedStudents.size) {
                    Log.d("TeacherNavHost", "HotspotActiveScreen - Connected students count: ${connectedStudents.size}")
                    connectedStudents.forEachIndexed { index, student ->
                        Log.d("TeacherNavHost", "  Student $index: ${student.name}")
                    }
                }

                HotspotActiveScreen(
                    ssid = it.ssid,
                    password = it.password,
                    qrBitmap = it.qrBitmap,
                    connectedStudents = connectedStudents,
                    onEndSession = {
                        viewModel.stopHotspot()
                    },
                    onDownloadList = {
                        viewModel.downloadStudentList()
                    }
                )
            }
        }
    }
}
