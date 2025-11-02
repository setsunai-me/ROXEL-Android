package setsunai.roxel.ext

import com.google.gson.reflect.TypeToken
import setsunai.roxel.Roxel
import setsunai.roxel.ext.hook.Effect
import setsunai.roxel.ext.hook.StateEffect
import setsunai.roxel.ext.request.Request
import java.io.Serializable
import kotlin.reflect.KProperty

class StatesEffectDelegate<T>(private val effect: StateEffect<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): StateEffect<T> = effect
}

class EffectDelegate<T>(private val effect: Effect<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Effect<T> = effect
}

class RequestDelegate<T : Serializable>(private val request: Request<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Request<T> = request
}

inline fun <reified T> ControllerScope.useStatesEffect(id: String, size: Int = 32): StatesEffectDelegate<T> {
    return StatesEffectDelegate(useStatesEffect(instance, id, size))
}

inline fun <reified T> ControllerScope.useEffect(id: String): EffectDelegate<T> {
    return EffectDelegate(useEffect(instance, id))
}

inline fun <reified T : Serializable> ControllerScope.useRequest(id: String): RequestDelegate<T> {
    return RequestDelegate(useRequest<T>(instance, id))
}

inline fun <reified T> useStatesEffect(controller: Roxel.Instance, id: String, size: Int = 32): StateEffect<T> {
    val type = object : TypeToken<T>() {}.type
    return StateEffect(controller, id, type, size)
}

inline fun <reified T> useEffect(controller: Roxel.Instance, id: String): Effect<T> {
    val type = object : TypeToken<T>() {}.type
    return Effect(controller, id, type)
}

inline fun <reified T : Serializable> useRequest(
    controller: Roxel.Instance,
    id: String
): Request<T> {
    val type = object : TypeToken<T>() {}.type
    return Request(controller, id, type)
}