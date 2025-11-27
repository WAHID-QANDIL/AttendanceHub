package org.wahid.attendancehub

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.wahid.attendancehub.ui.theme.AttendanceHubTheme

class MainActivity : ComponentActivity() {
    private val TAG = "HotspotActivity"
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    @Composable
    fun ShowRationaleDialog(context: Context, onConfirm: () -> Unit, onCancel: () -> Unit) {
        // simple Compose AlertDialog — for demo just call onConfirm immediately if not composing
        AlertDialog(
            onDismissRequest = { openAppSettings(context = context) },
            title = { Text("Permission needed") },
            text = { Text("We need the camera to take photos.") },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        )
    }


    fun Context.findActivity(): Activity =
        when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> throw IllegalStateException("Context is not an Activity")
        }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }


    @Composable
    fun PermissionsLauncher() {
        val context = LocalContext.current
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
        )

        val permissionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { resultMap: Map<String, Boolean> ->
            // resultMap[permission] == true if granted
            // You can update UI or state accordingly
            val grantedAll = permissions.all { resultMap[it] == true }
            // store state, show messages, etc.
        }

        Button(
            modifier = Modifier.fillMaxSize(),
            onClick = {
            permissionsLauncher.launch(permissions.toTypedArray())
        }) {
            Text("Request Location + ACCESS_WIFI_STATE + CHANGE_WIFI_STATE + NEARBY_WIFI_DEVICES")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()






        setContent {
            AttendanceHubTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val pd = innerPadding
                    PermissionsLauncher()




                }
            }
        }
    }


}