package setsunai.roxel.network.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import setsunai.roxel.network.client.TcpClient
import setsunai.roxel.network.client.UdpClient
import setsunai.roxel.network.client.data.ConnectionState
import setsunai.roxel.network.data.ServerCredentials
import setsunai.roxel.utils.RoxelUtils.toCRC32

class NetworkController(
    wifiIsConnected: MutableStateFlow<Boolean>,
    private val callback: (String.() -> Unit)?,
    private val stateCallback: (ConnectionState.() -> Unit)?
) : ViewModel() {
    private var serverCredentials: ServerCredentials? = null
    private var selectedHash: Long = -1

    private val incomingChannel = Channel<String>(Channel.BUFFERED)
    private val flow = MutableStateFlow(ConnectionState.UNAVAILABLE)
    private val udp = UdpClient(flow, wifiIsConnected)
    private val tcp = TcpClient(flow, incomingChannel)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (msg in incomingChannel) {
                if (!isActive) break
                try {
                    withContext(Dispatchers.Main) {
                        callback?.invoke(msg)
                    }
                } catch (_: Throwable) {
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            flow.collectLatest { state ->
                withContext(Dispatchers.Main) {
                    stateCallback?.invoke(state)
                }
            }
        }
    }

    private fun onLaunch() {
        tcp.cancel()
        udp.cancel()
        serverCredentials?.apply {
            udp.updateCredentials(this)
            tcp.updateCredentials(this)
        }
        tcp.launch()
        udp.launch()
    }

    fun transmit(hash: Long, message: String) {
        if (selectedHash == hash) {
            CoroutineScope(Dispatchers.IO).launch {
                tcp.send(message)
            }
        }
    }

    fun launch(id: String, serverCredentials: ServerCredentials) {
        val hash = id.toCRC32()
        if (selectedHash != hash) {
            this.serverCredentials = serverCredentials
            selectedHash = hash
            onLaunch()
        }
    }

    fun shutdown() {
        serverCredentials = null
        selectedHash = -1
        tcp.cancel()
        udp.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        shutdown()
    }
}