package org.wahid.attendancehub.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class MainViewModel: androidx.lifecycle.ViewModel() {

    val visiblePermissionDialogQueue = mutableStateListOf<String>()
    private val TAG = "HotspotActivity"



    private fun dismissDialog() {
        visiblePermissionDialogQueue.removeAt(visiblePermissionDialogQueue.lastIndex)
    }

    fun onAction(actionType: ActionType) {
        when(actionType) {
            is ActionType.RequestPermission -> {
                onPermissionResult(permission = actionType.permission)
                // No additional state to manage for now
            }
           is ActionType.OpenAppSettings -> {
               openAppSettings(activity = actionType.activity)
                // No additional state to manage for now
            }
            is ActionType.QrCodeGeneration -> {
                startQrScan(actionType.context)
                // No additional state to manage for now
            }
        }
    }



   private fun onPermissionResult(permission: String) {
            if (!visiblePermissionDialogQueue.contains(permission)) {
                visiblePermissionDialogQueue.add(permission)
            }
    }

  private  fun openAppSettings(activity: Activity) {
        with(activity){
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: android.net.Uri = android.net.Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    private fun startQrScan(context: Context){

        val scanner = GmsBarcodeScanning.getClient(context).startScan()
        scanner.addOnSuccessListener { barcode ->
            val rawValue = barcode.rawValue
            val format = barcode.format
            val valueType = barcode.valueType
            Log.d(TAG, "startQrScan: format  = $format, valueType = $valueType raw value = $rawValue")


            if (valueType == Barcode.TYPE_WIFI){
                val ssid = barcode.wifi?.ssid
                val password = barcode.wifi?.password
                val encryptionType = barcode.wifi?.encryptionType
                Log.d("WIFI_INFO", "Scanned WiFi QR Code: SSID=$ssid, Password=$password, EncryptionType=$encryptionType")
            }

            Log.d("WIFI_INFO", "Scanned QR Code: $rawValue")
        }.addOnFailureListener { e ->
            Log.e("WIFI_INFO", "Error scanning QR Code", e)
        }

    }

}

sealed class ActionType {
    data class RequestPermission(val permission: String) : ActionType()
    data class OpenAppSettings(val activity: Activity) : ActionType()
    data class QrCodeGeneration(val context: Context) : ActionType()
}