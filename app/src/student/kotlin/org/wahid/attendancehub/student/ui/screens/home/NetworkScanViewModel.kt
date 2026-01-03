package org.wahid.attendancehub.student.ui.screens.home

import android.app.Application
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wahid.attendancehub.base.BaseViewModel
import org.wahid.attendancehub.core.SharedPrefs
import org.wahid.attendancehub.models.WifiNetwork
import org.wahid.attendancehub.network.WiFiScanner

class NetworkScanViewModel(
    private val application: Application
) : BaseViewModel<NetworkScanUiState, NetworkScanEffect>(
    initialState = NetworkScanUiState.Idle
) {
    private val TAG = "NetworkScanViewModel"
    private val wifiScanner = WiFiScanner(application)
    private val prefs = SharedPrefs.getInstance(application)

    private val _availableNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val availableNetworks: StateFlow<List<WifiNetwork>> = _availableNetworks.asStateFlow()

    init {
        scanNetworks()
    }

    fun scanNetworks() {
        updateState { NetworkScanUiState.Scanning }

        execute(
            onSuccess = { networks ->
                Log.d(TAG, "Scan complete: ${networks.size} networks found")
                _availableNetworks.value = networks
                updateState { NetworkScanUiState.Idle }
            },
            onError = { error ->
                Log.e(TAG, "Error scanning networks", error)
                _availableNetworks.value = emptyList()
                updateState { NetworkScanUiState.Idle }
            }
        ) {
            wifiScanner.scanNetworks()
        }
    }

    fun onScanQRClicked() {
        // Check if student info exists
        if (hasStudentInfo()) {
            sendEffect(NetworkScanEffect.NavigateToQRScanner)
        } else {
            updateState { NetworkScanUiState.ShowStudentInfoSheet(PendingAction.ScanQR) }
        }
    }

    fun onManualEntryClicked() {
        // Check if student info exists
        if (hasStudentInfo()) {
            sendEffect(NetworkScanEffect.NavigateToManualEntry)
        } else {
            updateState { NetworkScanUiState.ShowStudentInfoSheet(PendingAction.ManualEntry) }
        }
    }

    fun onNetworkSelected(network: WifiNetwork) {
        // Check if student info exists
        if (hasStudentInfo()) {
            // TODO: Handle network connection (you may need to create QRData from network)


            Log.d(TAG, "Network selected: ${network.ssid}")
        } else {
            updateState {
                NetworkScanUiState.ShowStudentInfoSheet(
                    PendingAction.ConnectToNetwork(network.ssid, network.password)
                )
            }
        }
    }

    fun onStudentInfoSaved() {
        val currentState = state.value
        if (currentState is NetworkScanUiState.ShowStudentInfoSheet) {
            when (val action = currentState.pendingAction) {
                is PendingAction.ScanQR -> {
                    sendEffect(NetworkScanEffect.NavigateToQRScanner)
                }
                is PendingAction.ManualEntry -> {
                    sendEffect(NetworkScanEffect.NavigateToManualEntry)
                }
                is PendingAction.ConnectToNetwork -> {
                    sendEffect(NetworkScanEffect.NavigateToConnecting(action.ssid, action.password))
                }
            }
        }
        updateState { NetworkScanUiState.Idle }
    }

    fun dismissStudentInfoSheet() {
        updateState { NetworkScanUiState.Idle }
    }

    private fun hasStudentInfo(): Boolean {
        val firstName = prefs.firstName.value
        val lastName = prefs.lastName.value
        val studentId = prefs.studentId.value
        return firstName.isNotBlank() && lastName.isNotBlank() && studentId.isNotBlank()
    }

    fun showStudentInfoSheet() {
        updateState {
            NetworkScanUiState.ShowStudentInfoSheet(
                PendingAction.ScanQR
            )
        }
    }
}

