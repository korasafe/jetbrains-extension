package ai.korasafe.cloud

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpAndTelemetryTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun buildsMcpInitializeAndHeartbeatMessages() {
        val httpClient = io.ktor.client.HttpClient(io.ktor.client.engine.cio.CIO)
        val client = KoraSafeMcpWebSocketClient(httpClient)

        val initialize = mapper.readTree(client.initializeMessage("korasafe-jetbrains-test"))
        val heartbeat = mapper.readTree(client.heartbeatMessage())

        assertEquals("2.0", initialize["jsonrpc"].asText())
        assertEquals("initialize", initialize["method"].asText())
        assertEquals("korasafe-jetbrains-test", initialize["params"]["clientInfo"]["name"].asText())
        assertEquals("ping", heartbeat["method"].asText())
        httpClient.close()
    }

    @Test
    fun telemetryRequiresBothKoraSafeOptInAndJetBrainsDataSharing() {
        assertFalse(shouldSendTelemetry(CloudSettings(telemetryEnabled = false, jetBrainsDataSharingEnabled = true)))
        assertFalse(shouldSendTelemetry(CloudSettings(telemetryEnabled = true, jetBrainsDataSharingEnabled = false)))
        assertTrue(shouldSendTelemetry(CloudSettings(telemetryEnabled = true, jetBrainsDataSharingEnabled = true)))
    }
}
