package org.wahid.attendancehub.student.navigation

import android.net.Uri
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wahid.attendancehub.models.QRData

sealed class StudentScreen(val route: String) {
    object Permissions : StudentScreen("permissions")
    object StudentInfo : StudentScreen("student_info")
    object NetworkScan : StudentScreen("network_scan")
    object QRScanner : StudentScreen("qr_scanner")

    @OptIn(InternalSerializationApi::class)
    object Connecting : StudentScreen("connecting/{qrDataJson}") {
        fun createRoute(networkName: String) = "connecting/$networkName"

        fun createRouteWithQrData(qrData: QRData): String {
            val json = Json.encodeToString(qrData)
            val encodedJson = Uri.encode(json)
            return "connecting/$encodedJson"
        }
    }

    object Success : StudentScreen("success/{networkName}/{markedAtTime}") {
        fun createRoute(networkName: String, markedAtTime: String): String {
            val encodedNetworkName = Uri.encode(networkName)
            val encodedTime = Uri.encode(markedAtTime)
            return "success/$encodedNetworkName/$encodedTime"
        }
    }
}

