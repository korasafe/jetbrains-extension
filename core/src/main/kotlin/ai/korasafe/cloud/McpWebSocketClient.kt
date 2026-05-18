package ai.korasafe.cloud

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.withTimeoutOrNull

data class McpConnectionResult(
    val connected: Boolean,
    val decision: CloudCheckDecision,
    val received: List<String> = emptyList(),
)

class KoraSafeMcpWebSocketClient(
    private val client: HttpClient,
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    private val heartbeatTimeoutMs: Long = 5_000,
) {
    suspend fun connectOnce(
        webSocketUrl: String,
        settings: CloudSettings,
        clientName: String = "korasafe-jetbrains",
    ): McpConnectionResult {
        val decision = evaluateCloudCheckTrust(settings)
        if (!decision.allowed) return McpConnectionResult(connected = false, decision = decision)

        val received = mutableListOf<String>()
        client.webSocket(urlString = webSocketUrl) {
            send(initializeMessage(clientName))
            send(heartbeatMessage())

            val frame = withTimeoutOrNull(heartbeatTimeoutMs) { incoming.receive() }
            if (frame is Frame.Text) {
                received += frame.readText()
            }
        }

        return McpConnectionResult(connected = true, decision = decision, received = received)
    }

    fun initializeMessage(clientName: String): String =
        mapper.writeValueAsString(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to "initialize-1",
                "method" to "initialize",
                "params" to mapOf(
                    "protocolVersion" to "2024-11-05",
                    "clientInfo" to mapOf("name" to clientName, "version" to "0.1.0"),
                ),
            ),
        )

    fun heartbeatMessage(): String =
        mapper.writeValueAsString(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to "heartbeat-1",
                "method" to "ping",
                "params" to emptyMap<String, String>(),
            ),
        )
}
