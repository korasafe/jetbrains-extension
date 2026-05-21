package ai.korasafe.plugin.services

import ai.korasafe.analyzers.Analyzer
import ai.korasafe.analyzers.AnalysisResult
import ai.korasafe.analyzers.Finding
import ai.korasafe.analyzers.RuleManifest
import ai.korasafe.cloud.CloudSettings
import ai.korasafe.config.RulesManifestCache
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.time.Instant

data class KoraSafeWorkspaceSummary(
    val filesScanned: Int,
    val findings: List<Finding>,
    val manifestSha: String?,
    val scannedAt: Instant,
)

data class KoraSafeTraceExport(
    val runId: String,
    val spanName: String,
    val startedAt: Instant,
    val attributes: Map<String, String>,
)

@Service(Service.Level.PROJECT)
class KoraSafePluginService(private val project: Project) {
    private val analyzer = Analyzer()
    private val manifestCache = RulesManifestCache()
    private var lastSummary: KoraSafeWorkspaceSummary? = null
    private var mcpConnected = false
    private var lastTrace: KoraSafeTraceExport? = null

    fun analyzeText(text: String, languageId: String): AnalysisResult =
        analyzer.analyzeCode(text, languageId, manifest = activeManifest())

    fun scanWorkspace(files: Sequence<VirtualFile>): KoraSafeWorkspaceSummary {
        val findings = mutableListOf<Finding>()
        var scanned = 0
        files
            .filter { !it.isDirectory && it.isValid && it.length <= MAX_SCAN_BYTES }
            .filter { languageFor(it) != null }
            .forEach { file ->
                scanned += 1
                findings += analyzeText(String(file.contentsToByteArray()), languageFor(file) ?: "text").findings
            }
        return KoraSafeWorkspaceSummary(
            filesScanned = scanned,
            findings = findings,
            manifestSha = activeManifest().sha,
            scannedAt = Instant.now(),
        ).also { lastSummary = it }
    }

    fun generatePrReport(summary: KoraSafeWorkspaceSummary = lastSummary ?: emptySummary()): String =
        buildString {
            appendLine("# KoraSafe PR report")
            appendLine()
            appendLine("- Project: ${project.name}")
            appendLine("- Files scanned: ${summary.filesScanned}")
            appendLine("- Findings: ${summary.findings.size}")
            appendLine("- Rules manifest: ${summary.manifestSha ?: "bundled"}")
            appendLine()
            if (summary.findings.isEmpty()) {
                appendLine("No KoraSafe governance findings detected.")
            } else {
                appendLine("| Severity | Rule | Line | Message |")
                appendLine("|---|---|---:|---|")
                summary.findings.forEach { finding ->
                    appendLine("| ${finding.severity} | ${finding.rule} | ${finding.line} | ${finding.message.replace("|", "\\|")} |")
                }
            }
        }

    fun exportEvidenceBundle(summary: KoraSafeWorkspaceSummary = lastSummary ?: emptySummary()): String =
        buildString {
            appendLine("{")
            appendLine("  \"project\": \"${json(project.name)}\",")
            appendLine("  \"surface\": \"jetbrains\",")
            appendLine("  \"filesScanned\": ${summary.filesScanned},")
            appendLine("  \"manifestSha\": ${summary.manifestSha?.let { "\"${json(it)}\"" } ?: "null"},")
            appendLine("  \"findings\": [")
            summary.findings.forEachIndexed { index, finding ->
                append("    {\"rule\":\"${json(finding.rule)}\",\"severity\":\"${finding.severity}\",\"line\":${finding.line},\"message\":\"${json(finding.message)}\"}")
                appendLine(if (index == summary.findings.lastIndex) "" else ",")
            }
            appendLine("  ]")
            appendLine("}")
        }

    fun traceAgentRun(file: VirtualFile?): KoraSafeTraceExport {
        val trace = KoraSafeTraceExport(
            runId = "jetbrains-${Instant.now().toEpochMilli()}",
            spanName = "korasafe.jetbrains.traceRun",
            startedAt = Instant.now(),
            attributes = mapOf(
                "project" to project.name,
                "file" to (file?.path ?: "workspace"),
                "surface" to "jetbrains",
            ),
        )
        lastTrace = trace
        return trace
    }

    fun connectMcp(settings: CloudSettings): Boolean {
        mcpConnected = settings.workspaceTrusted && (settings.apiKey.isNotBlank() || !settings.enableCloudChecks)
        return mcpConnected
    }

    fun refreshRulesManifest(manifest: RuleManifest, fetchedAt: String): String {
        val diff = manifestCache.update(manifest, fetchedAt)
        return diff?.let { "+${it.added.size} ~${it.changed.size} -${it.removed.size}" } ?: "initial"
    }

    fun sidebarStatus(): String =
        "Project ${project.name}: ${lastSummary?.findings?.size ?: 0} finding(s), MCP ${if (mcpConnected) "connected" else "offline"}, trace ${lastTrace?.runId ?: "none"}"

    private fun activeManifest(): RuleManifest =
        manifestCache.current()?.manifest ?: RuleManifest()

    private fun emptySummary(): KoraSafeWorkspaceSummary =
        KoraSafeWorkspaceSummary(0, emptyList(), activeManifest().sha, Instant.now())

    companion object {
        private const val MAX_SCAN_BYTES = 512_000L

        fun languageFor(file: VirtualFile): String? =
            when (file.extension?.lowercase()) {
                "js", "jsx" -> "javascript"
                "ts", "tsx" -> "typescript"
                "py" -> "python"
                "go" -> "go"
                "rb" -> "ruby"
                "kt", "kts" -> "kotlin"
                else -> null
            }

        private fun json(value: String): String =
            value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }
}
