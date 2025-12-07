package org.wahid.attendancehub

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.wahid.attendancehub.navigation.TeacherNavHost
import org.wahid.attendancehub.theme.AttendanceHubTheme
import org.wahid.attendancehub.ui.core.PermissionCard
import org.wahid.attendancehub.ui.utils.openAppSettings
import org.wahid.attendancehub.ui.viewmodel.TeacherViewModel

class TeacherMainActivity : ComponentActivity() {
    private val viewModel by viewModels<TeacherViewModel>()
    private var hasPermissions: Boolean = false
    val TAG = "Callbacks"

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            hasPermissions = result.values.all { it }
            setupContent()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        hasPermissions = permissions.all { p ->
            ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
        }
        if (
            shouldShowPermissionDialog(Manifest.permission.ACCESS_FINE_LOCATION)
            ||
            shouldShowPermissionDialog(Manifest.permission.CHANGE_WIFI_STATE)
            ||
           shouldShowPermissionDialog(Manifest.permission.ACCESS_WIFI_STATE)
            ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shouldShowPermissionDialog(Manifest.permission.NEARBY_WIFI_DEVICES))
        ) {
            hasPermissions = false
            showAppSettingsDialog()

            // User has previously declined permissions; show rationale or direct to settings
        } else {
            setupContent()
        }
    }

    private fun shouldShowPermissionDialog(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }

    private fun retryRequestPermissionsAfterDeclined() {
        //Check if should show rationale
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        if (
            shouldShowPermissionDialog(Manifest.permission.ACCESS_FINE_LOCATION)
            ||
            shouldShowPermissionDialog(Manifest.permission.CHANGE_WIFI_STATE)
            ||
            shouldShowPermissionDialog(Manifest.permission.ACCESS_WIFI_STATE)
            ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shouldShowPermissionDialog(Manifest.permission.NEARBY_WIFI_DEVICES))
        )
        permissionLauncher.launch(permissions.toTypedArray())

    }

    private fun getContext(): TeacherMainActivity {
        return this
    }

    /**
     * Called whenever permissions state changes or on first launch.
     */
    private fun setupContent() {
        setContent {
            AttendanceHubTheme {
                TeacherNavHost(
                    viewModel = viewModel,
                    hasPermissions = hasPermissions,
                    showAppSettingDialog = { showAppSettingsDialog() }
                )
            }
        }
    }


    private fun checkPermissionStatus(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun allPermissionsGranted(): Boolean {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        return permissions.all { checkPermissionStatus(it) }
    }

    override fun onRestart() {
        super.onRestart()

        Log.d(TAG, "onRestart: onRestart called")
        if (allPermissionsGranted()) {
            hasPermissions = true
            setupContent()
        } else {
            hasPermissions = false
            retryRequestPermissionsAfterDeclined()
        }
    }


    private fun showAppSettingsDialog() {
        setContent {
            AttendanceHubTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionCard(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = stringResource(R.string.permissions_required),
                        description = buildString {
                            append(stringResource(R.string.please_grant_the_necessary_permissions_in_settings_to_continue_using_the_app))
                            append(stringResource(R.string.required_permissions))
                            append(stringResource(R.string.location_access))
                            append(stringResource(R.string.wifi_state_access))
                        },
                        onConfirm = {
                            openAppSettings(
                                packageName = packageName,
                                context = getContext()
                            )
                        }
                    )
                }
            }
        }
    }

}