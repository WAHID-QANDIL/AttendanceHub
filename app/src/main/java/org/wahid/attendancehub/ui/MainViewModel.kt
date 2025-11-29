package org.wahid.attendancehub.ui

import android.app.Activity
import androidx.compose.runtime.mutableStateListOf

class MainViewModel: androidx.lifecycle.ViewModel() {

    val visiblePermissionDialogQueue = mutableStateListOf<String>()



    fun dismissDialog() {
        visiblePermissionDialogQueue.removeAt(visiblePermissionDialogQueue.lastIndex)
    }


    fun onPermissionResult(permission: String, isGranted: Boolean) {
        if (!isGranted) {
            if (!visiblePermissionDialogQueue.contains(permission)) {
                visiblePermissionDialogQueue.add(permission)
            }
        } else {
            visiblePermissionDialogQueue.remove(permission)
        }
    }

    fun openAppSettings(activity: Activity) {
        with(activity){
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: android.net.Uri = android.net.Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

}