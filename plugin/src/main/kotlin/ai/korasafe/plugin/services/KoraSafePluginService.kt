package ai.korasafe.plugin.services

import ai.korasafe.analyzers.Analyzer
import ai.korasafe.analyzers.AnalysisResult
import ai.korasafe.analyzers.Finding
import ai.korasafe.analyzers.RuleManifest
import ai.korasafe.cloud.CloudCheckFile
import ai.korasafe.cloud.CodeDiscoveryOutcome
import ai.korasafe.cloud.CloudSettings
import ai.korasafe.cloud.defaultCloudCheckClient
import ai.korasafe.cloud.detectDiscoveryLanguage
import ai.korasafe.cloud.isCodeDiscoveryManifest
import ai.korasafe.cloud.reportCodeDiscoveryBlocking
import ai.korasafe.cloud.sanitizeWorkspaceId
import ai.korasafe.config.RulesManifestCache
import com.intellij.openapi.application.ApplicationManager
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

data class KoraSafeCodeDiscoveryReport(
    val files: List<CloudCheckFile>,
    val workspaceId: String,
    val language: String,
)

@Service(Service.Level.PROJECT)
class KoraSafePluginService(private val project: Project) {
    private val analyzer = Analyzer()
    private val manifestCache = RulesManifestCache()
    private var lastSummary: KoraSafeWorkspaceSummary? = null
    private var mcpConnected = false
    private var lastTrace: KoraSafeTraceExport? = null
    private var codeDiscoveryReporter: (
        files: List<CloudCheckFile>,
        settings: CloudSettings,
        workspaceId: String,
        language: String,
    ) -> CodeDiscoveryOutcome? = { files, settings, workspaceId, language ->
        defaultCloudCheckClient(settings).reportCodeDiscoveryBlocking(
            files = files,
            settings = settings,
            workspaceId = workspaceId,
            language = language,
        )
    }

    fun analyzeText(text: String, languageId: String): AnalysisResult =
        analyzer.analyzeCode(text, languageId, manifest = activeManifest())

    fun scanWorkspace(files: Sequence<VirtualFile>): KoraSafeWorkspaceSummary {
        val findings = mutableListOf<Finding>()
        var scanned = 0
        val discoveryFiles = mutableListOf<CloudCheckFile>()
        files
            .filter { !it.isDirectory && it.isValid && it.length <= MAX_SCAN_BYTES }
            .forEach { file ->
                val relativePath = relativePath(file)
                val content = String(file.contentsToByteArray())
                if (isCodeDiscoveryManifest(relativePath)) {
                    discoveryFiles += CloudCheckFile(relativePath, content)
                }
                val language = languageFor(file) ?: return@forEach
                scanned += 1
                findings += analyzeText(content, language).findings
            }
        reportCodeDiscoveryAsync(discoveryReport(discoveryFiles))
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

    fun discoveryReport(files: List<CloudCheckFile>): KoraSafeCodeDiscoveryReport? {
        val manifests = files
            .filter { isCodeDiscoveryManifest(it.path) }
            .take(MAX_DISCOVERY_FILES)
        if (manifests.isEmpty()) return null
        val workspaceId = sanitizeWorkspaceId(project.basePath ?: project.name)
        return KoraSafeCodeDiscoveryReport(
            files = manifests,
            workspaceId = workspaceId,
            language = detectDiscoveryLanguage(manifests.map { it.path }),
        )
    }

    fun setCodeDiscoveryReporterForTests(
        reporter: (
            files: List<CloudCheckFile>,
            settings: CloudSettings,
            workspaceId: String,
            language: String,
        ) -> CodeDiscoveryOutcome?,
    ) {
        codeDiscoveryReporter = reporter
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

    private fun reportCodeDiscoveryAsync(report: KoraSafeCodeDiscoveryReport?) {
        if (report == null) return
        val settings = cloudSettingsFromEnvironment()
        ApplicationManager.getApplication().executeOnPooledThread {
            runCatching {
                codeDiscoveryReporter(report.files, settings, report.workspaceId, report.language)
            }
        }
    }

    private fun relativePath(file: VirtualFile): String {
        val basePath = project.basePath ?: return file.path
        return file.path.removePrefix(basePath).trimStart('/', '\\')
    }

    companion object {
        private const val MAX_SCAN_BYTES = 512_000L
        private const val MAX_DISCOVERY_FILES = 50

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

        fun cloudSettingsFromEnvironment(): CloudSettings =
            CloudSettings(
                apiUrl = setting("korasafe.apiUrl", "KORASAFE_API_URL") ?: CloudSettings().apiUrl,
                apiKey = setting("korasafe.apiKey", "KORASAFE_API_KEY").orEmpty(),
                enableCloudChecks = boolSetting("korasafe.enableCloudChecks", "KORASAFE_ENABLE_CLOUD_CHECKS"),
                workspaceTrusted = boolSetting("korasafe.workspaceTrusted", "KORASAFE_WORKSPACE_TRUSTED", default = true),
            )

        private fun setting(property: String, environment: String): String? =
            System.getProperty(property)
                ?.takeIf(String::isNotBlank)
                ?: System.getenv(environment)?.takeIf(String::isNotBlank)

        private fun boolSetting(property: String, environment: String, default: Boolean = false): Boolean =
            setting(property, environment)
                ?.let { it.equals("true", ignoreCase = true) || it == "1" }
                ?: default
    }
}
