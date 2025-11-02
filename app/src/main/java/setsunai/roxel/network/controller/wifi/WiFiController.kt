package setsunai.roxel.network.controller.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import setsunai.roxel.network.data.WifiCredentials
import setsunai.roxel.runtime.Console

class WiFiController : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private var credentials: WifiCredentials? = null

    val isConnected: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun launch(context: Context, credentials: WifiCredentials) {
        if (!this::connectivityManager.isInitialized) {
            connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        }
        if (!this::wifiManager.isInitialized) {
            wifiManager = context.getSystemService(WifiManager::class.java)
        }
        updateInternalCallback()
        relaunchIfNeeded(credentials)
    }

    private fun relaunchIfNeeded(credentials: WifiCredentials) {
        val isDifferentCredentials = this.credentials == null || this.credentials != credentials
        if (!wifiManager.isWifiEnabled || isDifferentCredentials) {
            if (isDifferentCredentials) {
                setWifiEnabled(false)
            }
            onDisconnect()
        }
    }

    private fun updateInternalCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.apply {
            try {
                unregisterNetworkCallback(this@WiFiController)
            } catch (_: Throwable) {
            }
            requestNetwork(request, this@WiFiController)
        }
    }

    private fun onConnected() {
        CoroutineScope(Dispatchers.IO).launch {
            isConnected.emit(true)
        }
    }

    private fun onDisconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            isConnected.emit(false)
        }
        callRecoveryStrategy()
    }

    private fun forgetAllNetworks() {
        Console.execute("cmd -w wifi list-networks")?.apply {
            bufferedReader().apply {
                forEachLine {
                    if (it.isNotBlank()) {
                        val id: Int? = it.substring(0, 1).toIntOrNull()
                        if (id != null) {
                            Console.execute("cmd -w wifi forget-network $id")
                        }
                    }
                }
                close()
            }
        }
    }


    fun setWifiEnabled(enabled: Boolean) {
        Console.execute("svc wifi ${if (enabled) "enable" else "disable"}")
    }

    private fun updateCredentials() {
        forgetAllNetworks()
        credentials?.apply {
            Console.execute(
                "cmd -w wifi connect-network $ssid wpa2 $passkey ${if (hidden) "-h" else ""} -p -r auto"
            )
        }
    }

    private fun callRecoveryStrategy() {
        if (!wifiManager.isWifiEnabled) {
            setWifiEnabled(true)
        }
        updateCredentials()
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        onConnected()
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        onDisconnect()
    }
}