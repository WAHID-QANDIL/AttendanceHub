package org.wahid.attendancehub.data

import kotlinx.serialization.Serializable

/**
 * Represents a single attendance session
 */
@Serializable
data class AttendanceSession(
    val sessionId: String,
    val sessionName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val ssid: String,
    val students: List<SessionStudent>
)

/**
 * Student data stored in a session
 */
@Serializable
data class SessionStudent(
    val studentId: String,
    val name: String,
    val deviceId: String,
    val connectedAt: String,
    val timestamp: Long
)

