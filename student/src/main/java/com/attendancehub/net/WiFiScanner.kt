package com.attendancehub.net

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.attendancehub.student.ui.screens.WifiNetwork
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * WiFi Scanner for detecting available networks
 * Used by students to find teacher's hotspot
 */
class WiFiScanner(private val context: Context) {

    private val TAG = "WiFiScanner"
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /**
     * Scan for available WiFi networks
     * Returns list of WifiNetwork objects
     */
    suspend fun scanNetworks(): List<WifiNetwork> = suspendCancellableCoroutine { continuation ->
        // Check permissions
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        // Check if WiFi is enabled
        if (!wifiManager.isWifiEnabled) {
            Log.w(TAG, "WiFi is disabled")
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        // Register receiver for scan results
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)

                    if (success) {
                        val scanResults = wifiManager.scanResults
                        Log.d(TAG, "Scan successful: ${scanResults.size} networks found")

                        val networks = scanResults.map { result ->
                            val signalLevel = WifiManager.calculateSignalLevel(result.level, 5)
                            val isSecured = result.capabilities.contains("WPA") ||
                                           result.capabilities.contains("WEP") ||
                                           result.capabilities.contains("PSK")

                            // Detect teacher networks (common patterns)
                            val isTeacherNetwork = result.SSID.contains("DIRECT", ignoreCase = true) ||
                                                  result.SSID.contains("Teacher", ignoreCase = true) ||
                                                  result.SSID.contains("Class", ignoreCase = true) ||
                                                  result.SSID.contains("Attendance", ignoreCase = true)

                            WifiNetwork(
                                ssid = result.SSID,
                                signalStrength = signalLevel,
                                isSecured = isSecured,
                                isTeacherNetwork = isTeacherNetwork
                            )
                        }.filter { it.ssid.isNotEmpty() } // Filter out hidden networks

                        context?.unregisterReceiver(this)
                        continuation.resume(networks)
                    } else {
                        Log.e(TAG, "Scan failed")
                        context?.unregisterReceiver(this)
                        continuation.resume(emptyList())
                    }
                }
            }
        }

        // Register receiver
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }

        // Start scan
        val scanStarted = wifiManager.startScan()
        if (!scanStarted) {
            Log.e(TAG, "Failed to start WiFi scan")
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver not registered
            }
            continuation.resume(emptyList())
        } else {
            Log.d(TAG, "WiFi scan started")
        }

        // Cleanup on cancellation
        continuation.invokeOnCancellation {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver not registered
            }
        }
    }

    /**
     * Check if location permission is granted (required for WiFi scanning on Android 6+)
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if WiFi is enabled
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    /**
     * Enable WiFi (requires user to enable in settings on Android 10+)
     */
    @Suppress("DEPRECATION")
    fun enableWifi(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            wifiManager.isWifiEnabled = true
            true
        } else {
            // Android 10+ requires user to enable WiFi through settings
            false
        }
    }
}

