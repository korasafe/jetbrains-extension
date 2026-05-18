package ai.korasafe.analyzers

abstract class PatternDetector(
    override val id: String,
) : RuleDetector {
    protected fun finding(
        severity: Severity,
        line: Int,
        message: String,
        evidence: String,
        category: String,
        remediation: String,
        regulationRefs: List<String> = emptyList(),
    ): Finding =
        Finding(
            rule = id,
            severity = severity,
            line = line,
            message = message,
            evidence = evidence.take(120),
            category = category,
            remediation = remediation,
            regulationRefs = regulationRefs,
        )

    protected fun lineFor(code: String, index: Int): Int =
        code.take(index.coerceAtLeast(0)).count { it == '\n' } + 1
}

class HardcodedSecretDetector : PatternDetector("hardcoded-secret") {
    private val patterns = listOf(
        Regex("""sk-(?:ant|proj|live|test)-[A-Za-z0-9_-]{12,}"""),
        Regex("""(?i)(api[_-]?key|secret)["'\s:=]+[A-Za-z0-9_\-]{16,}"""),
    )

    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        patterns.flatMap { pattern ->
            pattern.findAll(code).map { match ->
                finding(
                    severity = Severity.Critical,
                    line = lineFor(code, match.range.first),
                    message = "Hardcoded secret detected",
                    evidence = match.value,
                    category = "security",
                    remediation = "Move credentials into a secret manager and rotate exposed keys.",
                    regulationRefs = listOf("OWASP A07:2021"),
                )
            }
        }
}

class PiiPromptDetector : PatternDetector("pii-in-prompt") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        emptyList()
}

class MissingErrorHandlingDetector : PatternDetector("missing-error-handling") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        emptyList()
}

class MissingHitlGateDetector : PatternDetector("missing-hitl-gate") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        emptyList()
}

class AutonomyTierDetector : PatternDetector("autonomy-tier-mismatch") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        emptyList()
}

class UnsafeEvalDetector : PatternDetector("unsafe-eval") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        emptyList()
}

class MissingRateLimitDetector : PatternDetector("missing-rate-limit") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        emptyList()
}
