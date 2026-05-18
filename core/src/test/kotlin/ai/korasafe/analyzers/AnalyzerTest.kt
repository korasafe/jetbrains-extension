package ai.korasafe.analyzers

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyzerTest {
    @Test
    fun findsHardcodedSecretsInSkeletonPort() {
        val result = Analyzer().analyzeCode(
            code = """const key = "sk-ant-abcdefghijklmnop";""",
            languageId = "typescript",
        )

        assertEquals(1, result.summary.critical)
        assertEquals("hardcoded-secret", result.findings.single().rule)
    }
}
