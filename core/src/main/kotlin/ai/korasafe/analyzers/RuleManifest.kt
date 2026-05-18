package ai.korasafe.analyzers

data class RuleManifestRule(
    val id: String,
    val title: String? = null,
    val enabled: Boolean = true,
    val severity: Severity? = null,
    val category: String? = null,
    val message: String? = null,
    val remediation: String? = null,
    val regex: String? = null,
    val regulationRefs: List<String> = emptyList(),
    val regulationRefJurisdictions: List<String> = emptyList(),
    val jurisdictions: List<String> = emptyList(),
)

data class RuleManifest(
    val version: String = "bundled-local-rules",
    val sha: String? = null,
    val rules: List<RuleManifestRule> = emptyList(),
)
