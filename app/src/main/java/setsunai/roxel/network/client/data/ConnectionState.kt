package setsunai.roxel.network.client.data

enum class ConnectionState(val state: String) {
    SUCCESS("OK"),
    CONNECT("CONNECT"),
    UNAVAILABLE("UNAVAILABLE"),
    AUTH("WAIT_AUTH");

    companion object {
        private val states: Array<ConnectionState> = arrayOf(SUCCESS, CONNECT, UNAVAILABLE, AUTH)
        fun get(state: String): ConnectionState = states.find { it.state == state.trim() } ?: CONNECT
    }
}