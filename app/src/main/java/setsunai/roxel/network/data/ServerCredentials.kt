package setsunai.roxel.network.data

data class ServerCredentials(
    val id: Long,
    val ip: String,
    val tcpPort: Int,
    val udpPort: Int,
    val token: String
)