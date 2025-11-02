package setsunai.roxel.network.model.payload

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RequestPayload(
    @SerializedName("e")
    val event: String = "req",
    @SerializedName("n")
    val name: String,
    @SerializedName("d")
    val data: Serializable?
) : Serializable