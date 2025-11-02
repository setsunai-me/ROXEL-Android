package setsunai.roxel.ext

import setsunai.roxel.Roxel
import setsunai.roxel.ext.impl.Hook

class ControllerScope(val instance: Roxel.Instance) {
    private val hooks = mutableListOf<Hook>()

    fun disposeAll() {
        hooks.forEach { it.dispose() }
        hooks.clear()
    }
}

fun controller(
    instance: Roxel.Instance,
    block: ControllerScope.() -> Unit
) {
    val scope = ControllerScope(instance)
    try {
        scope.block()
    } finally {
        scope.disposeAll()
    }
}