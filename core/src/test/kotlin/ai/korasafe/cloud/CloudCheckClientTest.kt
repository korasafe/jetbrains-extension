package ai.korasafe.cloud

import ai.korasafe.analyzers.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudCheckClientTest {
    @Test
    fun blocksCloudChecksInUntrustedWorkspace() = kotlinx.coroutines.test.runTest {
        val transport = RecordingTransport()
        val client = CloudCheckClient(transport)
        val settings = CloudSettings(
            apiKey = "ks_test",
            enableCloudChecks = true,
            workspaceTrusted = false,
            forceCloudOnUntrusted = true,
        )

        val findings = client.submitAudit(listOf(CloudCheckFile("src/App.kt", "println(\"hi\")")), settings)

        assertEquals(emptyList(), findings)
        assertEquals(emptyList(), transport.posts)
        val decision = evaluateCloudCheckTrust(settings)
        assertEquals(CloudCheckBlockReason.WorkspaceUntrusted, decision.reason)
        assertTrue(decision.warning!!.contains("not supported"))
    }

    @Test
    fun submitsHeuristicsRunAndMapsImmediateCloudFindings() = kotlinx.coroutines.test.runTest {
        val transport = RecordingTransport(
            postResponse = """
                {"status":"completed","findings":[{
                  "rule":"cloud-secret",
                  "severity":"high",
                  "line_number":7,
                  "description":"Cloud detected a secret",
                  "remediation":"Move to SecretStorage",
                  "category":"secrets",
                  "regulation_refs":["OWASP A07:2021"]
                }]}
            """.trimIndent(),
        )
        val client = CloudCheckClient(transport)
        val settings = CloudSettings(apiKey = "ks_test", enableCloudChecks = true, workspaceTrusted = true)

        val findings = client.submitAudit(listOf(CloudCheckFile("src/App.kt", "val key = \"sk-123\"")), settings)

        assertEquals("/api/v2/heuristics/run", transport.posts.single().path)
        assertEquals("jetbrains", transport.posts.single().payload["surface"])
        assertEquals("save", transport.posts.single().payload["trigger"])
        assertEquals("cloud-secret", findings.single().rule)
        assertEquals(Severity.High, findings.single().severity)
        assertEquals(7, findings.single().line)
        assertEquals(listOf("OWASP A07:2021"), findings.single().regulationRefs)
    }

    @Test
    fun pollsRunAndFetchesFindingsWhenSubmitReturnsRunId() = kotlinx.coroutines.test.runTest {
        val transport = RecordingTransport(
            postResponse = """{"run_id":"run-1","status":"queued"}""",
            getResponses = ArrayDeque(
                listOf(
                    """{"run":{"id":"run-1","status":"completed","finding_counts":{"high":1}}}""",
                    """{"findings":[{"category":"cloud","severity":"medium","line":3,"message":"Remote finding"}]}""",
                ),
            ),
        )
        val client = CloudCheckClient(transport, pollLimit = 2)
        val settings = CloudSettings(apiKey = "ks_test", enableCloudChecks = true, workspaceTrusted = true)

        val findings = client.submitAudit(listOf(CloudCheckFile("src/App.kt", "code")), settings)

        assertEquals(
            listOf("/api/audit/runs?id=run-1", "/api/audit/findings?run_id=run-1&limit=100"),
            transport.gets.map { it.path },
        )
        assertEquals("cloud", findings.single().rule)
        assertEquals("Remote finding", findings.single().message)
    }

    @Test
    fun reportsCodeDiscoveryManifestsToDiscoveryEndpoint() = kotlinx.coroutines.test.runTest {
        val transport = RecordingTransport(postResponse = """{"discovery_id":"disc-1","suggestion":"register"}""")
        val client = CloudCheckClient(transport)
        val settings = CloudSettings(apiKey = "ks_test", enableCloudChecks = true, workspaceTrusted = true)

        val outcome = client.reportCodeDiscovery(
            files = listOf(CloudCheckFile("package.json", """{"dependencies":{"openai":"^4"}}""")),
            settings = settings,
            workspaceId = "risk-service",
            language = "typescript",
        )

        assertEquals("/api/v2/discovery/code", transport.posts.single().path)
        assertEquals("risk-service", transport.posts.single().payload["workspace_id"])
        assertEquals("workspace_scan", transport.posts.single().payload["event_type"])
        assertEquals("disc-1", outcome?.discoveryId)
        assertEquals("register", outcome?.suggestion)
    }

    @Test
    fun skipsCodeDiscoveryWhenEmptyOrUntrusted() = kotlinx.coroutines.test.runTest {
        val transport = RecordingTransport(postResponse = """{"discovery_id":"disc-1"}""")
        val client = CloudCheckClient(transport)
        val trusted = CloudSettings(apiKey = "ks_test", enableCloudChecks = true, workspaceTrusted = true)
        val untrusted = CloudSettings(apiKey = "ks_test", enableCloudChecks = true, workspaceTrusted = false)

        assertEquals(null, client.reportCodeDiscovery(emptyList(), trusted, "ws"))
        assertEquals(null, client.reportCodeDiscovery(listOf(CloudCheckFile("package.json", "{}")), untrusted, "ws"))
        assertTrue(transport.posts.isEmpty())
    }
}

private data class RequestRecord(
    val path: String,
    val apiKey: String,
    val payload: Map<String, Any?> = emptyMap(),
)

private class RecordingTransport(
    private val postResponse: String? = null,
    private val getResponses: ArrayDeque<String> = ArrayDeque(),
) : CloudTransport {
    val posts = mutableListOf<RequestRecord>()
    val gets = mutableListOf<RequestRecord>()

    override suspend fun postJson(path: String, apiKey: String, payload: Map<String, Any?>): String? {
        posts += RequestRecord(path, apiKey, payload)
        return postResponse
    }

    override suspend fun getJson(path: String, apiKey: String): String? {
        gets += RequestRecord(path, apiKey)
        return getResponses.removeFirstOrNull()
    }
}
