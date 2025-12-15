package org.wahid.attendancehub.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi /**
 * Data encoded in QR code displayed by teacher
 * Students scan this to get connection parameters
 */
@Serializable
data class QRData(
    val ssid: String,                    // WiFi network name (from LocalOnlyHotspot)
    val password: String,                // WiFi password (from LocalOnlyHotspot)
    val serverIp: String,                // Teacher device IP (typically 192.168.49.1)
    val port: Int = 8080,               // HTTP server port
    val sessionId: String,               // Unique session identifier
    val token: String? = null,           // Optional authentication token
    val expiryTimestamp: Long? = null    // Optional session expiry (Unix timestamp)
)

@InternalSerializationApi /**
 * Student attendance data sent to teacher server
 */
@Serializable
data class StudentAttendance(
    val studentId: String,               // Unique student identifier
    val name: String,                    // Student full name
    val timestamp: String,               // ISO-8601 timestamp when attendance was marked
    val deviceId: String,                // Unique device identifier (for duplicate detection)
    val sessionId: String,               // Must match QR sessionId
    val token: String? = null            // Authentication token (if required)
)

@InternalSerializationApi /**
 * Server response after processing attendance
 */
@Serializable
data class ServerResponse(
    val status: String,                  // "confirmed", "rejected", "duplicate", etc.
    val serverTimestamp: String,         // Server-side timestamp (ISO-8601)
    val message: String? = null,         // Optional message (error details, success msg)
    val attendanceId: String? = null     // Unique ID assigned by server (for tracking)
)

@InternalSerializationApi /**
 * Real-time attendance update (for WebSocket/SSE streaming)
 */
@Serializable
data class AttendanceUpdate(
    val type: String,                    // "new_attendance", "session_closed", etc.
    val studentName: String? = null,
    val timestamp: String,
    val totalCount: Int? = null
)

@InternalSerializationApi /**
 * Session info for teacher dashboard
 */
@Serializable
data class SessionInfo(
    val sessionId: String,
    val startTime: String,               // ISO-8601
    val endTime: String? = null,         // ISO-8601 (null if active)
    val totalStudents: Int,
    val isActive: Boolean
)
data class ConnectedStudent(
    val name: String,
    val deviceModel: String,
    val connectedAt: String,
    val initials: String,
    val isPresent: Boolean = true
)
data class WifiNetwork(
    val ssid: String,
    val password: String = "",
    val signalStrength: Int, // 0-4
    val isSecured: Boolean,
    val isTeacherNetwork: Boolean = false
)

data class StudentInfo(
    val firstName: String,
    val lastName: String,
    val studentId: String,
    val deviceId: String,
)
