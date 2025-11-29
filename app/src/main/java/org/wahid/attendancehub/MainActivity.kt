package org.wahid.attendancehub

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.integration.android.IntentIntegrator
import org.wahid.attendancehub.ui.MainViewModel
import org.wahid.attendancehub.ui.theme.AttendanceHubTheme

class MainActivity : ComponentActivity() {
    private val TAG = "HotspotActivity"
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val viewModel by viewModels<MainViewModel>()
    fun Context.findActivity(): Activity? =
        when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }

    private fun startLocalHotspot(/*onStarted: (ssid: String, password: String) -> Unit,*/looper: Looper) {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        val callback = object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                super.onStarted(reservation)
                hotspotReservation = reservation
                val config = reservation.wifiConfiguration

//                onStarted(config.SSID ?: "", config.preSharedKey ?: "")

                Log.i(
                    TAG,
                    "LocalOnlyHotspot started. SSID=${config?.SSID}, PSK=${config?.preSharedKey}"
                )
            }

            override fun onStopped() {
                super.onStopped()
                Log.i(TAG, "LocalOnlyHotspot stopped by system or all apps released it.")
                hotspotReservation = null
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                Log.e(TAG, "LocalOnlyHotspot failed with reason=$reason")
            }
        }

        val hasLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val needsNearbyWifi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val hasNearbyWifi = if (needsNearbyWifi) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasLocation || !hasNearbyWifi) {
            Log.e(TAG, "startLocalHotspot: required permissions are not granted.")
            return
        }

        wifiManager.startLocalOnlyHotspot(callback, Handler(looper))
    }

    private fun stopLocalHotspot() {
        hotspotReservation?.close()
        Log.d(TAG, "stopLocalHotspot: \"LocalOnlyHotspot has been stopped\"")
        hotspotReservation = null
    }

    override fun onStop() {
        super.onStop()
//        Log.d(TAG, "stopLocalHotspot: \"LocalOnlyHotspot has been stopped\"")
        stopLocalHotspot()
    }

    override fun onPostResume() {
        super.onPostResume()
    }

    override fun onRestart() {
        super.onRestart()
        startLocalHotspot(Looper.getMainLooper())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startQrScan(this)
        val multiplePermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsMap: Map<String, Boolean> ->
                permissionsMap.forEach { (permission, isGranted) ->
                    viewModel.onPermissionResult(permission = permission, isGranted = isGranted)
                }

                val hasLocation = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                val needsNearbyWifi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                val hasNearbyWifi = if (needsNearbyWifi) {
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                if (hasLocation && hasNearbyWifi) {
                    startLocalHotspot(Looper.getMainLooper())
                } else {
                    viewModel.openAppSettings(activity = findActivity()!!)
                    Log.e(TAG, "Required permissions are not granted after request.")
                }
            }

        val permissionsToRequest = mutableListOf<String>().apply {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        val allGranted = permissionsToRequest.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startLocalHotspot(Looper.getMainLooper())
        } else {
            permissionsToRequest.forEach { permission ->
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    viewModel.onPermissionResult(permission, false)
                }
            }
            multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }

        setContent {
            AttendanceHubTheme {
                // UI content setup
            }
        }
    }

    private fun startQrScan(){

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
            )


            .build()



    }



//    private fun startQrScan(activity: Activity) {
//        val integrator = IntentIntegrator(activity)
//        integrator.setPrompt("Scan teacher QR")
//        integrator.setBeepEnabled(true)
//        integrator.setCameraId(0)
//        integrator.setBarcodeImageEnabled(true)
//        integrator.setOrientationLocked(false)
//        integrator.initiateScan()
//    }
}
