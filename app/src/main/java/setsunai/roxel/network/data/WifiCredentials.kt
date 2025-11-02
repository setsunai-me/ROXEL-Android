package setsunai.roxel.network.data

data class WifiCredentials(
    val ssid: String,
    val passkey: String,
    val hidden: Boolean
) {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WifiCredentials

        if (hidden != other.hidden) return false
        if (ssid != other.ssid) return false
        if (passkey != other.passkey) return false

        return true
    }
}