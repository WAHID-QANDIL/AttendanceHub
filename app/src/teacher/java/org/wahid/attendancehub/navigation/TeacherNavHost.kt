package org.wahid.attendancehub.navigation

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    hasPermissions: Boolean = false,
    showAppSettingDialog: () -> Unit,
) {

    NavHost(
        navController = navController,
        startDestination = if (hasPermissions) TeacherScreens.Home.route else TeacherScreens.Permissions.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(400)
            )
        },
//        exitTransition = {
//            slideOutOfContainer(
//                towards = AnimatedContentTransitionScope.SlideDirection.Down,
//                animationSpec = tween(400)
//            )
//        },

        popExitTransition = {
            scaleOut(
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                targetScale = .9f,

            )

        },
        popEnterTransition = {
            scaleIn(
               animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                initialScale = .9f,
            )
        }

    ) {
        // Permissions Screen
        composable(TeacherScreens.Permissions.route) {
            PermissionsScreen(
                onGrantPermissions = {
                    // After permissions are granted, navigate to home
                    navController.navigate(TeacherScreens.Home.route) {
                        popUpTo(TeacherScreens.Permissions.route) { inclusive = true }
                    }
                },
                showAppSettingDialog = showAppSettingDialog
            )
        }

        // Home/Welcome Screen
        composable(TeacherScreens.Home.route) {
            TeacherHomeScreen(
                onEnableHotspot = {
                    navController.navigate(TeacherScreens.HotspotActive.route) {
                        popUpTo(
                            TeacherScreens.Home.route
                        )
                    }
                    viewModel.startHotspot()
                },
                todaySessionsCount = 3,
                lastSessionTime = "2 hours ago"
            )
        }

        // Hotspot Active Screen
        composable(TeacherScreens.HotspotActive.route) {

            val onCallBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Custom back press handling, stop hotspot and navigate to home
                    // When it was NOT handled, pressing back would just minimize,and navigate back to home screen, but keep the hotspot active and this produce a run time crash when trying to start hotspot again
                    viewModel.stopHotspot()
                    navController.navigate(TeacherScreens.Home.route) {
                        popUpTo(TeacherScreens.Home.route) { inclusive = true }
                    }
                }
            }
            LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher?.addCallback(
                onCallBackPressedCallback
            )

            val currentUiState by viewModel.uiState.collectAsState()
            val connectedStudents by viewModel.connectedStudents.collectAsState()

            when (val state = currentUiState) {
                is TeacherUiState.HotspotActive -> {
                    // Log when connected students list changes
                    LaunchedEffect(connectedStudents.size) {
                        Log.d(
                            "TeacherNavHost",
                            "HotspotActiveScreen - Connected students count: ${connectedStudents.size}"
                        )
                        connectedStudents.forEachIndexed { index, student ->
                            Log.d("TeacherNavHost", "  Student $index: ${student.name}")
                        }
                    }

                    HotspotActiveScreen(
                        ssid = state.ssid,
                        password = state.password,
                        qrBitmap = state.qrBitmap,
                        connectedStudents = connectedStudents,
                        onEndSession = {
                            viewModel.stopHotspot()
                            navController.navigate(TeacherScreens.Home.route) {
                                popUpTo(TeacherScreens.Home.route) { inclusive = true }
                            }
                        },
                        onDownloadList = {
                            viewModel.downloadStudentList()
                        }
                    )
                }
                is TeacherUiState.Loading, TeacherUiState.Idle -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is TeacherUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message)
                    }
                }
            }
        }
    }
}
