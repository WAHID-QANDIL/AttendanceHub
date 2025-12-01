package com.attendancehub.student.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehub.api.AttendanceClient
import com.attendancehub.models.StudentAttendance
import com.attendancehub.net.StudentHotspotManager
import com.attendancehub.student.ui.screens.ConnectionStep
import com.attendancehub.student.ui.screens.WifiNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

sealed class StudentUiState {
    object Idle : StudentUiState()
    object QRScanning : StudentUiState()
    object ManualEntry : StudentUiState()
    data class Scanning(val networks: List<WifiNetwork>) : StudentUiState()
    data class Connecting(
        val networkName: String,
        val currentStep: ConnectionStep
    ) : StudentUiState()
    data class Success(
        val networkName: String,
        val connectedDuration: String,
        val markedAtTime: String
    ) : StudentUiState()
    data class Error(val message: String) : StudentUiState()
}

class StudentViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val MANUAL_CONNECTION_EXPIRY_MS = 2 * 60 * 60 * 1000L // 2 hours
    }

    private val TAG = "StudentViewModel"
    private val hotspotManager = StudentHotspotManager(application)
    private val attendanceClient = AttendanceClient()
    private val wifiScanner = com.attendancehub.net.WiFiScanner(application)

    private val _uiState = MutableStateFlow<StudentUiState>(StudentUiState.Idle)
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    private val _availableNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val availableNetworks: StateFlow<List<WifiNetwork>> = _availableNetworks.asStateFlow()

    private var connectionStartTime: Long = 0

    fun scanNetworks() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting WiFi scan...")

                // Scan for real WiFi networks
                val networks = withContext(Dispatchers.IO) {
                    wifiScanner.scanNetworks()
                }

                Log.d(TAG, "Scan complete: ${networks.size} networks found")

                if (networks.isEmpty()) {
                    Log.w(TAG, "No networks found - WiFi might be disabled")
                }

                _availableNetworks.value = networks
                _uiState.value = StudentUiState.Scanning(networks)
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning networks", e)
                // Fall back to empty list
                _availableNetworks.value = emptyList()
                _uiState.value = StudentUiState.Scanning(emptyList())
            }
        }
    }

    fun connectToNetwork(network: WifiNetwork, qrData: com.attendancehub.models.QRData? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = StudentUiState.Connecting(
                    network.ssid,
                    ConnectionStep.NETWORK_FOUND
                )

                connectionStartTime = System.currentTimeMillis()

                // Step 1: Network found
                delay(500)

                // Step 2: Authenticating
                _uiState.value = StudentUiState.Connecting(
                    network.ssid,
                    ConnectionStep.AUTHENTICATING
                )

                // Connect to WiFi with timeout
                val password = qrData?.password ?: "Attend2024" // Mock password
                Log.d(TAG, "Attempting to connect to: ${network.ssid}")

                val connectResult = withTimeoutOrNull(35000L) {
                    hotspotManager.connect(network.ssid, password)
                }

                if (connectResult == null) {
                    // Timeout occurred
                    Log.e(TAG, "Connection timeout")
                    _uiState.value = StudentUiState.Error(
                        "Connection timeout. Please check if WiFi is enabled and try again."
                    )
                    return@launch
                }

                if (connectResult.isFailure) {
                    val errorMsg = connectResult.exceptionOrNull()?.message ?: "Failed to connect"
                    Log.e(TAG, "Connection failed: $errorMsg")
                    _uiState.value = StudentUiState.Error(
                        "Failed to connect: $errorMsg"
                    )
                    return@launch
                }

                Log.d(TAG, "Successfully connected to WiFi")

                // Step 3: Registering attendance
                delay(1000)
                _uiState.value = StudentUiState.Connecting(
                    network.ssid,
                    ConnectionStep.REGISTERING
                )

                // Send attendance data
                val studentData = StudentAttendance(
                    studentId = "12345", // TODO: Get from user profile
                    name = "John Doe", // TODO: Get from user profile
                    timestamp = java.time.Instant.now().toString(),
                    deviceId = getDeviceId(),
                    sessionId = qrData?.sessionId ?: UUID.randomUUID().toString(),
                    token = qrData?.token
                )

                val serverIp = qrData?.serverIp ?: "192.168.49.1"
                val port = qrData?.port ?: 8080

                Log.d(TAG, "Sending attendance to $serverIp:$port")
                val sendResult = attendanceClient.sendAttendance(serverIp, port, studentData)

                if (sendResult.isFailure) {
                    val errorMsg = sendResult.exceptionOrNull()?.message ?: "Failed to mark attendance"
                    Log.e(TAG, "Attendance submission failed: $errorMsg")
                    _uiState.value = StudentUiState.Error(
                        "Connected but failed to mark attendance: $errorMsg"
                    )
                    return@launch
                }

                // Success!
                Log.d(TAG, "Attendance marked successfully")
                val duration = getDuration()
                val markedTime = getCurrentTime()

                _uiState.value = StudentUiState.Success(
                    networkName = network.ssid,
                    connectedDuration = duration,
                    markedAtTime = markedTime
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting", e)
                _uiState.value = StudentUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Disconnecting from network")
                hotspotManager.disconnect()
                _uiState.value = StudentUiState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect", e)
                _uiState.value = StudentUiState.Idle // Force idle state even on error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup on ViewModel destruction
        try {
            hotspotManager.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up hotspot manager", e)
        }
    }

    fun startQRScanning() {
        _uiState.value = StudentUiState.QRScanning
    }

    fun handleQRCode(qrData: com.attendancehub.models.QRData) {
        viewModelScope.launch {
            Log.d(TAG, "=== QR Code Handler Started ===")
            Log.d(TAG, "QR Code scanned - SSID: ${qrData.ssid}, IP: ${qrData.serverIp}, Port: ${qrData.port}")
            Log.d(TAG, "Session ID: ${qrData.sessionId}")
            Log.d(TAG, "Expiry: ${qrData.expiryTimestamp}")

            // Validate QR data
            if (qrData.ssid.isEmpty() || qrData.password.isEmpty()) {
                Log.e(TAG, "QR validation failed - empty SSID or password")
                _uiState.value = StudentUiState.Error("Invalid QR code data")
                return@launch
            }

            // Check if session is expired
            qrData.expiryTimestamp?.let { expiry ->
                if (System.currentTimeMillis() > expiry) {
                    Log.e(TAG, "QR session expired - current: ${System.currentTimeMillis()}, expiry: $expiry")
                    _uiState.value = StudentUiState.Error("Session has expired. Ask teacher to generate a new QR code.")
                    return@launch
                } else {
                    Log.d(TAG, "Session is valid - ${(expiry - System.currentTimeMillis()) / 1000 / 60} minutes remaining")
                }
            }

            // Create network object from QR data
            Log.d(TAG, "Creating network object from QR data")
            val network = WifiNetwork(
                ssid = qrData.ssid,
                signalStrength = 4,
                isSecured = true,
                isTeacherNetwork = true
            )

            // Connect using QR data
            Log.d(TAG, "Calling connectToNetwork with QR data")
            connectToNetwork(network, qrData)
        }
    }

    fun cancelQRScanning() {
        _uiState.value = StudentUiState.Idle
    }

    fun startManualEntry() {
        _uiState.value = StudentUiState.ManualEntry
    }

    fun cancelManualEntry() {
        _uiState.value = StudentUiState.Idle
    }

    fun connectManually(ssid: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "Manual connection - SSID: $ssid")

            // Create network object from manual entry
            val network = WifiNetwork(
                ssid = ssid,
                signalStrength = 4,
                isSecured = true,
                isTeacherNetwork = true
            )

            // Create mock QR data for manual connection
            val qrData = com.attendancehub.models.QRData(
                ssid = ssid,
                password = password,
                serverIp = "192.168.49.1", // Default IP
                port = 8080,
                sessionId = java.util.UUID.randomUUID().toString(),
                token = null,
                expiryTimestamp = System.currentTimeMillis() + MANUAL_CONNECTION_EXPIRY_MS
            )

            // Connect using manual data
            connectToNetwork(network, qrData)
        }
    }

    private fun getDeviceId(): String {
        // TODO: Store in SharedPreferences
        return UUID.randomUUID().toString()
    }

    private fun getDuration(): String {
        val durationMs = System.currentTimeMillis() - connectionStartTime
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / 1000) / 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun getCurrentTime(): String {
        val formatter = java.text.SimpleDateFormat("h:mm:ss a", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }
}
