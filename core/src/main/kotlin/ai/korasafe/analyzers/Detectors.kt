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
    private val patterns = listOf(
        Regex("""\b\d{3}-\d{2}-\d{4}\b"""),
        Regex("""\b[\w.%+-]+@[\w.-]+\.[A-Za-z]{2,}\b"""),
        Regex("""(?i)(patient|customer|applicant).{0,40}(ssn|dob|date of birth|email|phone)"""),
    )

    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        patterns.flatMap { pattern ->
            pattern.findAll(code).map { match ->
                finding(
                    severity = Severity.High,
                    line = lineFor(code, match.range.first),
                    message = "Potential personal data is embedded in an AI prompt or test fixture",
                    evidence = match.value,
                    category = "privacy",
                    remediation = "Redact or tokenize personal data before it reaches an LLM prompt, log, or fixture.",
                    regulationRefs = listOf("GDPR Art. 5(1)(c)", "EU AI Act Art. 10"),
                )
            }
        }
}

class MissingErrorHandlingDetector : PatternDetector("missing-error-handling") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        Regex("""(?i)(openai|anthropic|cohere|langchain|llm)\.""")
            .findAll(code)
            .filter { match ->
                val window = code.substring(match.range.first.coerceAtLeast(0), (match.range.last + 300).coerceAtMost(code.length))
                !window.contains("catch", ignoreCase = true) && !window.contains("try", ignoreCase = true)
            }
            .map { match ->
                finding(
                    severity = Severity.Medium,
                    line = lineFor(code, match.range.first),
                    message = "LLM call has no nearby error handling",
                    evidence = match.value,
                    category = "resilience",
                    remediation = "Wrap model calls with retry, timeout, and failure handling before shipping.",
                    regulationRefs = listOf("NIST AI RMF MEASURE-2.7"),
                )
            }
            .toList()
}

class MissingHitlGateDetector : PatternDetector("missing-hitl-gate") {
    private val actionPattern = Regex("""(?i)(transfer|payment|delete|approve|deny|submit|sendEmail|executeAction)\s*\(""")

    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        actionPattern.findAll(code)
            .filter { match ->
                val windowStart = (match.range.first - 500).coerceAtLeast(0)
                val windowEnd = (match.range.last + 500).coerceAtMost(code.length)
                val window = code.substring(windowStart, windowEnd)
                !window.contains("human", ignoreCase = true) &&
                    !window.contains("approval", ignoreCase = true) &&
                    !window.contains("checkpoint", ignoreCase = true)
            }
            .map { match ->
                finding(
                    severity = Severity.High,
                    line = lineFor(code, match.range.first),
                    message = "High-impact agent action has no visible human approval gate",
                    evidence = match.value,
                    category = "human-oversight",
                    remediation = "Add a human approval checkpoint before destructive, financial, or external actions execute.",
                    regulationRefs = listOf("EU AI Act Art. 14", "NIST AI RMF GOVERN-1.2"),
                )
            }
            .toList()
}

class AutonomyTierDetector : PatternDetector("autonomy-tier-mismatch") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        emptyList()
}

class UnsafeEvalDetector : PatternDetector("unsafe-eval") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        Regex("""(?i)\b(eval|exec|Function)\s*\(""")
            .findAll(code)
            .map { match ->
                finding(
                    severity = Severity.High,
                    line = lineFor(code, match.range.first),
                    message = "Dynamic code execution detected",
                    evidence = match.value,
                    category = "runtime-safety",
                    remediation = "Replace dynamic execution with a bounded parser, allowlist, or policy-checked command adapter.",
                    regulationRefs = listOf("OWASP A03:2021", "NIST AI RMF MAP-4.1"),
                )
            }
            .toList()
}

class MissingRateLimitDetector : PatternDetector("missing-rate-limit") {
    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        Regex("""(?i)(fetch|axios|openai|anthropic|cohere)\.""")
            .findAll(code)
            .filter { match ->
                val windowStart = (match.range.first - 400).coerceAtLeast(0)
                val windowEnd = (match.range.last + 400).coerceAtMost(code.length)
                val window = code.substring(windowStart, windowEnd)
                !window.contains("rate", ignoreCase = true) &&
                    !window.contains("throttle", ignoreCase = true) &&
                    !window.contains("limit", ignoreCase = true)
            }
            .map { match ->
                finding(
                    severity = Severity.Medium,
                    line = lineFor(code, match.range.first),
                    message = "External or model call has no nearby rate-limit guard",
                    evidence = match.value,
                    category = "supply-chain",
                    remediation = "Add throttling, budget limits, or queue backpressure around external model/tool calls.",
                    regulationRefs = listOf("NIST AI RMF MANAGE-4.1"),
                )
            }
            .toList()
}

class UnpinnedAiDependencyDetector : PatternDetector("c14-unpinned-ai-dependency") {
    private val dependencyPattern = Regex("""(?i)(openai|anthropic|langchain|cohere|huggingface)[^:\n]*:\s*["']?(\*|latest|next|main|master)["']?""")

    override fun detect(code: String, languageId: String, policy: PolicyContext, manifest: RuleManifest): List<Finding> =
        dependencyPattern.findAll(code)
            .map { match ->
                finding(
                    severity = Severity.High,
                    line = lineFor(code, match.range.first),
                    message = "AI supply-chain dependency is not pinned to an auditable version",
                    evidence = match.value,
                    category = "ai-supply-chain",
                    remediation = "Pin AI SDK/model dependencies to a reviewed version and capture provenance before release.",
                    regulationRefs = listOf("C14 AI Supply Chain", "NIST AI RMF MAP-3.5"),
                )
            }
            .toList()
}
