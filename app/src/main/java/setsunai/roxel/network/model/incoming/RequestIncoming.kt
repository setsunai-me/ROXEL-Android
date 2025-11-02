package setsunai.roxel.network.model.incoming

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RequestIncoming(
    @SerializedName("n")
    val name: String,
    @SerializedName("d")
    val data: JsonObject
) : Serializable