package setsunai.roxel.network.processor

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.Serializable

class DataProcessor {
    val gson: Gson = Gson()
    var element: JsonElement? = null
    var event: String? = null

    private fun parseEvent(payload: JsonObject) {
        if (payload.has("e")) {
            event = payload.get("e").asString
        }
    }

    fun process(data: String): Boolean {
        return try {
            element = JsonParser.parseString(data.trimIndent())
            if (element?.isJsonObject == true) {
                val payload = element!!.asJsonObject
                parseEvent(payload)
            }
            !event.isNullOrEmpty()
        } catch (_: Throwable) {
            false
        }
    }

    fun clear() {
        element = null
        event = null
    }

    inline fun <reified T : Serializable> parse(): T? {
        val type = object : TypeToken<T>() {}.type
        return try {
            gson.fromJson<T>(element, type)
        } catch (_: Throwable) {
            null
        }
    }
}