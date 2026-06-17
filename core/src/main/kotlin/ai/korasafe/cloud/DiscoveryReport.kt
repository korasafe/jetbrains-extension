package ai.korasafe.cloud

/**
 * Pure helpers for the Shadow AI code-discovery report (POST /api/v2/discovery/code).
 *
 * After a workspace scan the plugin sends dependency manifests (package.json,
 * requirements*.txt) to the discovery endpoint, which parses them server-side and
 * upserts one workspace-scoped shadow_ai_discoveries row. Keeping these helpers
 * pure and IDE-free makes them unit-testable without the platform.
 */

// The server parser only understands these two manifest shapes today.
private val CODE_DISCOVERY_MANIFEST =
    Regex("(^|/)(package\\.json|requirements([-.\\w]*)?\\.txt)$", RegexOption.IGNORE_CASE)

// Mirrors the server workspace_id contract: ^[a-zA-Z0-9._:/-]{1,200}$
private val WORKSPACE_ID_DISALLOWED = Regex("[^a-zA-Z0-9._:/-]+")
private const val WORKSPACE_ID_MAX = 200

private fun toPosix(path: String): String = path.replace('\\', '/')

/** True when the path is a dependency manifest the discovery endpoint can parse. */
fun isCodeDiscoveryManifest(relativePath: String): Boolean =
    CODE_DISCOVERY_MANIFEST.containsMatchIn(toPosix(relativePath))

/**
 * Sanitize a project name into a stable id satisfying the server's workspace_id
 * pattern. Stable per project so repeated scans upsert one row.
 */
fun sanitizeWorkspaceId(name: String): String {
    val cleaned = toPosix(name)
        .trim()
        .replace(WORKSPACE_ID_DISALLOWED, "-")
        .trim('-')
        .take(WORKSPACE_ID_MAX)
    return cleaned.ifEmpty { "workspace" }
}

/**
 * Coarse language tag for the manifests found. The server parses both manifest
 * types regardless of this tag; it is metadata only.
 */
fun detectDiscoveryLanguage(manifestPaths: List<String>): String {
    val paths = manifestPaths.map { toPosix(it).lowercase() }
    return when {
        paths.any { Regex("(^|/)package\\.json$").containsMatchIn(it) } -> "typescript"
        paths.any { Regex("requirements([-.\\w]*)?\\.txt$").containsMatchIn(it) } -> "python"
        else -> "other"
    }
}
