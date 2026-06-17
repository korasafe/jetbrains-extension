package ai.korasafe.cloud

import ai.korasafe.analyzers.Finding
import ai.korasafe.analyzers.Severity
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent

data class CloudCheckFile(
    val path: String,
    val content: String,
)

data class CodeDiscoveryOutcome(
    val discoveryId: String?,
    val suggestion: String?,
)

interface CloudTransport {
    suspend fun postJson(path: String, apiKey: String, payload: Map<String, Any?>): String?
    suspend fun getJson(path: String, apiKey: String): String?
}

class KtorCloudTransport(
    private val client: HttpClient,
    private val apiUrl: String,
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    private val userAgent: String = "KoraSafe-JetBrains/0.1.0",
) : CloudTransport {
    override suspend fun postJson(path: String, apiKey: String, payload: Map<String, Any?>): String? {
        val response = client.post(apiUrl.trimEnd('/') + path) {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header(HttpHeaders.UserAgent, userAgent)
            setBody(TextContent(mapper.writeValueAsString(payload), ContentType.Application.Json))
        }
        if (response.status.value !in 200..299) return null
        return response.body()
    }

    override suspend fun getJson(path: String, apiKey: String): String? {
        val response = client.get(apiUrl.trimEnd('/') + path) {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header(HttpHeaders.UserAgent, userAgent)
        }
        if (response.status.value !in 200..299) return null
        return response.body()
    }
}

class CloudCheckClient(
    private val transport: CloudTransport,
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    private val pollLimit: Int = 30,
) {
    suspend fun submitAudit(
        files: List<CloudCheckFile>,
        settings: CloudSettings,
        trigger: String = "save",
    ): List<Finding> {
        if (!evaluateCloudCheckTrust(settings).allowed) return emptyList()

        val payload = mapOf(
            "files" to files.map { mapOf("path" to it.path, "content" to it.content) },
            "surface" to "jetbrains",
            "trigger" to trigger,
        )
        val submitted = transport.postJson("/api/v2/heuristics/run", settings.apiKey, payload) ?: return emptyList()
        val root = mapper.readTree(submitted)
        val immediateFindings = root.findings()
        if (immediateFindings.isNotEmpty()) return immediateFindings.map(::toFinding)

        val runId = root.textOrNull("run_id") ?: return emptyList()
        val completed = pollRun(runId, settings.apiKey) ?: return emptyList()
        if (completed != "completed") return emptyList()

        val findingsResponse = transport.getJson("/api/audit/findings?run_id=$runId&limit=100", settings.apiKey)
            ?: return emptyList()
        return mapper.readTree(findingsResponse).findings().map(::toFinding)
    }

    /**
     * Report dependency manifests to the Shadow AI code-discovery endpoint.
     * Best-effort: returns null (never throws) when empty, untrusted, or on a
     * transport error, so a discovery failure can't break a save/scan.
     */
    suspend fun reportCodeDiscovery(
        files: List<CloudCheckFile>,
        settings: CloudSettings,
        workspaceId: String,
        language: String = "other",
        extensionVersion: String? = null,
    ): CodeDiscoveryOutcome? {
        if (files.isEmpty()) return null
        if (!evaluateCloudCheckTrust(settings).allowed) return null

        val payload = mapOf(
            "workspace_id" to workspaceId,
            "event_type" to "workspace_scan",
            "extension_version" to extensionVersion,
            "language" to language,
            "dependencies" to emptyList<Any?>(),
            "detected_models" to emptyList<Any?>(),
            "files" to files.map { mapOf("path" to it.path, "content" to it.content) },
        )
        val response = transport.postJson("/api/v2/discovery/code", settings.apiKey, payload) ?: return null
        val root = mapper.readTree(response)
        return CodeDiscoveryOutcome(
            discoveryId = root.textOrNull("discovery_id"),
            suggestion = root.textOrNull("suggestion"),
        )
    }

    private suspend fun pollRun(runId: String, apiKey: String): String? {
        repeat(pollLimit) {
            val response = transport.getJson("/api/audit/runs?id=$runId", apiKey) ?: return null
            val status = mapper.readTree(response).path("run").textOrNull("status")
            if (status == "completed" || status == "failed") return status
        }
        return null
    }

    private fun JsonNode.findings(): List<JsonNode> =
        get("findings")?.takeIf { it.isArray }?.toList() ?: emptyList()

    private fun toFinding(node: JsonNode): Finding =
        Finding(
            rule = node.textOrNull("rule") ?: node.textOrNull("category") ?: "cloud-finding",
            severity = parseSeverity(node.textOrNull("severity")) ?: Severity.Medium,
            line = node.intOrNull("line_number") ?: node.intOrNull("line") ?: 1,
            message = node.textOrNull("description") ?: node.textOrNull("message") ?: "KoraSafe cloud finding",
            evidence = "",
            remediation = node.textOrNull("remediation") ?: "",
            category = node.textOrNull("category") ?: node.textOrNull("rule") ?: "cloud",
            regulationRefs = node.stringList("regulation_refs"),
        )
}

private fun JsonNode.textOrNull(field: String): String? =
    get(field)?.takeIf { it.isTextual }?.asText()

private fun JsonNode.intOrNull(field: String): Int? =
    get(field)?.takeIf { it.isInt }?.asInt()

private fun JsonNode.stringList(field: String): List<String> =
    get(field)
        ?.takeIf { it.isArray }
        ?.mapNotNull { if (it.isTextual) it.asText() else null }
        ?: emptyList()

private fun parseSeverity(value: String?): Severity? =
    value?.let { raw -> Severity.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
