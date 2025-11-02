package setsunai.roxel.network.model.incoming

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class EffectIncoming(
    @SerializedName("l")
    val list: JsonObject
) : Serializable