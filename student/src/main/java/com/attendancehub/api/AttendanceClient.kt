package com.attendancehub.api

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import com.attendancehub.models.ServerResponse
import com.attendancehub.models.StudentAttendance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTP client for student to send attendance data to teacher's server
 *
 * Prerequisites:
 * 1. Student device MUST be connected to teacher's hotspot
 * 2. The teacher device is the Wi-Fi gateway; we resolve its IP dynamically
 *
 * NOTE:
 * - We NO LONGER trust "192.168.49.1". We always use WifiManager.dhcpInfo.gateway.
 */
class AttendanceClient(
    private val context: Context
) {

    private val TAG = "AttendanceClient"

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Resolve teacher (hotspot owner) IP using Wi-Fi gateway info.
     *
     * On the student device connected to the teacher hotspot, the teacher acts as the gateway.
     */
    private fun resolveTeacherIp(): String {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo

        val gatewayInt = dhcpInfo.gateway
        if (gatewayInt == 0) {
            // This usually means "not connected to Wi-Fi / hotspot"
            throw IllegalStateException("Gateway IP is 0. Are you connected to the teacher hotspot?")
        }

        val gatewayIp = Formatter.formatIpAddress(gatewayInt)
        Log.d(TAG, "Resolved teacher gateway IP: $gatewayIp")
        return gatewayIp
    }

    /**
     * Send attendance data to teacher's server via HTTP POST
     *
     * @param serverIp Ignored; kept only for backwards compatibility. Real IP is resolved dynamically.
     * @param port HTTP server port (default 8080)
     * @param studentData Student attendance information
     * @param endpoint API endpoint (default: "/join")
     */
    suspend fun sendAttendance(
        serverIp: String,         // kept but NOT used anymore
        port: Int,
        studentData: StudentAttendance,
        endpoint: String = "/join"
    ): Result<ServerResponse> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            // REAL server IP (teacher hotspot gateway)
            val realServerIp = resolveTeacherIp()

            // Build URL with resolved IP, ignore the passed serverIp parameter
            val url = URL("http://$realServerIp:$port$endpoint")
            Log.d(TAG, "Sending attendance to: $url")
            Log.d(TAG, "Caller passed serverIp=$serverIp, but using resolved IP=$realServerIp")

            // Open connection
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                doInput = true
                connectTimeout = 10000 // 10 seconds
                readTimeout = 10000    // 10 seconds
            }

            // Write JSON body
            val jsonBody = json.encodeToString(studentData)
            Log.d(TAG, "Request body: $jsonBody")

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            // Read response
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody =
                    BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { reader ->
                        reader.readText()
                    }

                Log.d(TAG, "Response body: $responseBody")

                val serverResponse = json.decodeFromString<ServerResponse>(responseBody)
                Result.success(serverResponse)

            } else {
                val errorBody = try {
                    BufferedReader(InputStreamReader(connection.errorStream, "UTF-8")).use { reader ->
                        reader.readText()
                    }
                } catch (e: Exception) {
                    "No error details"
                }

                Log.e(TAG, "Server error: $responseCode - $errorBody")
                Result.failure(Exception("Server returned error $responseCode: $errorBody"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send attendance", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "NOTE: serverIp parameter is ignored. Real IP comes from Wi-Fi gateway.")
            e.printStackTrace()
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Ping the server to check if it's reachable
     *
     * We use /status because your AttendanceServer exposes GET /status.
     */
    suspend fun ping(
        serverIp: String,       // ignored
        port: Int,
        endpoint: String = "/status"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val realServerIp = resolveTeacherIp()
            val url = URL("http://$realServerIp:$port$endpoint")

            Log.d(TAG, "Pinging teacher server at: $url (caller ip=$serverIp ignored)")

            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Ping response: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Result.success(true)
            } else {
                Result.success(false)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ping failed", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
