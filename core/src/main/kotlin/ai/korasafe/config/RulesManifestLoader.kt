package ai.korasafe.config

import ai.korasafe.analyzers.RuleManifest
import ai.korasafe.analyzers.RuleManifestRule
import ai.korasafe.analyzers.Severity
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import java.security.MessageDigest

data class RuleManifestDiff(
    val added: List<RuleManifestRule>,
    val removed: List<RuleManifestRule>,
    val changed: List<RuleManifestChange>,
)

data class RuleManifestChange(
    val before: RuleManifestRule,
    val after: RuleManifestRule,
)

data class CachedRulesManifest(
    val sha: String,
    val manifest: RuleManifest,
    val fetchedAt: String,
)

fun parseRulesManifest(raw: String, mapper: ObjectMapper = jacksonObjectMapper()): RuleManifest {
    val root = mapper.readTree(raw)
    val rulesNode = root.get("rules")
    require(rulesNode != null && rulesNode.isArray) { "rules manifest must include a rules array" }

    val rules = rulesNode
        .filter { it.get("id")?.isTextual == true && it.get("id").asText().isNotBlank() }
        .map { node ->
            RuleManifestRule(
                id = node.get("id").asText(),
                title = node.textOrNull("title"),
                enabled = node.get("enabled")?.asBoolean() ?: true,
                severity = node.textOrNull("severity")?.let(::parseSeverityOrThrow),
                category = node.textOrNull("category"),
                message = node.textOrNull("message"),
                remediation = node.textOrNull("remediation"),
                regex = node.textOrNull("regex"),
                regulationRefs = node.stringList("regulationRefs"),
                regulationRefJurisdictions = node.stringList("regulationRefJurisdictions"),
                jurisdictions = node.stringList("jurisdictions"),
            )
        }

    val manifest = RuleManifest(
        version = root.textOrNull("version")?.takeIf { it.isNotBlank() } ?: "unversioned",
        sha = root.textOrNull("sha")?.takeIf { it.isNotBlank() },
        rules = rules,
    )
    val sha = manifest.sha ?: manifestSha(manifest)
    return manifest.copy(sha = sha).also(::validateRulesManifest)
}

fun validateRulesManifest(manifest: RuleManifest) {
    require(manifest.version.isNotBlank()) { "rules manifest version must be non-empty" }
    require(!manifest.sha.isNullOrBlank()) { "rules manifest sha must be non-empty" }
    manifest.rules.forEach { rule ->
        require(rule.id.isNotBlank()) { "rules manifest rule id must be non-empty" }
    }
}

fun manifestSha(manifest: RuleManifest): String {
    val canonical = canonicalJson(
        mapOf(
            "rules" to manifest.rules,
            "sha" to "",
            "version" to manifest.version,
        ),
    )
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

fun diffRulesManifest(before: RuleManifest, after: RuleManifest): RuleManifestDiff {
    val beforeById = before.rules.associateBy { it.id }
    val afterById = after.rules.associateBy { it.id }
    val added = after.rules
        .filter { it.id !in beforeById }
        .sortedBy { it.id }
    val removed = before.rules
        .filter { it.id !in afterById }
        .sortedBy { it.id }
    val changed = after.rules
        .mapNotNull { afterRule ->
            val beforeRule = beforeById[afterRule.id] ?: return@mapNotNull null
            if (canonicalJson(beforeRule) == canonicalJson(afterRule)) null else RuleManifestChange(beforeRule, afterRule)
        }
        .sortedBy { it.after.id }

    return RuleManifestDiff(added = added, removed = removed, changed = changed)
}

fun isEmptyDiff(diff: RuleManifestDiff): Boolean =
    diff.added.isEmpty() && diff.removed.isEmpty() && diff.changed.isEmpty()

fun formatRuleDiffSummary(diff: RuleManifestDiff): String =
    "Rules updated: +${diff.added.size} new, ~${diff.changed.size} changed, -${diff.removed.size} removed."

class RulesManifestFetcher(
    private val client: HttpClient,
    private val userAgent: String = "KoraSafe-JetBrains/0.1.0",
) {
    suspend fun fetch(url: String): RuleManifest? {
        val response = client.get(url) {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, userAgent)
        }
        if (response.status.value !in 200..299) return null
        return parseRulesManifest(response.body())
    }
}

class RulesManifestCache(private var cached: CachedRulesManifest? = null) {
    fun current(): CachedRulesManifest? = cached

    fun update(manifest: RuleManifest, fetchedAt: String): RuleManifestDiff? {
        val sha = manifestSha(manifest)
        val previous = cached
        cached = CachedRulesManifest(sha = sha, manifest = manifest.copy(sha = sha), fetchedAt = fetchedAt)
        return previous?.let { diffRulesManifest(it.manifest, cached!!.manifest) }
    }
}

private fun JsonNode.textOrNull(field: String): String? =
    get(field)?.takeIf { it.isTextual }?.asText()

private fun JsonNode.stringList(field: String): List<String> =
    get(field)
        ?.takeIf { it.isArray }
        ?.mapNotNull { if (it.isTextual) it.asText() else null }
        ?: emptyList()

private fun parseSeverity(value: String): Severity? =
    Severity.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }

private fun parseSeverityOrThrow(value: String): Severity =
    parseSeverity(value) ?: throw IllegalArgumentException("invalid rules manifest severity: $value")

private fun canonicalJson(value: Any?): String = when (value) {
    null -> "null"
    is String -> jacksonObjectMapper().writeValueAsString(value)
    is Number, is Boolean -> value.toString()
    is Severity -> jacksonObjectMapper().writeValueAsString(value.name.lowercase())
    is RuleManifestRule -> canonicalJson(
        mapOf(
            "category" to value.category,
            "enabled" to value.enabled,
            "id" to value.id,
            "jurisdictions" to value.jurisdictions,
            "message" to value.message,
            "regex" to value.regex,
            "regulationRefJurisdictions" to value.regulationRefJurisdictions,
            "regulationRefs" to value.regulationRefs,
            "remediation" to value.remediation,
            "severity" to value.severity,
            "title" to value.title,
        ).filterValues { it != null && it != emptyList<String>() },
    )
    is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { canonicalJson(it) }
    is Map<*, *> -> value.entries
        .sortedBy { it.key.toString() }
        .joinToString(prefix = "{", postfix = "}") { entry ->
            "${canonicalJson(entry.key.toString())}:${canonicalJson(entry.value)}"
        }
    else -> jacksonObjectMapper().writeValueAsString(value)
}
