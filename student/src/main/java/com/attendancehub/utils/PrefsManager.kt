package com.attendancehub.utils

import android.content.Context
import kotlin.apply

class PrefsManager(context: Context) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveStudentInfo(id: String,firstName:String,lastName:String) {
        prefs.edit().apply {
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_STUDENT_ID, id)
            apply()
        }
    }

    fun getStudentId(): String? = prefs.getString(KEY_STUDENT_ID, null)
    fun getStudentFirstName():String? = prefs.getString(KEY_FIRST_NAME,null)
    fun getStudentLastName():String? = prefs.getString(KEY_LAST_NAME,null)


    private companion object {
        private const val PREFS_NAME = "student_prefs"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_STUDENT_ID = "student_id"
    }
}