package com.aisarathi.launcher.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryStatus(
    val percentage: Int = 100,
    val temperatureCelsius: Float = 30.0f,
    val isCharging: Boolean = true
)

data class NetworkStatus(
    val typeName: String = "Checking...",
    val isConnected: Boolean = true,
    val detail: String = ""
)

class BatteryNetworkMonitor(private val context: Context) {

    private val _batteryStatus = MutableStateFlow(BatteryStatus())
    val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

    private val _networkStatus = MutableStateFlow(NetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                val pct = if (level != -1 && scale != -1) (level * 100 / scale) else 100
                val tempC = temp / 10.0f
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                _batteryStatus.value = BatteryStatus(pct, tempC, charging)
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkInfo()
        }

        override fun onLost(network: Network) {
            updateNetworkInfo()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            updateNetworkInfo()
        }
    }

    fun start() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val request = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        updateNetworkInfo()
    }

    fun stop() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun updateNetworkInfo() {
        try {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)

            if (caps == null) {
                _networkStatus.value = NetworkStatus("Offline", false, "No Internet")
                return
            }

            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    var ssid = "Connected"
                    try {
                        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                        val info = wifiManager?.connectionInfo
                        val rawSsid = info?.ssid
                        if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                            ssid = rawSsid.replace("\"", "")
                        }
                    } catch (e: Exception) {
                        // Location permission not granted yet for SSID read
                        ssid = "Connected"
                    }
                    _networkStatus.value = NetworkStatus("Wi-Fi", true, ssid)
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    _networkStatus.value = NetworkStatus("SIM Data", true, "Mobile LTE/5G")
                }
                else -> {
                    _networkStatus.value = NetworkStatus("Connected", true, "Online")
                }
            }
        } catch (e: Exception) {
            _networkStatus.value = NetworkStatus("Connected", true, "Online")
        }
    }
}
