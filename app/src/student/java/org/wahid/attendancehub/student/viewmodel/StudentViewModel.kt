package org.wahid.attendancehub.student.viewmodel

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.wahid.attendancehub.api.AttendanceClient
import org.wahid.attendancehub.models.ServerResponse
import org.wahid.attendancehub.models.StudentAttendance
import org.wahid.attendancehub.network.StudentHotspotConnectionManager
import org.wahid.attendancehub.student.ui.screens.ConnectionStep
import org.wahid.attendancehub.student.ui.screens.WifiNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.InternalSerializationApi
import java.util.UUID
import androidx.core.content.edit

sealed class StudentUiState {
    object Idle : StudentUiState()
    object StudentInfo : StudentUiState()
    object QRScanning : StudentUiState()
    object ManualEntry : StudentUiState()
    data class Scanning(val networks: List<WifiNetwork>) : StudentUiState()
    data class Connecting(
        val networkName: String,
        val currentStep: ConnectionStep,
    ) : StudentUiState()
    data class Success(
        val networkName: String,
        val connectedDuration: String,
        val markedAtTime: String,
    ) : StudentUiState()
    data class Error(val message: String) : StudentUiState()
}

@OptIn(InternalSerializationApi::class)
class StudentViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val MANUAL_CONNECTION_EXPIRY_MS = 2 * 60 * 60 * 1000L // 2 hours
        private const val PREFS_NAME = "student_prefs"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_DEVICE_ID = "device_id"
    }

    private val TAG = "StudentViewModel"
    private val hotspotManager = StudentHotspotConnectionManager(application)
    private val attendanceClient = AttendanceClient(context = application)
    private val wifiScanner = org.wahid.attendancehub.network.WiFiScanner(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<StudentUiState>(StudentUiState.Idle)
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    private val _availableNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val availableNetworks: StateFlow<List<WifiNetwork>> = _availableNetworks.asStateFlow()

    // Student info state
    private val _firstName = MutableStateFlow(prefs.getString(KEY_FIRST_NAME, "") ?: "")
    val firstName: StateFlow<String> = _firstName.asStateFlow()

    private val _lastName = MutableStateFlow(prefs.getString(KEY_LAST_NAME, "") ?: "")
    val lastName: StateFlow<String> = _lastName.asStateFlow()

    private val _studentId = MutableStateFlow(prefs.getString(KEY_STUDENT_ID, "") ?: "")
    val studentId: StateFlow<String> = _studentId.asStateFlow()

    private var connectionStartTime: Long = 0

    init {
        // Check if student info is saved
        if (!hasStudentInfo()) {
            _uiState.value = StudentUiState.StudentInfo
        }
    }

    private fun hasStudentInfo(): Boolean = listOf(
            _firstName.value,
           _lastName.value,
           _studentId.value).any { it.isNotBlank() }


    fun saveStudentInfo(firstName: String, lastName: String, studentId: String) {
        _firstName.value = firstName
        _lastName.value = lastName
        _studentId.value = studentId

        prefs.edit().apply {
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_STUDENT_ID, studentId)
            apply()
        }

        Log.d(TAG, "Student info saved: $firstName $lastName ($studentId)")
        // Navigate to network scan after saving
        scanNetworks()
    }

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
                    if (!wifiScanner.isWifiEnabled()) {
                        wifiScanner.enableWifi()
                        Log.w(TAG, "WiFi is turned ON programmatically")
                    }else if (!wifiScanner.hasLocationPermission())  {
                        //open app settings to grant location permission
                        Log.e(TAG, "Location permission not granted")
                    }
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

    fun connectToNetwork(network: WifiNetwork, qrData: org.wahid.attendancehub.models.QRData? = null) {
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

                // Step 3: Wait for network to be fully ready
                // The network binding may succeed but routes may not be established yet
                delay(3000) // Increased from 1000ms to 3000ms

                _uiState.value = StudentUiState.Connecting(
                    network.ssid,
                    ConnectionStep.REGISTERING
                )

                // Get student full name
                val fullName = "${_firstName.value} ${_lastName.value}".trim()

                // Send attendance data with real student info
                val studentData = StudentAttendance(
                    studentId = _studentId.value,
                    name = fullName,
                    timestamp = System.currentTimeMillis().toString(),
                    deviceId = getDeviceId(),
                    sessionId = qrData?.sessionId ?: UUID.randomUUID().toString(),
                    token = qrData?.token
                )

                Log.d(TAG, "Submitting attendance for: $fullName (${_studentId.value})")

                val serverIp = qrData?.serverIp ?: "192.168.49.1"
                val port = qrData?.port ?: 8080

                Log.d(TAG, "Sending attendance to $serverIp:$port")

                // Retry logic for attendance submission
                var sendResult: Result<ServerResponse>? = null
                var attempts = 0
                val maxAttempts = 3

                while (attempts < maxAttempts && (sendResult == null || sendResult.isFailure)) {
                    if (attempts > 0) {
                        Log.d(TAG, "Retry attempt $attempts of $maxAttempts")
                        delay(2000) // Wait 2 seconds between retries
                    }
                    attempts++
                    sendResult = attendanceClient.sendAttendance(serverIp, port, studentData)
                }

                if (sendResult?.isFailure == true) {
                    val errorMsg = sendResult.exceptionOrNull()?.message ?: "Failed to mark attendance"
                    Log.e(TAG, "Attendance submission failed after $attempts attempts: $errorMsg")
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

    @OptIn(InternalSerializationApi::class)
    fun handleQRCode(qrData: org.wahid.attendancehub.models.QRData) {
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
                signalStrength = 5,
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

    private fun resolveTeacherIp(context: Context): String {
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
            val qrData = org.wahid.attendancehub.models.QRData(
                ssid = ssid,
                password = password,
                serverIp = "resolveServerIp()", //TODO: we need to dynamically pass a context that will the service run on,
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
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit { putString(KEY_DEVICE_ID, deviceId) }
        }
        return deviceId
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
    private fun resolveServerIp(context: Context): String {
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
}
