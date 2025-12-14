package org.wahid.attendancehub.student.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.wahid.attendancehub.api.AttendanceClient
import org.wahid.attendancehub.models.ServerResponse
import org.wahid.attendancehub.models.StudentAttendance
import org.wahid.attendancehub.network.StudentHotspotConnectionManager
import org.wahid.attendancehub.student.ui.screens.ConnectionStep
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
import org.wahid.attendancehub.models.WifiNetwork

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
        val markedAtTime: String,
    ) : StudentUiState()
    data class Error(val message: String) : StudentUiState()
}

@OptIn(InternalSerializationApi::class)
class StudentViewModel(application: Application) : AndroidViewModel(application) {


//    private val TAG = "StudentViewModel"
//
//    private val _uiState = MutableStateFlow<StudentUiState>(StudentUiState.Idle)
//    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()
//
//    private val _availableNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
//    val availableNetworks: StateFlow<List<WifiNetwork>> = _availableNetworks.asStateFlow()



//    init {
//        // Check if student info is saved
//        if (!hasStudentInfo()) {
//            _uiState.value = StudentUiState.StudentInfo
//        }
//    }
//
//    private fun hasStudentInfo(): Boolean = listOf(
//            _firstName.value,
//           _lastName.value,
//           _studentId.value).any { it.isNotBlank() }
//
//
//    fun saveStudentInfo(firstName: String, lastName: String, studentId: String) {
//        _firstName.value = firstName
//        _lastName.value = lastName
//        _studentId.value = studentId
//
//        prefs.edit().apply {
//            putString(KEY_FIRST_NAME, firstName)
//            putString(KEY_LAST_NAME, lastName)
//            putString(KEY_STUDENT_ID, studentId)
//            apply()
//        }
//
//        Log.d(TAG, "Student info saved: $firstName $lastName ($studentId)")
//        // Navigate to network scan after saving
//        scanNetworks()
//    }
//
//    fun scanNetworks() {
//        viewModelScope.launch {
//            try {
//                Log.d(TAG, "Starting WiFi scan...")
//
//                // Scan for real WiFi networks
//                val networks = withContext(Dispatchers.IO) {
//                    wifiScanner.scanNetworks()
//                }
//
//                Log.d(TAG, "Scan complete: ${networks.size} networks found")
//
//                if (networks.isEmpty()) {
//                    Log.w(TAG, "No networks found - WiFi might be disabled")
//                    if (!wifiScanner.isWifiEnabled()) {
//                        wifiScanner.enableWifi()
//                        Log.w(TAG, "WiFi is turned ON programmatically")
//                    }else if (!wifiScanner.hasLocationPermission())  {
//                        //open app settings to grant location permission
//                        Log.e(TAG, "Location permission not granted")
//                    }
//                }
//
//                _availableNetworks.value = networks
//                _uiState.value = StudentUiState.Scanning(networks)
//            } catch (e: Exception) {
//                Log.e(TAG, "Error scanning networks", e)
//                // Fall back to empty list
//                _availableNetworks.value = emptyList()
//                _uiState.value = StudentUiState.Scanning(emptyList())
//            }
//        }
//    }
//
//
//
//
//
//    override fun onCleared() {
//        super.onCleared()
//        // Cleanup on ViewModel destruction
//        try {
//            hotspotManager.disconnect()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error cleaning up hotspot manager", e)
//        }
//    }

//    fun startQRScanning() {
//        _uiState.value = StudentUiState.QRScanning
//    }
//
//
//
//    fun cancelQRScanning() {
//        _uiState.value = StudentUiState.Idle
//    }
//
//    fun startManualEntry() {
//        _uiState.value = StudentUiState.ManualEntry
//    }
//
//    fun cancelManualEntry() {
//        _uiState.value = StudentUiState.Idle
//    }

//    private fun resolveTeacherIp(context: Context): String {
//        val wifiManager =
//            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        val dhcpInfo = wifiManager.dhcpInfo
//
//        val gatewayInt = dhcpInfo.gateway
//        if (gatewayInt == 0) {
//            // This usually means "not connected to Wi-Fi / hotspot"
//            throw IllegalStateException("Gateway IP is 0. Are you connected to the teacher hotspot?")
//        }
//
//        val gatewayIp = Formatter.formatIpAddress(gatewayInt)
//        Log.d(TAG, "Resolved teacher gateway IP: $gatewayIp")
//        return gatewayIp
//    }


    fun connectManually(ssid: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "Manual connection - SSID: $ssid")

            // Create network object from manual entry
            val network = WifiNetwork(
                ssid = ssid,
                password = password,
                signalStrength = 4,
                isSecured = true,
                isTeacherNetwork = true
            )

            // Connect using manual data
            connectToNetwork(network)
        }
    }



//    private fun getDuration(): String {
//        val durationMs = System.currentTimeMillis() - connectionStartTime
//        val seconds = (durationMs / 1000) % 60
//        val minutes = (durationMs / 1000) / 60
//        return String.format("%d:%02d", minutes, seconds)
//    }





//    private fun resolveServerIp(context: Context): String {
//        val wifiManager =
//            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        val dhcpInfo = wifiManager.dhcpInfo
//
//        val gatewayInt = dhcpInfo.gateway
//        if (gatewayInt == 0) {
//            // This usually means "not connected to Wi-Fi / hotspot"
//            throw IllegalStateException("Gateway IP is 0. Are you connected to the teacher hotspot?")
//        }
//
//        val gatewayIp = Formatter.formatIpAddress(gatewayInt)
//        Log.d(TAG, "Resolved teacher gateway IP: $gatewayIp")
//        return gatewayIp
//    }
}
