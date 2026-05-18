package ai.korasafe.analyzers

enum class Severity {
    Critical,
    High,
    Medium,
    Low,
}

data class Finding(
    val rule: String,
    val severity: Severity,
    val line: Int,
    val column: Int? = null,
    val message: String,
    val evidence: String,
    val remediation: String,
    val category: String,
    val regulationRefs: List<String> = emptyList(),
)
