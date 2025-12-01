package com.attendancehub.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Student-side hotspot manager for connecting to teacher's local-only WiFi hotspot
 *
 * Flow:
 * 1. Student scans teacher's QR code containing SSID + password
 * 2. Call connect(ssid, password)
 * 3. System shows WiFi connection dialog (user must approve on Android Q+)
 * 4. When connected, onAvailable() fires and binds network
 * 5. All subsequent HTTP calls use the hotspot network
 * 6. Call disconnect() when done to release resources
 */
class StudentHotspotManager(private val ctx: Context) : HotspotManager {

    private val TAG = "StudentHotspotManager"
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var boundNetwork: Network? = null

    override suspend fun start(): Result<HotspotInfo> {
        return Result.failure(UnsupportedOperationException("Student cannot start hotspot"))
    }

    override suspend fun stop(): Result<Unit> {
        disconnect()
        return Result.success(Unit)
    }

    /**
     * Connect to teacher's WiFi hotspot using WifiNetworkSpecifier (Android Q+)
     *
     * @param ssid WiFi network name from QR code
     * @param password WiFi password from QR code
     * @return Result<Unit> - Success if connected, Failure otherwise
     *
     * IMPORTANT: This shows a system dialog that the user must approve.
     * The connection is app-scoped (other apps keep using default network).
     */
    override suspend fun connect(ssid: String, password: String): Result<Unit> = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Build WiFi network specifier
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            // Build network request with WiFi transport
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Local-only network
                .setNetworkSpecifier(specifier)
                .build()

            val deferred = CompletableDeferred<Result<Unit>>()

            // Create network callback
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available: $network")

                    // CRITICAL: Bind process to this network
                    // This ensures all HTTP/socket calls use the hotspot network
                    val bound = cm.bindProcessToNetwork(network)
                    if (bound) {
                        boundNetwork = network
                        Log.d(TAG, "Process bound to network successfully")
                        deferred.complete(Result.success(Unit))
                    } else {
                        Log.e(TAG, "Failed to bind process to network")
                        deferred.complete(Result.failure(RuntimeException("Failed to bind to network")))
                    }
                }

                override fun onUnavailable() {
                    Log.e(TAG, "Network unavailable (user may have declined)")
                    deferred.complete(Result.failure(RuntimeException("Network unavailable - connection rejected or failed")))
                }

                override fun onLost(network: Network) {
                    Log.w(TAG, "Network lost: $network")
                    boundNetwork = null
                    // Don't complete deferred here - this happens after disconnect
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    Log.d(TAG, "Network capabilities changed: $networkCapabilities")
                }
            }

            networkCallback = callback

            // Request network connection
            // This triggers system UI asking user to connect
            cm.requestNetwork(request, callback)

            // Wait for connection with timeout
            val result = withTimeoutOrNull(30000L) { // 30 second timeout
                deferred.await()
            }

            return@withContext result ?: run {
                // Timeout - clean up
                cm.unregisterNetworkCallback(callback)
                networkCallback = null
                Result.failure(RuntimeException("Connection timeout - user may not have approved"))
            }

        } else {
            // Pre-Android Q: Would need legacy WiFi APIs
            // WifiManager.addNetwork() + WifiManager.enableNetwork()
            // Requires CHANGE_WIFI_STATE and is deprecated
            Log.e(TAG, "Android Q+ required for WifiNetworkSpecifier")
            Result.failure(RuntimeException("Android 10+ required for secure WiFi connection"))
        }
    }

    /**
     * Disconnect from hotspot and release network resources
     * IMPORTANT: Always call this when done to avoid leaking resources
     */
    fun disconnect() {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Unbind process from network
        cm.bindProcessToNetwork(null)
        boundNetwork = null

        // Unregister callback to stop WiFi connection
        networkCallback?.let {
            try {
                cm.unregisterNetworkCallback(it)
                Log.d(TAG, "Network callback unregistered")
            } catch (e: IllegalArgumentException) {
                // Already unregistered
                Log.w(TAG, "Network callback already unregistered")
            }
        }
        networkCallback = null
    }

    /**
     * Get the currently bound network (for advanced use cases)
     * Returns null if not connected
     */
    fun getBoundNetwork(): Network? = boundNetwork

    /**
     * Check if currently connected to a network
     */
    fun isConnected(): Boolean = boundNetwork != null
}

