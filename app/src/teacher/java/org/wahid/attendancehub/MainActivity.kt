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
import androidx.core.content.ContextCompat
import org.wahid.attendancehub.navigation.TeacherNavHost
import org.wahid.attendancehub.ui.theme.AttendanceHubTheme
import org.wahid.attendancehub.ui.viewmodel.TeacherViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<TeacherViewModel>()
    private var hasPermissions: Boolean = false

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

        if (hasPermissions) {
            setupContent()
        } else {
            // Show permissions screen first; actual permission request is triggered from UI
            setupContent()
        }
    }

    /**
     * Called whenever permissions state changes or on first launch.
     */
    private fun setupContent() {
        setContent {
            AttendanceHubTheme {
                TeacherNavHost(
                    viewModel = viewModel,
                    hasPermissions = hasPermissions
                )
            }
        }
    }

    /**
     * Helper to be called from the PermissionsScreen button if you want
     * to trigger the system dialog directly from the Activity.
     */
    fun requestRuntimePermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}