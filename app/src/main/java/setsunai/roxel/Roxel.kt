package setsunai.roxel

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import setsunai.roxel.ext.hook.base.EffectImpl
import setsunai.roxel.ext.request.base.RequestImpl
import setsunai.roxel.network.client.data.ConnectionState
import setsunai.roxel.network.controller.NetworkController
import setsunai.roxel.network.controller.wifi.WiFiController
import setsunai.roxel.network.data.ServerCredentials
import setsunai.roxel.network.data.WifiCredentials
import setsunai.roxel.network.model.EmptyObject
import setsunai.roxel.network.model.incoming.EffectIncoming
import setsunai.roxel.network.model.incoming.RequestIncoming
import setsunai.roxel.network.model.payload.EffectPayload
import setsunai.roxel.network.model.payload.RequestPayload
import setsunai.roxel.network.processor.DataProcessor
import setsunai.roxel.utils.RoxelUtils.toCRC32
import java.io.Serializable
import java.util.Collections

class Roxel {
    class Instance {
        class Builder(private val sdk: Roxel, private val id: String) {
            private var ip: String = "0.0.0.0"
            private var token: String = ""
            private var udpPort: Int = 5001
            private var tcpPort: Int = 5002

            fun token(token: String): Builder {
                this.token = token
                return this
            }

            fun ip(ip: String): Builder {
                this.ip = ip
                return this
            }

            fun udp(port: Int): Builder {
                udpPort = port
                return this
            }

            fun tcp(port: Int): Builder {
                tcpPort = port
                return this
            }

            fun build(): Instance = Instance(sdk, id, ip, udpPort, tcpPort, token)
        }

        private val requests: MutableList<RequestImpl<out Serializable>> =
            Collections.synchronizedList(ArrayList())
        private val effects: MutableList<EffectImpl> = Collections.synchronizedList(ArrayList())

        private val token: String
        private val ip: String
        private val udpPort: Int
        private val tcpPort: Int

        private val sdk: Roxel
        private val hash: Long
        private val id: String

        private constructor(
            sdk: Roxel,
            id: String,
            ip: String,
            udpPort: Int,
            tcpPort: Int,
            token: String
        ) {
            this.udpPort = udpPort
            this.tcpPort = tcpPort
            this.token = token
            this.sdk = sdk
            this.ip = ip
            this.id = id
            hash = id.toCRC32()
            sdk.registerInstance(this)
        }

        fun credentials(): ServerCredentials {
            return ServerCredentials(id.toCRC32(), ip, tcpPort, udpPort, token)
        }

        fun id(): String {
            return id
        }

        private fun onRequestTransmit(
            requestImpl: RequestImpl<out Serializable>,
            serializable: Serializable?
        ) {
            sdk.onRequestTransmit(hash, requestImpl, serializable)
        }

        private fun onEffectUpdate(effectImpl: EffectImpl, state: Int) {
            sdk.onEffectUpdate(hash, effectImpl, state)
        }

        fun pushRequestUpdate(payload: RequestIncoming) {
            val hash = payload.name.toCRC32()
            requests.find { it.hash == hash }?.process(sdk.gson, payload.data)
        }

        fun pushEffectUpdate(payload: EffectIncoming) {
            for (key in payload.list.keySet()) {
                try {
                    val k = payload.list.get(key)
                    val hash = key.toCRC32()
                    effects.find { it.hash == hash }?.onValueUpdate(k.asInt)
                } catch (_: Exception) {
                }
            }
        }

        fun registerRequestImpl(requestImpl: RequestImpl<out Serializable>?) {
            requestImpl?.apply {
                if (requests.find { requestImpl.hash == it.hash } == null) {
                    requests += requestImpl
                    requestImpl.registerUpdateTransmitter(::onRequestTransmit)
                }
            }
        }

        fun registerEffectImpl(effectImpl: EffectImpl?) {
            effectImpl?.apply {
                if (effects.find { effectImpl.hash == it.hash } == null) {
                    effects += effectImpl
                    effectImpl.registerUpdateTransmitter(::onEffectUpdate)
                }
            }
        }
    }

    class Builder(private val activity: FragmentActivity) {
        private var passkey: String = ""
        private var ssid: String = ""
        private var hidden: Boolean = false

        fun passkey(passkey: String): Builder {
            this.passkey = passkey
            return this
        }

        fun ssid(ssid: String): Builder {
            this.ssid = ssid
            return this
        }

        fun hidden(hidden: Boolean): Builder {
            this.hidden = hidden
            return this
        }

        fun build(): Roxel = Roxel(activity, ssid, passkey, hidden)
    }

    private val instances: MutableMap<String, Instance> = Collections.synchronizedMap(HashMap())
    private val wifi: WiFiController = WiFiController()
    private val network: NetworkController =
        NetworkController(wifi.isConnected, ::onIncomingData, ::onConnectionState)
    private val processor: DataProcessor = DataProcessor()

    private val activity: FragmentActivity

    private val wifiCredentials: WifiCredentials
    private var selectedInstance: String? = null
    private var instance: Instance? = null

    private val isConnected: MutableLiveData<Boolean> = MutableLiveData()
    private val gson: Gson = GsonBuilder().create()

    private constructor(
        activity: FragmentActivity,
        ssid: String,
        passkey: String,
        hidden: Boolean
    ) {
        wifiCredentials = WifiCredentials(ssid, passkey, hidden)
        this.activity = activity
    }

    private fun registerInstance(instance: Instance) {
        if (instances.keys.find { it == instance.id() }.isNullOrEmpty()) {
            instances[instance.id()] = instance
        }
    }

    private fun onConnectionState(state: ConnectionState) {
        val newState = state == ConnectionState.SUCCESS
        if (isConnected.value != newState) {
            isConnected.postValue(newState)
        }
    }

    private fun onIncomingData(data: String) {
        if (processor.process(data)) {
            when (processor.event) {
                "req" -> {
                    val requestIncoming = processor.parse<RequestIncoming>() ?: return
                    instance?.pushRequestUpdate(requestIncoming)
                }

                "ef" -> {
                    val effectIncoming = processor.parse<EffectIncoming>() ?: return
                    instance?.pushEffectUpdate(effectIncoming)
                }

                else -> {}
            }
            processor.clear()
        }
    }

    private fun onRequestTransmit(
        hash: Long,
        requestImpl: RequestImpl<out Serializable>,
        serializable: Serializable?
    ) {
        try {
            network.transmit(
                hash,
                gson.toJson(
                    RequestPayload(
                        name = requestImpl.id,
                        data = serializable ?: EmptyObject()
                    )
                )
            )
        } catch (_: Throwable) {
        }
    }

    private fun onEffectUpdate(hash: Long, effectImpl: EffectImpl, state: Int) {
        try {
            network.transmit(
                hash,
                gson.toJson(EffectPayload(key = effectImpl.id, value = state))
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun listen(listener: ((Boolean) -> Unit)?) {
        isConnected.observe(activity, Observer { connected ->
            listener?.invoke(connected)
        })
    }

    fun launch(instance: Instance) {
        launch(instance.id())
    }

    fun launch(id: String) {
        wifi.launch(activity, wifiCredentials)
        if (selectedInstance != id) {
            instance = instances[id] ?: return
            network.launch(id, instance!!.credentials())
            selectedInstance = id
        }
    }
}