package ai.korasafe.analyzers

class Analyzer(
    private val detectors: List<RuleDetector> = defaultDetectors(),
) {
    fun analyzeCode(
        code: String,
        languageId: String,
        policy: PolicyContext = PolicyContext(),
        manifest: RuleManifest = RuleManifest(),
    ): AnalysisResult {
        val disabledRules = policy.disabledRules.toSet()
        val enabledManifestRules = manifest.rules
            .filter { it.enabled }
            .map { it.id }
            .toSet()

        val findings = detectors
            .filter { detector -> detector.id !in disabledRules }
            .filter { detector -> enabledManifestRules.isEmpty() || detector.id in enabledManifestRules }
            .flatMap { detector -> detector.detect(code, languageId, policy, manifest) }
            .sortedWith(compareBy<Finding> { severityOrder(it.severity) }.thenBy { it.line })

        return AnalysisResult(findings = findings, summary = summarizeFindings(findings))
    }

    private fun severityOrder(severity: Severity): Int =
        when (severity) {
            Severity.Critical -> 0
            Severity.High -> 1
            Severity.Medium -> 2
            Severity.Low -> 3
        }
}

interface RuleDetector {
    val id: String

    fun detect(
        code: String,
        languageId: String,
        policy: PolicyContext,
        manifest: RuleManifest,
    ): List<Finding>
}

fun defaultDetectors(): List<RuleDetector> =
    listOf(
        HardcodedSecretDetector(),
        PiiPromptDetector(),
        MissingErrorHandlingDetector(),
        MissingHitlGateDetector(),
        AutonomyTierDetector(),
        UnsafeEvalDetector(),
        MissingRateLimitDetector(),
    )
