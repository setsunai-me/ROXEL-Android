package setsunai.roxel.ext.hook.base

import setsunai.roxel.Roxel
import setsunai.roxel.utils.RoxelUtils.toCRC32

open class EffectImpl(instance: Roxel.Instance, val id: String) {
    protected var updateTransmitter: ((EffectImpl, Int) -> Unit)? = null
    open var hash: Long = id.toCRC32()

    init {
        instance.registerEffectImpl(this)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is EffectImpl -> hash == other.hash
            is Long -> hash == other
            else -> other?.hashCode() == hashCode()
        }
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    fun registerUpdateTransmitter(transmitter: ((EffectImpl, Int) -> Unit)?) {
        updateTransmitter = transmitter
    }

    open fun onValueUpdate(value: Int) {}
}