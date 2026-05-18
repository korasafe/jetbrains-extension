package ai.korasafe.analyzers

data class AnalysisSummary(
    val total: Int,
    val critical: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
)

data class AnalysisResult(
    val findings: List<Finding>,
    val summary: AnalysisSummary,
)

fun summarizeFindings(findings: List<Finding>): AnalysisSummary =
    AnalysisSummary(
        total = findings.size,
        critical = findings.count { it.severity == Severity.Critical },
        high = findings.count { it.severity == Severity.High },
        medium = findings.count { it.severity == Severity.Medium },
        low = findings.count { it.severity == Severity.Low },
    )
