package org.wahid.attendancehub.student.ui.screens.attendanceSuccess

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wahid.attendancehub.base.BaseViewModel
import org.wahid.attendancehub.network.StudentHotspotConnectionManager

class AttendanceViewModel(application: Application) :BaseViewModel<AttendanceState, AttendanceEffect>(
    initialState = AttendanceState.Idle
), AttendanceInteractionListener {
    private val TAG = "AttendanceViewModel"
    private val hotspotManager = StudentHotspotConnectionManager.getInstance(context = application)

    override fun onReturnHome() {
        updateState { AttendanceState.IsReturningHome() }
        sendEffect(AttendanceEffect.ReturnHome)
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Disconnecting from network")
                hotspotManager.disconnect()
                updateState {
                    AttendanceState.Idle
                }
//                _uiState.value = StudentUiState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect", e)
                updateState {
                    AttendanceState.Idle // Force idle state even on error
                }
//                _uiState.value = StudentUiState.Idle
            }
        }
    }
}