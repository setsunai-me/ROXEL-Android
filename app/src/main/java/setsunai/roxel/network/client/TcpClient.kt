package setsunai.roxel.network.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import setsunai.roxel.network.client.base.ClientBase
import setsunai.roxel.network.client.data.ConnectionState
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class TcpClient(
    private val stateFlow: MutableStateFlow<ConnectionState>,
    private val messageChannel: Channel<String>?
) : ClientBase() {

    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var socket: Socket? = null

    private val clientMutex = Mutex()
    private var readerJob: Job? = null

    private var credentialsHash: Long = -1

    override suspend fun onLaunch(): Job? = withContext(Dispatchers.IO) {
        supervisorScope {
            launch(Dispatchers.IO) {
                combine(
                    credentialsFlow.filterNotNull(),
                    stateFlow
                ) { credentials, state -> credentials to state }
                    .collectLatest { (credentials, state) ->
                        if (credentialsHash != credentials.id) {
                            stateFlow.emit(ConnectionState.UNAVAILABLE)
                            credentialsHash = credentials.id
                        }
                        when (state) {
                            ConnectionState.CONNECT -> {
                                connect(credentials.ip, credentials.tcpPort)
                            }

                            ConnectionState.AUTH -> {
                                send("AUTH:${credentials.token}")
                            }

                            ConnectionState.UNAVAILABLE -> {
                                disconnect()
                            }

                            else -> {}
                        }
                    }
            }
        }
    }

    private suspend fun connect(ip: String, port: Int) {
        disconnect()

        try {
            socket = Socket(ip, port)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))

            startReader()
        } catch (_: Throwable) {
            stateFlow.emit(ConnectionState.UNAVAILABLE)
        }
    }

    private fun startReader() {
        readerJob?.cancel()
        readerJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive) {
                    try {
                        val line = reader?.readLine() ?: break
                        if (line.isNotEmpty()) {
                            messageChannel?.send(line)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            } catch (_: Exception) {
            } finally {
                reader?.close()
                reader = null
                stateFlow.emit(ConnectionState.UNAVAILABLE)
            }
        }
    }

    fun disconnect() {
        readerJob?.cancel()
        readerJob = null

        try {
            socket?.close()
        } catch (_: Exception) {
        }

        try {
            writer?.close()
        } catch (_: Exception) {
        }

        socket = null
        writer = null
    }

    suspend fun send(message: String) {
        clientMutex.withLock {
            try {
                writer?.apply {
                    write("${message}\r\n\r\n")
                    flush()
                }
            } catch (_: Throwable) {
                stateFlow.value = ConnectionState.UNAVAILABLE
            }
        }
    }

    private fun closeClient() {
        disconnect()
    }

    override fun onClose() {
        closeClient()
    }
}