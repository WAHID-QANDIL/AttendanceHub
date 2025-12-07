package org.wahid.attendancehub.navigation

sealed class TeacherScreens(val route: String) {
    object Permissions : TeacherScreens("permissions")
    object Home : TeacherScreens("home")
    object HotspotActive : TeacherScreens("hotspot_active")
}

