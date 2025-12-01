package org.wahid.attendancehub.navigation

sealed class TeacherScreen(val route: String) {
    object Permissions : TeacherScreen("permissions")
    object Home : TeacherScreen("home")
    object HotspotActive : TeacherScreen("hotspot_active")
}

