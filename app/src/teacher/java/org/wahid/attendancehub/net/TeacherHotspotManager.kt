package org.wahid.attendancehub.net

import android.content.Context
import android.net.wifi.WifiManager
import com.attendancehub.net.HotspotInfo
import com.attendancehub.net.HotspotManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TeacherHotspotManager(private val context: Context): HotspotManager {
    private var res: WifiManager.LocalOnlyHotspotReservation? = null

    override suspend fun start(): Result<HotspotInfo> = withContext(Dispatchers.Main) {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val deferred = CompletableDeferred<Result<HotspotInfo>>()


        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                res = reservation
                val config = reservation?.wifiConfiguration
                val info = HotspotInfo(
                    ssid = config?.SSID ?: "attendance_hub",
                    password = config?.preSharedKey ?: "attendance_hub_password"
                )
                deferred.complete(Result.success(info))

            }

            override fun onStopped() {
                super.onStopped()
                deferred.complete(Result.failure(Exception("Hotspot stopped")))
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                deferred.complete(Result.failure(Exception("Hotspot failed to start with reason code $reason")))
            }
        }, null)
        return@withContext deferred.await()

    }

    override suspend fun stop(): Result<Unit> = withContext(Dispatchers.Main) {
        res?.close()
        res = null
        Result.success(Unit)
    }



    override suspend fun connect(ssid: String, password: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Teacher does not implement connect"))
    }
}