package org.wahid.attendancehub.student.ui.screens.connecting

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.InternalSerializationApi
import org.koin.core.component.KoinComponent
import org.wahid.attendancehub.api.AttendanceClient
import org.wahid.attendancehub.base.BaseViewModel
import org.wahid.attendancehub.core.SharedPrefs
import org.wahid.attendancehub.models.QRData
import org.wahid.attendancehub.models.ServerResponse
import org.wahid.attendancehub.models.StudentAttendance
import org.wahid.attendancehub.network.StudentHotspotConnectionManager
import java.util.UUID

@OptIn(InternalSerializationApi::class)
class ConnectingScreenViewModel(
    private val application: Application
) : BaseViewModel<ConnectingScreenUiState, ConnectingScreenEffect>(
    initialState = ConnectingScreenUiState.Idle
), KoinComponent {

    private val TAG = "ConnectingScreenViewModel"

    private val hotspotManager = StudentHotspotConnectionManager.getInstance(application)
    private val attendanceClient = AttendanceClient(context = application)
    private val prefs = SharedPrefs.getInstance(context = application)

    fun connectToNetwork(qrData: QRData) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting connection process for network: ${qrData.ssid}")

                // Step 1: Network found
                updateState {
                    ConnectingScreenUiState.Connecting(
                        networkName = qrData.ssid,
                        currentStep = ConnectionStep.NETWORK_FOUND
                    )
                }
                delay(500)

                // Step 2: Authenticating
                updateState {
                    ConnectingScreenUiState.Connecting(
                        networkName = qrData.ssid,
                        currentStep = ConnectionStep.AUTHENTICATING
                    )
                }

                // Connect to WiFi with timeout
                Log.d(TAG, "Attempting to connect to: ${qrData.ssid}")
                val connectResult = withTimeoutOrNull(35000L) {
                    hotspotManager.connect(qrData.ssid, qrData.password)
                }

                if (connectResult == null) {
                    Log.e(TAG, "Connection timeout")
                    updateState {
                        ConnectingScreenUiState.Error(
                            "Connection timeout. Please check if WiFi is enabled and try again."
                        )
                    }
                    return@launch
                }

                if (connectResult.isFailure) {
                    val errorMsg = connectResult.exceptionOrNull()?.message ?: "Failed to connect"
                    Log.e(TAG, "Connection failed: $errorMsg")
                    updateState {
                        ConnectingScreenUiState.Error("Failed to connect: $errorMsg")
                    }
                    return@launch
                }

                Log.d(TAG, "Successfully connected to WiFi")

                // Wait for network to be fully ready
                delay(3000)

                // Step 3: Registering attendance
                updateState {
                    ConnectingScreenUiState.Connecting(
                        networkName = qrData.ssid,
                        currentStep = ConnectionStep.REGISTERING
                    )
                }

                // Send attendance
                sendAttendance(qrData)

            } catch (e: Exception) {
                Log.e(TAG, "Error during connection", e)
                updateState {
                    ConnectingScreenUiState.Error(e.message ?: "Unknown error occurred")
                }
            }
        }
    }

    private suspend fun sendAttendance(qrData: QRData) {
        val studentInfo = prefs.getStudentInfo()
        val fullName = "${studentInfo.firstName} ${studentInfo.lastName}"

        val studentData = StudentAttendance(
            studentId = studentInfo.studentId,
            name = fullName,
            timestamp = System.currentTimeMillis().toString(),
            deviceId = getDeviceId(),
            sessionId = qrData.sessionId,
            token = qrData.token
        )

        Log.d(TAG, "Submitting attendance for: $fullName (${studentInfo.studentId})")
        Log.d(TAG, "Sending attendance to ${qrData.serverIp}:${qrData.port}")

        // Retry logic for attendance submission
        var sendResult: Result<ServerResponse>? = null
        var attempts = 0
        val maxAttempts = 3

        while (attempts < maxAttempts && (sendResult == null || sendResult.isFailure)) {
            if (attempts > 0) {
                Log.d(TAG, "Retry attempt $attempts of $maxAttempts")
                delay(2000)
            }
            attempts++
            sendResult = attendanceClient.sendAttendance(
                qrData.serverIp,
                qrData.port,
                studentData
            )
        }

        if (sendResult?.isFailure == true) {
            val errorMsg = sendResult.exceptionOrNull()?.message ?: "Failed to mark attendance"
            Log.e(TAG, "Attendance submission failed after $attempts attempts: $errorMsg")
            updateState {
                ConnectingScreenUiState.Error("Connected but failed to mark attendance: $errorMsg")
            }
        } else {
            Log.d(TAG, "Attendance marked successfully")
            val markedTime = getCurrentTime()
            updateState {
                ConnectingScreenUiState.Success(
                    networkName = qrData.ssid,
                    markedAtTime = markedTime
                )
            }
            sendEffect(
                ConnectingScreenEffect.NavigateToSuccess(
                    networkName = qrData.ssid,
                    markedAtTime = markedTime
                )
            )
        }
    }

    private fun getDeviceId(): String {
        val deviceId = prefs.deviceId
        if (deviceId.value.isBlank()) {
            prefs.addDeviceId(UUID.randomUUID().toString())
        }
        return deviceId.value
    }

    private fun getCurrentTime(): String {
        val formatter = java.text.SimpleDateFormat("h:mm:ss a", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }
}

