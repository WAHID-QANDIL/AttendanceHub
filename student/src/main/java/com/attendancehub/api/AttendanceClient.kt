package com.attendancehub.api

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
 * 1. Must be connected to teacher's hotspot (via StudentHotspotManager.connect())
 * 2. Process must be bound to hotspot network (done automatically by StudentHotspotManager)
 *
 * Usage:
 * ```
 * val client = AttendanceClient()
 * val studentData = StudentAttendance(...)
 * val result = client.sendAttendance("192.168.49.1", 8080, studentData)
 * ```
 */
class AttendanceClient {

    private val TAG = "AttendanceClient"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Send attendance data to teacher's server via HTTP POST
     *
     * @param serverIp Teacher device IP (typically 192.168.49.1 for local-only hotspot)
     * @param port HTTP server port (typically 8080)
     * @param studentData Student attendance information
     * @param endpoint API endpoint (default: "/join")
     * @return Result<ServerResponse> containing server response or error
     */
    suspend fun sendAttendance(
        serverIp: String,
        port: Int,
        studentData: StudentAttendance,
        endpoint: String = "/join"
    ): Result<ServerResponse> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            // Build URL
            val url = URL("http://$serverIp:$port$endpoint")
            Log.d(TAG, "Sending attendance to: $url")

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
                // Success - parse response
                val responseBody = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { reader ->
                    reader.readText()
                }

                Log.d(TAG, "Response body: $responseBody")

                val serverResponse = json.decodeFromString<ServerResponse>(responseBody)
                Result.success(serverResponse)

            } else {
                // Error - read error stream
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
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Ping the server to check if it's reachable
     * Useful for verifying network connection before sending attendance
     *
     * @param serverIp Teacher device IP
     * @param port HTTP server port
     * @return Result<Boolean> - true if server responds, false/error otherwise
     */
    suspend fun ping(
        serverIp: String,
        port: Int,
        endpoint: String = "/ping"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val url = URL("http://$serverIp:$port$endpoint")
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

