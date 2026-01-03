package org.wahid.attendancehub.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wahid.attendancehub.models.StudentInfo
import java.util.UUID

class SharedPrefs private constructor(application: Context) {




    companion object {
        private const val MANUAL_CONNECTION_EXPIRY_MS = 2 * 60 * 60 * 1000L // 2 hours
        private const val PREFS_NAME = "student_prefs"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_DEVICE_ID = "device_id"

        @Volatile
        private var INSTANCE: SharedPrefs? = null
        fun getInstance(context: Context): SharedPrefs {
            return INSTANCE ?: synchronized(this) {
                val instance = SharedPrefs(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun saveStudentInfo(firstName: String, lastName: String, studentId: String,deviceId: String) {
        _firstName.value = firstName
        _lastName.value = lastName
        _studentId.value = studentId

        prefs.edit().apply {
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_STUDENT_ID, studentId)
            putString(KEY_DEVICE_ID, deviceId)
            apply()
        }
    }

    fun getStudentInfo(): StudentInfo = StudentInfo(
        firstName   = firstName.value,
        lastName    = lastName.value,
        studentId   = studentId.value,
        deviceId    = deviceId.value,
    )

    fun addDeviceId(deviceId: String) {
        _deviceId.value = deviceId
        prefs.edit().apply {
            putString(KEY_DEVICE_ID, deviceId)
            apply()
        }
    }


    fun clearStudentInfo() {
        _firstName.value = ""
        _lastName.value = ""
        _studentId.value = ""
        prefs.edit().apply {
            remove(KEY_FIRST_NAME)
            remove(KEY_LAST_NAME)
            remove(KEY_STUDENT_ID)
            apply()
        }
    }


    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // Student info state
    private val _firstName = MutableStateFlow(prefs.getString(KEY_FIRST_NAME, "") ?: "")
    val firstName: StateFlow<String> = _firstName.asStateFlow()

    private val _lastName = MutableStateFlow(prefs.getString(KEY_LAST_NAME, "") ?: "")
    val lastName: StateFlow<String> = _lastName.asStateFlow()

    private val _studentId = MutableStateFlow(prefs.getString(KEY_STUDENT_ID, "") ?: "")
    val studentId: StateFlow<String> = _studentId.asStateFlow()

    private val _deviceId = MutableStateFlow(prefs.getString(KEY_DEVICE_ID, "") ?: UUID.randomUUID().toString())
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()



}

