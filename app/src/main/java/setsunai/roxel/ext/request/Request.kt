package setsunai.roxel.ext.request

import setsunai.roxel.Roxel
import setsunai.roxel.ext.impl.Hook
import setsunai.roxel.ext.request.base.RequestImpl
import java.io.Serializable
import java.lang.reflect.Type

class Request<T : Serializable>(instance: Roxel.Instance, id: String, type: Type) :
    RequestImpl<T>(instance, id, type), Hook {
    override fun dispose() {}
}