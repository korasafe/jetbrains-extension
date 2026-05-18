package ai.korasafe.analyzers

enum class PolicySource {
    Missing,
    Loaded,
    Invalid,
}

data class PolicyContext(
    val autonomyTier: Int? = null,
    val allowedProviders: List<String> = emptyList(),
    val approvalKeywords: List<String> = emptyList(),
    val rateLimitSlaMs: Int? = null,
    val disabledRules: List<String> = emptyList(),
    val jurisdiction: String? = null,
    val source: PolicySource = PolicySource.Missing,
    val error: String? = null,
)
