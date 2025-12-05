package org.wahid.attendancehub.network

import android.util.Log
import com.attendancehub.models.StudentAttendance
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Simple HTTP server using ServerSocket that runs on teacher's device to receive student attendance
 *
 * Endpoints:
 * - POST /join - Students submit attendance data
 * - GET /status - Health check
 */
class AttendanceServer(private val port: Int = 8080) {

    private val TAG = "AttendanceServer"

    @OptIn(InternalSerializationApi::class)
    private val _connectedStudents = MutableStateFlow<List<StudentAttendance>>(emptyList())
    @OptIn(InternalSerializationApi::class)
    val connectedStudents: StateFlow<List<StudentAttendance>> = _connectedStudents.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    /**
     * Start the HTTP server
     */
    fun startServer(): Result<Unit> {
        return try {
            serverSocket = ServerSocket(port).apply {
                soTimeout = 1000 // 1 second timeout to allow checking isActive
            }
            Log.d(TAG, "Server socket created on port $port")
            Log.d(TAG, "Server listening on 0.0.0.0:$port (all interfaces)")
            Log.d(TAG, "Students should call: http://<teacher_gateway_ip>:$port/join")

            serverJob = scope.launch {
                Log.d(TAG, "Server accept loop started")
                while (isActive) {
                    try {
                        val client = serverSocket?.accept()
                        Log.d(TAG, "Client is null: ${client == null}")
                        if (client != null) {
                            Log.d(TAG, "Client connected from ${client.inetAddress.hostAddress}")
                            handleClient(client)
                        }
                    } catch (e: SocketTimeoutException) {
                        // Timeout - continue loop (allows checking isActive)
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Error accepting client", e)
                        }
                    }
                Log.d(TAG, "Server accept loop is still active")
                }
                Log.d(TAG, "Server accept loop ended")
            }

            Log.d(TAG, "Attendance server started successfully on port $port")
            Log.d(TAG, "Students can connect to: http://192.168.49.1:$port/join")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            Result.failure(e)
        }
    }

    private fun handleClient(client: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val writer = PrintWriter(OutputStreamWriter(client.getOutputStream()), true)

                // Read request line: "POST /join HTTP/1.1"
                val requestLine = reader.readLine() ?: return@launch
                Log.d(TAG, "Request: $requestLine")

                val parts = requestLine.split(" ")
                if (parts.size < 2) return@launch

                val method = parts[0]
                val path = parts[1]

                // Read headers
                val headers = mutableMapOf<String, String>()
                var line: String?
                var contentLength = 0
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.isEmpty()) break
                    val headerParts = line!!.split(": ", limit = 2)
                    if (headerParts.size == 2) {
                        headers[headerParts[0]] = headerParts[1]
                        if (headerParts[0].equals("Content-Length", ignoreCase = true)) {
                            contentLength = headerParts[1].toIntOrNull() ?: 0
                        }
                    }
                }

                // Handle routes
                when {
                    method == "POST" && path == "/join" -> {


                        // Read body
                        val body = CharArray(contentLength)
                        reader.read(body, 0, contentLength)
                        val bodyString = String(body)

                        Log.d(TAG, "POST: method $body")
                        handleJoinRequest(bodyString, writer)
                    }

                    method == "GET" && path == "/status" -> {
                        handleStatusRequest(writer)
                    }

                    else -> {
                        sendResponse(writer, 404, "Not Found", """{"error":"Endpoint not found"}""")
                    }
                }

                client.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client", e)
            } finally {
                try {
                    client.close()
                    Log.d(TAG, "client socket closed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing client socket", e)
                }
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun handleJoinRequest(body: String, writer: PrintWriter) {
        try {
            Log.d(TAG, "=== JOIN REQUEST RECEIVED ===")
            Log.d(TAG, "Body length: ${body.length}")
            Log.d(TAG, "Body content: $body")

            val studentAttendance = json.decodeFromString<StudentAttendance>(body)
            Log.d(
                TAG,
                "Parsed student: ${studentAttendance.name} (ID: ${studentAttendance.studentId})"
            )
            Log.d(TAG, "Device ID: ${studentAttendance.deviceId}")
            Log.d(TAG, "Timestamp: ${studentAttendance.timestamp}")

            // Check for duplicate
            Log.d(TAG, "Current students count: ${_connectedStudents.value.size}")
            val isDuplicate = _connectedStudents.value.any {
                it.deviceId == studentAttendance.deviceId ||
                        it.studentId == studentAttendance.studentId
            }

            if (isDuplicate) {
                Log.w(TAG, "Duplicate submission from ${studentAttendance.name}")
                sendResponse(
                    writer,
                    409,
                    "Conflict",
                    """{"status":"error","message":"Already registered"}"""
                )
            } else {
                // Add to list
                val oldSize = _connectedStudents.value.size
                _connectedStudents.value += studentAttendance
                val newSize = _connectedStudents.value.size

                Log.d(TAG, "Student added successfully!")
                Log.d(TAG, "Students count: $oldSize → $newSize")
                Log.d(TAG, "StateFlow emitted: ${_connectedStudents.value.size} students")

                // Log all current students
                _connectedStudents.value.forEachIndexed { index, student ->
                    Log.d(TAG, "  [$index] ${student.name} - ${student.studentId}")
                }

                sendResponse(
                    writer,
                    200,
                    "OK",
                    """{"status":"success","message":"Attendance recorded"}"""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing attendance", e)
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            e.printStackTrace()
            sendResponse(
                writer,
                500,
                "Internal Server Error",
                """{"status":"error","message":"${e.message}"}"""
            )
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun handleStatusRequest(writer: PrintWriter) {
        val response =
            """{"status":"ok","connectedStudents":${_connectedStudents.value.size},"timestamp":${System.currentTimeMillis()}}"""
        sendResponse(writer, 200, "OK", response)
    }

    private fun sendResponse(writer: PrintWriter, code: Int, status: String, body: String) {
        writer.println("HTTP/1.1 $code $status")
        writer.println("Content-Type: application/json")
        writer.println("Content-Length: ${body.length}")
        writer.println("Connection: close")
        writer.println()
        writer.println(body)
        writer.flush()
    }

    /**
     * Stop the HTTP server
     */
    fun stopServer() {
        serverJob?.cancel()
        serverSocket?.close()
        serverSocket = null
        Log.d(TAG, "Attendance server stopped")
    }

    /**
     * Clear all connected students
     */
    @OptIn(InternalSerializationApi::class)
    fun clearStudents() {
        _connectedStudents.value = emptyList()
        Log.d(TAG, "Cleared student list")
    }

    /**
     * Get current student count
     */
    @OptIn(InternalSerializationApi::class)
    fun getStudentCount(): Int = _connectedStudents.value.size
}