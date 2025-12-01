package org.wahid.attendancehub.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for managing attendance sessions
 * Saves sessions to app's files directory
 */
class SessionRepository(private val context: Context) {

    private val TAG = "SessionRepository"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val sessionsDir: File by lazy {
        File(context.filesDir, "sessions").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Save a session to storage
     */
    suspend fun saveSession(session: AttendanceSession): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val filename = "session_${session.sessionId}.json"
            val file = File(sessionsDir, filename)

            val jsonString = json.encodeToString(session)
            file.writeText(jsonString)

            Log.d(TAG, "Session saved: ${session.sessionName} (${session.students.size} students)")
            Log.d(TAG, "File: ${file.absolutePath}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session", e)
            Result.failure(e)
        }
    }

    /**
     * Load all sessions from storage
     */
    suspend fun loadAllSessions(): Result<List<AttendanceSession>> = withContext(Dispatchers.IO) {
        try {
            val sessions = sessionsDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.mapNotNull { file ->
                    try {
                        val jsonString = file.readText()
                        json.decodeFromString<AttendanceSession>(jsonString)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse session file: ${file.name}", e)
                        null
                    }
                }
                ?.sortedByDescending { it.startTime }
                ?: emptyList()

            Log.d(TAG, "Loaded ${sessions.size} sessions from storage")
            Result.success(sessions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sessions", e)
            Result.failure(e)
        }
    }

    /**
     * Export session to CSV format
     */
    suspend fun exportSessionToCsv(session: AttendanceSession): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date(session.startTime))
            val filename = "attendance_${timestamp}.csv"

            val exportsDir = File(context.getExternalFilesDir(null), "exports").apply {
                if (!exists()) mkdirs()
            }
            val csvFile = File(exportsDir, filename)

            csvFile.bufferedWriter().use { writer ->
                // CSV Header
                writer.write("Session Name,${session.sessionName}\n")
                writer.write("Session ID,${session.sessionId}\n")
                writer.write("SSID,${session.ssid}\n")
                writer.write("Start Time,${formatTimestamp(session.startTime)}\n")
                writer.write("End Time,${session.endTime?.let { formatTimestamp(it) } ?: "In Progress"}\n")
                writer.write("Total Students,${session.students.size}\n")
                writer.write("\n")

                // Students Header
                writer.write("No.,Student ID,Name,Device ID,Connected At,Timestamp\n")

                // Students Data
                session.students.forEachIndexed { index, student ->
                    writer.write("${index + 1},${student.studentId},\"${student.name}\",${student.deviceId},${student.connectedAt},${student.timestamp}\n")
                }
            }

            Log.d(TAG, "CSV exported: ${csvFile.absolutePath}")
            Log.d(TAG, "File size: ${csvFile.length()} bytes")

            Result.success(csvFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CSV", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a session
     */
    suspend fun deleteSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val filename = "session_${sessionId}.json"
            val file = File(sessionsDir, filename)

            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Session deleted: $sessionId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete session", e)
            Result.failure(e)
        }
    }

    /**
     * Get total number of saved sessions
     */
    fun getSessionCount(): Int {
        return sessionsDir.listFiles()?.count { it.extension == "json" } ?: 0
    }

    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

