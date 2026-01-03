package com.attendancehub.student.navigation

sealed class StudentScreen(val route: String) {
    object Permissions : StudentScreen("permissions")
    object StudentInfo : StudentScreen("student_info")
    object NetworkScan : StudentScreen("network_scan")
    object QRScanner : StudentScreen("qr_scanner")
    object Connecting : StudentScreen("connecting/{networkName}") {
        fun createRoute(networkName: String) = "connecting/$networkName"
    }
    object Success : StudentScreen("success")
}