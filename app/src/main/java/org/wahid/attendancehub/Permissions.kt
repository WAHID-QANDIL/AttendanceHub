package org.wahid.attendancehub

object Permissions {
    const val cameraPermission: String = android.Manifest.permission.CAMERA
    const val locationPermission: String = android.Manifest.permission.ACCESS_FINE_LOCATION

    // Nearby wifi devices permission was added in API 33. Expose it via a function so callers can
    // check Build.VERSION before using it to avoid classloading issues on older devices.
    fun nearbyWifiPermission(): String = android.Manifest.permission.NEARBY_WIFI_DEVICES
}