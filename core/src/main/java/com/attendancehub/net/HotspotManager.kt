package com.attendancehub.net

interface HotspotManager {
    suspend fun start(): Result<HotspotInfo>
    suspend fun stop(): Result<Unit>
    suspend fun connect(ssid: String, password: String): Result<Unit>
}

data class HotspotInfo(
    val ssid: String,
    val password: String
)