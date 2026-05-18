package ai.korasafe.config

import ai.korasafe.analyzers.PolicyContext
import ai.korasafe.analyzers.PolicySource
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

private val defaultPolicy = PolicyContext(
    allowedProviders = emptyList(),
    approvalKeywords = emptyList(),
    disabledRules = emptyList(),
    source = PolicySource.Missing,
)

class PolicyLoader(private val yaml: Yaml = Yaml()) {
    fun defaultPolicy(): PolicyContext = defaultPolicy.copy()

    fun readWorkspacePolicy(workspaceRoot: Path): PolicyContext =
        readPolicyFile(workspaceRoot.resolve(".korasafe").resolve("policy.yaml"))

    fun readPolicyFile(path: Path): PolicyContext = try {
        parsePolicy(Files.readString(path))
    } catch (_: NoSuchFileException) {
        defaultPolicy()
    } catch (error: Exception) {
        invalid(error.message ?: error.toString())
    }

    fun parsePolicy(raw: String): PolicyContext {
        val loaded = yaml.load<Any?>(raw) ?: emptyMap<String, Any?>()
        if (loaded !is Map<*, *>) return invalid("policy.yaml must be an object")

        val errors = validate(loaded)
        if (errors.isNotEmpty()) return invalid(errors.joinToString("; "))

        return PolicyContext(
            autonomyTier = loaded["autonomyTier"] as? Int,
            allowedProviders = loaded.stringList("allowedProviders"),
            approvalKeywords = loaded.stringList("approvalKeywords"),
            rateLimitSlaMs = loaded["rateLimitSlaMs"] as? Int,
            disabledRules = loaded.stringList("disabledRules"),
            jurisdiction = loaded["jurisdiction"] as? String,
            source = PolicySource.Loaded,
        )
    }

    private fun validate(value: Map<*, *>): List<String> {
        val errors = mutableListOf<String>()
        val allowedKeys = setOf(
            "autonomyTier",
            "allowedProviders",
            "approvalKeywords",
            "rateLimitSlaMs",
            "disabledRules",
            "jurisdiction",
        )
        val unknownKeys = value.keys.filterIsInstance<String>().filter { it !in allowedKeys }
        if (unknownKeys.isNotEmpty()) errors += "unknown policy keys: ${unknownKeys.joinToString(", ")}"

        value["autonomyTier"]?.let {
            if (it !is Int || it !in 1..4) errors += "autonomyTier must be an integer from 1 to 4"
        }
        value["rateLimitSlaMs"]?.let {
            if (it !is Int || it < 1) errors += "rateLimitSlaMs must be a positive integer"
        }
        for (field in listOf("allowedProviders", "approvalKeywords", "disabledRules")) {
            value[field]?.let {
                if (it !is List<*> || it.any { item -> item !is String || item.isBlank() }) {
                    errors += "$field must be an array of non-empty strings"
                }
            }
        }
        value["jurisdiction"]?.let {
            if (it !is String || it !in setOf("global", "eu", "us", "uk", "ca")) {
                errors += "jurisdiction must be one of global, eu, us, uk, ca"
            }
        }
        return errors
    }

    private fun invalid(message: String): PolicyContext =
        defaultPolicy.copy(source = PolicySource.Invalid, error = message)
}

private fun Map<*, *>.stringList(key: String): List<String> =
    (this[key] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
