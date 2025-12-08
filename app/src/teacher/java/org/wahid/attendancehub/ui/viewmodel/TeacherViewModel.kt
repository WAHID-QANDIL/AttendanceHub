package org.wahid.attendancehub.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehub.models.ConnectedStudent
import com.attendancehub.network.HotspotInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wahid.attendancehub.data.SessionRepository
import org.wahid.attendancehub.data.SessionStudent
import org.wahid.attendancehub.data.AttendanceSession
import org.wahid.attendancehub.network.AttendanceServer
import org.wahid.attendancehub.network.TeacherHotspotManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.core.graphics.createBitmap

sealed class TeacherUiState {
    object Idle : TeacherUiState()
    object Loading : TeacherUiState()
    data class HotspotActive(
        val ssid: String,
        val password: String,
        val qrBitmap: Bitmap?,
        val connectedStudents: List<ConnectedStudent>,
    ) : TeacherUiState()
    data class Error(val message: String) : TeacherUiState()
}

@OptIn(InternalSerializationApi::class)
class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "TeacherViewModel"
    private val hotspotManager = TeacherHotspotManager(application)
    private val attendanceServer = AttendanceServer(port = 8080)
    private val sessionRepository = SessionRepository(application)

    private val _uiState = MutableStateFlow<TeacherUiState>(TeacherUiState.Idle)
    val uiState: StateFlow<TeacherUiState> = _uiState.asStateFlow()

    private val _connectedStudents = MutableStateFlow<List<ConnectedStudent>>(emptyList())
    val connectedStudents: StateFlow<List<ConnectedStudent>> = _connectedStudents.asStateFlow()

    // Current session tracking
    private var currentSessionId: String? = null
    private var currentSessionStartTime: Long = 0
    private var currentSsid: String = ""

    init {
        Log.d(TAG, "TeacherViewModel initialized")
        Log.d(TAG, "Starting to observe attendance server student list")

        // Observe server's connected students and update UI
        viewModelScope.launch {
            attendanceServer.connectedStudents.collect { studentAttendances ->
                Log.d(TAG, "=== FLOW COLLECTION TRIGGERED ===")
                Log.d(TAG, "Received ${studentAttendances.size} students from server")

                if (studentAttendances.isNotEmpty()) {
                    studentAttendances.forEachIndexed { index, student ->
                        Log.d(TAG, "Student $index: ${student.name} (${student.studentId})")
                    }
                }

                // Convert StudentAttendance to ConnectedStudent for UI
                val connectedStudents = studentAttendances.map { attendance ->
                    // Get initials from name
                    val nameParts = attendance.name.split(" ")
                    val initials = if (nameParts.size >= 2) {
                        "${nameParts[0].firstOrNull() ?: ""}${nameParts.last().firstOrNull() ?: ""}"
                    } else {
                        attendance.name.take(2)
                    }.uppercase()

                    // Parse timestamp - it's a String representation of milliseconds
                    val timestampMillis = try {
                        attendance.timestamp.toLongOrNull() ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse timestamp: ${attendance.timestamp}", e)
                        System.currentTimeMillis()
                    }

                    // Format timestamp for display
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val formattedTime = timeFormat.format(Date(timestampMillis))

                    ConnectedStudent(
                        name = attendance.name,
                        deviceModel = attendance.deviceId.take(10),
                        connectedAt = formattedTime,
                        initials = initials,
                        isPresent = true
                    )
                }

                Log.d(TAG, "Converted to ${connectedStudents.size} ConnectedStudent objects")

                // Update the _connectedStudents StateFlow
                _connectedStudents.value = connectedStudents
                Log.d(TAG, "✅ Updated _connectedStudents StateFlow: ${connectedStudents.size} students")

                // Reflect updates in UI state when hotspot is active so any observer reacts.
                val currentState = _uiState.value
                if (currentState is TeacherUiState.HotspotActive) {
                    Log.d(TAG, "Updating HotspotActive uiState with latest students")
                    _uiState.value = currentState.copy(connectedStudents = connectedStudents)
                } else {
                    Log.d(TAG, "Skipping uiState update because current state is ${currentState.javaClass.simpleName}")
                }
            }
        }
    }

    fun startHotspot() {
        viewModelScope.launch {
            _uiState.value = TeacherUiState.Loading

            // Ensure previous session state is cleared before starting a new one.
            attendanceServer.clearStudents()
            _connectedStudents.value = emptyList()

            val result = hotspotManager.start()
            result.fold(
                onSuccess = { hotspotInfo ->
                    Log.d(TAG, "Hotspot started: ${hotspotInfo.ssid}")

                    // Initialize new session
                    currentSessionId = UUID.randomUUID().toString()
                    currentSessionStartTime = System.currentTimeMillis()
                    currentSsid = hotspotInfo.ssid
                    Log.d(TAG, "✅ New session created: $currentSessionId")

                    // Start HTTP server to receive attendance
                    val serverResult = attendanceServer.startServer()
                    if (serverResult.isFailure) {
                        Log.e(TAG, "Failed to start attendance server", serverResult.exceptionOrNull())
                    } else {
                        Log.d(TAG, "Attendance server started on port 8080")
                    }

                    // Generate QR code with network info
                    val qrBitmap = generateQRCode(hotspotInfo)

                    Log.d(TAG, "Creating HotspotActive state with ${_connectedStudents.value.size} students")
                    _uiState.value = TeacherUiState.HotspotActive(
                        ssid = hotspotInfo.ssid,
                        password = hotspotInfo.password,
                        qrBitmap = qrBitmap,
                        connectedStudents = _connectedStudents.value
                    )
                    Log.d(TAG, "✅ HotspotActive state created and set")
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to start hotspot", error)
                    _uiState.value = TeacherUiState.Error(
                        error.message ?: "Failed to start hotspot"
                    )
                }
            )
        }
    }

    /**
     * Generate QR code bitmap containing network connection info
     */
    private suspend fun generateQRCode(hotspotInfo: HotspotInfo): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // Create QR data object
            val qrData = com.attendancehub.models.QRData(
                ssid = hotspotInfo.ssid,
                password = hotspotInfo.password,
                serverIp = "192.168.49.1", // Default local-only hotspot IP
                port = 8080,
                sessionId = UUID.randomUUID().toString(),
                token = null,
                expiryTimestamp = System.currentTimeMillis() + (2 * 60 * 60 * 1000) // 2 hours
            )

            // Serialize to JSON
            val qrContent = Json.encodeToString(qrData)
            Log.d(TAG, "QR Content: $qrContent")

            // Generate QR code
            val writer = QRCodeWriter()
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1
            )

            val bitMatrix = writer.encode(
                qrContent,
                BarcodeFormat.QR_CODE,
                512,
                512,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix.get(x, y)) { 0xFF000000.toInt() /* Black*/ } else { 0xFFFFFFFF.toInt() // White
                    }
                }
            }

            createBitmap(width, height).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate QR code", e)
            null
        }
    }

    fun stopHotspot() {
        viewModelScope.launch {
            // Save session before stopping
            saveCurrentSession()

            // Stop attendance server
            attendanceServer.stopServer()
            attendanceServer.clearStudents()
            Log.d(TAG, "Attendance server stopped")

            val result = hotspotManager.stop()
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Hotspot stopped")
                    _uiState.value = TeacherUiState.Idle
                    _connectedStudents.value = emptyList()

                    // Reset session tracking
                    currentSessionId = null
                    currentSessionStartTime = 0
                    currentSsid = ""
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to stop hotspot", error)
                }
            )
        }
    }

    private suspend fun saveCurrentSession() {
        currentSessionId?.let { sessionId ->
            try {
                Log.d(TAG, "=== SAVING SESSION ===")

                // Get current students from server
                val studentAttendances = attendanceServer.connectedStudents.value

                // Convert to SessionStudent format
                val sessionStudents = studentAttendances.map { attendance ->
                    SessionStudent(
                        studentId = attendance.studentId,
                        name = attendance.name,
                        deviceId = attendance.deviceId,
                        connectedAt = attendance.timestamp,
                        timestamp = System.currentTimeMillis()
                    )
                }

                // Create session object
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val sessionName = "Session ${dateFormat.format(Date(currentSessionStartTime))}"

                val session = AttendanceSession(
                    sessionId = sessionId,
                    sessionName = sessionName,
                    startTime = currentSessionStartTime,
                    endTime = System.currentTimeMillis(),
                    ssid = currentSsid,
                    students = sessionStudents
                )

                Log.d(TAG, "Session: $sessionName")
                Log.d(TAG, "Duration: ${(session.endTime!! - session.startTime) / 1000 / 60} minutes")
                Log.d(TAG, "Students: ${session.students.size}")

                // Save to repository
                val result = sessionRepository.saveSession(session)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Session saved successfully!")
                        Log.d(TAG, "Total saved sessions: ${sessionRepository.getSessionCount()}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to save session", error)
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error saving session", e)
            }
        } ?: run {
            Log.w(TAG, "No active session to save")
        }
    }

    fun downloadStudentList() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== EXPORTING STUDENT LIST ===")
                Log.d(TAG, "Connected students: ${_connectedStudents.value.size}")

                // Get current session data
                val sessionId = currentSessionId ?: UUID.randomUUID().toString()
                val studentAttendances = attendanceServer.connectedStudents.value

                if (studentAttendances.isEmpty()) {
                    Log.w(TAG, "No students to export")
                    return@launch
                }

                // Convert to SessionStudent format
                val sessionStudents = studentAttendances.map { attendance ->
                    SessionStudent(
                        studentId = attendance.studentId,
                        name = attendance.name,
                        deviceId = attendance.deviceId,
                        connectedAt = attendance.timestamp,
                        timestamp = System.currentTimeMillis()
                    )
                }

                // Create temporary session object for export
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val sessionName = "Session ${dateFormat.format(Date(currentSessionStartTime))}"

                val session = AttendanceSession(
                    sessionId = sessionId,
                    sessionName = sessionName,
                    startTime = currentSessionStartTime,
                    endTime = System.currentTimeMillis(),
                    ssid = currentSsid,
                    students = sessionStudents
                )

                // Export to CSV
                val result = sessionRepository.exportSessionToCsv(session)
                result.fold(
                    onSuccess = { csvFile ->
                        Log.d(TAG, "CSV exported successfully: ${csvFile.absolutePath}")

                        // Share the file using Android's share intent
                        shareFile(csvFile)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to export CSV", error)
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error in downloadStudentList", e)
            }
        }
    }

    private fun shareFile(file: File) {
        try {
            val context = getApplication<Application>()
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Attendance List - ${file.nameWithoutExtension}")
                putExtra(android.content.Intent.EXTRA_TEXT, "Attendance list exported from AttendanceHub")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = android.content.Intent.createChooser(shareIntent, "Share Attendance List")
            chooserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(chooserIntent)
            Log.d(TAG, "Share intent launched successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to share file", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up hotspot and server when ViewModel is cleared
        attendanceServer.stopServer()
        viewModelScope.launch {
            hotspotManager.stop()
        }
    }
}
