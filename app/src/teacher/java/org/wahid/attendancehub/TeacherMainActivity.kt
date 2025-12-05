package org.wahid.attendancehub

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.wahid.attendancehub.navigation.TeacherNavHost
import org.wahid.attendancehub.ui.core.PermissionCard
import org.wahid.attendancehub.ui.theme.AttendanceHubTheme
import org.wahid.attendancehub.ui.utils.openAppSettings
import org.wahid.attendancehub.ui.viewmodel.TeacherViewModel

class TeacherMainActivity : ComponentActivity() {
    private val viewModel by viewModels<TeacherViewModel>()
    private var hasPermissions: Boolean = false
    private val permissionsDeclined = false

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
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            ||
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CHANGE_WIFI_STATE
            )
            ||
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_WIFI_STATE
            )
            ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ))
        ) {
            hasPermissions = false
            // User has previously declined permissions; show rationale or direct to settings
        } else {
            if (hasPermissions) {
                setupContent()
            } else {
                // Show permissions screen first; actual permission request is triggered from UI
                setupContent()
            }
        }
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
                    showAppSettingDialog = {
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
                                        title = "Permissions Required",
                                        description = "Please grant the necessary permissions in Settings to continue using the app.\n\n" +
                                                "Required Permissions:\n" +
                                                "- Location Access\n" +
                                                "- WiFi State Access",
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

    override fun onStart() {
        super.onStart()
        if (allPermissionsGranted()) {
            hasPermissions = true
            setupContent()
        } else {
            hasPermissions = false

        }
    }


}