package setsunai.roxel.network.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import setsunai.roxel.network.client.base.ClientBase
import setsunai.roxel.network.client.data.ConnectionState
import setsunai.roxel.network.data.ServerCredentials
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.coroutines.coroutineContext

class UdpClient(
    private val stateFlow: MutableStateFlow<ConnectionState>,
    private val wifiIsConnected: MutableStateFlow<Boolean>
) : ClientBase() {
    companion object {
        private const val MAX_FAILURES: Int = 3
    }

    private var failureCount: Int = 0

    override suspend fun onLaunch(): Job? = withContext(Dispatchers.IO) {
        supervisorScope {
            launch(Dispatchers.IO) {
                credentialsFlow.collectLatest { credentials ->
                    if (credentials == null) {
                        delay(1000)
                        return@collectLatest
                    }
                    while (isActive) {
                        launchClient(credentials)
                        delay(1000)
                    }
                }
            }
        }
    }

    private suspend fun launchClient(credentials: ServerCredentials) {
        try {
            DatagramSocket(credentials.udpPort).use { socket ->
                while (coroutineContext.isActive) {
                    if (!wifiIsConnected.value) {
                        stateFlow.emit(ConnectionState.UNAVAILABLE)
                        delay(1000)
                        continue
                    }
                    val address = InetAddress.getByName(credentials.ip)
                    val sendData = "VERIFY".toByteArray()
                    val sendPacket =
                        DatagramPacket(
                            sendData,
                            sendData.size,
                            address,
                            credentials.udpPort
                        )

                    val buffer = ByteArray(1024)
                    val packet = DatagramPacket(buffer, buffer.size)

                    try {
                        socket.send(sendPacket)
                        socket.soTimeout = 1000
                    } catch (_: Throwable) {
                    }

                    try {
                        socket.receive(packet)
                        val msg = String(packet.data, 0, packet.length)
                        stateFlow.emit(ConnectionState.get(msg))
                        failureCount = 0
                    } catch (_: Throwable) {
                        if (++failureCount >= MAX_FAILURES) {
                            stateFlow.emit(ConnectionState.UNAVAILABLE)
                            failureCount = 0
                        }
                    }

                    delay(350)
                }
            }
        } catch (_: Throwable) {
        }
    }
}