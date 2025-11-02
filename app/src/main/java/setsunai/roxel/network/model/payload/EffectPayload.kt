package setsunai.roxel.network.model.payload

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class EffectPayload(
    @SerializedName("e")
    val event: String = "ef",
    @SerializedName("k")
    val key: String,
    @SerializedName("v")
    val value: Int
) : Serializable
