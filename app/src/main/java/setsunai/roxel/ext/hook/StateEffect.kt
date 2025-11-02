package setsunai.roxel.ext.hook

import com.google.gson.reflect.TypeToken
import setsunai.roxel.Roxel
import setsunai.roxel.ext.hook.base.EffectImpl
import setsunai.roxel.ext.impl.Hook
import java.lang.reflect.Type
import java.util.Collections
import kotlin.math.min

class StateEffect<T>(
    instance: Roxel.Instance,
    id: String,
    private val type: Type,
    private val size: Int
) :
    EffectImpl(instance, id), Hook {
    companion object {
        private val typeBoolean: Type = object : TypeToken<Boolean>() {}.type
        private val typeInteger: Type = object : TypeToken<Int>() {}.type

        private val supportTypes: Array<Type> = arrayOf(typeBoolean, typeInteger)

        private fun isBitSet(value: UInt, index: Int): Boolean {
            require(index in 0..32)
            return (value shr index) and 1u == 1u
        }

        private fun setBit(value: UInt, index: Int, state: Boolean): UInt {
            require(index in 0..<32)
            return if (state) {
                value or (1u shl index)
            } else {
                value and (1u shl index).inv()
            }
        }
    }

    private val callbacks: MutableMap<Int, ((T) -> Unit)?> =
        Collections.synchronizedMap(HashMap())
    private val allCallbacks: MutableList<((T) -> Unit)?> =
        Collections.synchronizedList(ArrayList())
    private var bundle: UInt = 0.toUInt()
    private val mask: UInt = if (size >= 32) UInt.MAX_VALUE else (1u shl size) - 1u

    init {
        if (type !in supportTypes) {
            throw RuntimeException(
                "Type [${type.typeName}] is not supported for StateEffect. Supported types: [${
                    supportTypes.joinToString(
                        ","
                    ) { it.typeName }
                }]")
        }
    }

    override fun onValueUpdate(value: Int) {
        for (i in 0..<min(size, 32)) {
            val state = isBitSet(value.toUInt(), i)
            bundle = setBit(bundle, i, state)
            @Suppress("UNCHECKED_CAST")
            callbacks[i]?.invoke(
                when (type) {
                    typeInteger -> (if (state) 1 else 0) as T
                    else -> state as T
                }
            )
        }
        allCallbacks.forEach { action ->
            @Suppress("UNCHECKED_CAST")
            action?.invoke((bundle == mask).let { value ->
                when (type) {
                    typeInteger -> (if (value) 1 else 0)
                    else -> value
                }
            } as T)
        }
    }

    fun listen(index: Int, callback: ((T) -> Unit)?) {
        if (index < min(size, 32)) {
            callbacks[index] = callback
        }
    }

    fun listenAll(callback: ((T) -> Unit)?) {
        if (callback != null) {
            allCallbacks += callback
        }
    }

    fun set(index: Int, state: T) {
        if (index < min(size, 32)) {
            val newBundle = setBit(
                bundle, index, when (type) {
                    typeInteger -> (state as Int) > 0
                    else -> state as Boolean
                }
            )
            updateTransmitter?.invoke(this, newBundle.toInt())
        }
    }

    fun set(state: T) {
        val newBundle = if (when (type) {
                typeInteger -> (state as Int) > 0
                else -> state as Boolean
            }
        ) mask else 0.toUInt()
        updateTransmitter?.invoke(this, newBundle.toInt())
    }

    override fun dispose() {}
}