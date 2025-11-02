package setsunai.roxel.ext.hook

import com.google.gson.reflect.TypeToken
import setsunai.roxel.Roxel
import setsunai.roxel.ext.hook.base.EffectImpl
import setsunai.roxel.ext.impl.Hook
import java.lang.reflect.Type

class Effect<T>(instance: Roxel.Instance, id: String, private val type: Type) :
    EffectImpl(instance, id), Hook {
    companion object {
        private val typeBoolean: Type = object : TypeToken<Boolean>() {}.type
        private val typeInteger: Type = object : TypeToken<Int>() {}.type

        private val supportTypes: Array<Type> = arrayOf(typeBoolean, typeInteger)
    }

    private var listener: ((T) -> Unit)? = null

    @Suppress("UNCHECKED_CAST")
    private var lastValue: T = when (type) {
        typeBoolean -> false as T
        else -> 0 as T
    }

    init {
        if (type !in supportTypes) {
            throw RuntimeException(
                "Type [${type.typeName}] is not supported for Effect. Supported types: [${
                    supportTypes.joinToString(
                        ","
                    ) { it.typeName }
                }]")
        }
    }

    override fun onValueUpdate(value: Int) {
        @Suppress("UNCHECKED_CAST")
        lastValue = when (type) {
            typeBoolean -> (value > 0) as T
            else -> value as T
        }
        listener?.invoke(lastValue)
    }

    fun listen(listener: (T) -> Unit) {
        this.listener = listener
        this.listener?.invoke(lastValue)
    }

    fun set(value: T) {
        updateTransmitter?.invoke(
            this, when (type) {
                typeBoolean -> if (value as Boolean) 1 else 0
                typeInteger -> value as Int
                else -> -1
            }
        )
    }

    override fun dispose() {

    }
}