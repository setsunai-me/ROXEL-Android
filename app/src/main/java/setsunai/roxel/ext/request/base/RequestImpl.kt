package setsunai.roxel.ext.request.base

import com.google.gson.Gson
import com.google.gson.JsonObject
import setsunai.roxel.Roxel
import setsunai.roxel.utils.RoxelUtils.toCRC32
import java.io.Serializable
import java.lang.reflect.Type

open class RequestImpl<T : Serializable>(
    instance: Roxel.Instance,
    val id: String,
    private val type: Type
) {
    protected var updateTransmitter: ((RequestImpl<out Serializable>, Serializable?) -> Unit)? =
        null
    private var listener: (T.() -> Unit)? = null
    open var hash: Long = id.toCRC32()

    init {
        instance.registerRequestImpl(this)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is RequestImpl<T> -> hash == other.hash
            is Long -> hash == other
            else -> other?.hashCode() == hashCode()
        }
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    fun registerUpdateTransmitter(transmitter: ((RequestImpl<out Serializable>, Serializable?) -> Unit)?) {
        updateTransmitter = transmitter
    }

    fun process(gson: Gson, data: JsonObject) {
        try {
            gson.fromJson<T>(data, type)
                .apply(::onCall)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun onCall(data: T) {
        listener?.invoke(data)
    }

    fun listen(listener: T.() -> Unit) {
        this.listener = listener
    }

    fun send(payload: Serializable? = null) {
        updateTransmitter?.invoke(this@RequestImpl, payload)
    }
}